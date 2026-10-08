package net.msalt.axnotes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.msalt.axnotes.AxApplication
import net.msalt.axnotes.data.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AxViewModel(app: Application) : AndroidViewModel(app) {
    val graph = (app as AxApplication).graph
    private var repository = graph.content()
    val articles = graph.contentDb.dao().observeArticles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val feed = graph.contentDb.dao().observeFeed().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val bookmarks = graph.personal.dao.observeBookmarks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val memos = graph.personal.dao.observeMemos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminders = graph.personal.dao.observeReminders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val notificationEnabled = MutableStateFlow(graph.scheduler.enabled())
    val sampleMode = MutableStateFlow(graph.sampleMode)
    val query = MutableStateFlow("")
    val searchArticles = MutableStateFlow<List<Article>>(emptyList())
    val searchMemos = MutableStateFlow<List<Memo>>(emptyList())
    val searchLimit = MutableStateFlow(50)
    val searching = MutableStateFlow(false)
    init {
        refresh()
        viewModelScope.launch {
            combine(query.debounce(250), searchLimit, memos, articles) { q, limit, _, _ -> q to limit }
                .collectLatest { (q, limit) ->
                    searching.value = true
                    try {
                        if (q.isBlank()) { searchArticles.value = emptyList(); searchMemos.value = emptyList() }
                        else {
                            val pattern = FeedContract.searchPattern(q)
                            searchArticles.value = graph.contentDb.dao().search(pattern, limit)
                            searchMemos.value = graph.personal.dao.search(pattern, limit)
                        }
                    } finally { searching.value = false }
                }
        }
    }
    fun refresh() {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                val failed = repository.refresh()
                if (failed > 0) message.value = "목록을 갱신했습니다. 본문 ${failed}개는 다시 받아야 합니다"
            } catch (e: Exception) { if (e is CancellationException) throw e; message.value = "갱신하지 못했습니다. 저장된 내용은 유지됩니다. ${e.message.orEmpty().take(160)}" }
            finally { busy.value = false }
        }
    }
    fun onResume() = action { notificationEnabled.value = graph.scheduler.enabled(); graph.scheduler.reconcile() }
    fun article(id: String): Flow<Article?> = graph.contentDb.dao().observeArticle(id)
    fun load(id: String) = action { graph.scheduler.markOpened(id); repository.load(id) }
    fun toggleBookmark(id: String) = action { graph.personal.toggleBookmark(id) }
    fun deleteMemo(id: String) = action { graph.personal.deleteMemo(id) }
    fun cancelReminder(id: String) = action { graph.scheduler.cancel(id) }
    fun schedule(id: String, dueAt: Long) = action {
        val (title, url) = graph.personal.snapshot(id)
        graph.scheduler.schedule(id, title, url, dueAt)
        notificationEnabled.value = graph.scheduler.enabled()
        message.value = if (notificationEnabled.value) "읽기 알림을 예약했습니다. 절전 설정에 따라 늦어질 수 있습니다" else "일정은 저장됐지만 알림이 차단되어 있습니다"
    }
    fun clearCache() = action { repository.clear(); message.value = "콘텐츠 캐시를 지웠습니다. 북마크·메모·알림은 유지됩니다" }
    fun clearPersonal() = action { graph.scheduler.clearPersonalData(); message.value = "개인 데이터를 지웠습니다" }
    fun switchSample(value: Boolean) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                // Refresh transactionally; a failed connection must not erase the
                // previous readable cache or any personal data.
                graph.sampleMode = value; sampleMode.value = value; repository = graph.content()
                val failed = repository.refresh()
                if (failed > 0) message.value = "본문 ${failed}개는 다시 받아야 합니다"
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                message.value = "선택한 콘텐츠를 받지 못했습니다. ${e.message.orEmpty().take(160)}"
            } finally { busy.value = false }
        }
    }
    suspend fun saveMemo(id: String?, articleId: String, title: String, body: String, newMemoId: String) = graph.personal.saveMemo(id, articleId, title, body, newMemoId)
    fun action(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() } catch (e: Exception) { if (e is CancellationException) throw e; message.value = e.message ?: "작업을 완료하지 못했습니다. 다시 시도하세요" }
    }
}
