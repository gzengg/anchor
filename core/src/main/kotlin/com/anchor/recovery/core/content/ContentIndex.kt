package com.anchor.recovery.core.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * assets/content/index.json 的模型。
 */
@Serializable
data class ContentIndex(
    val version: Int = 1,
    val total: Int = 0,
    val categories: List<Category> = emptyList(),
)

/** 内容数据自检失败。 */
class ContentFormatException(message: String) : IllegalStateException(message)

/**
 * 内存中的离线知识库：把 articles.json 与 index.json 组装成可查询的只读结构。
 *
 * [parse] 会做严格自检（总数一致、每个分类计数与 id 列表一致、所有 id 可解析），
 * 让内容管线的错误在启动时就暴露，而不是渲染到一半崩溃。
 */
class ContentLibrary(
    val articles: List<Article>,
    val index: ContentIndex,
) {
    private val articlesById: Map<String, Article> = articles.associateBy { it.id }
    private val articleOrder: Map<String, Int> = articles.withIndex().associate { (i, a) -> a.id to i }

    fun article(id: String): Article? = articlesById[id]

    fun category(key: String): Category? = index.categories.firstOrNull { it.key == key }

    fun articlesOf(key: String): List<Article> =
        index.categories.firstOrNull { it.key == key }
            ?.articleIds
            ?.mapNotNull { articlesById[it] }
            ?: emptyList()

    /** 标题/摘要/标签/正文的朴素子串检索（离线、无索引，71 篇规模足够）。 */
    fun search(query: String): List<Article> {
        val needle = query.trim()
        if (needle.isEmpty()) return articles
        return articles.filter { article ->
            article.title.contains(needle, ignoreCase = true) ||
                article.summary.contains(needle, ignoreCase = true) ||
                article.tags.any { it.contains(needle, ignoreCase = true) } ||
                article.bodyMarkdown.contains(needle, ignoreCase = true)
        }
    }

    /** 按 index.json 的定义顺序拍平全部文章。 */
    fun orderedArticles(): List<Article> =
        index.categories.flatMap { articlesOf(it.key) }
            .ifEmpty { articles.sortedBy { articleOrder[it.id] ?: Int.MAX_VALUE } }

    companion object {
        val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = false
        }

        fun parse(
            articlesJson: String,
            indexJson: String,
            decoder: Json = json,
        ): ContentLibrary {
            val articles = try {
                decoder.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(Article.serializer()),
                    articlesJson,
                )
            } catch (error: Exception) {
                throw ContentFormatException("articles.json 解析失败：${error.message}")
            }
            val index = try {
                decoder.decodeFromString(ContentIndex.serializer(), indexJson)
            } catch (error: Exception) {
                throw ContentFormatException("index.json 解析失败：${error.message}")
            }
            return validate(ContentLibrary(articles, index))
        }

        internal fun validate(library: ContentLibrary): ContentLibrary {
            val articles = library.articles
            val index = library.index

            if (index.total != articles.size) {
                throw ContentFormatException(
                    "index.total=${index.total} 与文章数 ${articles.size} 不一致",
                )
            }

            val duplicated = articles.groupBy { it.id }.filterValues { it.size > 1 }.keys
            if (duplicated.isNotEmpty()) {
                throw ContentFormatException("文章 id 重复：$duplicated")
            }

            var counted = 0
            for (category in index.categories) {
                if (category.count != category.articleIds.size) {
                    throw ContentFormatException(
                        "分类 ${category.key} 的 count=${category.count} 与 id 数 " +
                            "${category.articleIds.size} 不一致",
                    )
                }
                for (id in category.articleIds) {
                    if (library.article(id) == null) {
                        throw ContentFormatException("分类 ${category.key} 引用了不存在的文章 id=$id")
                    }
                }
                counted += category.count
            }
            if (counted != articles.size) {
                throw ContentFormatException("分类计数合计 $counted 与文章数 ${articles.size} 不一致")
            }

            for (article in articles) {
                if (article.id.isBlank() || article.title.isBlank() || article.source.isBlank()) {
                    throw ContentFormatException("文章字段缺失（id=${article.id}）")
                }
                if (!article.source.startsWith("http://") && !article.source.startsWith("https://")) {
                    throw ContentFormatException("文章 ${article.id} 的 source 不是 http(s) URL")
                }
            }
            return library
        }
    }
}
