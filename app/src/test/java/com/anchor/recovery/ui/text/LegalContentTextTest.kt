package com.anchor.recovery.ui.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.R
import com.anchor.recovery.core.content.Credibility
import com.anchor.recovery.core.legal.Disclaimer
import com.anchor.recovery.core.legal.DisclaimerParagraph
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 免责声明 / 可信度 / 内容错误文案的映射完整性。
 *
 * :core 只给稳定 id（[DisclaimerParagraph]、[Credibility]），文案在 `strings_legal.xml`。
 * 这里断言每个 id 都有非空资源、格式串的参数够用、渲染完不残留 `%`；将来在 :core 新增
 * 段落或可信度等级时，这里会先失败，而不是界面上出现空白或崩溃。
 *
 * 原先写在 `:core` `DisclaimerTest` 的正文合规断言（必备告知、不得出现绝对化功效宣称）
 * 也随文案一起搬到本文件。
 */
@RunWith(RobolectricTestRunner::class)
class LegalContentTextTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** 只读展示、不带参数的资源；渲染后不应残留 `%`。 */
    private val plainRes: List<Int> = listOf(
        R.string.legal_disclaimer_title,
        R.string.legal_disclaimer_ack_label,
        R.string.legal_credibility_high,
        R.string.legal_credibility_medium,
        R.string.legal_credibility_low,
    )

    /** 需要异常说明作为 `%1$s` 的内容错误文案。 */
    private val contentErrorArgs: Map<Int, List<Any>> = mapOf(
        R.string.legal_content_load_failed to listOf("boom"),
        R.string.legal_content_format_error to listOf("boom"),
    )

    @Test
    fun `免责段落顺序与枚举一致 且隐私段可单独取用`() {
        assertEquals(DisclaimerParagraph.entries.toList(), Disclaimer.paragraphs)
        assertTrue(Disclaimer.PRIVACY_PARAGRAPH in Disclaimer.paragraphs)
    }

    @Test
    fun `每一段声明都能渲染出非空文案 且无残留占位符`() {
        Disclaimer.paragraphs.forEach { paragraph ->
            val text = context.getString(disclaimerParagraphRes(paragraph))

            assertTrue(text.isNotBlank(), "$paragraph 文案为空")
            assertFalse(text.contains("%"), "$paragraph 有未替换的占位符：$text")
        }
    }

    @Test
    fun `固定文案都能渲染出非空文案 且无残留占位符`() {
        plainRes.forEach { res ->
            val text = context.getString(res)

            assertTrue(text.isNotBlank(), "资源 $res 文案为空")
            assertFalse(text.contains("%"), "资源 $res 有未替换的占位符：$text")
        }
    }

    @Test
    fun `每个可信度等级映射到独立文案`() {
        val rendered = Credibility.entries.map { context.getString(credibilityRes(it)) }

        assertEquals(Credibility.entries.size, rendered.toSet().size, "可信度等级文案重复：$rendered")
    }

    @Test
    fun `内容错误文案参数够用 渲染后不残留占位符`() {
        contentErrorArgs.forEach { (res, args) ->
            assertTrue(
                context.getString(res).contains("%1\$s"),
                "资源 $res 应保留 %1\$s 以承载异常说明",
            )

            val text = context.getString(res, *args.toTypedArray())

            assertTrue(text.isNotBlank(), "资源 $res 文案为空")
            assertFalse(text.contains("%"), "资源 $res 有未替换的占位符：$text")
        }
    }

    @Test
    fun `声明正文覆盖必备告知`() {
        val text = Disclaimer.paragraphs.joinToString("\n") { context.getString(disclaimerParagraphRes(it)) }

        assertTrue(text.contains("不是医疗器械"), "必须说明不是医疗器械")
        assertTrue(text.contains("不提供诊断"), "必须说明不提供诊断")
        assertTrue(text.contains("只保存在你自己的手机上"), "必须说明数据仅存本机")
        assertTrue(Disclaimer.paragraphs.size >= 4)
    }

    @Test
    fun `声明正文不含绝对化功效宣称`() {
        val text = Disclaimer.paragraphs.joinToString("\n") { context.getString(disclaimerParagraphRes(it)) }

        listOf("治愈", "治疗成瘾", "一定", "保证").forEach { banned ->
            assertFalse(text.contains(banned), "声明不得出现「$banned」")
        }
    }
}
