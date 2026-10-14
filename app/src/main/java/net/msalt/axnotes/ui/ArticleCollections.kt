package net.msalt.axnotes.ui

import net.msalt.axnotes.data.Article

/** Only publisher-authored metadata is used; article titles never infer taxonomy. */
data class ArticleCollection(val id: String, val title: String, val count: Int)
fun articleCollections(articles: List<Article>, type: String): List<ArticleCollection> = articles
    .filter { collectionId(it, type) != null }
    .groupBy { collectionId(it, type)!! }
    .map { (id, notes) -> ArticleCollection(id, collectionTitle(notes.first(), type) ?: id, notes.size) }
    .sortedBy { it.title }
fun articlesInCollection(articles: List<Article>, type: String, id: String?): List<Article> {
    if(type == "전체" || id == null) return articles
    return articles.filter { collectionId(it, type) == id }
        .sortedWith(compareBy<Article> { if(type == "시리즈") it.seriesOrder ?: Int.MAX_VALUE else it.projectOrder ?: Int.MAX_VALUE }
            .thenBy { it.publishedAt }.thenBy { it.id })
}
fun Article.collectionLabel(): String? = when {
    seriesId != null -> "시리즈 · ${seriesTitle ?: seriesId}"
    projectId != null -> "프로젝트 · ${projectTitle ?: projectId}"
    else -> null
}
private fun collectionId(article: Article, type: String) = when(type) {
    "시리즈" -> article.seriesId
    "프로젝트" -> article.projectId
    else -> null
}
private fun collectionTitle(article: Article, type: String) = when(type) {
    "시리즈" -> article.seriesTitle
    "프로젝트" -> article.projectTitle
    else -> null
}
