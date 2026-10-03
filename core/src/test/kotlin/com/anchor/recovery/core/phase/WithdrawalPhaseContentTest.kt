package com.anchor.recovery.core.phase

import com.anchor.recovery.core.content.ContentLibrary
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 阶段文案 ↔ 真实内容的交叉校验。
 *
 * UI 上的每个来源 chip 都指向 `articles.json` 里的一个 id，如果这个 id 不存在，
 * 用户点开就是一个空页面——所以这条约束必须由测试而不是"写的时候小心点"来保证。
 */
class WithdrawalPhaseContentTest {

    private val library: ContentLibrary by lazy {
        val dir = findContentDir()
        ContentLibrary.parse(
            File(dir, "articles.json").readText(Charsets.UTF_8),
            File(dir, "index.json").readText(Charsets.UTF_8),
        )
    }

    @Test
    fun `阶段文案引用的每一篇文章都真实存在`() {
        val known = library.articles.map { it.id }.toSet()
        val dangling = mutableListOf<String>()

        WithdrawalPhaseCatalog.phases.forEach { phase ->
            phase.sourceArticleIds.forEach { id ->
                if (id !in known) dangling += "${phase.id} -> $id"
            }
        }

        assertEquals(emptyList(), dangling, "阶段文案引用了不存在的文章 id")
    }

    @Test
    fun `每个阶段至少引用一篇真实文章`() {
        WithdrawalPhaseCatalog.phases.forEach { phase ->
            assertTrue(
                phase.sourceArticleIds.isNotEmpty(),
                "${phase.id} 没有引用任何文章",
            )
            assertNotNull(
                library.article(assertNotNull(phase.sourceArticleIds.firstOrNull())),
                "${phase.id} 引用的文章无法解析",
            )
        }
    }

    @Test
    fun `时间线阶段引用的都是 05-cases 目录的文章`() {
        WithdrawalPhaseCatalog.phases.forEach { phase ->
            phase.sourceArticleIds.forEach { id ->
                val article = library.article(id)
                if (id.startsWith("cases-")) {
                    assertNotNull(article, "$id 不存在")
                    assertEquals(
                        "05-cases",
                        article.categoryDir,
                        "$id 应在 05-cases 目录下",
                    )
                }
            }
        }
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
