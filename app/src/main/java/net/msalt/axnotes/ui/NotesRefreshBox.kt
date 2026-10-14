package net.msalt.axnotes.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

/** The same refresh operation is available to touch and assistive technology. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesRefreshBox(isRefreshing: Boolean, onRefresh: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { if(!isRefreshing) onRefresh() },
        modifier = Modifier.fillMaxSize().testTag("pull_refresh").semantics {
            customActions = listOf(CustomAccessibilityAction("새로고침") {
                if(isRefreshing) false else { onRefresh(); true }
            })
        },
        content = content
    )
}
