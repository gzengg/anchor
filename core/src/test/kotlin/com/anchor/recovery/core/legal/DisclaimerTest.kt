package com.anchor.recovery.core.legal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 免责声明门禁（提示词 §5.1：首启必须同意；声明更新后强制重读）。
 *
 * 正文文案外移到 `:app` 之后，本测试只守卫门禁逻辑与段落结构；正文内容的合规断言
 * （必备告知、不得出现绝对化功效宣称）跟着文案搬到 `LegalContentTextTest`。
 */
class DisclaimerTest {

    @Test
    fun `首启未同意时必须先阅读声明`() {
        assertTrue(Disclaimer.requiresAcknowledgement(onboardingDone = false, acknowledgedVersion = 0))
        assertTrue(Disclaimer.requiresAcknowledgement(onboardingDone = false, acknowledgedVersion = Disclaimer.VERSION))
    }

    @Test
    fun `已同意且版本一致时不再拦截`() {
        assertFalse(
            Disclaimer.requiresAcknowledgement(
                onboardingDone = true,
                acknowledgedVersion = Disclaimer.VERSION,
            ),
        )
    }

    @Test
    fun `声明版本升级后强制重读`() {
        assertTrue(
            Disclaimer.requiresAcknowledgement(
                onboardingDone = true,
                acknowledgedVersion = Disclaimer.VERSION - 1,
            ),
        )
    }

    @Test
    fun `版本号必须为正`() {
        assertTrue(Disclaimer.VERSION >= 1)
    }

    @Test
    fun `声明段落顺序固定 且隐私段可单独取用`() {
        assertEquals(DisclaimerParagraph.entries.toList(), Disclaimer.paragraphs)
        assertTrue(Disclaimer.PRIVACY_PARAGRAPH in Disclaimer.paragraphs)
        assertTrue(Disclaimer.paragraphs.size >= 4)
    }
}
