package com.anchor.recovery.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.core.clock.FakeClock
import com.anchor.recovery.core.export.DataExporter
import com.anchor.recovery.core.export.DataImporter
import com.anchor.recovery.core.export.ImportResult
import com.anchor.recovery.core.export.ImportedData
import com.anchor.recovery.core.model.AssessmentRecord
import com.anchor.recovery.core.model.AssessmentType
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import com.anchor.recovery.core.model.UrgeTool
import com.anchor.recovery.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * P0-3 的端到端验收：造数据 → 导出 → 清空 → 导入 → 断言一致，而且再导出一次应该逐字节相同。
 *
 * 走真实的 Room 库（内存库，无 SQLCipher，所以能本地跑），只把「用户选文件」这段系统交互省掉。
 */
@RunWith(RobolectricTestRunner::class)
class ImportExportRoundTripTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: AnchorRepository
    private lateinit var clock: FakeClock

    private val exportedAt = Instant.parse("2024-05-04T09:31:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock = FakeClock(Instant.parse("2024-05-04T09:00:00Z"))
        repository = AnchorRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seed() {
        repository.checkInToday(note = "第一天")
        clock.advanceBy(kotlin.time.Duration.parse("1d"))
        repository.checkInToday(note = "第二天")
        repository.recordRelapse(
            RelapseRecord(
                occurredAt = Instant.parse("2024-05-02T22:10:00Z"),
                situation = "独处",
                emotions = listOf("焦虑", "空虚"),
                triggers = listOf("刷到擦边内容"),
                note = "事后写了复盘",
            ),
        )
        repository.recordUrgeEpisode(
            UrgeEpisodeRecord(
                startedAt = Instant.parse("2024-05-03T20:00:00Z"),
                durationSec = 600,
                peakIntensity = 8,
                endIntensity = 3,
                tool = UrgeTool.DELAY_TOOL,
            ),
        )
        repository.recordAssessment(
            AssessmentRecord(
                type = AssessmentType.MORAL,
                takenAt = Instant.parse("2024-05-01T10:00:00Z"),
                totalScore = 21,
                level = "中度",
                answers = listOf(1, 2, 3, 4, 5),
            ),
        )
    }

    private suspend fun exportJson(): String = DataExporter.toJson(
        DataExporter.build(
            appName = "磐石",
            appVersion = "0.1.0",
            exportedAt = exportedAt,
            checkIns = repository.checkIns.first(),
            relapses = repository.relapses.first(),
            urgeEpisodes = repository.urgeEpisodes.first(),
            assessments = repository.assessments.first(),
            reminderEnabled = true,
            reminderTime = "21:30",
            motivationPrompts = listOf("先不做决定", "去洗个澡"),
        ),
    )

    @Test
    fun `导出后清空再导入四类记录逐字段还原且再导出内容相同`() = runTest {
        seed()
        val before = exportJson()

        repository.clearAll()
        assertTrue(repository.checkIns.first().isEmpty())
        assertTrue(repository.relapses.first().isEmpty())
        assertTrue(repository.urgeEpisodes.first().isEmpty())
        assertTrue(repository.assessments.first().isEmpty())

        val ready = assertIs<ImportResult.Ready>(DataImporter.parse(before))
        repository.replaceAll(ready.data)

        assertEquals(2, repository.checkIns.first().size)
        assertEquals(1, repository.relapses.first().size)
        assertEquals(1, repository.urgeEpisodes.first().size)
        assertEquals(1, repository.assessments.first().size)
        assertEquals("打卡 2 天 · 破戒 1 次 · 渴求 1 次 · 问卷 1 份", ready.data.countsLine())

        // 再导出一次与导入前逐字节相同：说明 id、时间、标签、设置都没在往返中被改动。
        assertEquals(before, exportJson())
    }

    @Test
    fun `导入的文件带重复记录 id 时整笔回滚原记录不动`() = runTest {
        seed()
        val colliding = ImportedData(
            checkIns = emptyList(),
            relapses = listOf(
                RelapseRecord(id = 5, occurredAt = Instant.parse("2024-07-01T10:00:00Z")),
                RelapseRecord(id = 5, occurredAt = Instant.parse("2024-07-02T10:00:00Z")),
            ),
            urgeEpisodes = emptyList(),
            assessments = emptyList(),
            reminderEnabled = false,
            reminderTime = "21:00",
            motivationPrompts = emptyList(),
        )

        assertFailsWith<Exception> { repository.replaceAll(colliding) }

        // 事务回滚：原记录还在，没有被「清空一半」。
        assertEquals(2, repository.checkIns.first().size)
        assertEquals(1, repository.relapses.first().size)
    }

    @Test
    fun `导入空记录文件等于清空四张表`() = runTest {
        seed()

        val empty = ImportedData(
            checkIns = emptyList(),
            relapses = emptyList(),
            urgeEpisodes = emptyList(),
            assessments = emptyList(),
            reminderEnabled = false,
            reminderTime = "21:00",
            motivationPrompts = emptyList(),
        )
        assertTrue(empty.isEmpty)

        repository.replaceAll(empty)

        assertTrue(repository.checkIns.first().isEmpty())
        assertTrue(repository.relapses.first().isEmpty())
        assertTrue(repository.urgeEpisodes.first().isEmpty())
        assertTrue(repository.assessments.first().isEmpty())
    }
}
