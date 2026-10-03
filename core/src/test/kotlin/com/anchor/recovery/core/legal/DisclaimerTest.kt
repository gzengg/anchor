package com.anchor.recovery.core.legal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 免责声明门禁（提示词 §5.1：首启必须同意；声明更新后强制重读）。
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
    fun `声明正文覆盖必备告知`() {
        val text = Disclaimer.paragraphs.joinToString("\n")

        assertTrue(text.contains("不是医疗器械"), "必须说明不是医疗器械")
        assertTrue(text.contains("不提供诊断"), "必须说明不提供诊断")
        assertTrue(text.contains("只保存在你自己的手机上"), "必须说明数据仅存本机")
        assertTrue(Disclaimer.paragraphs.size >= 4)
    }

    @Test
    fun `声明正文不含绝对化功效宣称`() {
        val text = Disclaimer.paragraphs.joinToString("\n")
        listOf("治愈", "治疗成瘾", "一定", "保证").forEach { banned ->
            assertFalse(text.contains(banned), "声明不得出现「$banned」")
        }
    }
}
