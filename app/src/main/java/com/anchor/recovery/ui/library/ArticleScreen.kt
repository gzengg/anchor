package com.anchor.recovery.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.anchor.recovery.core.content.Article
import com.anchor.recovery.core.content.MarkdownLite
import com.anchor.recovery.data.content.ContentRepository
import com.anchor.recovery.data.content.ContentSnapshot

/**
 * F6 阅读器：极简 Markdown 渲染 + 来源链接（CustomTabs）+ 页底免责声明卡。
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            snapshot == null -> Text(text = "正在加载…", style = MaterialTheme.typography.bodyMedium)
            article == null -> Text(
                text = "没找到这篇文章（id=$articleId）。${snapshot?.error.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )

            else -> ArticleBody(article = article, blocks = blocks, onOpenSource = openUrl)
        }
    }
}

/** 正文整体包在 [SelectionContainer] 里：长按即可选中、复制原文。 */
@Composable
private fun ArticleBody(
    article: Article,
    blocks: List<com.anchor.recovery.core.content.MdBlock>,
    onOpenSource: (String) -> Unit,
) {
    SelectionContainer {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ArticleHeader(article = article)
            HorizontalDivider()
            MarkdownBlocks(blocks = blocks)
            HorizontalDivider()
            SourceSection(article = article, onOpenSource = onOpenSource)
        }
    }

    DisclaimerCard()
}

@Composable
private fun ArticleHeader(article: Article) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CredibilityBadge(credibility = article.credibility)
        Text(
            text = article.categoryDir.ifBlank { article.category },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Text(
        text = article.title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )

    val meta = listOf(article.author, article.date).filter { it.isNotBlank() }.joinToString(" · ")
    if (meta.isNotBlank()) {
        Text(
            text = meta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (article.summary.isNotBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
            )
        }
    }

}

/** 站外来源链接：显式提示“会离开应用”。 */
@Composable
private fun SourceSection(article: Article, onOpenSource: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "原文来源", style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = { onOpenSource(article.source) }) {
            Text(
                text = article.source,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "链接会用系统浏览器/CustomTabs 打开，属于站外内容。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
