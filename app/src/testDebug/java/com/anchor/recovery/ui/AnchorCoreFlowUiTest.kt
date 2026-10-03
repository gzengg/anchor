package com.anchor.recovery.ui

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.core.clock.Clock
import com.anchor.recovery.data.db.AppDatabase
import com.anchor.recovery.data.db.entity.CheckInEntity
import com.anchor.recovery.data.repo.AnchorRepository
import com.anchor.recovery.ui.assessment.CsbdQuizViewModel
import com.anchor.recovery.ui.assessment.QuizScreen
import com.anchor.recovery.ui.home.CheckInScreen
import com.anchor.recovery.ui.milestones.MilestoneWallScreen
import com.anchor.recovery.ui.milestones.MilestoneWallViewModel
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.tools.RelapseEditScreen
import com.anchor.recovery.ui.tools.RelapseEditViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executor

/**
 * 核心流程的 Compose UI 测试（P3-2）。
 *
 * **为什么跑在 JVM（Robolectric）而不是 `androidTest`**：本机没有真机/模拟器，放进 `androidTest`
 * 就等于永远不执行；写在这里就能随 `:app:testDebugUnitTest` 一起纳入门禁。
 * 代价是拿不到 SQLCipher 原生库与 Keystore（那是真机用例 `SqlCipherAvailabilityTest` 的职责），
 * 所以数据库用 Room 的内存库 + 直连执行器，不经 `AnchorDatabaseFactory`。
 *
 * 时钟固定：断言里出现的日期都锚在 [TODAY]，不依赖运行时刻（周末/月末也不会飘）。
 */
@RunWith(RobolectricTestRunner::class)
class AnchorCoreFlowUiTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: AnchorRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // 直连执行器 + 允许主线程查询：Room 的 Flow 在同一线程上同步发射，测试不用等待后台线程。
        val directExecutor = Executor { it.run() }
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(directExecutor)
            .setTransactionExecutor(directExecutor)
            .build()
        repository = AnchorRepository(database, FixedClock(TODAY))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun checkInFlow_savesTodayAndReflectsInUi() {
        val viewModel = CheckInViewModel(repository)
        compose.setContent {
            AnchorTheme { CheckInScreen(viewModel = viewModel) }
        }

        compose.onNodeWithText("状态：今天还没打卡").assertIsDisplayed()
        compose.onNodeWithText("保存打卡").performClick()

        awaitCount(1, "打卡应写入一条记录") { repository.checkIns.first().size }
        compose.onNodeWithText("状态：已打卡").assertIsDisplayed()
        // 打过卡后按钮文案与「撤销」入口都要跟着变，避免用户重复点击。
        compose.onNodeWithText("今天已打卡").assertIsDisplayed()
        compose.onNodeWithText("撤销今天").assertIsDisplayed()
    }

    @Test
    fun relapseForm_savesAndOffersTimelineEntry() {
        val viewModel = RelapseEditViewModel(repository)
        var saved = false
        compose.setContent {
            AnchorTheme {
                RelapseEditScreen(viewModel = viewModel, onSaved = { saved = true })
            }
        }

        // 空表单不能保存：按钮应处于禁用态（比「点了没反应」更直接，也不会因异步竞态而假绿）。
        compose.onNodeWithText("保存记录").performScrollTo().assertIsNotEnabled()

        // 只填备注一项即可保存（「至少填一项」是产品口径，这里顺带把它测出来）。
        compose.onNodeWithText("备注（当时在想什么）").performScrollTo().performTextInput("今天很累，想被安慰")
        compose.onNodeWithText("保存记录").performScrollTo().assertIsDisplayed().performClick()

        awaitCount(1, "表单应写入一条破戒记录") { repository.relapses.first().size }
        compose.onNodeWithText("去看日志与统计").performScrollTo().performClick()
        assertTrue("保存成功后应能进入日志页", saved)
    }

    @Test
    fun quizFlow_answersAllPagesThenSubmits() {
        val viewModel = CsbdQuizViewModel(repository)
        var finished = false
        compose.setContent {
            val state by viewModel.state.collectAsState()
            AnchorTheme {
                QuizScreen(
                    state = state,
                    onSelect = viewModel::select,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                    onSubmit = viewModel::submit,
                    onFinished = { finished = true },
                )
            }
        }

        val total = viewModel.state.value.total
        compose.onNodeWithText("第 1 / $total 题", substring = true).assertIsDisplayed()

        repeat(total) { index ->
            compose.onNodeWithText("完全符合").performClick()
            compose.waitForIdle()
            if (index < total - 1) {
                compose.onNodeWithText("下一题").performClick()
                compose.waitForIdle()
            }
        }

        // 全部作答后才允许提交；提交后落库并跳结果页。
        compose.onNodeWithText("提交并查看结果").performScrollTo().assertIsDisplayed().performClick()

        awaitCount(1, "问卷结果应写入一条") { repository.assessments.first().size }
        // 提交后的跳转由 ViewModel 的状态驱动，晚一帧才发生：等到它发生再断言。
        compose.waitUntil(timeoutMillis = 5_000) { finished }
        assertTrue("提交成功后应通知上层跳转结果页", finished)
    }

    @Test
    fun milestoneWall_lightsUpOnlyReachedBadges() {
        runBlocking {
            // 连续 7 天（含今天）→ 第 1 天、第 1 周点亮，其余仍未达成。
            val entities = (6 downTo 0).map { offset ->
                CheckInEntity(
                    date = LocalDate.fromEpochDays(TODAY.toEpochDays() - offset).toString(),
                    note = "",
                    createdAt = 0,
                )
            }
            entities.forEach { database.checkInDao().insert(it) }
            repository.syncMilestones()
        }

        compose.setContent {
            AnchorTheme { MilestoneWallScreen(viewModel = MilestoneWallViewModel(repository)) }
        }

        compose.onNodeWithText("第 1 天").assertIsDisplayed()
        compose.onNodeWithText("第 1 周").assertIsDisplayed()
        compose.onNodeWithText("90 天重启").performScrollTo().assertIsDisplayed()
        // 已达成 2 个（文案含「达成」），其余 3 个显示还差几天。
        compose.onAllNodesWithText("达成", substring = true).assertCountEquals(2)
        compose.onAllNodesWithText("还差", substring = true).assertCountEquals(3)
    }

    @Test
    fun checkInScreen_atFontScale1_3_keepsPrimaryActionReachable() {
        val viewModel = CheckInViewModel(repository)
        compose.setContent {
            // 系统字体放大到 1.3 倍（无障碍常见值）：不是缩小字号，而是内容要能被滚到、按钮仍可点。
            // 只改 fontScale，保留真实 density，否则整张页面的尺寸假设都被改掉。
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = base.density, fontScale = 1.3f),
            ) {
                AnchorTheme { CheckInScreen(viewModel = viewModel) }
            }
        }

        compose.onNodeWithText("今日打卡").assertIsDisplayed()
        compose.onNodeWithText("保存打卡").performScrollTo().assertIsDisplayed().performClick()

        awaitCount(1, "大字体下打卡仍应能保存") { repository.checkIns.first().size }
    }

    /**
     * ViewModel 的写库走 `viewModelScope`（主线程调度器），断言可能跑在写入之前；
     * 这里等到目标条数出现再断言，避免用 sleep 掩盖真正的失败。
     */
    private fun awaitCount(expected: Int, label: String, count: suspend () -> Int) {
        compose.waitUntil(timeoutMillis = 5_000) { runBlocking { count() } == expected }
        assertEquals(label, expected, runBlocking { count() })
    }

    private companion object {
        val TODAY: LocalDate = LocalDate(2024, 5, 20)
    }
}

/** 固定时钟：日期逻辑按本地自然日判定，这里把「今天」钉死，断言不随运行时刻变化。 */
private class FixedClock(
    private val today: LocalDate,
    override val timeZone: TimeZone = TimeZone.UTC,
) : Clock {

    override fun now(): Instant = LocalDateTime(today, LocalTime(12, 0)).toInstant(timeZone)
}
