package com.anchor.recovery.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.core.clock.FakeClock
import com.anchor.recovery.core.model.AssessmentRecord
import com.anchor.recovery.core.model.AssessmentType
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import com.anchor.recovery.core.model.UrgeTool
import com.anchor.recovery.core.streak.CheckInDecision
import com.anchor.recovery.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AnchorRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: AnchorRepository

    private val today = LocalDate.parse("2024-06-15")
    private val clock = FakeClock.at(today)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AnchorRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `打卡后 streak 与今日状态同步更新且重复打卡被拦下`() = runTest {
        assertIs<CheckInDecision.Allowed>(repository.checkInToday(note = " 第一天 "))
        assertIs<CheckInDecision.AlreadyCheckedIn>(repository.checkInToday())

        val streak = repository.streak.first()
        assertEquals(1, streak.currentDays)
        assertEquals(1, streak.longestDays)
        assertTrue(streak.todayCheckedIn)
        assertEquals(1, repository.checkIns.first().size)
        assertEquals("第一天", repository.checkIns.first().first().note)
    }

    @Test
    fun `撤销今天打卡后 streak 归零`() = runTest {
        repository.checkInToday()
        repository.removeCheckIn(today)

        val streak = repository.streak.first()
        assertEquals(0, streak.currentDays)
        assertEquals(0, streak.totalCheckInDays)
    }

    @Test
    fun `复吸记录落地后清零当前 streak 并保留历史最长`() = runTest {
        val yesterday = LocalDate.fromEpochDays(today.toEpochDays() - 1)
        clock.setLocalDateTime(yesterday)
        repository.checkInToday()
        clock.setLocalDateTime(today)
        repository.checkInToday()

        repository.recordRelapse(
            RelapseRecord(
                occurredAt = clock.now(),
                situation = "深夜独自在家",
                emotions = listOf("焦虑"),
                triggers = listOf("失眠"),
            ),
        )

        val streak = repository.streak.first()
        assertEquals(0, streak.currentDays)
        assertEquals(2, streak.longestDays)
        assertEquals(1, repository.relapses.first().size)
        assertEquals(listOf("焦虑"), repository.relapses.first().first().emotions)
    }

    @Test
    fun `渴求记录与问卷结果能按领域模型读回`() = runTest {
        repository.recordUrgeEpisode(
            UrgeEpisodeRecord(
                startedAt = clock.now(),
                durationSec = 600,
                peakIntensity = 9,
                endIntensity = 4,
                tool = UrgeTool.DELAY_TOOL,
            ),
        )
        repository.recordAssessment(
            AssessmentRecord(
                type = AssessmentType.CSBD,
                takenAt = clock.now(),
                totalScore = 21,
                level = "中度",
                answers = listOf(1, 2, 3),
            ),
        )

        val episode = repository.urgeEpisodes.first().single()
        assertEquals(UrgeTool.DELAY_TOOL, episode.tool)
        assertEquals(9, episode.peakIntensity)

        val latest = repository.latestAssessment(AssessmentType.CSBD)
        assertEquals(21, latest?.totalScore)
        assertEquals(listOf(1, 2, 3), latest?.answers)
    }

    @Test
    fun `一键清空会清掉全部四张表`() = runTest {
        repository.checkInToday()
        repository.recordRelapse(RelapseRecord(occurredAt = clock.now()))
        repository.recordUrgeEpisode(
            UrgeEpisodeRecord(
                startedAt = clock.now(),
                durationSec = 60,
                peakIntensity = 5,
                endIntensity = 2,
                tool = UrgeTool.URGE_SURFING,
            ),
        )
        repository.recordAssessment(
            AssessmentRecord(
                type = AssessmentType.MORAL,
                takenAt = clock.now(),
                totalScore = 10,
                level = "低",
            ),
        )

        repository.clearAll()

        assertEquals(0, repository.checkIns.first().size)
        assertEquals(0, repository.relapses.first().size)
        assertEquals(0, repository.urgeEpisodes.first().size)
        assertEquals(0, repository.assessments.first().size)
    }
}
