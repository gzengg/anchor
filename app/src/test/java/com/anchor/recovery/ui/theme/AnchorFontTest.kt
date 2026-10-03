package com.anchor.recovery.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import org.junit.Test
import kotlin.test.assertEquals

/*
 * 打包字体的接线回归测试。
 *
 * 字体漏接是「静默失败」：编译通过、单测全绿，在开发机上（Robolectric / 模拟器）也看不出异常，
 * 只有在小米、一加这类把系统中文黑体换成自家字体的 ROM 上才露出不一致。
 * 唯一能自动守住的就是「每个字阶槽位都指向 AnchorFontFamily」这一点。
 */
class AnchorFontTest {

    private val slots: Map<String, TextStyle> = mapOf(
        "largeTitle" to AnchorType.largeTitle,
        "title1" to AnchorType.title1,
        "title2" to AnchorType.title2,
        "title3" to AnchorType.title3,
        "headline" to AnchorType.headline,
        "body" to AnchorType.body,
        "callout" to AnchorType.callout,
        "subheadline" to AnchorType.subheadline,
        "subheadlineSemibold" to AnchorType.subheadlineSemibold,
        "footnote" to AnchorType.footnote,
        "footnoteSemibold" to AnchorType.footnoteSemibold,
        "caption1" to AnchorType.caption1,
        "caption2" to AnchorType.caption2,
    )

    @Test
    fun `AnchorType 每个槽位都使用打包字体`() {
        val missing = slots.filterValues { it.fontFamily != AnchorFontFamily }.keys
        assertEquals(emptySet(), missing, "以下槽位没接上打包字体，会退回系统字体：$missing")
    }

    @Test
    fun `copy 出来的派生槽位同样继承打包字体`() {
        assertEquals(AnchorFontFamily, AnchorType.subheadlineSemibold.fontFamily)
        assertEquals(AnchorFontFamily, AnchorType.footnoteSemibold.fontFamily)
    }

    /**
     * HarmonyOS Sans SC 只有 400/500/700 三档，没有 600。
     * medium.ttf 在 FontFamily 里被声明为 W600，这样一个文件同时接住 AnchorType 的
     * SemiBold 与 M3 里大量 W500 槽位（Compose 取「不低于请求值的最近字重」）。
     */
    @Test
    fun `打包字体覆盖 400 500 700 三档字重`() {
        val weights = (AnchorFontFamily as FontListFontFamily).fonts.map { it.weight }.toSet()
        assertEquals(setOf(FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold), weights)
    }
}
