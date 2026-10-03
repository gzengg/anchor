package com.anchor.recovery.core.content

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ContentIndexTest {

    private val sampleArticles = """
        [
          {
            "id": "science-001",
            "category": "science",
            "categoryDir": "01-science",
            "title": "示例文章",
            "source": "https://example.org/paper",
            "author": "作者",
            "date": "2020",
            "tags": ["标签"],
            "credibility": "高",
            "credibilityRaw": "高",
            "summary": "摘要",
            "bodyMarkdown": "## 标题\n正文"
          }
        ]
    """.trimIndent()

    private val sampleIndex = """
        {
          "version": 1,
          "total": 1,
          "categories": [
            {
              "key": "science",
              "dir": "01-science",
              "title": "科学研究",
              "description": "机制研究",
              "count": 1,
              "articleIds": ["science-001"]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `解析样例文章与索引`() {
        val library = ContentLibrary.parse(sampleArticles, sampleIndex)
        val article = assertNotNull(library.article("science-001"))
        assertEquals("示例文章", article.title)
        assertEquals(Credibility.HIGH, article.credibility)
        assertEquals(listOf("标签"), article.tags)
        assertEquals(1, library.index.categories.size)
        assertEquals("科学研究", library.category("science")?.title)
        assertEquals(listOf("science-001"), library.orderedArticles().map { it.id })
    }

    @Test
    fun `未知可信度兜底为低而非崩溃`() {
        val broken = sampleArticles.replace("\"credibility\": \"高\"", "\"credibility\": \"极高\"")
        val library = ContentLibrary.parse(broken, sampleIndex)
        assertEquals(Credibility.LOW, assertNotNull(library.article("science-001")).credibility)
        assertEquals(Credibility.LOW, Credibility.fromRaw(null))
        assertEquals(Credibility.MEDIUM, Credibility.fromRaw("中"))
    }

    @Test
    fun `索引计数不一致时抛出内容格式异常`() {
        val brokenIndex = sampleIndex.replace("\"total\": 1", "\"total\": 2")
        assertFailsWith<ContentFormatException> {
            ContentLibrary.parse(sampleArticles, brokenIndex)
        }

        val danglingIndex = sampleIndex.replace("\"science-001\"", "\"science-999\"")
        assertFailsWith<ContentFormatException> {
            ContentLibrary.parse(sampleArticles, danglingIndex)
        }
    }

    @Test
    fun `检索命中标题摘要标签与正文`() {
        val library = ContentLibrary.parse(sampleArticles, sampleIndex)
        assertEquals(1, library.search("示例").size)
        assertEquals(1, library.search("标签").size)
        assertEquals(1, library.search("正文").size)
        assertEquals(0, library.search("不存在的关键词").size)
    }

    /**
     * 真实内容自检：直接读取仓库内内容管线的产物，保证 71 篇、字段完整、分类计数自洽。
     */
    @Test
    fun `真实内容包含71篇文章且字段完整`() {
        val contentDir = findContentDir()
        val library = ContentLibrary.parse(
            File(contentDir, "articles.json").readText(Charsets.UTF_8),
            File(contentDir, "index.json").readText(Charsets.UTF_8),
        )

        assertEquals(71, library.articles.size, "内容管线应产出 71 篇文章")
        assertEquals(71, library.index.total)
        assertEquals(6, library.index.categories.size)

        val allowedCredibility = setOf(Credibility.HIGH, Credibility.MEDIUM, Credibility.LOW)
        for (article in library.articles) {
            assertTrue(article.id.isNotBlank(), "文章 id 为空")
            assertTrue(article.title.isNotBlank(), "${article.id} 标题为空")
            assertTrue(article.summary.isNotBlank(), "${article.id} 摘要为空")
            assertTrue(article.bodyMarkdown.isNotBlank(), "${article.id} 正文为空")
            assertTrue(article.tags.isNotEmpty(), "${article.id} 标签为空")
            assertTrue(
                article.source.startsWith("https://") || article.source.startsWith("http://"),
                "${article.id} 来源不是 URL",
            )
            assertTrue(article.credibility in allowedCredibility, "${article.id} 可信度非法")
        }

        assertEquals(71, library.orderedArticles().size, "索引应覆盖全部文章")
        assertEquals(
            12,
            library.articlesOf("science").size,
            "science 分类应有 12 篇",
        )
        assertEquals(0, library.articles.count { it.credibility == Credibility.HIGH } - 47)
    }

    private fun findContentDir(): File {
        val relative = "app/src/main/assets/content"
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, relative)
            if (File(candidate, "articles.json").isFile) return candidate
            dir = dir.parentFile
        }
        error("未能从 ${System.getProperty("user.dir")} 向上找到 $relative")
    }
}
