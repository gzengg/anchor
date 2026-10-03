package com.anchor.recovery.data.repo

import com.anchor.recovery.core.clock.Clock
import com.anchor.recovery.core.clock.SystemClock
import com.anchor.recovery.core.model.AssessmentRecord
import com.anchor.recovery.core.model.AssessmentType
import com.anchor.recovery.core.model.CheckInRecord
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import com.anchor.recovery.core.streak.CheckInDecision
import com.anchor.recovery.core.streak.CheckInPolicy
import com.anchor.recovery.core.streak.StreakCalculator
import com.anchor.recovery.core.streak.StreakState
import com.anchor.recovery.data.db.AppDatabase
import com.anchor.recovery.data.mapping.toDomain
import com.anchor.recovery.data.mapping.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * 唯一的数据入口：向上暴露领域模型与 Flow，向下负责 Entity 映射与打卡策略。
 *
 * UI/ViewModel 只做状态转发，任何"能不能打卡/streak 是多少"的判断都在 core + 这里完成。
 */
class AnchorRepository(
    database: AppDatabase,
    private val clock: Clock = SystemClock(),
) {
    private val checkInDao = database.checkInDao()
    private val relapseDao = database.relapseDao()
    private val urgeEpisodeDao = database.urgeEpisodeDao()
    private val assessmentDao = database.assessmentResultDao()

    private val streakCalculator = StreakCalculator(clock)
    private val checkInPolicy = CheckInPolicy(clock)

    val checkIns: Flow<List<CheckInRecord>> =
        checkInDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    val relapses: Flow<List<RelapseRecord>> =
        relapseDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    val urgeEpisodes: Flow<List<UrgeEpisodeRecord>> =
        urgeEpisodeDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    val assessments: Flow<List<AssessmentRecord>> =
        assessmentDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /** 打卡与复吸记录联动出的实时 streak 状态（首页大数字卡）。 */
    val streak: Flow<StreakState> = combine(checkIns, relapses) { checkIns, relapses ->
        streakCalculator.compute(
            checkInDates = checkIns.map { it.date },
            relapseInstants = relapses.map { it.occurredAt },
        )
    }

    fun today(): LocalDate = clock.today()

    fun nowMillis(): Long = clock.nowMillis()

    /** 为今天打卡；重复打卡由 [CheckInPolicy] 拦截，落库由 date 唯一索引兜底。 */
    suspend fun checkInToday(note: String = ""): CheckInDecision {
        val existing = checkInDao.all().map { LocalDate.parse(it.date) }
        val decision = checkInPolicy.evaluate(existing)
        if (decision is CheckInDecision.Allowed) {
            checkInDao.insert(
                CheckInRecord(
                    date = decision.date,
                    note = note.trim(),
                    createdAt = clock.now(),
                ).toEntity(),
            )
        }
        return decision
    }

    suspend fun removeCheckIn(date: LocalDate) = checkInDao.deleteByDate(date.toString())

    suspend fun recordRelapse(record: RelapseRecord): Long = relapseDao.insert(record.toEntity())

    suspend fun deleteRelapse(id: Long) = relapseDao.deleteById(id)

    suspend fun recordUrgeEpisode(record: UrgeEpisodeRecord): Long =
        urgeEpisodeDao.insert(record.toEntity())

    suspend fun recordAssessment(record: AssessmentRecord): Long =
        assessmentDao.insert(record.toEntity())

    suspend fun latestAssessment(type: AssessmentType): AssessmentRecord? =
        assessmentDao.latestOfType(type.name)?.toDomain()

    suspend fun clearAll() {
        checkInDao.clear()
        relapseDao.clear()
        urgeEpisodeDao.clear()
        assessmentDao.clear()
    }
}
