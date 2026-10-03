package com.anchor.recovery.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.data.db.entity.RelapseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RelapseDaoTest {

    private lateinit var database: AppDatabase
    private val dao get() = database.relapseDao()

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
    fun `按发生时间倒序列出`() = runTest {
        dao.insert(RelapseEntity(occurredAt = 1_000, situation = "早", emotions = "焦虑", triggers = "手机"))
        dao.insert(RelapseEntity(occurredAt = 2_000, situation = "晚", emotions = "无聊", triggers = "失眠"))

        assertEquals(listOf(2_000L, 1_000L), dao.all().map { it.occurredAt })
        assertEquals(listOf(2_000L, 1_000L), dao.observeAll().first().map { it.occurredAt })
        assertEquals("焦虑", dao.all().last().emotions)
    }

    @Test
    fun `按 id 删除与清空`() = runTest {
        val id = dao.insert(RelapseEntity(occurredAt = 1_000))
        dao.insert(RelapseEntity(occurredAt = 2_000))

        dao.deleteById(id)
        assertEquals(1, dao.all().size)

        dao.clear()
        assertEquals(0, dao.all().size)
    }
}
