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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
        AnchorLargeTitle("自评问卷")
        Text(
            text = "两份自评问卷，都可以随时重做，结果只保存在本机。",
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        HubCard(
            title = "成瘾倾向自评",
            description = "19 题，五个维度：控制失控、情绪应对、强迫性、负面后果、尝试失败。",
            lastText = state.latestCsbd?.let { record ->
                "上次：${record.takenAt.formatLocal()} · 总分 ${record.totalScore}"
            },
            onStart = onOpenCsbd,
            onOpenLast = if (state.latestCsbd != null) onOpenLastCsbd else null,
        )

        HubCard(
            title = "道德冲突 vs 真实问题",
            description = "12 题，把“行为影响”和“价值观冲突”分开计分，看看困扰主要来自哪一边。",
            lastText = state.latestMoral?.let { record ->
                "上次：${record.takenAt.formatLocal()} · 合计 ${record.totalScore}"
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
                    text = "开始作答",
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                )
                if (onOpenLast != null) {
                    AnchorButton(
                        text = "查看上次结果",
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

        AnchorSectionHeader("说明")
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
                text = "我理解：这是自评参考，不是诊断。",
            )
        }

        AnchorButton(
            text = "开始答题",
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
        AnchorLargeTitle(state.title)
        Text(
            text = "作答范围：${state.timeWindow} · 第 ${state.positionText} 题 · 已完成 ${state.answeredCount} 题",
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
                        text = question.dimensionLabel,
                        style = AnchorType.footnote,
                        color = AnchorTheme.colors.tint,
                    )
                    Text(
                        text = question.text,
                        style = AnchorType.headline,
                        color = AnchorTheme.colors.label,
                    )

                    val selectedValue = state.answers[question.id]
                    QuizOptions.labels.forEachIndexed { value, label ->
                        QuizOptionRow(
                            label = label,
                            isChosen = selectedValue == value,
                            onClick = { onSelect(question.id, value) },
                        )
                    }
                }
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
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
                text = "上一题",
                onClick = onPrevious,
                style = AnchorButtonStyle.Tinted,
                enabled = state.index > 0,
                modifier = Modifier.weight(1f),
            )
            AnchorButton(
                text = "下一题",
                onClick = onNext,
                style = AnchorButtonStyle.Tinted,
                enabled = !state.isLast,
                modifier = Modifier.weight(1f),
            )
        }
        AnchorButton(
            text = if (state.saving) "保存中…" else "提交并查看结果",
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
        AnchorLargeTitle("成瘾倾向自评")

        if (record == null) {
            Text(
                text = "还没有作答记录，先回上一页完成问卷。",
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
                    text = "作答时间：${record.takenAt.formatLocal()}",
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
                Text(
                    text = "总分 ${record.totalScore} / ${CsbdQuestionnaire.maxTotalScore}",
                    style = AnchorType.largeTitle,
                    color = AnchorTheme.colors.label,
                )
                if (result != null) {
                    Text(
                        text = "自评分级：${result.level.label}（${result.level.rangeText} 分区间）",
                        style = AnchorType.headline,
                        color = if (result.level == CsbdLevel.LOW) {
                            AnchorTheme.colors.success
                        } else {
                            AnchorTheme.colors.warning
                        },
                    )
                    Text(
                        text = result.level.advice,
                        style = AnchorType.body,
                        color = if (result.level == CsbdLevel.HIGH || result.level == CsbdLevel.URGENT) {
                            AnchorTheme.colors.danger
                        } else {
                            AnchorTheme.colors.label
                        },
                    )
                } else {
                    Text(
                        text = "分级：${record.level}（原始作答缺失，无法重新计分）",
                        style = AnchorType.headline,
                        color = AnchorTheme.colors.label,
                    )
                }
            }
        }

        if (result != null) {
            AnchorSectionHeader("五个维度小计")
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
                                    text = dimension.label,
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
                        text = "得分最高的维度：${result.topDimension.label}（${result.topDimensionScore} 分）。" +
                            "只说明这组题得分最高，不是诊断。",
                        style = AnchorType.footnote,
                        color = AnchorTheme.colors.labelSecondary,
                    )
                }
            }
        }

        AnchorSectionHeader("下一步可以做什么")
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "· 带着这份自评和 App 里的记录去咨询心理咨询师或精神科 / 心理科医生；" +
                        "自评分数不是诊断，专业评估才决定要不要治疗。",
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                )
                Text(
                    text = "· 继续用打卡与渴求工具看规律；情绪持续低落或有失控感时尽快就诊。",
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                )
            }
        }

        if (result != null) {
            Text(
                text = result.thresholdNote,
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
        AnchorLargeTitle("道德冲突评估")

        if (record == null) {
            Text(
                text = "还没有作答记录，先回上一页完成问卷。",
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
                    text = "作答时间：${record.takenAt.formatLocal()}",
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
                if (result != null) {
                    Text(
                        text = result.quadrant.label,
                        style = AnchorType.headline,
                        color = when {
                            result.behaviorHigh && result.moralHigh -> AnchorTheme.colors.danger
                            result.behaviorHigh || result.moralHigh -> AnchorTheme.colors.warning
                            else -> AnchorTheme.colors.success
                        },
                    )
                    Text(
                        text = result.quadrant.interpretation,
                        style = AnchorType.body,
                        color = AnchorTheme.colors.label,
                    )
                } else {
                    Text(
                        text = "上次结果：$record.level（原始作答缺失，无法重新解读）",
                        style = AnchorType.headline,
                        color = AnchorTheme.colors.label,
                    )
                }
            }
        }

        if (result != null) {
            AnchorSectionHeader("两维度得分")
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
                                    text = kind.label,
                                    style = AnchorType.body,
                                    color = AnchorTheme.colors.label,
                                )
                                Text(
                                    text = "$score / $max · ${if (high) "偏高" else "不高"}",
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
                        text = "价值观冲突不等于成瘾，行为影响大也不等于成瘾；这两项只是帮你看清困扰从哪来。",
                        style = AnchorType.footnote,
                        color = AnchorTheme.colors.labelSecondary,
                    )
                }
            }
        }

        AnchorSectionHeader("下一步可以做什么")
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "· 如果主要是价值观冲突：和信任的人或咨询师谈这件事本身就有价值，不必先贴标签。",
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                )
                Text(
                    text = "· 如果行为影响明显：带上记录去咨询心理咨询师或精神科 / 心理科医生，由专业评估判断。",
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                )
            }
        }

        if (result != null) {
            Text(
                text = result.thresholdNote,
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
    AnchorSectionHeader(text = "免责声明", modifier = modifier)
    AnchorListGroup {
        Text(
            text = "自评参考信息，不是医疗器械、不做诊断，不能替代专业评估；困扰明显时建议咨询专业人士。",
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
        title = "成瘾倾向自评",
        subtitle = "19 道题，按过去 6 个月的实际情况作答。",
        disclaimer = CsbdQuestionnaire.DISCLAIMER,
        points = listOf(
            "19 题，每题 5 个选项（0–4 分），没有对错。",
            "题目参照 CSBD 五个维度自撰，不是 CSBD-19 原版、未经验证；分级阈值也是自研，未经临床验证。",
            "作答与结果只保存在本机。",
        ),
        onStart = onStart,
        modifier = modifier,
    )
}

/** F8 intro 页包装。 */
@Composable
fun MoralIntroScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    AssessmentIntroScreen(
        title = "道德冲突 vs 真实问题",
        subtitle = "12 道题，按过去 1 个月的实际情况作答。",
        disclaimer = MoralIncongruenceScale.DISCLAIMER,
        points = listOf(
            "前 6 题看“行为影响”，后 6 题看“价值观冲突”，分别计分。",
            "任一维度 ≥ ${MoralIncongruenceScale.HIGH_THRESHOLD} 分记为偏高（自研阈值，未经临床验证）。",
            "结果是四象限解读，只看困扰来源，不做诊断；作答与结果只保存在本机。",
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
            text = "重新作答",
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
        )
        AnchorButton(
            text = "结果已保存在本机",
            onClick = {},
            enabled = false,
            style = AnchorButtonStyle.Plain,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
