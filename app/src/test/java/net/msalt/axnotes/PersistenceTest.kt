package net.msalt.axnotes

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import net.msalt.axnotes.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PersistenceTest {
    @Test fun schemaOneReopensWithoutLosingPrivateSnapshots(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "persistence-test-${System.nanoTime()}.db"
        val db = Room.databaseBuilder(context,PersonalDatabase::class.java,name).allowMainThreadQueries().build()
        db.dao().putBookmark(Bookmark("a","글 제목","https://ax.msalt.net/notes/a/",123))
        db.dao().putMemo(Memo("m","a","글 제목","https://ax.msalt.net/notes/a/","메모","보존할 본문",123,456))
        db.dao().putReminder(Reminder("r","a","글 제목","https://ax.msalt.net/notes/a/",999,"blocked",4))
        db.close()
        val reopened = Room.databaseBuilder(context,PersonalDatabase::class.java,name).allowMainThreadQueries().build()
        assertEquals("글 제목",reopened.dao().bookmark("a")!!.titleSnapshot)
        assertEquals("보존할 본문",reopened.dao().memo("m")!!.body)
        assertEquals(4L,reopened.dao().reminder("r")!!.generation)
        reopened.close(); context.deleteDatabase(name)
    }
    @Test fun retryingNewMemoWithStableDraftIdIsIdempotent(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val content = Room.inMemoryDatabaseBuilder(context,ContentDatabase::class.java).allowMainThreadQueries().build()
        val personal = Room.inMemoryDatabaseBuilder(context,PersonalDatabase::class.java).allowMainThreadQueries().build()
        personal.dao().putBookmark(Bookmark("a","글","https://ax.msalt.net/notes/a/",123))
        val repo = PersonalRepository(content.dao(),personal)
        val first = repo.saveMemo(null,"a","","생각","stable-draft")
        val second = repo.saveMemo(null,"a","","생각","stable-draft")
        assertEquals(first,second); assertEquals(1,personal.dao().memosFor("a").size)
        content.close();personal.close()
    }
}
