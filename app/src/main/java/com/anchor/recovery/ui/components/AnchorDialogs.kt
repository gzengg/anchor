package com.anchor.recovery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType
import kotlinx.coroutines.launch

/*
 * iOS 的两种模态：居中弹窗（AnchorAlert）与底部动作面板（AnchorActionSheet）。
 *
 * 两者都替换掉 M3 的 AlertDialog/DropdownMenu 用法：iOS 的语义是
 * 「一屏只问一件事、按钮纵向排列、危险动作是红的」，横向排按钮的 M3 弹窗做不到。
 *
 * 底部面板底下仍是 M3 的 ModalBottomSheet（拿它的进出场动画、拖拽与焦点陷阱），
 * 只把外观换成透明容器 + 自绘的圆角卡片。
 */

/**
 * iOS 风格居中弹窗。
 *
 * @param confirmLabel 主操作文案（永远加粗显示）
 * @param destructive 主操作是危险动作（删除、清空），文案用红色
 * @param dismissLabel 次要操作；与 [onDismiss] 一起给才会显示
 */
@Composable
fun AnchorAlert(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    destructive: Boolean = false,
) {
    val colors = AnchorTheme.colors
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(270.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardBackground),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = AnchorType.headline,
                        color = colors.label,
                        textAlign = TextAlign.Center,
                    )
                    if (message != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message,
                            style = AnchorType.footnote,
                            color = colors.labelSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                AnchorHairline(inset = 0.dp)
                AlertActionRow(
                    label = confirmLabel,
                    color = if (destructive) colors.danger else colors.tint,
                    bold = true,
                    onClick = onConfirm,
                )
                if (dismissLabel != null && onDismiss != null) {
                    AnchorHairline(inset = 0.dp)
                    AlertActionRow(
                        label = dismissLabel,
                        color = colors.tint,
                        bold = false,
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}

/** 弹窗里的一个按钮：整行可点、文字居中、44dp 高。 */
@Composable
private fun AlertActionRow(
    label: String,
    color: Color,
    bold: Boolean,
    onClick: () -> Unit,
) {
    AnchorClickableSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RectangleShape,
        role = Role.Button,
    ) {
        Text(
            text = label,
            style = if (bold) AnchorType.body.copy(fontWeight = FontWeight.SemiBold) else AnchorType.body,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** 底部动作面板的一项。 */
data class AnchorSheetAction(
    val label: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
)

/**
 * iOS 风格底部动作面板。
 *
 * 关闭时先等面板滑下去（`sheetState.hide()`）再回调，避免「点完页面直接跳走、
 * 面板凭空消失」的观感。
 *
 * @param cancelLabel 取消文案；面板里的取消永远单独一张卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorActionSheet(
    actions: List<AnchorSheetAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    cancelLabel: String = "取消",
) {
    val colors = AnchorTheme.colors
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val closeThen: ((() -> Unit)?) -> Unit = { after ->
        scope.launch {
            sheetState.hide()
            onDismiss()
            after?.invoke()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        contentColor = colors.label,
        tonalElevation = 0.dp,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardBackground),
            ) {
                if (title != null || message != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (title != null) {
                            Text(
                                text = title,
                                style = AnchorType.footnoteSemibold,
                                color = colors.labelSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (message != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message,
                                style = AnchorType.footnote,
                                color = colors.labelSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    AnchorHairline(inset = 0.dp)
                }
                actions.forEachIndexed { index, action ->
                    if (index > 0) AnchorHairline(inset = 0.dp)
                    SheetActionRow(
                        action = action,
                        onClick = { closeThen(action.onClick) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnchorClickableSurface(
                onClick = { closeThen(null) },
                modifier = Modifier.fillMaxWidth().height(57.dp),
                shape = RoundedCornerShape(14.dp),
                color = colors.cardBackground,
                role = Role.Button,
            ) {
                Text(
                    text = cancelLabel,
                    style = AnchorType.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.tint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

/** 面板里的一项动作：57dp 高、文字居中、破坏性动作用红色。 */
@Composable
private fun SheetActionRow(
    action: AnchorSheetAction,
    onClick: () -> Unit,
) {
    val colors = AnchorTheme.colors
    val haptics = rememberAnchorHaptics()
    AnchorClickableSurface(
        onClick = {
            if (action.destructive) haptics.reject() else haptics.light()
            onClick()
        },
        modifier = Modifier.fillMaxWidth().height(57.dp),
        shape = RectangleShape,
        enabled = action.enabled,
        role = Role.Button,
    ) {
        Text(
            text = action.label,
            style = AnchorType.body,
            color = when {
                !action.enabled -> colors.labelTertiary
                action.destructive -> colors.danger
                else -> colors.label
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
