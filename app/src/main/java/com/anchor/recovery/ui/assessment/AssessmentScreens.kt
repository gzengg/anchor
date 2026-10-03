package com.anchor.recovery.ui.assessment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.anchor.recovery.core.assessment.CsbdDimension
import com.anchor.recovery.core.assessment.CsbdQuestionnaire
import com.anchor.recovery.core.assessment.MoralIncongruenceScale
import com.anchor.recovery.core.assessment.MoralItemKind
import com.anchor.recovery.ui.journal.formatLocal

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "两份自评问卷，都可以随时重做，结果只保存在本机。",
            style = MaterialTheme.typography.bodyMedium,
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            lastText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart) { Text(text = "开始作答") }
                if (onOpenLast != null) {
                    OutlinedButton(onClick = onOpenLast) { Text(text = "查看上次结果") }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "说明", style = MaterialTheme.typography.titleSmall)
                Text(text = disclaimer, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            points.forEach { point ->
                Text(text = "· $point", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = acknowledged,
                    role = Role.Checkbox,
                    onClick = { acknowledged = !acknowledged },
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = acknowledged, onCheckedChange = null)
            Text(
                text = "我理解：这是自评参考，不是诊断。",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = onStart,
            enabled = acknowledged,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(text = "开始答题") }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = state.title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "作答范围：${state.timeWindow} · 第 ${state.positionText} 题 · 已完成 ${state.answeredCount} 题",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth(),
        )

        state.current?.let { question ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = question.dimensionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(text = question.text, style = MaterialTheme.typography.titleMedium)

                    val selectedValue = state.answers[question.id]
                    QuizOptions.labels.forEachIndexed { value, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedValue == value,
                                    role = Role.RadioButton,
                                    onClick = { onSelect(question.id, value) },
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selectedValue == value, onClick = null)
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onPrevious, enabled = state.index > 0) { Text(text = "上一题") }
            OutlinedButton(onClick = onNext, enabled = !state.isLast) { Text(text = "下一题") }
            Button(
                onClick = onSubmit,
                enabled = state.allAnswered && !state.saving,
                modifier = Modifier.weight(1f),
            ) { Text(text = if (state.saving) "保存中…" else "提交并查看结果") }
        }

        DisclaimerCard()
    }
}

/** F7 结果页：总分分级 + 五维小计 + 就医引导。 */
@Composable
fun CsbdResultScreen(state: CsbdResultUiState, modifier: Modifier = Modifier) {
    val record = state.record
    val result = state.result

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (record == null) {
            Text(text = "还没有作答记录，先回上一页完成问卷。", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "作答时间：${record.takenAt.formatLocal()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "总分 ${record.totalScore} / ${CsbdQuestionnaire.maxTotalScore}",
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (result != null) {
                    Text(
                        text = "自评分级：${result.level.label}（${result.level.rangeText} 分区间）",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(text = result.level.advice, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        text = "分级：${record.level}（原始作答缺失，无法重新计分）",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        if (result != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "五个维度小计", style = MaterialTheme.typography.titleSmall)
                    CsbdDimension.entries.forEach { dimension ->
                        val score = result.dimensionScores[dimension] ?: 0
                        val max = dimension.questionCount * CsbdQuestionnaire.MAX_PER_ITEM
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = dimension.label, style = MaterialTheme.typography.bodyMedium)
                                Text(text = "$score / $max", style = MaterialTheme.typography.bodyMedium)
                            }
                            LinearProgressIndicator(
                                progress = { score.toFloat() / max },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Text(
                        text = "得分最高的维度：${result.topDimension.label}（${result.topDimensionScore} 分）。" +
                            "只说明这组题得分最高，不是诊断。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "下一步可以做什么", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "· 带着这份自评和 App 里的记录去咨询心理咨询师或精神科 / 心理科医生；" +
                        "自评分数不是诊断，专业评估才决定要不要治疗。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "· 继续用打卡与渴求工具看规律；情绪持续低落或有失控感时尽快就诊。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (result != null) {
            Text(
                text = result.thresholdNote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (record == null) {
            Text(text = "还没有作答记录，先回上一页完成问卷。", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "作答时间：${record.takenAt.formatLocal()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (result != null) {
                    Text(text = result.quadrant.label, style = MaterialTheme.typography.titleMedium)
                    Text(text = result.quadrant.interpretation, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        text = "上次结果：$record.level（原始作答缺失，无法重新解读）",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        if (result != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "两维度得分", style = MaterialTheme.typography.titleSmall)
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
                                Text(text = kind.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "$score / $max · ${if (high) "偏高" else "不高"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { score.toFloat() / max },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Text(
                        text = "价值观冲突不等于成瘾，行为影响大也不等于成瘾；这两项只是帮你看清困扰从哪来。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "下一步可以做什么", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "· 如果主要是价值观冲突：和信任的人或咨询师谈这件事本身就有价值，不必先贴标签。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "· 如果行为影响明显：带上记录去咨询心理咨询师或精神科 / 心理科医生，由专业评估判断。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (result != null) {
            Text(
                text = result.thresholdNote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DisclaimerCard()
    }
}

/** 问卷相关页面统一出现的免责声明卡（合规要求 §5.2）。 */
@Composable
private fun DisclaimerCard(modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "免责声明", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "自评参考信息，不是医疗器械、不做诊断，不能替代专业评估；困扰明显时建议咨询专业人士。",
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onRetake) { Text(text = "重新作答") }
        TextButton(onClick = {}, enabled = false) { Text(text = "结果已保存在本机") }
    }
}
