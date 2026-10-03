package com.anchor.recovery.ui.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.R
import com.anchor.recovery.core.export.ImportField
import com.anchor.recovery.core.export.ImportRejection
import com.anchor.recovery.core.export.ImportResult
import com.anchor.recovery.core.streak.CheckInRejection
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `:core` 原因码 → 文案资源的映射完整性。
 *
 * 表里列出每个原因码期望的参数，并断言表与枚举一一对应：将来在 :core 新增原因码时，
 * 这里会先失败，而不是等到界面上出现空白或崩溃。
 */
@RunWith(RobolectricTestRunner::class)
class CoreTextTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val importArgs: Map<ImportRejection, List<Any>> = mapOf(
        ImportRejection.EMPTY_FILE to emptyList(),
        ImportRejection.NOT_ANCHOR_FILE to emptyList(),
        ImportRejection.MISSING_VERSION to emptyList(),
        ImportRejection.UNSUPPORTED_VERSION to listOf(99, 1),
        ImportRejection.PARSE_ERROR to listOf("格式异常"),
        ImportRejection.UNKNOWN_PARSE_ERROR to emptyList(),
        ImportRejection.DUPLICATE_DATES to listOf("2024-05-03"),
        ImportRejection.INVALID_FIELD to listOf(ImportField.CHECK_IN_TIME, "昨天下午"),
    )

    private val checkInArgs: Map<CheckInRejection, List<Any>> = mapOf(
        CheckInRejection.FUTURE_DATE to emptyList(),
        CheckInRejection.NOT_TODAY to emptyList(),
    )

    @Test
    fun `导入拒绝原因与参数表一一对应`() {
        assertEquals(ImportRejection.entries.toSet(), importArgs.keys)
    }

    @Test
    fun `每个导入拒绝原因都能渲染出完整文案`() {
        importArgs.forEach { (reason, args) ->
            val (res, textArgs) = importRejectionText(ImportResult.Rejected(reason, args))
            val text = context.getString(res, *textArgs.toTypedArray())

            assertTrue(text.isNotBlank(), "$reason 文案为空")
            assertFalse(text.contains("%"), "$reason 有未替换的占位符：$text")
        }
    }

    @Test
    fun `每个导入字段都有整句文案 界面不拼中文`() {
        ImportField.entries.forEach { field ->
            val res = importFieldMessageRes(field)
            val text = context.getString(res, "2024-13-01")

            assertTrue(text.isNotBlank(), "$field 文案为空")
            assertFalse(text.contains("%"), "$field 有未替换的占位符：$text")
        }
    }

    @Test
    fun `打卡拒绝原因与参数表一一对应`() {
        assertEquals(CheckInRejection.entries.toSet(), checkInArgs.keys)
    }

    @Test
    fun `每个打卡拒绝原因都能渲染出完整文案`() {
        checkInArgs.forEach { (reason, args) ->
            val text = context.getString(checkInRejectionRes(reason), *args.toTypedArray())

            assertTrue(text.isNotBlank(), "$reason 文案为空")
            assertFalse(text.contains("%"), "$reason 有未替换的占位符：$text")
        }
    }

    @Test
    fun `里程碑天数映射到徽章名 未知天数有兜底`() {
        val expected = mapOf(
            1 to R.string.milestone_days_title_1,
            7 to R.string.milestone_days_title_7,
            30 to R.string.milestone_days_title_30,
            60 to R.string.milestone_days_title_60,
            90 to R.string.milestone_days_title_90,
        )

        expected.forEach { (days, res) ->
            assertEquals(res, milestoneTitleRes(days), "$days 天应映射到固定徽章名")
            assertTrue(context.getString(res).isNotBlank())
        }
        assertEquals(R.string.milestone_days_title_unknown, milestoneTitleRes(42))
    }
}
