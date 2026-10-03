package com.anchor.recovery.ui.library

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.anchor.recovery.core.content.Credibility
import com.anchor.recovery.core.content.MdBlock
import com.anchor.recovery.core.content.MdSpan

/** 可信度徽章配色：高=绿、中=黄、低=灰（与理念一致性要求一致）。 */
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
            text = "可信度 ${credibility.label}",
            style = MaterialTheme.typography.labelSmall,
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "来源",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        articleIds.forEach { id ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(4.dp),
                onClick = { onOpenArticle(id) },
            ) {
                Text(
                    text = id,
                    style = MaterialTheme.typography.labelSmall,
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
    text: String = DEFAULT_DISCLAIMER,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "免责声明", style = MaterialTheme.typography.labelLarge)
            Text(text = text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

const val DEFAULT_DISCLAIMER: String =
    "本内容整理自公开来源，仅供科普与自助参考，不构成诊断或治疗建议。" +
        "若你正处于明显痛苦中，请咨询专业医生或当地心理援助热线。"

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

/** 把极简 Markdown 块渲染成 Compose 组件。 */
@Composable
fun MarkdownBlocks(blocks: List<MdBlock>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = block.text,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.SemiBold,
                )

                is MdBlock.Paragraph -> Text(
                    text = block.spans.toAnnotatedString(),
                    style = MaterialTheme.typography.bodyMedium,
                )

                is MdBlock.Bullets -> Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = item.spans.toAnnotatedString(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                is MdBlock.Quote -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    Text(
                        text = block.spans.toAnnotatedString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is MdBlock.Table -> Column(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = block.header.joinToString(separator = " | "),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = block.rows.joinToString(separator = "\n") {
                            it.joinToString(separator = " | ")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
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
