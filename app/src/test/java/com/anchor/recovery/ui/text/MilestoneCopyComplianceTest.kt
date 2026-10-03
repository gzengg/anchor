package com.anchor.recovery.ui.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.core.streak.RebootFramework
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 里程碑文案合规：里程碑是自我记录的参照点，不是医学疗效承诺。
 *
 * 这条守卫原本在 :core（文案在那里），文案外移到资源后跟着搬到这里。
 */
@RunWith(RobolectricTestRunner::class)
class MilestoneCopyComplianceTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val bannedWords = listOf("保证", "一定", "必然", "已恢复", "已经恢复", "彻底")

    private val negation = Regex("(不是|不代表|并非|并不)[^。！？]*(痊愈|治愈)")

    @Test
    fun `里程碑文案不承诺疗效`() {
        RebootFramework.milestones.forEach { milestone ->
            val text = context.getString(milestoneTitleRes(milestone.days))

            assertTrue(text.isNotBlank(), "${milestone.days} 天徽章名为空")
            bannedWords.forEach { word ->
                assertFalse(text.contains(word), "里程碑文案不得包含「$word」：$text")
            }
            listOf("痊愈", "治愈").forEach { word ->
                if (text.contains(word)) {
                    assertTrue(negation.containsMatchIn(text), "「$word」只能出现在否定语境中：$text")
                }
            }
        }
    }
}
