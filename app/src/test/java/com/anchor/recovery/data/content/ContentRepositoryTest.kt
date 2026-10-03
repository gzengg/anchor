package com.anchor.recovery.data.content

import com.anchor.recovery.core.phase.WithdrawalPhaseCatalog
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 离线知识库的接线自检：仓库里真实的 assets JSON 能被 [ContentRepository.fromStrings] 解析，
 * 且阶段文案的来源 id 都能在这份内容里找到（UI 上的来源 chip 不会点开空页面）。
 */
class ContentRepositoryTest {

    private val articlesJson = assetFile("articles.json").readText(Charsets.UTF_8)
    private val indexJson = assetFile("index.json").readText(Charsets.UTF_8)

    private val snapshot = ContentRepository.fromStrings(articlesJson, indexJson)

    @Test
    fun `assets 里的真实内容可以被解析`() {
        assertNull(snapshot.error)
        val library = assertNotNull(snapshot.library, "知识库未加载：${snapshot.error}")
        assertEquals(71, library.articles.size)
        assertEquals(6, library.index.categories.size)
    }

    @Test
    fun `分类筛选与检索在真实内容上可用`() {
        val library = assertNotNull(snapshot.library, "知识库未加载：${snapshot.error}")

        library.index.categories.forEach { category ->
            assertEquals(
                category.count,
                library.articlesOf(category.key).size,
                "分类 ${category.key} 的文章数与索引计数不一致",
            )
        }

        assertTrue(library.search("多巴胺").isNotEmpty(), "检索「多巴胺」应命中文章")
        // 空关键词的语义是“不过滤”（UI 在调用前已单独处理空查询，这里把它固定下来）
        assertEquals(library.articles.size, library.search("   ").size, "空关键词应返回全部文章")
    }

    @Test
    fun `阶段文案的来源 id 都能在知识库里找到`() {
        val library = assertNotNull(snapshot.library, "知识库未加载：${snapshot.error}")
        val missing = WithdrawalPhaseCatalog.phases
            .flatMap { phase -> phase.sourceArticleIds.map { phase.id to it } }
            .filter { (_, id) -> library.article(id) == null }

        assertEquals(emptyList<Pair<String, String>>(), missing)
    }

    @Test
    fun `内容格式错误时降级为错误快照而不是抛异常`() {
        val broken = ContentRepository.fromStrings("{ not json", indexJson)
        assertNull(broken.library)
        assertNotNull(broken.error)
        assertTrue(broken.error!!.isNotBlank())
    }

    private fun assetFile(name: String): File {
        val file = File("src/main/assets/content/$name")
        check(file.isFile) { "缺少内容资产：${file.absolutePath}" }
        return file
    }
}
