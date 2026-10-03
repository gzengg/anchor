package com.anchor.recovery.core.content

/** 行内片段。[bold] 为 true 表示被 `**…**` 包裹。 */
data class MdSpan(val text: String, val bold: Boolean = false)

/** 列表项。[level] 0 为顶层，1 为嵌套（原文用两个以上空格缩进表示）。 */
data class MdItem(val level: Int, val spans: List<MdSpan>)

/** 极简 Markdown 块级元素（F6 阅读器只需标题/段落/列表/引用/表格）。 */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock

    data class Paragraph(val spans: List<MdSpan>) : MdBlock

    data class Bullets(val items: List<MdItem>) : MdBlock

    data class Quote(val spans: List<MdSpan>) : MdBlock

    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
}

/**
 * 极简 Markdown 解析器：不引入第三方库，只覆盖内容库实际用到的语法。
 *
 * 支持：`#`–`######` 标题、`-`/`*`/有序号 列表（含缩进层级）、`>` 引用、`|` 表格、
 * `**加粗**`、`[文本](链接)`（只保留文本）、行内 `<html>` 标签与 `&nbsp;` 清理。
 * 未识别的语法一律按普通段落输出，绝不抛异常——阅读器宁可不完美也不能崩。
 */
object MarkdownLite {

    private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")
    private val bulletRegex = Regex("^(\\s*)(?:[-*+]|\\d+[.)])\\s+(.*)$")
    private val quoteRegex = Regex("^>\\s?(.*)$")
    private val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
    private val linkRegex = Regex("\\[(.*?)]\\(([^)]*)\\)")
    private val htmlTagRegex = Regex("<[^<>]{1,120}>")

    fun parse(markdown: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val normalized = markdown.replace("\r\n", "\n").replace("\r", "\n").replace("&nbsp;", " ")

        val paragraph = StringBuilder()
        val quote = StringBuilder()
        var bullets: MutableList<MdItem>? = null
        var tableRows: MutableList<List<String>>? = null

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks += MdBlock.Paragraph(parseInline(paragraph.toString()))
                paragraph.clear()
            }
        }

        fun flushQuote() {
            if (quote.isNotEmpty()) {
                blocks += MdBlock.Quote(parseInline(quote.toString()))
                quote.clear()
            }
        }

        fun flushBullets() {
            bullets?.takeIf { it.isNotEmpty() }?.let { blocks += MdBlock.Bullets(it.toList()) }
            bullets = null
        }

        fun flushTable() {
            tableRows?.let { rows ->
                if (rows.isNotEmpty()) {
                    val separatorIndex = rows.indexOfFirst { isSeparatorRow(it) }
                    if (separatorIndex > 0) {
                        blocks += MdBlock.Table(rows[separatorIndex - 1], rows.drop(separatorIndex + 1))
                    } else {
                        rows.forEach { cells ->
                            blocks += MdBlock.Paragraph(parseInline(cells.joinToString(" ")))
                        }
                    }
                }
            }
            tableRows = null
        }

        fun flushAll() {
            flushParagraph()
            flushQuote()
            flushBullets()
            flushTable()
        }

        for (rawLine in normalized.split('\n')) {
            val line = rawLine.trimEnd()
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                flushAll()
                continue
            }

            headingRegex.matchEntire(trimmed)?.let { match ->
                flushAll()
                val level = match.groupValues[1].length
                blocks += MdBlock.Heading(level, cleanInline(match.groupValues[2]))
                continue
            }

            if (trimmed.startsWith("|")) {
                flushParagraph()
                flushQuote()
                flushBullets()
                val rows = tableRows ?: mutableListOf<List<String>>().also { tableRows = it }
                rows += splitTableRow(trimmed)
                continue
            }
            if (tableRows != null) flushTable()

            bulletRegex.matchEntire(line)?.let { match ->
                flushParagraph()
                flushQuote()
                val indent = match.groupValues[1].replace("\t", "  ").length
                val items = bullets ?: mutableListOf<MdItem>().also { bullets = it }
                items += MdItem(level = if (indent >= 2) 1 else 0, spans = parseInline(match.groupValues[2]))
                continue
            }
            if (bullets != null) flushBullets()

            quoteRegex.matchEntire(trimmed)?.let { match ->
                flushParagraph()
                if (quote.isNotEmpty()) quote.append(' ')
                quote.append(match.groupValues[1].trim())
                continue
            }
            if (quote.isNotEmpty()) flushQuote()

            if (paragraph.isNotEmpty()) paragraph.append(' ')
            paragraph.append(trimmed)
        }
        flushAll()

        return blocks
    }

    /** 行内解析：拆出加粗片段，并清掉链接/HTML 等 Markdown 噪声。 */
    fun parseInline(text: String): List<MdSpan> {
        val spans = mutableListOf<MdSpan>()
        var index = 0
        boldRegex.findAll(text).forEach { match ->
            if (match.range.first > index) {
                spans += MdSpan(cleanInline(text.substring(index, match.range.first)))
            }
            spans += MdSpan(cleanInline(match.groupValues[1]), bold = true)
            index = match.range.last + 1
        }
        if (index < text.length) {
            spans += MdSpan(cleanInline(text.substring(index)))
        }
        return spans.filter { it.text.isNotEmpty() }
    }

    /** 去掉链接 URL、残留强调符与 HTML 标签，并压缩空白。 */
    private fun cleanInline(raw: String): String = raw
        .let { linkRegex.replace(it, "$1") }
        .replace("**", "")
        .replace(htmlTagRegex, "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun splitTableRow(line: String): List<String> {
        val trimmed = line.trim().removePrefix("|").removeSuffix("|")
        return trimmed.split('|').map { it.trim() }
    }

    private fun isSeparatorRow(cells: List<String>): Boolean =
        cells.isNotEmpty() && cells.all { cell ->
            cell.isNotEmpty() && cell.all { it == '-' || it == ':' || it == ' ' }
        }
}
