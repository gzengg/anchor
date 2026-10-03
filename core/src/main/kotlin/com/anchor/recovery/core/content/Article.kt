package com.anchor.recovery.core.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 一篇知识库文章，字段与 assets/content/articles.json 一一对应。
 */
@Serializable
data class Article(
    val id: String,
    val category: String,
    @SerialName("categoryDir") val categoryDir: String = "",
    val title: String,
    val source: String,
    val author: String = "",
    val date: String = "",
    val tags: List<String> = emptyList(),
    val credibility: Credibility = Credibility.LOW,
    /** 归一化前的原始可信度写法（例如「中-高」），仅用于追溯，不参与 UI。 */
    val credibilityRaw: String? = null,
    val summary: String = "",
    val bodyMarkdown: String = "",
)
