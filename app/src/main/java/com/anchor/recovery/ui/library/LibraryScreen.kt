package com.anchor.recovery.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anchor.recovery.R
import com.anchor.recovery.core.content.Article
import com.anchor.recovery.data.content.ContentRepository
import com.anchor.recovery.data.content.ContentSnapshot
import com.anchor.recovery.ui.components.AnchorClickableSurface
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.AnchorSegmentedControl
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/**
 * F6 知识库列表：六分类筛选 + 标题/正文检索 + 可信度徽章。
 *
 * 版式为 iOS 分组列表：搜索框与分类筛选固定在顶部，文章卡沿用圆角白卡 + 分组底色。
 */
@Composable
fun LibraryScreen(
    content: ContentRepository,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var snapshot by remember { mutableStateOf<ContentSnapshot?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(content) { snapshot = content.snapshot() }

    val library = snapshot?.library
    val articles = remember(library, selectedCategory, query) {
        library?.let { lib ->
            val base = selectedCategory?.let { lib.articlesOf(it) } ?: lib.orderedArticles()
            if (query.isBlank()) {
                base
            } else {
                val hits = lib.search(query).map { it.id }.toSet()
                base.filter { it.id in hits }
            }
        } ?: emptyList()
    }

    val listState = rememberLazyListState()
    // 本页没有页内大标题：交滚动状态只为让顶栏标题保持常显。
    PublishAnchorNavBar(listState, hasLargeTitle = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            snapshot == null -> LoadingState()
            library == null -> ErrorState(
                snapshot?.error ?: stringResource(R.string.library_content_unavailable),
            )
            else -> {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(text = stringResource(R.string.library_search_placeholder)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
                val categories = library.index.categories
                val categoryKeys = remember(categories) { listOf(CATEGORY_ALL) + categories.map { it.key } }
                val categoryLabels = remember(categories) { categories.associate { it.key to "${it.title} ${it.count}" } }
                // 分段控件的 label 参数不是 @Composable，计数文案在调用前取成资源。
                val allCategoriesLabel = stringResource(R.string.library_category_all_count, library.articles.size)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    AnchorSegmentedControl(
                        options = categoryKeys,
                        selected = selectedCategory ?: CATEGORY_ALL,
                        onSelect = { key -> selectedCategory = if (key == CATEGORY_ALL) null else key },
                        // 七段挤满一屏必然截断分类名，给每段固定宽度、整体横向滚动。
                        modifier = Modifier.width(CATEGORY_SEGMENT_WIDTH * categoryKeys.size),
                        label = { key ->
                            if (key == CATEGORY_ALL) {
                                allCategoriesLabel
                            } else {
                                categoryLabels.getValue(key)
                            }
                        },
                    )
                }
                Text(
                    text = if (articles.isEmpty()) {
                        stringResource(R.string.library_no_match)
                    } else {
                        pluralStringResource(
                            R.plurals.library_article_total,
                            articles.size,
                            articles.size,
                        )
                    },
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(items = articles, key = { it.id }) { article ->
                        AnchorListGroup {
                            ArticleRow(article = article, onOpen = { onOpenArticle(article.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleRow(
    article: Article,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnchorClickableSurface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        // 整卡可点，语义按内容卡片上报，不上报成按钮。
        role = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CredibilityBadge(credibility = article.credibility)
                Text(
                    text = article.date,
                    style = AnchorType.caption1,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
            Text(
                text = article.title,
                style = AnchorType.headline,
                color = AnchorTheme.colors.label,
            )
            if (article.summary.isNotBlank()) {
                Text(
                    text = article.summary,
                    style = AnchorType.subheadline,
                    maxLines = 3,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
            if (article.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(items = article.tags) { tag ->
                        Text(
                            text = "#$tag",
                            style = AnchorType.caption1,
                            color = AnchorTheme.colors.tint,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = AnchorTheme.colors.tint)
        Text(
            text = stringResource(R.string.library_loading),
            style = AnchorType.subheadline,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.library_error_title),
            style = AnchorType.headline,
            color = AnchorTheme.colors.label,
        )
        Text(
            text = message,
            style = AnchorType.subheadline,
            color = AnchorTheme.colors.danger,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** 分类筛选里「全部」那一项的 key；真实分类 key 都来自 index.json，不会为空串。 */
private const val CATEGORY_ALL = ""

/** 分类分段固定宽度：容得下「科学研究 12」这类两字标题 + 两位计数。 */
private val CATEGORY_SEGMENT_WIDTH = 88.dp
