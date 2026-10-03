package com.anchor.recovery.core.content

import kotlinx.serialization.Serializable

/**
 * 知识库分类，字段与 assets/content/index.json 的 categories 数组一一对应。
 */
@Serializable
data class Category(
    val key: String,
    val dir: String = "",
    val title: String,
    val description: String = "",
    val count: Int = 0,
    val articleIds: List<String> = emptyList(),
)
