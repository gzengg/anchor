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
import com.anchor.recovery.core.streak.MilestoneAchievement
import com.anchor.recovery.core.streak.MilestoneStatus
import com.anchor.recovery.core.streak.MilestoneTracker
import com.anchor.recovery.core.streak.StreakCalculator
import com.anchor.recovery.core.streak.StreakState
import com.anchor.recovery.core.export.ImportedData
import com.anchor.recovery.data.db.AppDatabase
import com.anchor.recovery.data.mapping.toDomain
import com.anchor.recovery.data.mapping.toEntity
import androidx.room.withTransaction
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
    private val database: AppDatabase,
    val clock: Clock = SystemClock(),
) {
    private val checkInDao = database.checkInDao()
    private val relapseDao = database.relapseDao()
    private val urgeEpisodeDao = database.urgeEpisodeDao()
    private val assessmentDao = database.assessmentResultDao()
    private val milestoneDao = database.milestoneAchievementDao()

    private val streakCalculator = StreakCalculator(clock)
    private val checkInPolicy = CheckInPolicy(clock)
    private val milestoneTracker = MilestoneTracker(clock)

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

    /** 已落库的里程碑达成记录（只增不删；含因后来删除打卡而无法再从历史推导出的历史达成）。 */
    val milestoneAchievements: Flow<List<MilestoneAchievement>> =
        milestoneDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /**
     * 徽章墙状态：`落库记录 ∪ 当前历史推导` 与实时 streak 合并后的结果。
     *
     * 两者取并集的原因：历史推导能立即反映当天的打卡，而落库记录保证了
     * 「已获得的徽章不会因为删记录而熄灭」与「达成日期永久保留」。
     */
    val milestoneWall: Flow<List<MilestoneStatus>> =
        combine(checkIns, relapses, milestoneAchievements) { checkIns, relapses, stored ->
            val checkInDates = checkIns.map { it.date }
            val relapseInstants = relapses.map { it.occurredAt }
            val state = streakCalculator.compute(checkInDates, relapseInstants)
            milestoneTracker.wall(
                checkInDates = checkInDates,
                relapseInstants = relapseInstants,
                currentDays = state.currentDays,
                extraAchievements = stored,
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
            syncMilestones()
        }
        return decision
    }

    suspend fun removeCheckIn(date: LocalDate) {
        checkInDao.deleteByDate(date.toString())
        syncMilestones()
    }

    suspend fun recordRelapse(record: RelapseRecord): Long = relapseDao.insert(record.toEntity()).also {
        syncMilestones()
    }

    suspend fun deleteRelapse(id: Long) {
        relapseDao.deleteById(id)
        syncMilestones()
    }

    /**
     * 把历史推导出的达成记录补写进 `milestone_achievement`（只增不删，冲突则忽略）。
     *
     * 为什么需要落库而不是每次现算：达成日期是「发生过的事实」——用户后来删掉某天的打卡，
     * 已经拿到的徽章不应该跟着消失。写入在每次写入型操作之后触发，成本是两次全表读。
     */
    suspend fun syncMilestones() {
        val checkInDates = checkInDao.all().map { LocalDate.parse(it.date) }
        // 经 domain 转换取 Instant：时间列语义只在 AnchorMappings 里定义一次。
        val relapseInstants = relapseDao.all().map { it.toDomain().occurredAt }
        val detected = milestoneTracker.detect(checkInDates, relapseInstants)
        if (detected.isEmpty()) return
        milestoneDao.insertAll(detected.map { it.toEntity(clock.nowMillis()) })
    }

    suspend fun recordUrgeEpisode(record: UrgeEpisodeRecord): Long =
        urgeEpisodeDao.insert(record.toEntity())

    suspend fun recordAssessment(record: AssessmentRecord): Long =
        assessmentDao.insert(record.toEntity())

    suspend fun latestAssessment(type: AssessmentType): AssessmentRecord? =
        assessmentDao.latestOfType(type.name)?.toDomain()

    /** 清空全部记录（单事务）。达成记录同步清掉：它同样是用户数据，且能从历史重新推导。 */
    suspend fun clearAll() = database.withTransaction {
        checkInDao.clear()
        relapseDao.clear()
        urgeEpisodeDao.clear()
        assessmentDao.clear()
        milestoneDao.clear()
    }

    /**
     * 导入用整体替换：清空四张表后写入 [imported]（单事务，任一条失败就全部回滚）。
     *
     * 回滚是导入的安全网：文件里有重复打卡日期或重复 id 时，用户的现有记录不会被清掉一半。
     * 记录 id 按文件里的值还原，方便用户对着导出文件核对。
     */
    suspend fun replaceAll(imported: ImportedData) = database.withTransaction {
        checkInDao.clear()
        relapseDao.clear()
        urgeEpisodeDao.clear()
        assessmentDao.clear()
        // 达成记录是派生数据：导入是整体替换，旧的达成必须一起清掉（否则会留下不属于这份数据的徽章），
        // 清完立即按导入的历史重新推导。
        milestoneDao.clear()
        imported.checkIns.forEach { checkInDao.insert(it.toEntity()) }
        imported.relapses.forEach { relapseDao.insert(it.toEntity()) }
        imported.urgeEpisodes.forEach { urgeEpisodeDao.insert(it.toEntity()) }
        imported.assessments.forEach { assessmentDao.insert(it.toEntity()) }
        syncMilestones()
    }
}
