package com.anchor.recovery.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.data.db.entity.CheckInEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CheckInDaoTest {

    private lateinit var database: AppDatabase
    private val dao get() = database.checkInDao()

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
    fun `插入后可以按日期查询并升序列出`() = runTest {
        dao.insert(CheckInEntity(date = "2024-06-15", note = "b", createdAt = 2))
        dao.insert(CheckInEntity(date = "2024-06-14", note = "a", createdAt = 1))

        assertEquals("b", dao.byDate("2024-06-15")?.note)
        assertEquals(listOf("2024-06-14", "2024-06-15"), dao.all().map { it.date })
        assertEquals(listOf("2024-06-14", "2024-06-15"), dao.observeAll().first().map { it.date })
    }

    @Test
    fun `同一天重复打卡被唯一索引拦下且只保留一条`() = runTest {
        val first = dao.insert(CheckInEntity(date = "2024-06-15", note = "第一次", createdAt = 1))
        val second = dao.insert(CheckInEntity(date = "2024-06-15", note = "第二次", createdAt = 2))

        check(first > 0) { "首次插入应返回行号" }
        assertEquals(-1L, second)
        assertEquals(1, dao.all().size)
        assertEquals("第一次", dao.byDate("2024-06-15")?.note)
    }

    @Test
    fun `按日期删除与清空`() = runTest {
        dao.insert(CheckInEntity(date = "2024-06-15", createdAt = 1))
        dao.insert(CheckInEntity(date = "2024-06-16", createdAt = 2))

        dao.deleteByDate("2024-06-15")
        assertEquals(listOf("2024-06-16"), dao.all().map { it.date })

        dao.clear()
        assertEquals(0, dao.all().size)
    }
}
