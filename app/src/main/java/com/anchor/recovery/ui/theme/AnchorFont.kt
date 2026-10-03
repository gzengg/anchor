package com.anchor.recovery.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.anchor.recovery.R

/*
 * 打包字体：HarmonyOS Sans SC（Huawei Device Co., Ltd 2021，字库：汉仪）。
 * 许可是华为《HarmonyOS Sans Fonts License Agreement》，免费可用于商业产品；
 * 版权与许可全文见 app/src/main/assets/licenses/HarmonyOS-Sans.txt。
 *
 * 为什么不是 SF Pro / 苹方：Apple 专有字体，不能随应用再分发。v0.1 因此用系统默认字体，
 * 结果在小米/一加/OPPO 等 ROM 上会被各家定制中文黑体（MiSans、OPPO Sans…）替换，
 * 每个用户看到的字形都不一样。打包一款免费商用的近苹方黑体，观感才在所有机型上一致。
 *
 * 字重映射：HarmonyOS Sans SC 只有 400/500/700，没有 600。
 *   - regular.ttf → W400（Normal）
 *   - medium.ttf  → 在 FontFamily 里声明为 W600：AnchorType 的 SemiBold 槽位正好命中。
 *     又因为 Compose 的选型规则是「优先取不低于请求值的最近字重」，M3 里大量 W500 槽位
 *     （Button 的 labelLarge、TextField 的 bodyLarge 等）也会落到这个 500 字面的文件上，
 *     与改动前的观感一致。
 *   - bold.ttf    → W700（Bold）
 * 不额外声明 W500 条目：同一个 FontFamily 里不允许存在字重与样式完全相同的两个 Font。
 *
 * 体积：全量字体每个约 7.8MB，三个字重直接打包就是 23MB。这里用 fontTools 子集化到
 * GB2312（6763 常用汉字）+ 应用实际出现的字符 + 常用符号区间，每个约 1.6MB，合计 4.9MB。
 * 子集之外的字符（用户在笔记/标签里输入的生僻字）由 Android 系统字体 fallback 兜底，
 * 不会出现方框。
 */
val AnchorFontFamily = FontFamily(
    Font(R.font.harmonyos_sans_sc_regular, FontWeight.Normal),
    Font(R.font.harmonyos_sans_sc_medium, FontWeight.SemiBold),
    Font(R.font.harmonyos_sans_sc_bold, FontWeight.Bold),
)
