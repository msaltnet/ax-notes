package net.msalt.axnotes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import net.msalt.axnotes.R
import net.msalt.axnotes.data.Article
import net.msalt.axnotes.data.FeedState

@Composable
internal fun NotesScreen(
    articles: List<Article>, savedIds: Set<String>, selectedId: String?, feed: FeedState?,
    busy: Boolean, scrollState: LazyListState, collectionType: String, collectionId: String?,
    onCollection: (String, String?) -> Unit, onRefresh: () -> Unit, onArticle: (String) -> Unit,
    onSearch: () -> Unit
) {
    NotesRefreshBox(isRefreshing = busy, onRefresh = onRefresh) {
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize().testTag("notes_list"),
            contentPadding = PaddingValues(horizontal = AxComponentTokens.pageMargin, vertical = AxSpacing.xl)) {
            item {
                Text("EXPERIMENTS & IDEAS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(AxSpacing.md))
                Text("실험을 읽는 시간", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(AxSpacing.sm))
                Text("AX 실험과 개발 기록, 나의 메모로 이어 읽기", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(AxSpacing.lg))
                Row(Modifier.fillMaxWidth().heightIn(min = AxSize.minTouch)
                    .clickable(role = Role.Button, onClick = onSearch).testTag("notes_search")
                    .padding(vertical = AxSpacing.md), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AxSpacing.sm)) {
                    Icon(Icons.Default.Search, null, Modifier.size(AxSize.supportingIcon), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("제목, 문장, 메모로 찾아보기", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(AxSpacing.md)) {
                    listOf("전체", "시리즈", "프로젝트").forEach { name ->
                        AxSectionTab(collectionType == name, { onCollection(name, null) }, name, Modifier.testTag("collection_$name"))
                    }
                }
                HorizontalDivider()
                Spacer(Modifier.height(AxSpacing.xxl))
            }
            if(feed?.sample == true) item {
                Warning("이전에 저장한 글을 표시하고 있어요. 아래로 당겨 최신 글을 받아보세요")
                Spacer(Modifier.height(AxSpacing.lg))
            }
            if(collectionType != "전체" && collectionId == null) {
                val groups = articleCollections(articles, collectionType)
                if(groups.isEmpty() && !busy) item { EmptyState("${collectionType} 정보를 아직 받지 못했어요", "공개된 분류 정보가 도착하면 이곳에서 모아볼 수 있어요. 아래로 당겨 다시 확인하세요") }
                items(groups, key = { it.id }) { group -> CollectionCard(group, collectionType) { onCollection(collectionType, group.id) } }
            } else {
                val visibleArticles = articlesInCollection(articles, collectionType, collectionId)
                item {
                    if(collectionId != null) AxTextButton(onClick = { onCollection(collectionType, null) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Spacer(Modifier.width(AxSpacing.sm)); Text("${collectionType} 목록") }
                    Text(collectionId?.let { id -> articleCollections(articles, collectionType).find { it.id == id }?.title } ?: "최신 Notes · ${visibleArticles.size}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(AxSpacing.md))
                    HorizontalDivider()
                }
                if(visibleArticles.isEmpty() && !busy) item { EmptyState("아직 내려받은 글이 없어요", "인터넷에 연결한 뒤 아래로 당겨 글을 받아보세요") }
                itemsIndexed(visibleArticles, key = { _, article -> article.id }) { index, article ->
                    ArticleCard(article, article.id in savedIds, selectedId == article.id, ordinal = index + 1) { onArticle(article.id) }
                }
            }
        }
    }
}

@Composable
private fun CollectionCard(group: ArticleCollection, type: String, onClick: () -> Unit) {
    AxCard(onClick = onClick) {
        Row(Modifier.padding(vertical = AxSpacing.xxl, horizontal = AxSpacing.sm), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AxSpacing.lg)) {
            Icon(painterResource(if(type == "시리즈") R.drawable.ic_series else R.drawable.ic_project), null, Modifier.size(AxSize.supportingIcon), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AxSpacing.sm)) {
                Text(group.title, style = MaterialTheme.typography.titleMedium)
                Text("글 ${group.count}개", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
