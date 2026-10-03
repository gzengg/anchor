package com.anchor.recovery.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.anchor.recovery.R
import com.anchor.recovery.core.AnchorCore
import com.anchor.recovery.core.legal.Disclaimer
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorHairline
import com.anchor.recovery.ui.components.AnchorLargeTitle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.AnchorSectionHeader
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/**
 * 首启门禁页（提示词 §5.1）。
 *
 * 独立成一个页面而不是塞进首页弹窗：阅读完整声明并勾选，才能进入应用。
 * 声明内容版本升级后（[Disclaimer.VERSION] 变大）会再次走这里。
 * 该页面不放在 NavHost 里，而是由 [com.anchor.recovery.ui.AnchorApp] 在满足
 * [Disclaimer.requiresAcknowledgement] 时整屏替换导航图，避免用户绕过门禁。
 */
@Composable
fun OnboardingScreen(
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var acknowledged by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    // 这一页在 Scaffold 之外渲染，没有全局导航栏可挂；调用是空操作，只为和其他页面写法一致。
    PublishAnchorNavBar(scrollState)

    // 显式铺一层背景色：这一页在 Scaffold 之外整屏渲染，
    // 不铺就会露出 windowBackground，两处色值一旦不同源就会闪一下别的颜色。
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground)
            .safeDrawingPadding()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnchorLargeTitle(AnchorCore.APP_DISPLAY_NAME)
        Text(
            text = stringResource(R.string.onboarding_offline_notice),
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        AnchorSectionHeader(stringResource(R.string.onboarding_purpose_header))
        AnchorListGroup {
            PurposeRow(stringResource(R.string.onboarding_purpose_log))
            AnchorHairline()
            PurposeRow(stringResource(R.string.onboarding_purpose_coping))
            AnchorHairline()
            PurposeRow(stringResource(R.string.onboarding_purpose_articles))
        }

        AnchorSectionHeader(Disclaimer.TITLE)
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Disclaimer.paragraphs.forEach { paragraph ->
                    Text(
                        text = "· $paragraph",
                        style = AnchorType.body,
                        color = AnchorTheme.colors.label,
                    )
                }
            }
        }

        AnchorListGroup {
            CheckRow(
                checked = acknowledged,
                onCheckedChange = { acknowledged = it },
                text = Disclaimer.ACK_LABEL,
            )
        }

        AnchorButton(
            text = stringResource(R.string.onboarding_start),
            onClick = onAccept,
            enabled = acknowledged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun PurposeRow(text: String) {
    Text(
        text = "· $text",
        style = AnchorType.body,
        color = AnchorTheme.colors.label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun CheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = AnchorTheme.colors.tint,
                uncheckedColor = AnchorTheme.colors.labelTertiary,
                checkmarkColor = Color.White,
            ),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = AnchorType.body,
            color = AnchorTheme.colors.label,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
