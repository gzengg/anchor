package com.anchor.recovery.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anchor.recovery.core.AnchorCore
import com.anchor.recovery.core.legal.Disclaimer

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
    var understood by rememberSaveable { mutableStateOf(false) }
    var acknowledged by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            // 门禁页在 Scaffold 之外整屏渲染，系统栏 insets 必须自己消费。
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = AnchorCore.APP_DISPLAY_NAME,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "一个完全离线的自助记录工具：数据只留在这台手机上。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "它能做什么", style = MaterialTheme.typography.titleMedium)
                PurposeRow("记录：每天一次打卡，看清自己连续了多久")
                PurposeRow("应对：冲动来时用十分钟延时与渴求冲浪")
                PurposeRow("科普：71 篇离线文章，每篇标注可信度与出处")
            }
        }

        Text(text = Disclaimer.TITLE, style = MaterialTheme.typography.titleLarge)
        Disclaimer.paragraphs.forEach { paragraph ->
            Text(
                text = "· $paragraph",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        CheckRow(
            checked = understood,
            onCheckedChange = { understood = it },
            text = "我知道磐石是自助工具，不提供诊断、治疗或医疗建议。",
        )
        CheckRow(
            checked = acknowledged,
            onCheckedChange = { acknowledged = it },
            text = Disclaimer.ACK_LABEL,
        )

        Button(
            onClick = onAccept,
            enabled = understood && acknowledged,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "开始使用")
        }
        Text(
            text = "点「开始使用」即表示你已阅读并同意以上说明。之后随时可以在设置页了解数据去向。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PurposeRow(text: String) {
    Text(text = "· $text", style = MaterialTheme.typography.bodyMedium)
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
            .toggleable(value = checked, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
