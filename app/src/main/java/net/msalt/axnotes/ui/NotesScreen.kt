package net.msalt.axnotes.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import net.msalt.axnotes.R
import net.msalt.axnotes.data.Article
import net.msalt.axnotes.data.FeedState

@Composable
internal fun NotesScreen(
    articles: List<Article>, savedIds: Set<String>, selectedId: String?, feed: FeedState?,
    busy: Boolean, scrollState: LazyListState, collectionType: String, collectionId: String?,
    onCollection: (String, String?) -> Unit, onRefresh: () -> Unit, onArticle: (String) -> Unit
) {
    NotesRefreshBox(isRefreshing = busy, onRefresh = onRefresh) {
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize().testTag("notes_list"), contentPadding = PaddingValues(AxComponentTokens.pageMargin), verticalArrangement = Arrangement.spacedBy(AxSpacing.lg)) {
            item {
                Text("읽고, 기록하고, 다시 꺼내보는 AX", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(AxSpacing.sm))
                Text("AI로 일하는 방식을 바꾸며 배우고 생각한 것들", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AxSpacing.sm)) {
                    listOf("전체", "시리즈", "프로젝트").forEach { name ->
                        AxFilterChip(selected = collectionType == name, onClick = { onCollection(name, null) }, label = { Text(name) }, modifier = Modifier.testTag("collection_$name"))
                    }
                }
            }
            if(feed?.sample == true) item { Warning("이전에 저장한 글을 표시하고 있어요. 아래로 당겨 최신 글을 받아보세요") }
            if(collectionType != "전체" && collectionId == null) {
                val groups = articleCollections(articles, collectionType)
                if(groups.isEmpty() && !busy) item { EmptyState("${collectionType} 정보를 아직 받지 못했어요", "공개된 분류 정보가 도착하면 이곳에서 모아볼 수 있어요. 아래로 당겨 다시 확인하세요") }
                items(groups, key = { it.id }) { group -> CollectionCard(group, collectionType) { onCollection(collectionType, group.id) } }
            } else {
                val visibleArticles = articlesInCollection(articles, collectionType, collectionId)
                item {
                    if(collectionId != null) AxTextButton(onClick = { onCollection(collectionType, null) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Spacer(Modifier.width(AxSpacing.sm)); Text("${collectionType} 목록") }
                    Text(collectionId?.let { id -> articleCollections(articles, collectionType).find { it.id == id }?.title } ?: "최신 Notes · ${visibleArticles.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if(visibleArticles.isEmpty() && !busy) item { EmptyState("아직 내려받은 글이 없어요", "인터넷에 연결한 뒤 아래로 당겨 글을 받아보세요") }
                items(visibleArticles, key = { it.id }) { article -> ArticleCard(article, article.id in savedIds, selectedId == article.id) { onArticle(article.id) } }
            }
        }
    }
}

@Composable
private fun CollectionCard(group: ArticleCollection, type: String, onClick: () -> Unit) {
    AxCard(onClick = onClick) {
        Row(Modifier.padding(AxComponentTokens.cardPadding), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AxSpacing.lg)) {
            Icon(painterResource(if(type == "시리즈") R.drawable.ic_series else R.drawable.ic_project), null, Modifier.size(AxSize.icon), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AxSpacing.xs)) {
                Text(group.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("글 ${group.count}개", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
