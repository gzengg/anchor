package com.anchor.recovery.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.data.db.entity.UrgeEpisodeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class UrgeEpisodeDaoTest {

    private lateinit var database: AppDatabase
    private val dao get() = database.urgeEpisodeDao()

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
    fun `记录渴求事件并按开始时间倒序列出`() = runTest {
        dao.insert(
            UrgeEpisodeEntity(
                startedAt = 1_000,
                durationSec = 120,
                peakIntensity = 8,
                endIntensity = 3,
                tool = "URGE_SURFING",
            ),
        )
        dao.insert(
            UrgeEpisodeEntity(
                startedAt = 2_000,
                durationSec = 600,
                peakIntensity = 9,
                endIntensity = 5,
                tool = "DELAY_TOOL",
            ),
        )

        assertEquals(listOf(2_000L, 1_000L), dao.all().map { it.startedAt })
        assertEquals("DELAY_TOOL", dao.observeAll().first().first().tool)
    }

    @Test
    fun `按 id 删除与清空`() = runTest {
        val id = dao.insert(
            UrgeEpisodeEntity(
                startedAt = 1_000,
                durationSec = 60,
                peakIntensity = 5,
                endIntensity = 2,
                tool = "URGE_SURFING",
            ),
        )

        dao.deleteById(id)
        assertEquals(0, dao.all().size)

        dao.insert(
            UrgeEpisodeEntity(
                startedAt = 2_000,
                durationSec = 60,
                peakIntensity = 5,
                endIntensity = 2,
                tool = "URGE_SURFING",
            ),
        )
        dao.clear()
        assertEquals(0, dao.all().size)
    }
}
