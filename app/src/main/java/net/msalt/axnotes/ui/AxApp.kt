@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package net.msalt.axnotes.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.msalt.axnotes.data.*
import java.text.DateFormat
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
        var editorBody by rememberSaveable { mutableStateOf("") }
        var originalBody by rememberSaveable { mutableStateOf("") }
        var editorReturn by rememberSaveable { mutableStateOf("reader") }
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
        val settingsScroll = rememberLazyListState()
        val paneState = rememberSaveableStateHolder()
        val detailScreens = setOf("reader", "editor")
        val listScreens = setOf("notes", "library", "search")
        fun selectCollection(type: String, id: String? = null) {
            collectionType = type
            collectionId = id
            scope.launch { notesScroll.scrollToItem(0) }
        }
        fun hasUnsavedChanges() = screen == "editor" && editorBody != originalBody
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
                "reader" -> previous
                else -> "notes"
            }
        }
        fun beginEdit(memo: Memo?) {
            if (screen in listScreens) previous = screen
            editorDraftId = memo?.id ?: java.util.UUID.randomUUID().toString()
            editorId = memo?.id
            editorBody = memo?.body.orEmpty()
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
                value == "newMemo" -> beginEdit(latestMemoForArticle(memos, selectedId))
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
                        onArticle = { navigate("article:$it") }
                    )
                    "library" -> LazyColumn(state = libraryScroll, modifier = Modifier.fillMaxSize().testTag("library_list"), contentPadding = PaddingValues(AxComponentTokens.pageMargin), verticalArrangement = Arrangement.spacedBy(AxSpacing.md)) {
                        item { AxPageHeading("내 보관함", "기기에 보관된 북마크와 메모, 알림 입니다."); Spacer(Modifier.height(AxSpacing.lg)); Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(AxSpacing.sm)) { listOf("북마크", "메모", "읽기 알림").forEach { name -> AxSectionTab(selected = libraryType == name, onClick = { libraryType = name }, label = name) } } }
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
                    "search" -> LazyColumn(state = searchScroll, modifier = Modifier.fillMaxSize().imePadding().testTag("search_results"), verticalArrangement = Arrangement.spacedBy(AxSpacing.md), contentPadding = PaddingValues(AxComponentTokens.pageMargin)) {
                        item { AxPageHeading("통합 검색", "내려받은 글과 나의 메모에서 찾아보세요"); Spacer(Modifier.height(AxSpacing.sm)) }
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
                    "reader" -> key(selectedId) {
                        // The list already owns the cached body. Do not render a false
                        // missing-article screen while Room delivers its first value.
                        val preview = remember(selectedId) { (articles + searchArticles).firstOrNull { it.id == selectedId } }
                        val articleFlow = remember(selectedId) { vm.article(selectedId).map { ReaderArticleLoad(it, true) } }
                        val articleLoad by articleFlow.collectAsStateWithLifecycle(ReaderArticleLoad(preview, preview != null))
                        val article = articleLoad.article
                        val readerActive = screen == "reader"
                        val fallbackTitle = bookmarks.find { it.articleId == selectedId }?.titleSnapshot ?: memos.find { it.articleId == selectedId }?.titleSnapshot ?: reminders.find { it.articleId == selectedId }?.titleSnapshot ?: "글"
                        val fallbackUrl = bookmarks.find { it.articleId == selectedId }?.urlSnapshot ?: memos.find { it.articleId == selectedId }?.urlSnapshot ?: reminders.find { it.articleId == selectedId }?.urlSnapshot
                        val selectedMemo = latestMemoForArticle(memos, selectedId)
                        Column(Modifier.fillMaxSize().testTag("reader_pane")) {
                            ReaderActions(enabled = readerActive) {
                                AxTextButton(enabled = readerActive, onClick = { vm.toggleBookmark(selectedId) }, modifier = Modifier.semantics { stateDescription = if(bookmarks.any { it.articleId == selectedId }) "저장됨" else "저장 안 됨" }) { Icon(painterResource(if(bookmarks.any { it.articleId == selectedId }) R.drawable.ic_bookmark else R.drawable.ic_bookmark_outline), null); Spacer(Modifier.width(AxSpacing.xs)); Text(if(bookmarks.any { it.articleId == selectedId }) "북마크됨" else "북마크") }
                                AxTextButton(enabled = readerActive, onClick = { reminderArticleId = selectedId; dueAt = reminders.find { it.articleId == selectedId && it.state in listOf("scheduled", "blocked") }?.dueAt ?: (System.currentTimeMillis() + 3600000); reminderDialog = true }) { Icon(painterResource(R.drawable.ic_reminder), null); Spacer(Modifier.width(AxSpacing.xs)); Text("읽기 알림") }
                                AxTextButton(enabled = readerActive, onClick = { edit(null) }, modifier = Modifier.testTag("reader_memo_action")) {
                                    Icon(painterResource(R.drawable.ic_memo), null); Spacer(Modifier.width(AxSpacing.xs))
                                    Text(if (selectedMemo == null) "메모 남기기" else "메모 수정")
                                }
                                AxTextButton(enabled = readerActive, onClick = { (article?.canonicalUrl ?: fallbackUrl)?.let { shareArticle(context, article?.title ?: fallbackTitle, it) { vm.message.value = it } } }) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(AxSpacing.xs)); Text("공유") }
                            }
                            HorizontalDivider()
                            if(articleLoad.ready && (article?.available == false || article == null)) Warning("원문을 현재 목록에서 찾을 수 없어요. 저장한 메모와 북마크는 유지됩니다")
                            if(article?.cachedRevision != null && article?.cachedRevision != article?.wantedRevision) Warning("이전에 저장한 본문입니다. 새 버전 다운로드를 다시 시도하세요")
                            if (!articleLoad.ready) {
                                Box(Modifier.weight(1f).fillMaxWidth().testTag("reader_loading"), contentAlignment = Alignment.Center) {
                                    Text("글을 여는 중…", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                                }
                            } else if(article?.bodyHtml != null) ReaderWebView(article, Modifier.weight(1f), visible = readerActive) { vm.message.value = it }
                            else { Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(AxSpacing.xxl)) { Text(article?.title ?: fallbackTitle, style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(AxSpacing.xxl)); EmptyState("본문을 아직 받지 못했어요", "인터넷에 연결하고 다시 시도하세요. 개인 메모는 계속 볼 수 있어요"); AxButton(enabled = readerActive, onClick = { vm.load(selectedId); if(article == null) vm.refresh() }, modifier = Modifier.padding(top = AxSpacing.md)) { Text("다시 시도") } } }
                        }
                    }
                    "editor" -> Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(AxComponentTokens.pageMargin).testTag("memo_editor"), verticalArrangement = Arrangement.spacedBy(AxSpacing.md)) {
                        Text("글에 연결된 메모 · 이 기기에만 저장", style = MaterialTheme.typography.labelMedium)
                        AxTextField(editorBody, { editorBody = it.take(100000) }, label = { Text("기억할 생각이나 적용할 아이디어") }, modifier = Modifier.fillMaxWidth().heightIn(min = AxSize.editorMinHeight), minLines = 6, maxLines = 14, enabled = !saving)
                        AxButton(onClick = { saving = true; scope.launch { try { vm.saveMemo(editorId, selectedId, "", editorBody, editorDraftId); screen = editorReturn; originalBody = editorBody } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; vm.message.value = e.message ?: "저장하지 못했습니다" } finally { saving = false } } }, enabled = editorBody.isNotBlank(), loading = saving, modifier = Modifier.fillMaxWidth()) { Text(if(saving) "저장 중…" else "메모 저장") }
                    }
                    "settings" -> LazyColumn(state = settingsScroll, modifier = Modifier.fillMaxSize().testTag("settings_list"), contentPadding = PaddingValues(AxComponentTokens.pageMargin), verticalArrangement = Arrangement.spacedBy(AxSpacing.section)) {
                        item { AxPageHeading("설정 및 소개", "앱 정보와 이 기기의 읽기 환경을 확인하세요") }
                        item { AxSettingsSection("앱 정보") {
                            Text("버전 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                            Text("악소리 나는 AX 경험담. AI와 Agent가 실제 일에 들어올 때 생기는 변화와 질문을 기록합니다.", style = MaterialTheme.typography.bodyMedium)
                            AxOutlinedButton(onClick = { openExternal(context, feed?.aboutUrl ?: "https://ax.msalt.net/about/") { vm.message.value = it } }) { Text("채널 소개") }
                            feed?.let { f -> val channels = org.json.JSONArray(f.channelsJson); for(i in 0 until channels.length()) {
                                val channel = channels.getJSONObject(i)
                                AxTextButton(onClick = { openExternal(context, channel.getString("url")) { vm.message.value = it } }) { Text(channel.getString("label")) }
                            } }
                        } }
                        item { AxSettingsSection("콘텐츠") {
                            Text("ax.msalt.net의 공개 글을 가져옵니다", style = MaterialTheme.typography.bodyMedium)
                            Text("마지막 갱신: ${feed?.fetchedAt?.let(::formatTime) ?: "아직 없음"}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            AxOutlinedButton(onClick = { showOfflineInfo = true }) { Text("오프라인 읽기 안내") }
                        } }
                        item { AxSettingsSection("알림") {
                            Text(if(notificationEnabled) "알림 사용 가능" else "알림 차단됨 · 일정은 보관함에 유지", style = MaterialTheme.typography.bodyMedium)
                            AxOutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("시스템 알림 설정") }
                        } }
                        item { AxSettingsSection("기기 안의 데이터") {
                            Text("계정·백업·동기화·내보내기가 없습니다. 앱 삭제·데이터 초기화·기기 변경 때 북마크, 메모, 알림을 잃을 수 있으며 복구할 수 없습니다. 자동 클라우드 백업과 기기 이전에서 제외하도록 구성했습니다.", style = MaterialTheme.typography.bodyMedium)
                            Text("메모와 검색어는 서버로 보내지 않습니다. 본문 이미지와 외부 링크에는 인터넷이 사용되며 외부 사이트의 정책이 적용됩니다.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        } }
                        item { AxSettingsSection("글꼴") {
                            Text("제목 · Noto Serif KR\n화면·조작 · IBM Plex Sans KR\n글 본문 · Pretendard", style = MaterialTheme.typography.bodyMedium)
                            AxOutlinedButton(onClick = { showFontLicense = true }) { Text("글꼴 라이선스") }
                        } }
                        item { AxSettingsSection("데이터 관리") {
                            Text("삭제 전 확인 화면이 표시됩니다", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            AxOutlinedButton(onClick = { confirmCache = true }, enabled = !busy) { Text("콘텐츠 캐시만 삭제") }
                            AxTextButton(onClick = { confirmPersonal = true }, destructive = true) { Text("개인 데이터 전체 삭제") }
                        } }
                    }
                }
            }
        }
        // Retain only the current reader while its memo is open. The native view
        // stays attached, so returning never reloads the document or loses scroll.
        // Leaving this flow or changing article disposes it normally.
        val keepReader = screen == "reader" || (screen == "editor" && editorReturn == "reader")
        val renderCurrentPane: @Composable () -> Unit = {
            Box(Modifier.fillMaxSize()) {
                if (keepReader) {
                    val visible = screen == "reader"
                    Box(
                        Modifier.fillMaxSize().graphicsLayer { alpha = if (visible) 1f else 0f }
                            .focusProperties { canFocus = visible }
                            .then(if (visible) Modifier else Modifier.clearAndSetSemantics { })
                    ) { renderScreen("reader") }
                }
                if (screen != "reader") {
                    Box(Modifier.fillMaxSize().background(scheme.background)) { renderScreen(screen) }
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
                else -> "AX Notes"
            }
            Scaffold(
                containerColor = scheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    TopAppBar(
                        expandedHeight = maxOf(64.dp, (MaterialTheme.typography.headlineLarge.lineHeight.value * LocalDensity.current.fontScale + 16f).dp),
                        title = {
                            if (screen in listOf("notes", "library", "search", "settings", "reader")) AxMasthead()
                            else Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        navigationIcon = {
                            if (screen in detailScreens) IconButton(onClick = ::back) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background)
                    )
                },
                bottomBar = {
                    if (!layout.useRail && screen in listOf("notes", "library", "search", "settings")) {
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
                                    Box(Modifier.widthIn(max = AxSize.readerMaxWidth).fillMaxSize()) { renderCurrentPane() }
                                } else ReaderPlaceholder()
                            }
                        }
                    } else {
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.widthIn(max = if (keepReader) AxSize.readerMaxWidth else AxSize.listMaxWidth).fillMaxSize()) { renderCurrentPane() }
                        }
                    }
                }
            }
        }
        if(showFontLicense) {
            val license = remember(context) {
                listOf(
                    "Noto Serif KR · © 2017–2024 Adobe" to "NotoSerifKR-OFL.txt",
                    "IBM Plex Sans KR" to "IBMPlexSansKR-OFL.txt",
                    "Pretendard Variable" to "Pretendard-OFL.txt"
                ).joinToString("\n\n────────────────\n\n") { (family, name) ->
                    "$family\n\n" + context.assets.open("licenses/$name").bufferedReader().use { it.readText() }
                }
            }
            AlertDialog(onDismissRequest = { showFontLicense = false }, title = { Text("글꼴 · SIL OFL 1.1") },
                text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(license, style = MaterialTheme.typography.bodySmall) } },
                confirmButton = { AxTextButton(onClick = { showFontLicense = false }) { Text("닫기") } })
        }
        if(showOfflineInfo) AlertDialog(
            onDismissRequest = { showOfflineInfo = false },
            title = { Text("오프라인 읽기") },
            text = { Text("글을 열면 본문을 자동으로 기기에 저장합니다. 한 번 받은 본문은 인터넷 없이 다시 읽을 수 있어요. 아직 받지 못한 글은 목록에 제목과 요약만 보일 수 있습니다. 이미지와 외부 링크는 인터넷이 필요할 수 있습니다. 북마크·메모·읽기 알림은 본문 저장과 별개로 보관됩니다.", modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { AxTextButton(onClick = { showOfflineInfo = false }) { Text("확인") } }
        )
        if(confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false; pendingTarget = null }, title = { Text("저장하지 않은 메모가 있어요") }, text = { Text("나가면 이번 변경을 잃습니다") }, confirmButton = { AxTextButton(onClick = { confirmDiscard = false; val destination = pendingTarget; pendingTarget = null; if(destination != null) acceptTarget(destination) else screen = editorReturn }) { Text("변경 버리고 나가기") } }, dismissButton = { AxTextButton(onClick = { confirmDiscard = false; pendingTarget = null }) { Text("계속 작성") } })
        deleteMemo?.let { memo -> AlertDialog(onDismissRequest = { deleteMemo = null }, title = { Text("메모를 삭제할까요?") }, text = { Text("삭제한 메모는 복구할 수 없습니다. 글 북마크는 유지됩니다") }, confirmButton = { AxTextButton(onClick = { vm.deleteMemo(memo.id); deleteMemo = null }, destructive = true) { Text("삭제") } }, dismissButton = { AxTextButton(onClick = { deleteMemo = null }) { Text("취소") } }) }
        if(confirmCache) AlertDialog(onDismissRequest = { confirmCache = false }, title = { Text("콘텐츠 캐시를 지울까요?") }, text = { Text("다운로드한 글만 지웁니다. 메모·북마크·읽기 알림은 남습니다. 다시 받으려면 글 목록을 아래로 당기세요") }, confirmButton = { AxTextButton(onClick = { vm.clearCache(); confirmCache = false }) { Text("캐시 삭제") } }, dismissButton = { AxTextButton(onClick = { confirmCache = false }) { Text("취소") } })
        if(confirmPersonal) AlertDialog(onDismissRequest = { confirmPersonal = false }, title = { Text("개인 데이터를 모두 삭제할까요?") }, text = { Text("북마크·메모·읽기 알림을 모두 지우고 예약을 취소합니다. 백업이 없어 복구할 수 없습니다") }, confirmButton = { AxTextButton(onClick = { vm.clearPersonal(); confirmPersonal = false }, destructive = true) { Text("모두 삭제") } }, dismissButton = { AxTextButton(onClick = { confirmPersonal = false }) { Text("취소") } })
        if(reminderDialog) AxReminderDialog(
            dueAt = dueAt, onDueAtChange = { dueAt = it }, onDismiss = { reminderDialog = false },
            onConfirm = {
                reminderDialog = false
                if(Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionArticle = reminderArticleId; permissionDue = dueAt
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else vm.schedule(reminderArticleId, dueAt)
            }
        )
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
                    if(article.bodyHtml != null) Icon(painterResource(R.drawable.ic_download_done), "오프라인에서 읽을 수 있음", Modifier.size(AxSize.supportingIcon), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    if(saved) Icon(painterResource(R.drawable.ic_bookmark), "북마크됨", Modifier.size(AxSize.supportingIcon), tint = MaterialTheme.colorScheme.primary)
                }
                if(!article.available) Text("공개 목록에서 삭제됨", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
@Composable internal fun SavedCard(title: String, subtitle: String, onClick: () -> Unit, isSelected: Boolean = false, subtitleMaxLines: Int = Int.MAX_VALUE, actions: @Composable () -> Unit) { AxCardFrame(selected = isSelected) { Column(Modifier.padding(AxComponentTokens.cardPadding)) { Column(Modifier.fillMaxWidth().heightIn(min = AxSize.minTouch).clickable(onClick = onClick).padding(vertical = AxSpacing.sm)) { Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis); if (subtitle.isNotBlank()) Text(subtitle, maxLines = subtitleMaxLines, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; actions() } } }
@Composable
private fun MemoCard(memo: Memo, onEdit: () -> Unit, onArticle: () -> Unit, onDelete: () -> Unit) {
    val bodyLines = memo.body.lineSequence().dropWhile { it.isBlank() }.toList()
    val previewTitle = bodyLines.firstOrNull().orEmpty().take(180)
    val previewBody = bodyLines.drop(1).joinToString("\n").trimStart().take(180)
    SavedCard(previewTitle, previewBody, onEdit, subtitleMaxLines = 3) {
        Text("연결된 글 · ${memo.titleSnapshot}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = AxSpacing.sm))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            AxTextButton(onClick = onArticle) { Text("글 읽기") }
            AxTextButton(onClick = onEdit) { Text("수정") }
            AxTextButton(onClick = onDelete, destructive = true) { Text("삭제") }
        }
    }
}
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
        Box(Modifier.padding(vertical = AxSpacing.md)) { AxMasthead(compact = true) }
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
                colors = NavigationRailItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary, selectedIconColor = MaterialTheme.colorScheme.onPrimary)
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
internal fun CompactNavigation(selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface) {
        Column {
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().navigationBarsPadding().height(AxSize.bottomNavigation).selectableGroup().testTag("bottom_navigation"), verticalAlignment = Alignment.CenterVertically) {
                listOf("notes" to "Notes", "library" to "내 보관함", "search" to "검색", "settings" to "설정").forEach { (route, label) ->
                    val interactionSource = remember { MutableInteractionSource() }
                    // Keep the whole destination tappable; draw feedback only around its icon.
                    Box(Modifier.weight(1f).fillMaxHeight().selectable(
                        selected = selected == route,
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(route) }
                    )
                        .semantics { contentDescription = label }.testTag("nav_$route"), contentAlignment = Alignment.Center) {
                        val tint = if (selected == route) colors.primary else colors.onSurfaceVariant
                        Box(Modifier.size(AxSize.minTouch).clip(CircleShape)
                            // Unbounded ripples start at the center, even for taps at the tab edge.
                            // The circle clips press, keyboard-focus and hover feedback alike.
                            .indication(interactionSource, ripple(bounded = false, radius = AxSize.minTouch / 2, color = colors.primary)),
                            contentAlignment = Alignment.Center) {
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
}

private data class ReaderArticleLoad(val article: Article?, val ready: Boolean)

/** One stable reader target: newest edit, then creation time, then ID for ties. */
internal fun latestMemoForArticle(memos: List<Memo>, articleId: String): Memo? =
    memos.filter { it.articleId == articleId }
        .sortedWith(compareByDescending<Memo> { it.updatedAt }.thenByDescending { it.createdAt }.thenBy { it.id })
        .firstOrNull()
