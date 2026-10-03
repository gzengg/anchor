package com.anchor.recovery.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anchor.recovery.R
import com.anchor.recovery.core.relapse.RelapseTags
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorLargeTitle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.AnchorSectionHeader
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/**
 * F5 破戒记录表单：情境 / 情绪（多选）/ 触发源（多选）/ 备注。
 *
 * 时间取记录当下的时刻（一律经 Clock），页面不做任何评价性表达。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelapseEditScreen(
    viewModel: RelapseEditViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    PublishAnchorNavBar(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnchorLargeTitle(stringResource(R.string.relapse_title))

        Text(
            text = stringResource(R.string.relapse_hint),
            style = AnchorType.body,
            color = AnchorTheme.colors.label,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        AnchorListGroup {
            OutlinedTextField(
                value = state.situation,
                onValueChange = viewModel::updateSituation,
                label = { Text(text = stringResource(R.string.relapse_situation_label)) },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AnchorTheme.colors.tint,
                    unfocusedBorderColor = AnchorTheme.colors.separator,
                    focusedLabelColor = AnchorTheme.colors.tint,
                    unfocusedLabelColor = AnchorTheme.colors.labelSecondary,
                    cursorColor = AnchorTheme.colors.tint,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

        TagSection(
            title = stringResource(R.string.relapse_emotions_title),
            options = RelapseTags.emotions,
            selected = state.emotions,
            onToggle = viewModel::toggleEmotion,
        )

        TagSection(
            title = stringResource(R.string.relapse_triggers_title),
            options = RelapseTags.triggers,
            selected = state.triggers,
            onToggle = viewModel::toggleTrigger,
        )

        AnchorListGroup {
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::updateNote,
                label = { Text(text = stringResource(R.string.relapse_note_label)) },
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AnchorTheme.colors.tint,
                    unfocusedBorderColor = AnchorTheme.colors.separator,
                    focusedLabelColor = AnchorTheme.colors.tint,
                    unfocusedLabelColor = AnchorTheme.colors.labelSecondary,
                    cursorColor = AnchorTheme.colors.tint,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

        state.messageRes?.let { messageRes ->
            AnchorListGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(messageRes),
                        style = AnchorType.body,
                        color = AnchorTheme.colors.label,
                    )
                    if (state.saved) {
                        AnchorButton(
                            text = stringResource(R.string.relapse_open_journal_button),
                            onClick = onSaved,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        AnchorListGroup {
            AnchorButton(
                text = stringResource(R.string.relapse_save_button),
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

        Text(
            text = stringResource(R.string.relapse_footnote),
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column {
        AnchorSectionHeader(text = title)
        AnchorListGroup {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    FilterChip(
                        selected = option in selected,
                        onClick = { onToggle(option) },
                        label = { Text(text = option) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AnchorTheme.colors.fill,
                            labelColor = AnchorTheme.colors.label,
                            selectedContainerColor = AnchorTheme.colors.tint,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
        }
    }
}
