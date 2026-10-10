package net.msalt.axnotes

import net.msalt.axnotes.data.Article
import net.msalt.axnotes.ui.*
import org.junit.Assert.*
import org.junit.Test

class ArticleCollectionsTest {
    private fun note(id: String) = Article(id, id, "", "2026-10-09", null, "https://ax.msalt.net/notes/$id/", "https://ax.msalt.net/app/v1/notes/$id/a.json", "a".repeat(64), null)
    @Test fun separatesPublisherDefinedProjectsAndSeriesWithoutGuessingTitles() {
        val notes = listOf(note("mentions-a-project"), note("p").copy(projectId="app", projectTitle="앱 만들기"), note("s").copy(seriesId="workflow", seriesTitle="일하는 방식"))
        assertEquals(listOf(ArticleCollection("app", "앱 만들기", 1)), articleCollections(notes,"프로젝트"))
        assertEquals(listOf(ArticleCollection("workflow", "일하는 방식", 1)), articleCollections(notes,"시리즈"))
        assertNull(notes.first().collectionLabel())
    }
    @Test fun usesEditorialOrderThenDateAndIdDeterministically() {
        val notes = listOf(note("b").copy(seriesId="s",seriesOrder=2), note("z").copy(seriesId="s",seriesOrder=1),note("a").copy(seriesId="s"),note("other").copy(seriesId="other"))
        assertEquals(listOf("z","b","a"), articlesInCollection(notes,"시리즈","s").map { it.id })
        assertEquals(notes, articlesInCollection(notes,"전체",null))
    }
    @Test fun duplicateCollectionNamesDoNotMergeDistinctIds() {
        val notes = listOf(note("1").copy(projectId="a",projectTitle="Same"),note("2").copy(projectId="b",projectTitle="Same"),note("3").copy(projectId="a",projectTitle="Same"))
        assertEquals(2, articleCollections(notes,"프로젝트").size)
        assertEquals(2, articleCollections(notes,"프로젝트").first { it.id == "a" }.count)
    }
}
