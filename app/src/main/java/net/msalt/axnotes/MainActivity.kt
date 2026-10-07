package net.msalt.axnotes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.flow.MutableStateFlow
import net.msalt.axnotes.ui.AxApp

class MainActivity : ComponentActivity() {
    private val target = MutableStateFlow<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) acceptIntent(intent) else target.value = savedInstanceState.getString("pendingArticle")
        setContent { AxApp(target) }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("pendingArticle", target.value); super.onSaveInstanceState(outState) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); acceptIntent(intent) }
    private fun acceptIntent(intent: Intent) {
        target.value = if (intent.getBooleanExtra("openLibrary", false)) "library" else intent.getStringExtra("articleId")?.let { "article:$it" }
        intent.removeExtra("openLibrary"); intent.removeExtra("articleId")
    }
}
