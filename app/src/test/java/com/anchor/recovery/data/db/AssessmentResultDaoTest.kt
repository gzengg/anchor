package com.anchor.recovery.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.data.db.entity.AssessmentResultEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class AssessmentResultDaoTest {

    private lateinit var database: AppDatabase
    private val dao get() = database.assessmentResultDao()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `latestOfType 只取该类型最近一次结果`() = runTest {
        dao.insert(
            AssessmentResultEntity(
                type = "CSBD",
                takenAt = 1_000,
                totalScore = 30,
                level = "中度",
                answersJson = "[1,1]",
            ),
        )
        dao.insert(
            AssessmentResultEntity(
                type = "CSBD",
                takenAt = 3_000,
                totalScore = 12,
                level = "轻度",
                answersJson = "[0,0]",
            ),
        )
        dao.insert(
            AssessmentResultEntity(
                type = "MORAL",
                takenAt = 5_000,
                totalScore = 20,
                level = "高行为 · 高冲突",
                answersJson = "[3,3]",
            ),
        )

        assertEquals("轻度", dao.latestOfType("CSBD")?.level)
        assertEquals(5_000L, dao.latestOfType("MORAL")?.takenAt)
        assertNull(dao.latestOfType("NOT_EXIST"))
        assertEquals(listOf(5_000L, 3_000L, 1_000L), dao.observeAll().first().map { it.takenAt })
    }

    @Test
    fun `清空结果表`() = runTest {
        dao.insert(
            AssessmentResultEntity(
                type = "MORAL",
                takenAt = 1_000,
                totalScore = 1,
                level = "低",
                answersJson = "[]",
            ),
        )
        dao.clear()
        assertEquals(0, dao.observeAll().first().size)
    }
}
