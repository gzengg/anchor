package com.anchor.recovery.ui.assessment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.anchor.recovery.R
import com.anchor.recovery.core.assessment.CsbdDimension
import com.anchor.recovery.core.assessment.CsbdLevel
import com.anchor.recovery.core.assessment.CsbdQuestionnaire
import com.anchor.recovery.core.assessment.MoralIncongruenceScale
import com.anchor.recovery.core.assessment.MoralItemKind
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorButtonStyle
import com.anchor.recovery.ui.components.AnchorClickableSurface
import com.anchor.recovery.ui.components.AnchorHairline
import com.anchor.recovery.ui.components.AnchorLargeTitle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.AnchorSectionHeader
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.journal.formatLocal
import com.anchor.recovery.ui.text.csbdDimensionRes
import com.anchor.recovery.ui.text.csbdLevelAdviceRes
import com.anchor.recovery.ui.text.csbdLevelLabelRes
import com.anchor.recovery.ui.text.csbdLevelRangeRes
import com.anchor.recovery.ui.text.moralItemKindRes
import com.anchor.recovery.ui.text.moralQuadrantInterpretationRes
import com.anchor.recovery.ui.text.moralQuadrantLabelRes
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/** 问卷首页：F7 / F8 两条入口 + 上次结果摘要。 */
@Composable
fun AssessmentHubScreen(
    state: AssessmentHubUiState,
    onOpenCsbd: () -> Unit,
    onOpenMoral: () -> Unit,
    onOpenLastCsbd: () -> Unit,
    onOpenLastMoral: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    PublishAnchorNavBar(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        AnchorLargeTitle(stringResource(R.string.assessment_hub_title))
        Text(
            text = stringResource(R.string.assessment_hub_subtitle),
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        HubCard(
            title = stringResource(R.string.csbd_title),
            description = stringResource(R.string.csbd_hub_description),
            lastText = state.latestCsbd?.let { record ->
                stringResource(R.string.csbd_hub_last_summary, record.takenAt.formatLocal(), record.totalScore)
            },
            onStart = onOpenCsbd,
            onOpenLast = if (state.latestCsbd != null) onOpenLastCsbd else null,
        )

        HubCard(
            title = stringResource(R.string.moral_title),
            description = stringResource(R.string.moral_hub_description),
            lastText = state.latestMoral?.let { record ->
                stringResource(R.string.moral_hub_last_summary, record.takenAt.formatLocal(), record.totalScore)
            },
            onStart = onOpenMoral,
            onOpenLast = if (state.latestMoral != null) onOpenLastMoral else null,
        )

        DisclaimerCard()
    }
}

@Composable
private fun HubCard(
    title: String,
    description: String,
    lastText: String?,
    onStart: () -> Unit,
    onOpenLast: (() -> Unit)?,
) {
    AnchorSectionHeader(title)
    AnchorListGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = description, style = AnchorType.body, color = AnchorTheme.colors.label)
            lastText?.let {
                Text(
                    text = it,
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnchorButton(
                    text = stringResource(R.string.quiz_start),
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                )
                if (onOpenLast != null) {
                    AnchorButton(
                        text = stringResource(R.string.quiz_view_last_result),
                        onClick = onOpenLast,
                        style = AnchorButtonStyle.Tinted,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 问卷 intro 页：独立免责声明 + 勾选确认，勾选前不能开始（合规要求 §5.2）。
 */
@Composable
fun AssessmentIntroScreen(
    title: String,
    subtitle: String,
    disclaimer: String,
    points: List<String>,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var acknowledged by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    PublishAnchorNavBar(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        AnchorLargeTitle(title)
        Text(
            text = subtitle,
            style = AnchorType.body,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        AnchorSectionHeader(stringResource(R.string.assessment_section_explain))
        AnchorListGroup {
            Text(
                text = disclaimer,
                style = AnchorType.body,
                color = AnchorTheme.colors.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

        AnchorListGroup {
            points.forEachIndexed { index, point ->
                if (index > 0) AnchorHairline()
                Text(
                    text = "· $point",
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        AnchorListGroup {
            CheckRow(
                checked = acknowledged,
                onCheckedChange = { acknowledged = it },
                text = stringResource(R.string.assessment_intro_acknowledge),
            )
        }

        AnchorButton(
            text = stringResource(R.string.assessment_intro_start),
            onClick = onStart,
            enabled = acknowledged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/** 逐题作答页（19 题 / 12 题共用）。 */
@Composable
fun QuizScreen(
    state: QuizUiState,
    onSelect: (questionId: Int, value: Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }

    val scrollState = rememberScrollState()
    PublishAnchorNavBar(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        AnchorLargeTitle(state.titleRes?.let { stringResource(it) }.orEmpty())
        Text(
            text = stringResource(
                R.string.quiz_progress_line,
                state.timeWindowRes?.let { stringResource(it) }.orEmpty(),
                state.positionText,
                state.answeredCount,
            ),
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            color = AnchorTheme.colors.tint,
            trackColor = AnchorTheme.colors.fill,
        )

        state.current?.let { question ->
            AnchorListGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(question.dimensionLabelRes),
                        style = AnchorType.footnote,
                        color = AnchorTheme.colors.tint,
                    )
                    Text(
                        text = stringResource(question.textRes),
                        style = AnchorType.headline,
                        color = AnchorTheme.colors.label,
                    )

                    val selectedValue = state.answers[question.id]
                    QuizOptions.labels.forEachIndexed { value, label ->
                        QuizOptionRow(
                            label = stringResource(label),
                            isChosen = selectedValue == value,
                            onClick = { onSelect(question.id, value) },
                        )
                    }
                }
            }
        }

        state.messageRes?.let { res ->
            val args = state.messageArgs.toTypedArray()
            val text = state.messageQuantity
                ?.let { pluralStringResource(res, it, *args) }
                ?: stringResource(res, *args)
            Text(
                text = text,
                style = AnchorType.footnote,
                color = AnchorTheme.colors.danger,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnchorButton(
                text = stringResource(R.string.quiz_previous),
                onClick = onPrevious,
                style = AnchorButtonStyle.Tinted,
                enabled = state.index > 0,
                modifier = Modifier.weight(1f),
            )
            AnchorButton(
                text = stringResource(R.string.quiz_next),
                onClick = onNext,
                style = AnchorButtonStyle.Tinted,
                enabled = !state.isLast,
                modifier = Modifier.weight(1f),
            )
        }
        AnchorButton(
            text = if (state.saving) stringResource(R.string.quiz_saving) else stringResource(R.string.quiz_submit),
            onClick = onSubmit,
            enabled = state.allAnswered && !state.saving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        DisclaimerCard()
    }
}

/** 单选项：选中项主色描边 + 浅底，未选中项 fill 灰底（点击与选中语义仍在整张卡片上）。 */
@Composable
private fun QuizOptionRow(
    label: String,
    isChosen: Boolean,
    onClick: () -> Unit,
) {
    val colors = AnchorTheme.colors
    val shape = RoundedCornerShape(10.dp)

    AnchorClickableSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isChosen) Modifier.border(1.dp, colors.tint, shape) else Modifier)
            .semantics { selected = isChosen },
        shape = shape,
        color = if (isChosen) colors.tint.copy(alpha = 0.12f) else colors.fill,
        role = Role.RadioButton,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isChosen,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colors.tint,
                    unselectedColor = colors.labelTertiary,
                ),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = AnchorType.body,
                color = colors.label,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

/** F7 结果页：总分分级 + 五维小计 + 就医引导。 */
@Composable
fun CsbdResultScreen(state: CsbdResultUiState, modifier: Modifier = Modifier) {
    val record = state.record
    val result = state.result

    val scrollState = rememberScrollState()
    PublishAnchorNavBar(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        AnchorLargeTitle(stringResource(R.string.csbd_title))

        if (record == null) {
            Text(
                text = stringResource(R.string.assessment_no_record),
                style = AnchorType.body,
                color = AnchorTheme.colors.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            return@Column
        }

        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.assessment_taken_at, record.takenAt.formatLocal()),
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
                Text(
                    text = stringResource(R.string.csbd_total_score, record.totalScore, CsbdQuestionnaire.maxTotalScore),
                    style = AnchorType.largeTitle,
                    color = AnchorTheme.colors.label,
                )
                if (result != null) {
                    Text(
                        text = stringResource(
                            R.string.csbd_level_summary,
                            stringResource(csbdLevelLabelRes(result.level)),
                            stringResource(csbdLevelRangeRes(result.level)),
                        ),
                        style = AnchorType.headline,
                        color = if (result.level == CsbdLevel.LOW) {
                            AnchorTheme.colors.success
                        } else {
                            AnchorTheme.colors.warning
                        },
                    )
                    Text(
                        text = stringResource(csbdLevelAdviceRes(result.level)),
                        style = AnchorType.body,
                        color = if (result.level == CsbdLevel.HIGH || result.level == CsbdLevel.URGENT) {
                            AnchorTheme.colors.danger
                        } else {
                            AnchorTheme.colors.label
                        },
                    )
                } else {
                    Text(
                        text = stringResource(R.string.csbd_level_raw_fallback, record.level),
                        style = AnchorType.headline,
                        color = AnchorTheme.colors.label,
                    )
                }
            }
        }

        if (result != null) {
            AnchorSectionHeader(stringResource(R.string.csbd_dimension_section))
            AnchorListGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CsbdDimension.entries.forEach { dimension ->
                        val score = result.dimensionScores[dimension] ?: 0
                        val max = dimension.questionCount * CsbdQuestionnaire.MAX_PER_ITEM
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = stringResource(csbdDimensionRes(dimension)),
                                    style = AnchorType.body,
                                    color = AnchorTheme.colors.label,
                                )
                                Text(
                                    text = "$score / $max",
                                    style = AnchorType.body,
                                    color = AnchorTheme.colors.labelSecondary,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { score.toFloat() / max },
                                modifier = Modifier.fillMaxWidth(),
                                color = AnchorTheme.colors.tint,
                                trackColor = AnchorTheme.colors.fill,
                            )
                        }
                    }
                    Text(
                        text = stringResource(
                            R.string.csbd_top_dimension,
                            stringResource(csbdDimensionRes(result.topDimension)),
                            result.topDimensionScore,
                        ),
                        style = AnchorType.footnote,
                        color = AnchorTheme.colors.labelSecondary,
                    )
                }
            }
        }

        AnchorSectionHeader(stringResource(R.string.assessment_next_steps))
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.csbd_next_step_1),
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                )
                Text(
                    text = stringResource(R.string.csbd_next_step_2),
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                )
            }
        }

        if (result != null) {
            Text(
                text = stringResource(
                    R.string.csbd_threshold_note,
                    stringResource(csbdLevelRangeRes(CsbdLevel.LOW)),
                    stringResource(csbdLevelRangeRes(CsbdLevel.WATCH)),
                    stringResource(csbdLevelRangeRes(CsbdLevel.HIGH)),
                    stringResource(csbdLevelRangeRes(CsbdLevel.URGENT)),
                ),
                style = AnchorType.footnote,
                color = AnchorTheme.colors.labelSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        DisclaimerCard()
    }
}

/** F8 结果页：四象限解读 + 两维度得分。 */
@Composable
fun MoralResultScreen(state: MoralResultUiState, modifier: Modifier = Modifier) {
    val record = state.record
    val result = state.result

    val scrollState = rememberScrollState()
    PublishAnchorNavBar(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        AnchorLargeTitle(stringResource(R.string.moral_result_title))

        if (record == null) {
            Text(
                text = stringResource(R.string.assessment_no_record),
                style = AnchorType.body,
                color = AnchorTheme.colors.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            return@Column
        }

        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.assessment_taken_at, record.takenAt.formatLocal()),
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
                if (result != null) {
                    Text(
                        text = stringResource(moralQuadrantLabelRes(result.quadrant)),
                        style = AnchorType.headline,
                        color = when {
                            result.behaviorHigh && result.moralHigh -> AnchorTheme.colors.danger
                            result.behaviorHigh || result.moralHigh -> AnchorTheme.colors.warning
                            else -> AnchorTheme.colors.success
                        },
                    )
                    Text(
                        text = stringResource(moralQuadrantInterpretationRes(result.quadrant)),
                        style = AnchorType.body,
                        color = AnchorTheme.colors.label,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.moral_level_raw_fallback, record.level),
                        style = AnchorType.headline,
                        color = AnchorTheme.colors.label,
                    )
                }
            }
        }

        if (result != null) {
            AnchorSectionHeader(stringResource(R.string.moral_dimension_section))
            AnchorListGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MoralItemKind.entries.forEach { kind ->
                        val score = when (kind) {
                            MoralItemKind.BEHAVIOR -> result.behaviorScore
                            MoralItemKind.MORAL -> result.moralScore
                        }
                        val max = MoralIncongruenceScale.maxScorePerKind
                        val high = score >= MoralIncongruenceScale.HIGH_THRESHOLD
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = stringResource(moralItemKindRes(kind)),
                                    style = AnchorType.body,
                                    color = AnchorTheme.colors.label,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.moral_dimension_score,
                                        score,
                                        max,
                                        stringResource(if (high) R.string.moral_level_high else R.string.moral_level_low),
                                    ),
                                    style = AnchorType.body,
                                    color = if (high) AnchorTheme.colors.warning else AnchorTheme.colors.labelSecondary,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { score.toFloat() / max },
                                modifier = Modifier.fillMaxWidth(),
                                color = AnchorTheme.colors.tint,
                                trackColor = AnchorTheme.colors.fill,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.moral_dimension_note),
                        style = AnchorType.footnote,
                        color = AnchorTheme.colors.labelSecondary,
                    )
                }
            }
        }

        AnchorSectionHeader(stringResource(R.string.assessment_next_steps))
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.moral_next_step_1),
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                )
                Text(
                    text = stringResource(R.string.moral_next_step_2),
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                )
            }
        }

        if (result != null) {
            Text(
                text = stringResource(
                    R.string.moral_threshold_note,
                    MoralIncongruenceScale.maxScorePerKind,
                    MoralIncongruenceScale.HIGH_THRESHOLD,
                ),
                style = AnchorType.footnote,
                color = AnchorTheme.colors.labelSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        DisclaimerCard()
    }
}

/** 问卷相关页面统一出现的免责声明（合规要求 §5.2）。 */
@Composable
private fun DisclaimerCard(modifier: Modifier = Modifier) {
    AnchorSectionHeader(text = stringResource(R.string.assessment_disclaimer_section), modifier = modifier)
    AnchorListGroup {
        Text(
            text = stringResource(R.string.assessment_disclaimer_body),
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

/** 勾选确认行：整行可点，复选框只做展示（语义仍是 Checkbox）。 */
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

/** F7 intro 页包装：写入规格要求的"非 CSBD-19 原版、未经验证"提示。 */
@Composable
fun CsbdIntroScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    AssessmentIntroScreen(
        title = stringResource(R.string.csbd_title),
        subtitle = stringResource(R.string.csbd_intro_subtitle),
        disclaimer = stringResource(R.string.csbd_disclaimer),
        points = listOf(
            stringResource(R.string.csbd_intro_point_1),
            stringResource(R.string.csbd_intro_point_2),
            stringResource(R.string.csbd_intro_point_3),
        ),
        onStart = onStart,
        modifier = modifier,
    )
}

/** F8 intro 页包装。 */
@Composable
fun MoralIntroScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    AssessmentIntroScreen(
        title = stringResource(R.string.moral_title),
        subtitle = stringResource(R.string.moral_intro_subtitle),
        disclaimer = stringResource(R.string.moral_disclaimer),
        points = listOf(
            stringResource(R.string.moral_intro_point_1),
            stringResource(
                R.string.moral_intro_point_2,
                MoralIncongruenceScale.HIGH_THRESHOLD,
            ),
            stringResource(R.string.moral_intro_point_3),
        ),
        onStart = onStart,
        modifier = modifier,
    )
}

/** 结果页底部统一的操作行。 */
@Composable
fun ResultActions(onRetake: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnchorButton(
            text = stringResource(R.string.quiz_retake),
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
        )
        AnchorButton(
            text = stringResource(R.string.assessment_result_saved),
            onClick = {},
            enabled = false,
            style = AnchorButtonStyle.Plain,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
