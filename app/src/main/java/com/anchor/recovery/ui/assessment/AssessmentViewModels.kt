package com.anchor.recovery.ui.assessment

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.recovery.R
import com.anchor.recovery.core.assessment.CsbdQuestionnaire
import com.anchor.recovery.core.assessment.CsbdResult
import com.anchor.recovery.core.assessment.CsbdScorer
import com.anchor.recovery.core.assessment.MoralIncongruenceScale
import com.anchor.recovery.core.assessment.MoralIncongruenceScorer
import com.anchor.recovery.core.assessment.MoralResult
import com.anchor.recovery.core.model.AssessmentRecord
import com.anchor.recovery.core.model.AssessmentType
import com.anchor.recovery.data.repo.AnchorRepository
import com.anchor.recovery.ui.text.csbdDimensionRes
import com.anchor.recovery.ui.text.csbdQuestionRes
import com.anchor.recovery.ui.text.moralItemKindRes
import com.anchor.recovery.ui.text.moralQuestionRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 单道题的渲染数据；题干与维度名只带资源 id，文案在 Compose 层解析（ViewModel 不持有 Context）。 */
data class QuizQuestionUi(
    val id: Int,
    @StringRes val textRes: Int,
    @StringRes val dimensionLabelRes: Int,
)

/** 每题的五个选项（0–4 分）；只暴露资源 id，文案在 Compose 层解析。 */
object QuizOptions {
    @StringRes
    val labels = listOf(
        R.string.quiz_option_0,
        R.string.quiz_option_1,
        R.string.quiz_option_2,
        R.string.quiz_option_3,
        R.string.quiz_option_4,
    )

    @StringRes
    fun labelResOf(value: Int): Int = labels.getOrElse(value) { R.string.quiz_option_unanswered }
}

/** 校验失败提示：资源 id + 占位符参数（数量词走 [quantity]）。 */
data class QuizMessage(
    @StringRes val res: Int,
    val args: List<Any> = emptyList(),
    val quantity: Int? = null,
)

data class QuizUiState(
    @StringRes val titleRes: Int? = null,
    /** 作答时间窗；初始态为 null（尚未绑定具体问卷），Compose 侧取空串。 */
    @StringRes val timeWindowRes: Int? = null,
    val questions: List<QuizQuestionUi> = emptyList(),
    val answers: Map<Int, Int> = emptyMap(),
    val index: Int = 0,
    val saving: Boolean = false,
    val finished: Boolean = false,
    @StringRes val messageRes: Int? = null,
    val messageArgs: List<Any> = emptyList(),
    /** 非 null 时按数量词解析（plurals）。 */
    val messageQuantity: Int? = null,
) {
    val current: QuizQuestionUi? get() = questions.getOrNull(index)

    val total: Int get() = questions.size

    val answeredCount: Int get() = answers.size

    val allAnswered: Boolean get() = questions.isNotEmpty() && answeredCount == total

    val isLast: Boolean get() = index >= total - 1

    val positionText: String get() = "${index + 1} / $total"

    val progress: Float get() = if (total == 0) 0f else answeredCount.toFloat() / total
}

/**
 * 两套问卷共用的作答流程：逐题作答、可回退、全部作答后才能提交，提交后落库再跳结果页。
 * 计分全部交给 :core 的 Scorer，这里只做状态转发。
 */
abstract class QuizViewModel(protected val repository: AnchorRepository) : ViewModel() {

    private val answers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    private val index = MutableStateFlow(0)
    private val saving = MutableStateFlow(false)
    private val finished = MutableStateFlow(false)
    private val message = MutableStateFlow<QuizMessage?>(null)

    protected abstract val type: AssessmentType

    @get:StringRes
    protected abstract val titleRes: Int

    @get:StringRes
    protected abstract val timeWindowRes: Int

    protected abstract val questions: List<QuizQuestionUi>

    /** 用 :core 的 Scorer 把作答转成待落库的记录（totalScore / level 由 Scorer 决定）。 */
    protected abstract fun buildRecord(answers: List<Int>): AssessmentRecord

    val state: StateFlow<QuizUiState> =
        combine(answers, index, saving, finished, message) { answers, index, saving, finished, message ->
            QuizUiState(
                titleRes = titleRes,
                timeWindowRes = timeWindowRes,
                questions = questions,
                answers = answers,
                index = index.coerceIn(0, (questions.size - 1).coerceAtLeast(0)),
                saving = saving,
                finished = finished,
                messageRes = message?.res,
                messageArgs = message?.args ?: emptyList(),
                messageQuantity = message?.quantity,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuizUiState())

    /** 选中某题的分值。非最后一题时自动前进，符合逐题作答的节奏。 */
    fun select(questionId: Int, value: Int) {
        answers.value = answers.value + (questionId to value)
        message.value = null
        if (questionId == questions.getOrNull(index.value)?.id && index.value < questions.size - 1) {
            index.value += 1
        }
    }

    fun previous() {
        if (index.value > 0) index.value -= 1
    }

    fun next() {
        if (index.value < questions.size - 1) index.value += 1
    }

    fun goTo(position: Int) {
        index.value = position.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
    }

    fun submit() {
        val ordered = orderedAnswers()
        if (ordered == null) {
            val remaining = questions.size - answers.value.size
            message.value = QuizMessage(
                res = R.plurals.quiz_unanswered_message,
                args = listOf(remaining),
                quantity = remaining,
            )
            return
        }
        viewModelScope.launch {
            saving.value = true
            repository.recordAssessment(buildRecord(ordered))
            saving.value = false
            finished.value = true
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    private fun orderedAnswers(): List<Int>? =
        if (!state.value.allAnswered) {
            null
        } else {
            questions.map { question -> answers.value.getValue(question.id) }
        }
}

class CsbdQuizViewModel(repository: AnchorRepository) : QuizViewModel(repository) {

    override val type = AssessmentType.CSBD
    override val titleRes = R.string.quiz_title_csbd
    override val timeWindowRes = R.string.csbd_time_window
    override val questions = CsbdQuestionnaire.questions.map {
        QuizQuestionUi(
            id = it.id,
            textRes = csbdQuestionRes(it.id),
            dimensionLabelRes = csbdDimensionRes(it.dimension),
        )
    }

    override fun buildRecord(answers: List<Int>): AssessmentRecord {
        val result = CsbdScorer.score(answers)
        return AssessmentRecord(
            type = type,
            takenAt = repository.clock.now(),
            totalScore = result.totalScore,
            level = result.level.name,
            answers = answers,
        )
    }
}

class MoralQuizViewModel(repository: AnchorRepository) : QuizViewModel(repository) {

    override val type = AssessmentType.MORAL
    override val titleRes = R.string.quiz_title_moral
    override val timeWindowRes = R.string.moral_time_window
    override val questions = MoralIncongruenceScale.questions.map {
        QuizQuestionUi(
            id = it.id,
            textRes = moralQuestionRes(it.id),
            dimensionLabelRes = moralItemKindRes(it.kind),
        )
    }

    override fun buildRecord(answers: List<Int>): AssessmentRecord {
        val result = MoralIncongruenceScorer.score(answers)
        return AssessmentRecord(
            type = type,
            takenAt = repository.clock.now(),
            totalScore = result.behaviorScore + result.moralScore,
            level = result.quadrant.name,
            answers = answers,
        )
    }
}

/** 问卷首页：两条入口 + 上次结果摘要。 */
data class AssessmentHubUiState(
    val latestCsbd: AssessmentRecord? = null,
    val latestMoral: AssessmentRecord? = null,
)

class AssessmentHubViewModel(repository: AnchorRepository) : ViewModel() {

    val state: StateFlow<AssessmentHubUiState> = repository.assessments
        .map { records ->
            AssessmentHubUiState(
                latestCsbd = records.firstOrNull { it.type == AssessmentType.CSBD },
                latestMoral = records.firstOrNull { it.type == AssessmentType.MORAL },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssessmentHubUiState())
}

/**
 * 结果页数据：优先用落库的原始作答在 :core 重新计分（这样阈值调整后旧记录也能重新解读），
 * 作答缺失或格式不符时退回记录里保存的总分与分级。
 */
data class CsbdResultUiState(
    val record: AssessmentRecord? = null,
    val result: CsbdResult? = null,
) {
    val hasResult: Boolean get() = record != null
}

class CsbdResultViewModel(repository: AnchorRepository) : ViewModel() {

    val state: StateFlow<CsbdResultUiState> = repository.assessments
        .map { records ->
            val record = records.firstOrNull { it.type == AssessmentType.CSBD }
            CsbdResultUiState(
                record = record,
                result = record?.answers
                    ?.takeIf { it.size == CsbdQuestionnaire.questionCount }
                    ?.let { answers -> runCatching { CsbdScorer.score(answers) }.getOrNull() },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CsbdResultUiState())
}

data class MoralResultUiState(
    val record: AssessmentRecord? = null,
    val result: MoralResult? = null,
) {
    val hasResult: Boolean get() = record != null
}

class MoralResultViewModel(repository: AnchorRepository) : ViewModel() {

    val state: StateFlow<MoralResultUiState> = repository.assessments
        .map { records ->
            val record = records.firstOrNull { it.type == AssessmentType.MORAL }
            MoralResultUiState(
                record = record,
                result = record?.answers
                    ?.takeIf { it.size == MoralIncongruenceScale.questionCount }
                    ?.let { answers -> runCatching { MoralIncongruenceScorer.score(answers) }.getOrNull() },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoralResultUiState())
}
