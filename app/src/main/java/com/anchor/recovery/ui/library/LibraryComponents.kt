package com.anchor.recovery.ui.library

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.recovery.R
import com.anchor.recovery.core.content.Credibility
import com.anchor.recovery.core.content.MdBlock
import com.anchor.recovery.core.content.MdSpan
import com.anchor.recovery.ui.components.AnchorClickableSurface
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/**
 * 可信度徽章配色：高=绿、中=黄、低=灰（与理念一致性要求一致）。
 *
 * 这三档底色要同时兜住白字/深字的对比度，所以不用 AnchorTheme 的 systemGreen（过亮、
 * 配白字只有约 2:1），按深浅各取一档固定色。
 */
private val CredibilityHighContainer = Color(0xFF2E7D32)
private val CredibilityMediumContainer = Color(0xFFF9A825)
private val CredibilityLowContainer = Color(0xFF757575)

@Composable
fun CredibilityBadge(credibility: Credibility, modifier: Modifier = Modifier) {
    val container = when (credibility) {
        Credibility.HIGH -> CredibilityHighContainer
        Credibility.MEDIUM -> CredibilityMediumContainer
        Credibility.LOW -> CredibilityLowContainer
    }
    val content = when (credibility) {
        Credibility.MEDIUM -> Color(0xFF3E2C00)
        else -> Color.White
    }

    Surface(
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = stringResource(R.string.library_credibility, credibility.label),
            style = AnchorType.caption1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** 每条阶段文案下方的来源文章 chip；点击跳到对应文章。 */
@Composable
fun SourceChips(
    articleIds: List<String>,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AnchorTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.library_source_label),
            style = AnchorType.footnote,
            color = colors.labelSecondary,
        )
        articleIds.forEach { id ->
            AnchorClickableSurface(
                onClick = { onOpenArticle(id) },
                color = colors.fill,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = id,
                    style = AnchorType.caption1,
                    color = colors.tint,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** 每页底部的免责声明卡（合规要求：内容页与问卷页都要有）。 */
@Composable
fun DisclaimerCard(
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val colors = AnchorTheme.colors
    // 未传 text 时用标准免责声明：原文是两句话连排（中间无分隔符），这里也直接拼接。
    val body = text ?: stringResource(R.string.library_disclaimer_content) +
        stringResource(R.string.library_disclaimer_help)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.fill)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.library_disclaimer_title),
            style = AnchorType.subheadlineSemibold,
            color = colors.label,
        )
        Text(text = body, style = AnchorType.footnote, color = colors.labelSecondary)
    }
}

/** 用 CustomTabs 打开外部链接；没有可用浏览器时退回普通 Intent。 */
@Composable
fun rememberUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { url: String -> context.openUrl(url) } }
}

fun Context.openUrl(url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, uri)
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

/** 正文行高放宽到 26sp（约 1.5 倍）：长文阅读比 17/22 的默认行距舒展，字号仍是 HIG body。 */
private val ReaderBody = AnchorType.body.copy(lineHeight = 26.sp)

/** 把极简 Markdown 块渲染成 Compose 组件。 */
@Composable
fun MarkdownBlocks(blocks: List<MdBlock>, modifier: Modifier = Modifier) {
    val colors = AnchorTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = block.text,
                    style = when (block.level) {
                        1 -> AnchorType.title2
                        2 -> AnchorType.title3
                        else -> AnchorType.headline
                    },
                    color = colors.label,
                )

                is MdBlock.Paragraph -> Text(
                    text = block.spans.toAnnotatedString(),
                    style = ReaderBody,
                    color = colors.label,
                )

                is MdBlock.Bullets -> Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    block.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (item.level * 16).dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (item.level == 0) "•" else "◦",
                                style = ReaderBody,
                                color = colors.labelSecondary,
                            )
                            Text(
                                text = item.spans.toAnnotatedString(),
                                style = ReaderBody,
                                color = colors.label,
                            )
                        }
                    }
                }

                is MdBlock.Quote -> Row(
                    // 竖线要跟引文一样高，得先让行高由内容决定（Row 默认不进 intrinsic 测量）。
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .background(colors.opaqueSeparator),
                    )
                    Text(
                        text = block.spans.toAnnotatedString(),
                        style = ReaderBody,
                        color = colors.labelSecondary,
                    )
                }

                is MdBlock.Table -> Column(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = block.header.joinToString(separator = " | "),
                        style = AnchorType.footnote,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.label,
                    )
                    Text(
                        text = block.rows.joinToString(separator = "\n") {
                            it.joinToString(separator = " | ")
                        },
                        style = AnchorType.footnote,
                        fontFamily = FontFamily.Monospace,
                        color = colors.labelSecondary,
                    )
                }
            }
        }
    }
}

fun List<MdSpan>.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    this@toAnnotatedString.forEach { span ->
        if (span.bold) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(span.text) }
        } else {
            append(span.text)
        }
    }
}
