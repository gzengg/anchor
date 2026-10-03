package com.anchor.recovery.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anchor.recovery.R
import com.anchor.recovery.core.content.Article
import com.anchor.recovery.core.content.MarkdownLite
import com.anchor.recovery.core.content.MdBlock
import com.anchor.recovery.data.content.ContentRepository
import com.anchor.recovery.data.content.ContentSnapshot
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorButtonStyle
import com.anchor.recovery.ui.components.AnchorHairline
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/**
 * F6 阅读器：极简 Markdown 渲染 + 来源链接（CustomTabs）+ 页底免责声明卡。
 *
 * 版式为 iOS 阅读面：整页白底（色值取 cardBackground），正文左右 20dp 留白、段落间距 16dp，
 * 行长与行距以长文阅读为先，不做紧凑排布。
 */
@Composable
fun ArticleScreen(
    articleId: String,
    content: ContentRepository,
    modifier: Modifier = Modifier,
) {
    var snapshot by remember { mutableStateOf<ContentSnapshot?>(null) }
    val openUrl = rememberUrlOpener()

    LaunchedEffect(content, articleId) { snapshot = content.snapshot() }

    val article = snapshot?.library?.article(articleId)
    val blocks = remember(article) { article?.let { MarkdownLite.parse(it.bodyMarkdown) } ?: emptyList() }

    val scrollState = rememberScrollState()
    // 文章标题本身就很长，不往页内放大标题；顶栏标题常显，滚动状态交出去以便统一联动。
    PublishAnchorNavBar(scrollState, hasLargeTitle = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.cardBackground)
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            snapshot == null -> Text(
                text = stringResource(R.string.article_loading),
                style = AnchorType.body,
                color = AnchorTheme.colors.labelSecondary,
            )

            article == null -> {
                // 错误原因由 data 层以 `@StringRes + args` 回传，这里才渲染成文案。
                val snap = snapshot
                val errorText = snap?.errorRes
                    ?.let { stringResource(it, *snap.errorArgs.toTypedArray()) }
                    .orEmpty()
                Text(
                    text = stringResource(R.string.article_not_found, articleId, errorText),
                    style = AnchorType.body,
                    color = AnchorTheme.colors.danger,
                )
            }

            else -> ArticleBody(article = article, blocks = blocks, onOpenSource = openUrl)
        }
    }
}

/** 正文整体包在 [SelectionContainer] 里：长按即可选中、复制原文。 */
@Composable
private fun ArticleBody(
    article: Article,
    blocks: List<MdBlock>,
    onOpenSource: (String) -> Unit,
) {
    SelectionContainer {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ArticleHeader(article = article)
            AnchorHairline(inset = 0.dp)
            MarkdownBlocks(blocks = blocks)
            AnchorHairline(inset = 0.dp)
            SourceSection(article = article, onOpenSource = onOpenSource)
        }
    }

    DisclaimerCard()
}

@Composable
private fun ArticleHeader(article: Article) {
    val colors = AnchorTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CredibilityBadge(credibility = article.credibility)
            Text(
                text = article.categoryDir.ifBlank { article.category },
                style = AnchorType.footnote,
                color = colors.labelSecondary,
            )
        }

        Text(
            text = article.title,
            style = AnchorType.title2,
            color = colors.label,
        )

        val meta = listOf(article.author, article.date).filter { it.isNotBlank() }.joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = AnchorType.footnote,
                color = colors.labelSecondary,
            )
        }

        if (article.summary.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.fill)
                    .padding(14.dp),
            ) {
                Text(
                    text = article.summary,
                    style = AnchorType.callout,
                    color = colors.label,
                )
            }
        }
    }
}

/** 站外来源链接：显式提示“会离开应用”。 */
@Composable
private fun SourceSection(article: Article, onOpenSource: (String) -> Unit) {
    val colors = AnchorTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.article_source_title),
            style = AnchorType.subheadlineSemibold,
            color = colors.label,
        )
        AnchorButton(
            text = article.source,
            onClick = { onOpenSource(article.source) },
            style = AnchorButtonStyle.Plain,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.article_source_hint),
            style = AnchorType.footnote,
            color = colors.labelSecondary,
        )
    }
}
