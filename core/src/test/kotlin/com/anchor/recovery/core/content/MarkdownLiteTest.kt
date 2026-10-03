package com.anchor.recovery.core.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownLiteTest {

    @Test
    fun `解析标题与段落`() {
        val blocks = MarkdownLite.parse("## 核心要点\n\n第一句话。\n第二句话。\n\n### 小标题")

        assertEquals(3, blocks.size)
        assertEquals(MdBlock.Heading(2, "核心要点"), blocks[0])
        assertEquals(
            MdBlock.Paragraph(listOf(MdSpan("第一句话。 第二句话。"))),
            blocks[1],
        )
        assertEquals(MdBlock.Heading(3, "小标题"), blocks[2])
    }

    @Test
    fun `解析列表与嵌套层级`() {
        val blocks = MarkdownLite.parse(
            """
            - 顶层一项
            - 顶层二项
              - 嵌套项
            """.trimIndent(),
        )

        assertEquals(1, blocks.size)
        val bullets = blocks[0] as MdBlock.Bullets
        assertEquals(3, bullets.items.size)
        assertEquals(0, bullets.items[0].level)
        assertEquals("顶层二项", bullets.items[1].spans.single().text)
        assertEquals(1, bullets.items[2].level)
    }

    @Test
    fun `有序列表也算列表`() {
        val blocks = MarkdownLite.parse("1. 第一步\n2. 第二步")

        val bullets = blocks.single() as MdBlock.Bullets
        assertEquals(listOf("第一步", "第二步"), bullets.items.map { it.spans.single().text })
    }

    @Test
    fun `加粗被拆成独立片段`() {
        val blocks = MarkdownLite.parse("- 注意 **重点** 之后")

        val item = (blocks.single() as MdBlock.Bullets).items.single()
        assertEquals(listOf("注意", "重点", "之后"), item.spans.map { it.text })
        assertEquals(listOf(false, true, false), item.spans.map { it.bold })
    }

    @Test
    fun `连续引用行合并为一段引用`() {
        val blocks = MarkdownLite.parse("> 第一行\n> 第二行\n\n正文")
        val quote = blocks[0] as MdBlock.Quote
        assertEquals("第一行 第二行", quote.spans.single().text)
        assertTrue(blocks[1] is MdBlock.Paragraph)
    }

    @Test
    fun `解析表格并识别分隔行`() {
        val blocks = MarkdownLite.parse(
            """
            | 项目 | 说明 |
            | --- | --- |
            | 证据等级 | 高 |
            | 样本 | 104 |
            """.trimIndent(),
        )

        val table = blocks.single() as MdBlock.Table
        assertEquals(listOf("项目", "说明"), table.header)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("证据等级", "高"), table.rows[0])
        assertEquals(listOf("样本", "104"), table.rows[1])
    }

    @Test
    fun `没有分隔行的竖线文本降级为普通段落`() {
        val blocks = MarkdownLite.parse("| 只有一个 | 竖线行 |")

        // 首行即分隔行判断失败时按段落处理，不崩也不丢内容
        assertTrue(blocks.isNotEmpty())
        assertTrue(blocks.all { it !is MdBlock.Table })
    }

    @Test
    fun `链接只保留文本且清掉 HTML 标签`() {
        val blocks = MarkdownLite.parse("- 参见 [Kraus 2018](https://example.org/a) <span>补充</span>")

        val spans = (blocks.single() as MdBlock.Bullets).items.single().spans
        assertEquals("参见 Kraus 2018 补充", spans.single().text)
    }

    @Test
    fun `未闭合的加粗标记不会抛异常`() {
        val blocks = MarkdownLite.parse("**孤立的加粗标记")

        val paragraph = blocks.single() as MdBlock.Paragraph
        assertEquals("孤立的加粗标记", paragraph.spans.single().text)
        assertEquals(false, paragraph.spans.single().bold)
    }

    @Test
    fun `空输入与纯空白返回空列表`() {
        assertEquals(emptyList(), MarkdownLite.parse(""))
        assertEquals(emptyList(), MarkdownLite.parse("\n\n   \n\t\n"))
    }

    @Test
    fun `CRLF 换行与 nbsp 实体被归一化`() {
        val blocks = MarkdownLite.parse("# 标题\r\n\r\n正文&nbsp;续")

        assertEquals(MdBlock.Heading(1, "标题"), blocks[0])
        assertEquals("正文 续", (blocks[1] as MdBlock.Paragraph).spans.single().text)
    }

    @Test
    fun `真实文章正文都能被解析成非空块`() {
        val sample = """
            ## 核心内容
            - 要点一
              - 子要点
            > 引用一句

            | 维度 | 说明 |
            | --- | --- |
            | 控制 | 失控 |
        """.trimIndent()

        val blocks = MarkdownLite.parse(sample)
        assertEquals(4, blocks.size)
        assertTrue(blocks.none { it is MdBlock.Paragraph && it.spans.isEmpty() })
    }
}
