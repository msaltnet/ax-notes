@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package net.msalt.axnotes.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.painterResource
import net.msalt.axnotes.R
import net.msalt.axnotes.BuildConfig
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.msalt.axnotes.data.*
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun AxApp(target: MutableStateFlow<String?>, vm: AxViewModel = viewModel()) {
    val dark = isSystemInDarkTheme()
    AxTheme(darkTheme = dark) {
        val scheme = MaterialTheme.colorScheme
        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current
        val scope = rememberCoroutineScope()
        val articles by vm.articles.collectAsStateWithLifecycle()
        val feed by vm.feed.collectAsStateWithLifecycle()
        val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
        val memos by vm.memos.collectAsStateWithLifecycle()
        val reminders by vm.reminders.collectAsStateWithLifecycle()
        val busy by vm.busy.collectAsStateWithLifecycle()
        val notificationEnabled by vm.notificationEnabled.collectAsStateWithLifecycle()
        val query by vm.query.collectAsStateWithLifecycle()
        val searchArticles by vm.searchArticles.collectAsStateWithLifecycle()
        val searchMemos by vm.searchMemos.collectAsStateWithLifecycle()
        val searching by vm.searching.collectAsStateWithLifecycle()
        val incoming by target.collectAsStateWithLifecycle()
        var screen by rememberSaveable { mutableStateOf("notes") }
        var previous by rememberSaveable { mutableStateOf("notes") }
        var selectedId by rememberSaveable { mutableStateOf("") }
        var libraryType by rememberSaveable { mutableStateOf("북마크") }
        var collectionType by rememberSaveable { mutableStateOf("전체") }
        var collectionId by rememberSaveable { mutableStateOf<String?>(null) }
        var showOfflineInfo by rememberSaveable { mutableStateOf(false) }
        var showFontLicense by rememberSaveable { mutableStateOf(false) }
        var editorId by rememberSaveable { mutableStateOf<String?>(null) }
        var editorDraftId by rememberSaveable { mutableStateOf(java.util.UUID.randomUUID().toString()) }
        var editorTitle by rememberSaveable { mutableStateOf("") }
        var editorBody by rememberSaveable { mutableStateOf("") }
        var originalTitle by rememberSaveable { mutableStateOf("") }
        var originalBody by rememberSaveable { mutableStateOf("") }
        var editorReturn by rememberSaveable { mutableStateOf("articleMemos") }
        var saving by remember { mutableStateOf(false) }
        var confirmDiscard by rememberSaveable { mutableStateOf(false) }
        var pendingTarget by rememberSaveable { mutableStateOf<String?>(null) }
        var deleteMemo by remember { mutableStateOf<Memo?>(null) }
        var confirmPersonal by rememberSaveable { mutableStateOf(false) }
        var confirmCache by rememberSaveable { mutableStateOf(false) }
        var reminderDialog by rememberSaveable { mutableStateOf(false) }
        var reminderArticleId by rememberSaveable { mutableStateOf("") }
        var dueAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis() + 3600000) }
        var permissionArticle by rememberSaveable { mutableStateOf<String?>(null) }
        var permissionDue by rememberSaveable { mutableLongStateOf(0) }
        val snackbar = remember { SnackbarHostState() }
        val message by vm.message.collectAsStateWithLifecycle()
        val notesScroll = rememberLazyListState()
        val libraryScroll = rememberLazyListState()
        val searchScroll = rememberLazyListState()
        val memoScroll = rememberLazyListState()
        val settingsScroll = rememberLazyListState()
        val paneState = rememberSaveableStateHolder()
        val detailScreens = setOf("reader", "articleMemos", "editor")
        val listScreens = setOf("notes", "library", "search")
        fun selectCollection(type: String, id: String? = null) {
            collectionType = type
            collectionId = id
            scope.launch { notesScroll.scrollToItem(0) }
        }
        fun hasUnsavedChanges() = screen == "editor" && (editorTitle != originalTitle || editorBody != originalBody)
        fun openArticle(id: String) {
            if (screen in listScreens) previous = screen
            selectedId = id
            screen = "reader"
        }
        fun back() {
            if (saving) return
            if (hasUnsavedChanges()) { pendingTarget = null; confirmDiscard = true }
            else if(screen == "notes" && collectionId != null) selectCollection(collectionType)
            else if(screen == "notes" && collectionType != "전체") selectCollection("전체")
            else screen = when (screen) {
                "editor" -> editorReturn
                "articleMemos" -> "reader"
                "reader" -> previous
                else -> "notes"
            }
        }
        fun beginEdit(memo: Memo?) {
            if (screen in listScreens) previous = screen
            editorDraftId = memo?.id ?: java.util.UUID.randomUUID().toString()
            editorId = memo?.id
            editorTitle = memo?.title.orEmpty()
            editorBody = memo?.body.orEmpty()
            originalTitle = editorTitle
            originalBody = editorBody
            if (memo != null) selectedId = memo.articleId
            editorReturn = if (screen == "editor") editorReturn else screen
            screen = "editor"
        }
        fun acceptTarget(value: String) {
            when {
                value == "library" -> { screen = "library"; libraryType = "읽기 알림" }
                value.startsWith("screen:") -> { screen = value.removePrefix("screen:") }
                value.startsWith("memo:") -> memos.find { it.id == value.removePrefix("memo:") }?.let(::beginEdit)
                value == "newMemo" -> beginEdit(null)
                else -> openArticle(value.removePrefix("article:"))
            }
        }
        fun navigate(value: String) {
            if (saving) return
            if (hasUnsavedChanges()) { pendingTarget = value; confirmDiscard = true }
            else acceptTarget(value)
        }
        fun edit(memo: Memo?) = navigate(memo?.let { "memo:${it.id}" } ?: "newMemo")
        fun requestDelete(memo: Memo) {
            if (saving) return
            if (screen == "editor" && editorId == memo.id) {
                vm.message.value = "편집 중인 메모입니다. 먼저 저장하거나 편집을 마치세요"
            } else deleteMemo = memo
        }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> permissionArticle?.let { vm.schedule(it, permissionDue) }; permissionArticle = null }
        LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.message.value = null } }
        LaunchedEffect(incoming, saving) { if(saving) return@LaunchedEffect; incoming?.let { if(hasUnsavedChanges()) { pendingTarget = it; confirmDiscard = true } else acceptTarget(it); target.value = null } }
        LaunchedEffect(screen, selectedId) { if(screen == "reader" && selectedId.isNotBlank()) vm.load(selectedId) }
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event -> if(event == Lifecycle.Event.ON_RESUME) vm.onResume() }
            lifecycle.lifecycle.addObserver(observer); vm.onResume()
            onDispose { lifecycle.lifecycle.removeObserver(observer) }
        }
        BackHandler(screen != "notes" || collectionId != null || collectionType != "전체") { back() }
        val renderScreen: @Composable (String) -> Unit = { paneScreen ->
            paneState.SaveableStateProvider(paneScreen) {
                when(paneScreen) {
                    "notes" -> NotesScreen(
                        articles = articles, savedIds = bookmarks.map { it.articleId }.toSet(),
                        selectedId = selectedId.takeIf { screen in detailScreens },
                        feed = feed, busy = busy, scrollState = notesScroll,
                        collectionType = collectionType, collectionId = collectionId,
                        onCollection = ::selectCollection, onRefresh = vm::refresh,
                        onArticle = { navigate("article:$it") }, onSearch = { navigate("screen:search") }
                    )
                    "library" -> LazyColumn(state = libraryScroll, modifier = Modifier.fillMaxSize().testTag("library_list"), contentPadding = PaddingValues(AxComponentTokens.pageMargin), verticalArrangement = Arrangement.spacedBy(AxSpacing.md)) {
                        item { Text("나만의 읽기 공간", style = MaterialTheme.typography.headlineMedium); Text("이 기기에만 저장됩니다", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(AxSpacing.md)); Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(AxSpacing.sm)) { listOf("북마크", "메모", "읽기 알림").forEach { name -> AxSectionTab(selected = libraryType == name, onClick = { libraryType = name }, label = name) } } }
                        when(libraryType) {
                            "북마크" -> { if(bookmarks.isEmpty()) item { EmptyState("저장한 글이 없어요", "글의 북마크 버튼으로 다시 읽을 글을 모으세요") }; items(bookmarks, key = { it.articleId }) { saved -> SavedCard(saved.titleSnapshot, if(articles.none { it.id == saved.articleId }) "목록에 없는 글 · 저장 당시 제목" else "북마크", { navigate("article:${saved.articleId}") }, isSelected = screen in detailScreens && selectedId == saved.articleId) { AxTextButton(onClick = { vm.toggleBookmark(saved.articleId) }) { Text("해제") } } } }
                            "메모" -> { if(memos.isEmpty()) item { EmptyState("남긴 메모가 없어요", "글을 열고 메모를 남겨보세요") }; items(memos, key = { it.id }) { memo -> MemoCard(memo, { edit(memo) }, { navigate("article:${memo.articleId}") }, { requestDelete(memo) }) } }
                            else -> {
                                if(!notificationEnabled) item { Warning("알림이 차단되어 있어요. 일정은 남아 있지만 전달되지 않습니다"); AxTextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("알림 설정 열기") } }
                                item { Text("예약 시각 이후 실행합니다. 절전·강제 종료 등으로 지연되거나 전달되지 않을 수 있어요", style = MaterialTheme.typography.bodySmall) }
                                if(reminders.isEmpty()) item { EmptyState("읽기 일정이 없어요", "글에서 나중에 읽기 알림을 예약하세요") }
                                items(reminders, key = { it.id }) { reminder -> SavedCard(reminder.titleSnapshot, "${formatTime(reminder.dueAt)} · ${reminderLabel(reminder.state)}", { navigate("article:${reminder.articleId}") }, isSelected = screen in detailScreens && selectedId == reminder.articleId) { Row { AxTextButton(onClick = { reminderArticleId = reminder.articleId; dueAt = maxOf(reminder.dueAt, System.currentTimeMillis() + 60000); reminderDialog = true }) { Text("변경") }; AxTextButton(onClick = { vm.cancelReminder(reminder.articleId) }) { Text("취소") } } } }
                            }
                        }
                    }
                    "search" -> LazyColumn(state = searchScroll, modifier = Modifier.fillMaxSize().imePadding().testTag("search_results"), verticalArrangement = Arrangement.spacedBy(AxSpacing.md), contentPadding = PaddingValues(horizontal = AxSpacing.xl, vertical = AxSpacing.md)) {
                        item {
                        AxTextField(value = query, onValueChange = { vm.query.value = it.take(200); vm.searchLimit.value = 50 }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("글과 메모에서 검색") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if(query.isNotEmpty()) IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Default.Clear, "검색어 지우기") } })
                        Text("내려받은 글의 제목·요약·본문과 메모를 기기 안에서만 검색합니다", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = AxSpacing.md))
                        }
                        if(searching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                            if(query.isBlank()) item { EmptyState("기억나는 단어를 입력하세요", "한국어 일부 단어도 찾을 수 있어요") }
                            else if(!searching && searchArticles.isEmpty() && searchMemos.isEmpty()) item { EmptyState("검색 결과가 없어요", "더 짧거나 다른 단어로 찾아보세요. 아직 받지 않은 본문은 검색되지 않아요") }
                            if(searchArticles.isNotEmpty()) item { Text("글 · ${searchArticles.size}", fontWeight = FontWeight.Bold) }
                            items(searchArticles, key = { "a:${it.id}" }) { article -> ArticleCard(article, bookmarks.any { it.articleId == article.id }, screen in detailScreens && selectedId == article.id) { navigate("article:${article.id}") } }
                            if(searchMemos.isNotEmpty()) item { Text("메모 · ${searchMemos.size}", fontWeight = FontWeight.Bold) }
                            items(searchMemos, key = { "m:${it.id}" }) { memo -> MemoCard(memo, { edit(memo) }, { navigate("article:${memo.articleId}") }, { requestDelete(memo) }) }
                            if(searchArticles.size >= vm.searchLimit.value || searchMemos.size >= vm.searchLimit.value) item { AxTextButton(onClick = { vm.searchLimit.value += 50 }) { Text("결과 더 보기") } }
                    }
                    "reader" -> {
                        val articleFlow = remember(selectedId) { vm.article(selectedId) }
                        val article by articleFlow.collectAsStateWithLifecycle(null)
                        val fallbackTitle = bookmarks.find { it.articleId == selectedId }?.titleSnapshot ?: memos.find { it.articleId == selectedId }?.titleSnapshot ?: reminders.find { it.articleId == selectedId }?.titleSnapshot ?: "글"
                        val fallbackUrl = bookmarks.find { it.articleId == selectedId }?.urlSnapshot ?: memos.find { it.articleId == selectedId }?.urlSnapshot ?: reminders.find { it.articleId == selectedId }?.urlSnapshot
                        Column(Modifier.fillMaxSize().testTag("reader_pane")) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = AxSpacing.sm), horizontalArrangement = Arrangement.spacedBy(AxSpacing.xs)) {
                                AxTextButton(onClick = { vm.toggleBookmark(selectedId) }, modifier = Modifier.semantics { stateDescription = if(bookmarks.any { it.articleId == selectedId }) "저장됨" else "저장 안 됨" }) { Icon(painterResource(if(bookmarks.any { it.articleId == selectedId }) R.drawable.ic_bookmark else R.drawable.ic_bookmark_outline), null); Spacer(Modifier.width(AxSpacing.xs)); Text(if(bookmarks.any { it.articleId == selectedId }) "북마크됨" else "북마크") }
                                AxTextButton(onClick = { screen = "articleMemos" }) { Icon(painterResource(R.drawable.ic_memo), null); Spacer(Modifier.width(AxSpacing.xs)); Text("메모") }
                                AxTextButton(onClick = { reminderArticleId = selectedId; dueAt = reminders.find { it.articleId == selectedId && it.state in listOf("scheduled", "blocked") }?.dueAt ?: (System.currentTimeMillis() + 3600000); reminderDialog = true }) { Icon(painterResource(R.drawable.ic_reminder), null); Spacer(Modifier.width(AxSpacing.xs)); Text("읽기 알림") }
                                AxTextButton(onClick = { (article?.canonicalUrl ?: fallbackUrl)?.let { shareArticle(context, article?.title ?: fallbackTitle, it) { vm.message.value = it } } }) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(AxSpacing.xs)); Text("공유") }
                            }
                            HorizontalDivider()
                            if(article?.available == false || article == null) Warning("원문을 현재 목록에서 찾을 수 없어요. 저장한 메모와 북마크는 유지됩니다")
                            if(article?.cachedRevision != null && article?.cachedRevision != article?.wantedRevision) Warning("이전에 저장한 본문입니다. 새 버전 다운로드를 다시 시도하세요")
                            if(article?.bodyHtml != null) ReaderWebView(article!!, Modifier.weight(1f)) { vm.message.value = it }
                            else { Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(AxSpacing.xxl)) { Text(article?.title ?: fallbackTitle, style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(AxSpacing.xxl)); EmptyState("본문을 아직 받지 못했어요", "인터넷에 연결하고 다시 시도하세요. 개인 메모는 계속 볼 수 있어요"); AxButton(onClick = { vm.load(selectedId); if(article == null) vm.refresh() }, modifier = Modifier.padding(top = AxSpacing.md)) { Text("다시 시도") } } }
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = AxSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                                if (article?.bodyHtml != null) AxTextButton(onClick = { showOfflineInfo = true }) {
                                    Icon(painterResource(R.drawable.ic_download_done), null, Modifier.size(AxSize.supportingIcon))
                                    Spacer(Modifier.width(AxSpacing.xs)); Text("본문 저장됨", style = MaterialTheme.typography.labelMedium)
                                }
                                AxTextButton(onClick = { screen = "articleMemos" }) { Text("연결된 메모 ${memos.count { it.articleId == selectedId }}", style = MaterialTheme.typography.labelMedium) }
                                AxTextButton(onClick = { edit(null) }) { Icon(Icons.Default.Add, null, Modifier.size(AxSize.supportingIcon)); Text("메모 남기기", style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                    "articleMemos" -> LazyColumn(state = memoScroll, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(AxComponentTokens.pageMargin), verticalArrangement = Arrangement.spacedBy(AxSpacing.md)) {
                        item { AxButton(onClick = { edit(null) }) { Icon(Icons.Default.Add, null); Text("새 메모") } }
                        val selectedMemos = memos.filter { it.articleId == selectedId }
                        if(selectedMemos.isEmpty()) item { EmptyState("이 글의 첫 생각을 남겨보세요", "북마크와 별개로 여러 메모를 저장할 수 있어요") }
                        items(selectedMemos, key = { it.id }) { memo -> MemoCard(memo, { edit(memo) }, { screen = "reader" }, { requestDelete(memo) }) }
                    }
                    "editor" -> Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(AxComponentTokens.pageMargin).testTag("memo_editor"), verticalArrangement = Arrangement.spacedBy(AxSpacing.md)) {
                        Text("글에 연결된 메모 · 이 기기에만 저장", style = MaterialTheme.typography.labelMedium)
                        AxTextField(editorTitle, { editorTitle = it.take(500) }, label = { Text("제목 (선택)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving)
                        AxTextField(editorBody, { editorBody = it.take(100000) }, label = { Text("기억할 생각이나 적용할 아이디어") }, modifier = Modifier.fillMaxWidth().heightIn(min = AxSize.editorMinHeight), minLines = 6, maxLines = 14, enabled = !saving)
                        AxButton(onClick = { saving = true; scope.launch { try { vm.saveMemo(editorId, selectedId, editorTitle, editorBody, editorDraftId); screen = editorReturn; originalTitle = editorTitle; originalBody = editorBody } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; vm.message.value = e.message ?: "저장하지 못했습니다" } finally { saving = false } } }, enabled = editorBody.isNotBlank(), loading = saving, modifier = Modifier.fillMaxWidth()) { Text(if(saving) "저장 중…" else "메모 저장") }
                    }
                    "settings" -> LazyColumn(state = settingsScroll, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(AxComponentTokens.pageMargin), verticalArrangement = Arrangement.spacedBy(AxSpacing.xxl)) {
                        item { Text("AX Notes", style = MaterialTheme.typography.headlineLarge); Text("${BuildConfig.VERSION_NAME}\n읽고, 직접 해보고, 만드는 과정을 함께 따라가는 AX 채널", style = MaterialTheme.typography.bodyMedium) }
                        item { AxTextButton(onClick = { openExternal(context, feed?.aboutUrl ?: "https://ax.msalt.net/about/") { vm.message.value = it } }) { Text("${feed?.authorName ?: "AX Notes"} 소개") }; feed?.let { f -> val channels = org.json.JSONArray(f.channelsJson); for(i in 0 until channels.length()) { val channel = channels.getJSONObject(i); AxTextButton(onClick = { openExternal(context, channel.getString("url")) { vm.message.value = it } }) { Text(channel.getString("label")) } } } }
                        item { HorizontalDivider(); Text("콘텐츠", style = MaterialTheme.typography.titleMedium); Text("ax.msalt.net의 공개 글을 가져옵니다", style = MaterialTheme.typography.bodyMedium); Text("마지막 갱신: ${feed?.fetchedAt?.let(::formatTime) ?: "아직 없음"}", style = MaterialTheme.typography.bodyMedium); AxTextButton(onClick = { showOfflineInfo = true }) { Icon(painterResource(R.drawable.ic_download_done), null); Spacer(Modifier.width(AxSpacing.sm)); Text("저장된 본문 안내") } }
                        item { HorizontalDivider(); Text("알림", style = MaterialTheme.typography.titleMedium); Text(if(notificationEnabled) "알림 사용 가능" else "알림 차단됨 · 일정은 보관함에 유지"); AxTextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("시스템 알림 설정") } }
                        item { HorizontalDivider(); Text("기기 안의 데이터", style = MaterialTheme.typography.titleMedium); Text("계정·백업·동기화·내보내기가 없습니다. 앱 삭제·데이터 초기화·기기 변경 때 북마크, 메모, 알림을 잃을 수 있으며 복구할 수 없습니다. 자동 클라우드 백업과 기기 이전에서 제외하도록 구성했습니다."); Text("메모와 검색어는 서버로 보내지 않습니다. 본문 이미지와 외부 링크에는 인터넷이 사용되며 외부 사이트의 정책이 적용됩니다.", style = MaterialTheme.typography.bodySmall) }
                        item { HorizontalDivider(); Text("글꼴", style = MaterialTheme.typography.titleMedium); Text("제목 · 나눔명조 / 본문·조작 · 시스템 글꼴", style = MaterialTheme.typography.bodyMedium); AxTextButton(onClick = { showFontLicense = true }) { Text("글꼴 라이선스") } }
                        item { AxOutlinedButton(onClick = { confirmCache = true }, enabled = !busy) { Text("콘텐츠 캐시만 삭제") }; AxTextButton(onClick = { confirmPersonal = true }, destructive = true) { Text("개인 데이터 전체 삭제") } }
                    }
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val layout = adaptiveLayout(maxWidth.value, maxHeight.value, LocalDensity.current.fontScale)
            val origin = if (screen in detailScreens) previous else screen
            val twoPane = layout.useTwoPanes && origin in listScreens
            val title = when (screen) {
                "library" -> "내 보관함"
                "search" -> "통합 검색"
                "settings" -> "설정 및 소개"
                "editor" -> if (editorId == null) "새 메모" else "메모 수정"
                "articleMemos" -> "글에 남긴 메모"
                else -> "AX Notes"
            }
            Scaffold(
                containerColor = scheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    TopAppBar(
                        expandedHeight = maxOf(64.dp, ((if (screen == "notes") MaterialTheme.typography.headlineLarge.lineHeight.value else MaterialTheme.typography.titleLarge.lineHeight.value) * LocalDensity.current.fontScale + 16f).dp),
                        title = {
                            if (screen == "notes") Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AxSpacing.xs)) {
                                Text("AX.", style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                                Text("notes", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = AxSpacing.sm))
                            } else Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        navigationIcon = {
                            if (screen !in listOf("notes", "library")) IconButton(onClick = ::back) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background)
                    )
                },
                bottomBar = {
                    if (!layout.useRail && screen in listOf("notes", "library")) {
                        CompactNavigation(screen) { navigate("screen:$it") }
                    }
                }
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                    if (layout.useRail) {
                        AppNavigationRail(origin, saving) { navigate("screen:$it") }
                        VerticalDivider()
                    }
                    if (twoPane) {
                        Row(Modifier.weight(1f).fillMaxHeight().testTag("list_detail_workspace")) {
                            Box(Modifier.width(layout.listWidthDp.dp).fillMaxHeight().testTag("list_pane")) { renderScreen(origin) }
                            VerticalDivider()
                            Box(Modifier.weight(1f).fillMaxHeight().testTag("detail_pane"), contentAlignment = Alignment.TopCenter) {
                                if (screen in detailScreens) {
                                    Box(Modifier.widthIn(max = AxSize.readerMaxWidth).fillMaxSize()) { renderScreen(screen) }
                                } else ReaderPlaceholder()
                            }
                        }
                    } else {
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.widthIn(max = if (screen == "reader") AxSize.readerMaxWidth else AxSize.listMaxWidth).fillMaxSize()) { renderScreen(screen) }
                        }
                    }
                }
            }
        }
        if(showFontLicense) {
            val license = remember(context) { context.assets.open("licenses/NanumMyeongjo-OFL.txt").bufferedReader().use { it.readText() } }
            AlertDialog(onDismissRequest = { showFontLicense = false }, title = { Text("나눔명조 · SIL OFL 1.1") },
                text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(license, style = MaterialTheme.typography.bodySmall) } },
                confirmButton = { AxTextButton(onClick = { showFontLicense = false }) { Text("닫기") } })
        }
        if(showOfflineInfo) AlertDialog(
            onDismissRequest = { showOfflineInfo = false },
            title = { Text("저장된 본문") },
            text = { Text("받은 글의 본문은 이 기기에 저장되어 인터넷 없이 다시 읽을 수 있어요. 이미지와 외부 링크는 인터넷이 필요할 수 있습니다. 북마크·메모·읽기 알림은 본문 저장과 별개로 보관됩니다.") },
            confirmButton = { AxTextButton(onClick = { showOfflineInfo = false }) { Text("확인") } }
        )
        if(confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false; pendingTarget = null }, title = { Text("저장하지 않은 메모가 있어요") }, text = { Text("나가면 이번 변경을 잃습니다") }, confirmButton = { AxTextButton(onClick = { confirmDiscard = false; val destination = pendingTarget; pendingTarget = null; if(destination != null) acceptTarget(destination) else screen = editorReturn }) { Text("변경 버리고 나가기") } }, dismissButton = { AxTextButton(onClick = { confirmDiscard = false; pendingTarget = null }) { Text("계속 작성") } })
        deleteMemo?.let { memo -> AlertDialog(onDismissRequest = { deleteMemo = null }, title = { Text("메모를 삭제할까요?") }, text = { Text("삭제한 메모는 복구할 수 없습니다. 글 북마크는 유지됩니다") }, confirmButton = { AxTextButton(onClick = { vm.deleteMemo(memo.id); deleteMemo = null }, destructive = true) { Text("삭제") } }, dismissButton = { AxTextButton(onClick = { deleteMemo = null }) { Text("취소") } }) }
        if(confirmCache) AlertDialog(onDismissRequest = { confirmCache = false }, title = { Text("콘텐츠 캐시를 지울까요?") }, text = { Text("다운로드한 글만 지웁니다. 메모·북마크·읽기 알림은 남습니다. 다시 받으려면 글 목록을 아래로 당기세요") }, confirmButton = { AxTextButton(onClick = { vm.clearCache(); confirmCache = false }) { Text("캐시 삭제") } }, dismissButton = { AxTextButton(onClick = { confirmCache = false }) { Text("취소") } })
        if(confirmPersonal) AlertDialog(onDismissRequest = { confirmPersonal = false }, title = { Text("개인 데이터를 모두 삭제할까요?") }, text = { Text("북마크·메모·읽기 알림을 모두 지우고 예약을 취소합니다. 백업이 없어 복구할 수 없습니다") }, confirmButton = { AxTextButton(onClick = { vm.clearPersonal(); confirmPersonal = false }, destructive = true) { Text("모두 삭제") } }, dismissButton = { AxTextButton(onClick = { confirmPersonal = false }) { Text("취소") } })
        if(reminderDialog) AlertDialog(onDismissRequest = { reminderDialog = false }, title = { Text("나중에 읽기") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AxSpacing.md)) { Text(formatTime(dueAt)); AxOutlinedButton(onClick = { val c = Calendar.getInstance().apply { timeInMillis = dueAt }; DatePickerDialog(context, { _, year, month, day -> c.set(Calendar.YEAR, year); c.set(Calendar.MONTH, month); c.set(Calendar.DAY_OF_MONTH, day); dueAt = c.timeInMillis }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply { datePicker.minDate = System.currentTimeMillis(); show() } }) { Text("날짜 선택") }; AxOutlinedButton(onClick = { val c = Calendar.getInstance().apply { timeInMillis = dueAt }; TimePickerDialog(context, { _, hour, minute -> c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, minute); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); dueAt = c.timeInMillis }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show() }) { Text("시각 선택") }; Text("정확한 시각을 보장하지 않아요. 절전·OS 제한으로 늦어지거나 전달되지 않을 수 있습니다", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { AxTextButton(onClick = { if(dueAt <= System.currentTimeMillis()) { vm.message.value = "미래 시각을 선택하세요" } else { reminderDialog = false; if(Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) { permissionArticle = reminderArticleId; permissionDue = dueAt; permission.launch(Manifest.permission.POST_NOTIFICATIONS) } else vm.schedule(reminderArticleId, dueAt) } }) { Text("예약") } }, dismissButton = { AxTextButton(onClick = { reminderDialog = false }) { Text("취소") } })
    }
}

@Composable internal fun EmptyState(title: String, subtitle: String) = AxEmptyState(title, subtitle)
@Composable internal fun Warning(text: String) = AxNotice(text, tone = AxNoticeTone.Warning)
@Composable internal fun ArticleCard(article: Article, saved: Boolean, isSelected: Boolean = false, ordinal: Int? = null, onClick: () -> Unit) {
    AxCard(onClick = onClick, selected = isSelected) {
        Row(Modifier.padding(vertical = AxSpacing.lg, horizontal = AxSpacing.sm), horizontalArrangement = Arrangement.spacedBy(AxSpacing.md)) {
            if (ordinal != null) Text(ordinal.toString().padStart(2, '0'), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = AxSpacing.xs))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AxSpacing.sm)) {
                Text(article.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(article.description, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AxSpacing.sm)) {
                    Text(listOfNotNull(article.collectionLabel(), article.publishedAt.take(10)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if(article.bodyHtml != null) Icon(painterResource(R.drawable.ic_download_done), "본문 저장됨", Modifier.size(AxSize.supportingIcon), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    if(saved) Icon(painterResource(R.drawable.ic_bookmark), "북마크됨", Modifier.size(AxSize.supportingIcon), tint = MaterialTheme.colorScheme.primary)
                }
                if(!article.available) Text("공개 목록에서 삭제됨", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
@Composable internal fun SavedCard(title: String, subtitle: String, onClick: () -> Unit, isSelected: Boolean = false, subtitleMaxLines: Int = Int.MAX_VALUE, actions: @Composable () -> Unit) { AxCardFrame(selected = isSelected) { Column(Modifier.padding(AxComponentTokens.cardPadding)) { Column(Modifier.fillMaxWidth().heightIn(min = AxSize.minTouch).clickable(onClick = onClick).padding(vertical = AxSpacing.sm)) { Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis); Text(subtitle, maxLines = subtitleMaxLines, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; actions() } } }
@Composable private fun MemoCard(memo: Memo, onEdit: () -> Unit, onArticle: () -> Unit, onDelete: () -> Unit) { SavedCard(memo.title.ifBlank { "제목 없는 메모" }, memo.body.take(180), onEdit, subtitleMaxLines = 3) { Text("연결된 글 · ${memo.titleSnapshot}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = AxSpacing.sm)); Row(Modifier.horizontalScroll(rememberScrollState())) { AxTextButton(onClick = onArticle) { Text("글 읽기") }; AxTextButton(onClick = onEdit) { Text("수정") }; AxTextButton(onClick = onDelete, destructive = true) { Text("삭제") } } } }
private fun formatTime(time: Long) = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(time))
private fun reminderLabel(state: String) = when(state) { "scheduled" -> "예정"; "blocked" -> "알림 차단됨"; "delivered" -> "알림 전송 · 아직 열지 않음"; "opened" -> "열어봄"; else -> "전송 확인 중" }

@Composable
private fun AppNavigationRail(selectedDestination: String, saving: Boolean, onSelect: (String) -> Unit) {
    val enlargedText = LocalDensity.current.fontScale >= 1.5f
    // A scrollable rail also fits short landscape windows and 200% system text.
    Column(
        Modifier.width(if (enlargedText) AxSize.largeTextRailWidth else AxSize.railWidth).fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLow).verticalScroll(rememberScrollState())
            .padding(vertical = AxSpacing.md).testTag("navigation_rail"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AxSpacing.md)
    ) {
        Text("AX.", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = AxSpacing.md))
        listOf("notes" to "Notes", "library" to "내 보관함", "search" to "검색", "settings" to "설정").forEach { (route, label) ->
            NavigationRailItem(
                selected = selectedDestination == route,
                onClick = { onSelect(route) },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = AxSize.railItemMinHeight).semantics { contentDescription = label }.testTag("nav_$route"),
                icon = {
                    when (route) {
                        "notes" -> Icon(painterResource(R.drawable.ic_article), null)
                        "library" -> Icon(painterResource(R.drawable.ic_library), null)
                        "search" -> Icon(Icons.Default.Search, null)
                        else -> Icon(Icons.Default.Settings, null)
                    }
                },
                alwaysShowLabel = false,
                colors = NavigationRailItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.secondaryContainer)
            )
        }
    }
}

@Composable
private fun ReaderPlaceholder() {
    Column(
        Modifier.widthIn(max = AxSize.placeholderMaxWidth).fillMaxWidth().fillMaxHeight()
            .verticalScroll(rememberScrollState()).padding(horizontal = AxComponentTokens.placeholderHorizontalPadding, vertical = AxComponentTokens.placeholderVerticalPadding)
            .testTag("reader_placeholder"),
        verticalArrangement = Arrangement.spacedBy(AxSpacing.xl)
    ) {
        Icon(painterResource(R.drawable.ic_article), null, Modifier.size(AxSize.emptyStateIcon), tint = MaterialTheme.colorScheme.primary)
        Text("읽고, 생각을 남기는 공간", style = MaterialTheme.typography.headlineMedium)
        Text("목록에서 글을 선택하세요", style = MaterialTheme.typography.titleMedium)
        Text("글을 읽으며 북마크하고, 나만의 메모와 읽기 알림을 남겨보세요. 목록은 이곳에 그대로 있어요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CompactNavigation(selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface) {
        Column {
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().navigationBarsPadding().height(AxSize.bottomNavigation).selectableGroup().testTag("bottom_navigation"), verticalAlignment = Alignment.CenterVertically) {
                listOf("notes" to "Notes", "library" to "내 보관함", "search" to "검색", "settings" to "설정").forEach { (route, label) ->
                    Box(Modifier.weight(1f).fillMaxHeight().selectable(selected = selected == route, role = Role.Tab, onClick = { onSelect(route) })
                        .semantics { contentDescription = label }.testTag("nav_$route")
                        .drawBehind { if (selected == route) drawCircle(colors.primary, 2.dp.toPx(), Offset(size.width / 2, size.height - 5.dp.toPx())) }, contentAlignment = Alignment.Center) {
                        val tint = if(selected == route) colors.primary else colors.onSurfaceVariant
                        when(route) {
                            "notes" -> Icon(painterResource(R.drawable.ic_article), null, Modifier.size(AxSize.icon), tint = tint)
                            "library" -> Icon(painterResource(R.drawable.ic_library), null, Modifier.size(AxSize.icon), tint = tint)
                            "search" -> Icon(Icons.Default.Search, null, Modifier.size(AxSize.icon), tint = tint)
                            else -> Icon(Icons.Default.Settings, null, Modifier.size(AxSize.icon), tint = tint)
                        }
                    }
                }
            }
        }
    }
}
