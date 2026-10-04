package com.anchor.recovery.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.R
import com.anchor.recovery.core.clock.Clock
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import com.anchor.recovery.core.model.UrgeTool
import com.anchor.recovery.data.content.ContentRepository
import com.anchor.recovery.data.db.AppDatabase
import com.anchor.recovery.data.db.entity.CheckInEntity
import com.anchor.recovery.data.repo.AnchorRepository
import com.anchor.recovery.data.settings.AnchorSettings
import com.anchor.recovery.ui.components.AnchorIconButton
import com.anchor.recovery.ui.components.AnchorNavBar
import com.anchor.recovery.ui.components.AnchorTab
import com.anchor.recovery.ui.components.AnchorTabBar
import com.anchor.recovery.ui.components.LocalAnchorNavBarState
import com.anchor.recovery.ui.components.rememberAnchorNavBarState
import com.anchor.recovery.ui.home.CheckInScreen
import com.anchor.recovery.ui.home.HomeScreen
import com.anchor.recovery.ui.home.PhaseCard
import com.anchor.recovery.ui.journal.TimelineScreen
import com.anchor.recovery.ui.library.LibraryScreen
import com.anchor.recovery.ui.milestones.MilestoneWallScreen
import com.anchor.recovery.ui.milestones.MilestoneWallViewModel
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.tools.DelayToolScreen
import com.anchor.recovery.ui.tools.DelayToolViewModel
import com.anchor.recovery.ui.tools.RelapseEditScreen
import com.anchor.recovery.ui.tools.RelapseEditViewModel
import com.anchor.recovery.ui.tools.ToolsScreen
import com.anchor.recovery.ui.tools.UrgeSurfingScreen
import com.anchor.recovery.ui.tools.UrgeSurfingViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor
/**
 * 把真实界面渲染成 PNG（发布视频用的「界面复刻 vs 真机」素材）。
 *
 * **为什么能在 JVM 出图**：Robolectric 4.14 的 native graphics（`@GraphicsMode(NATIVE)`）提供真实
 * Skia 光栅化，`captureToImage()` 就能拿到有像素的位图；`@Config(qualifiers = ...)` 把画布钉成
 * iPhone 尺寸的手机竖屏（393×852dp @ xxhdpi = 1179×2556 px），出图和真机截图可比。
 *
 * 数据用 Room 内存库 + 固定时钟（`@Before` 里 seed），不改 app 源码；界面外壳（Scaffold + 顶栏 + Tab 栏）
 * 按 `AnchorApp.kt` 的单 Scaffold 结构在测试里复刻一份，让截图带上真实导航栏与底栏。
 *
 * 每个 `@Test` 独立出一张图，`@After` 把本轮结果写进 `film/shots/manifest.json`。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-port-xxhdpi")
class FilmShotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var database: AppDatabase
    private lateinit var repository: AnchorRepository
    private lateinit var settings: AnchorSettings
    private lateinit var content: ContentRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // 与 AnchorCoreFlowUiTest 同款：直连执行器 + 允许主线程查询，Room 的 Flow 同步发射。
        val directExecutor = Executor { it.run() }
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(directExecutor)
            .setTransactionExecutor(directExecutor)
            .build()
        repository = AnchorRepository(database, ShotClock(TODAY))
        settings = AnchorSettings(context)
        content = ContentRepository(context.assets)
        seedHistory()
    }

    @After
    fun tearDown() {
        database.close()
        // 静态结论 + 本轮动态记录的失败原因。静态部分每次重建，避免跨用例重复累积。
        val allNotes = JSONArray()
        BASE_NOTES.forEach { allNotes.put(it) }
        for (index in 0 until notes.length()) allNotes.put(notes.get(index))
        val root = JSONObject()
            .put("generator", "app/src/testDebug/java/com/anchor/recovery/ui/FilmShotTest.kt")
            .put(
                "renderer",
                "Robolectric 4.14.1 + Compose ui-test；@GraphicsMode(NATIVE)；" +
                    "@Config(sdk=35, qualifiers=w393dp-h852dp-port-xxhdpi)；" +
                    "Room 内存库 + 固定时钟 TODAY=2024-05-20；出图走 activity.window.decorView.draw(Canvas(bitmap))",
            )
            .put("pixelSize", "393dp x 852dp @ xxhdpi(3.0) = 1179 x 2556 px")
            .put("systemWindowInsetsPx", windowInsetsPx ?: JSONObject.NULL)
            .put("reproduce", "$CMD $CLASS_FILTER  （在 E:\\Anchor 下执行）")
            .put("shots", shots)
            .put("notes", allNotes)
        File(SHOTS_DIR).mkdirs()
        File(SHOTS_DIR, "manifest.json").writeText(root.toString(2) + "\n", Charsets.UTF_8)
    }

    // ---------------------------------------------------------------- 屏幕

    @Test
    fun home() {
        shot(
            name = "home",
            screen = "首页 HomeScreen（连续天数卡 + 打卡 + 速援工具 + 阶段卡 + 知识库入口）",
            state = "42 天连续打卡（含今天）；最长 42 天；距上次破戒 42 天；下一里程碑 90 天；进度 46%",
            titleRes = R.string.route_title_home,
            tabIndex = 0,
        ) {
            HomeScreen(
                viewModel = HomeViewModel(repository),
                onOpenCheckIn = {},
                onOpenUrgeSurfing = {},
                onOpenDelayTool = {},
                onOpenArticle = {},
                onOpenLibrary = {},
            )
        }
    }

    @Test
    fun checkin() {
        shot(
            name = "checkin",
            screen = "今日打卡 CheckInScreen",
            state = "今天已打卡（按钮变「今天已打卡」+「撤销今天」）",
            titleRes = R.string.route_title_check_in,
            tabIndex = null,
        ) {
            CheckInScreen(viewModel = CheckInViewModel(repository))
        }
    }

    @Test
    fun milestones() {
        shot(
            name = "milestones",
            screen = "里程碑徽章墙 MilestoneWallScreen",
            state = "42 天连续打卡 → 已点亮「第 1 天 / 第 1 周 / 30 天」",
            titleRes = R.string.route_title_milestones,
            tabIndex = null,
        ) {
            MilestoneWallScreen(viewModel = MilestoneWallViewModel(repository))
        }
    }

    @Test
    fun phases() {
        shot(
            name = "phases",
            screen = "戒断阶段卡 PhaseCard（五个阶段中的当前阶段）",
            state = "dayNumber = 42 → 阶段 3/5，含「可能经历 / 可做的事 / 警示」",
            titleRes = R.string.route_title_home,
            tabIndex = null,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PhaseCard(dayNumber = 42, onOpenArticle = {})
            }
        }
    }

    @Test
    fun library() {
        // 预热缓存：ContentRepository.snapshot() 命中缓存时不再切 IO 线程，截图时间点才可控。
        runBlocking { content.snapshot() }
        val placeholder = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.library_search_placeholder)
        shot(
            name = "library",
            screen = "知识库列表 LibraryScreen",
            state = "assets/content/*.json 全量文章；无分类筛选、无检索词",
            titleRes = R.string.route_title_library,
            tabIndex = 2,
            awaitText = placeholder,
        ) {
            LibraryScreen(content = content, onOpenArticle = {})
        }
    }

    @Test
    fun urge() {
        shot(
            name = "urge",
            screen = "渴求冲浪 UrgeSurfingScreen",
            state = "UrgeSurfingViewModel 初始态（未开始冲浪）",
            titleRes = R.string.route_title_urge_surfing,
            tabIndex = null,
        ) {
            UrgeSurfingScreen(viewModel = UrgeSurfingViewModel(repository), onDone = {})
        }
    }

    @Test
    fun delay() {
        shot(
            name = "delay",
            screen = "十分钟延时 DelayToolScreen",
            state = "DelayToolViewModel 初始态（计时未启动）",
            titleRes = R.string.route_title_delay_tool,
            tabIndex = null,
        ) {
            DelayToolScreen(viewModel = DelayToolViewModel(repository, settings), onDone = {})
        }
    }

    @Test
    fun relapse() {
        shot(
            name = "relapse",
            screen = "记录一次破戒 RelapseEditScreen",
            state = "RelapseEditViewModel 空表单（保存按钮禁用）",
            titleRes = R.string.route_title_relapse_edit,
            tabIndex = null,
        ) {
            RelapseEditScreen(viewModel = RelapseEditViewModel(repository), onSaved = {})
        }
    }

    @Test
    fun journal() {
        shot(
            name = "journal",
            screen = "日志与统计 TimelineScreen",
            state = "筛选「全部」；打卡 42 条 + 渴求 3 条 + 破戒 4 条 + 统计卡",
            titleRes = R.string.route_title_journal,
            tabIndex = 3,
        ) {
            TimelineScreen(viewModel = TimelineViewModel(repository), onOpenRelapseEdit = {})
        }
    }

    @Test
    fun tools() {
        shot(
            name = "tools",
            screen = "工具页 ToolsScreen",
            state = "静态入口列表（无需状态）",
            titleRes = R.string.route_title_tools,
            tabIndex = 1,
        ) {
            ToolsScreen(
                onOpenUrgeSurfing = {},
                onOpenDelayTool = {},
                onOpenRelapseEdit = {},
                onOpenAssessmentHub = {},
                onOpenMilestones = {},
            )
        }
    }

    // ---------------------------------------------------------------- 设施

    /**
     * 渲染一个屏幕并出图。
     *
     * 失败时把原因写进 manifest 的 notes 再抛出：单张出不了图不应该让整轮结果无可查。
     */
    private fun shot(
        name: String,
        screen: String,
        state: String,
        titleRes: Int,
        tabIndex: Int?,
        awaitText: String? = null,
        content: @Composable () -> Unit,
    ) {
        try {
            compose.setContent {
                AnchorTheme(darkTheme = false) {
                    ShotFrame(titleRes = titleRes, tabIndex = tabIndex) { content() }
                }
            }
            compose.waitForIdle()
            if (awaitText != null) {
                compose.waitUntil(timeoutMillis = 30_000) {
                    compose.onAllNodesWithText(awaitText).fetchSemanticsNodes().isNotEmpty()
                }
            }
            compose.waitForIdle()

            val bitmap = captureWindow()
            val file = File(SHOTS_DIR, "$name.png")
            val (width, height) = writePng(bitmap, file)
            windowInsetsPx = systemWindowInsetsPx()
            shots.put(
                JSONObject()
                    .put("name", name)
                    .put("screen", screen)
                    .put("state", state)
                    .put("file", "film/shots/$name.png")
                    .put("width", width)
                    .put("height", height)
                    .put("reproduce", "$CMD --tests \"com.anchor.recovery.ui.FilmShotTest.$name\""),
            )
        } catch (error: Throwable) {
            notes.put("$name 出图失败：${error::class.java.name}: ${error.message}")
            throw error
        }
    }

    /** `AnchorApp.kt` 的单 Scaffold 结构：顶栏 + 底部 Tab 栏 + 把滚动状态交给顶栏。 */
    @Composable
    private fun ShotFrame(titleRes: Int, tabIndex: Int?, body: @Composable () -> Unit) {
        val navBarState = rememberAnchorNavBarState()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AnchorNavBar(
                    title = stringResource(titleRes),
                    onBack = if (tabIndex == null) ({}) else null,
                    titleAlpha = { navBarState.titleAlpha() },
                    actions = {
                        if (tabIndex != null) {
                            AnchorIconButton(
                                icon = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.nav_settings),
                                onClick = {},
                            )
                        }
                    },
                )
            },
            bottomBar = {
                if (tabIndex != null) {
                    AnchorTabBar(tabs = tabs(), selectedIndex = tabIndex, onSelect = {})
                }
            },
        ) { padding ->
            CompositionLocalProvider(LocalAnchorNavBarState provides navBarState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding),
                ) {
                    body()
                }
            }
        }
    }

    /** 与 `AnchorApp.tabBarItems()` 同款四项（选中实心、未选中线形）。 */
    @Composable
    private fun tabs(): List<AnchorTab> = listOf(
        AnchorTab(stringResource(R.string.nav_tab_home), Icons.Outlined.Home, Icons.Filled.Home),
        AnchorTab(stringResource(R.string.nav_tab_tools), Icons.Outlined.Build, Icons.Filled.Build),
        AnchorTab(
            stringResource(R.string.nav_tab_library),
            Icons.AutoMirrored.Outlined.MenuBook,
            Icons.AutoMirrored.Filled.MenuBook,
        ),
        AnchorTab(
            stringResource(R.string.nav_tab_journal),
            Icons.Outlined.EditNote,
            Icons.Filled.EditNote,
        ),
    )

    /**
     * 出图：把 Activity 的 decorView 画到软件 Canvas 上。
     *
     * 不用 `compose.onRoot().captureToImage()`：它在 Robolectric 下走 `WindowCapture.forceRedraw`，
     * 会等 `ViewTreeObserver.OnDrawListener` 被真实绘制回调——JVM 里没有真实绘制帧，只会抛
     * `ComposeTimeoutException: Condition still not satisfied after 2000 ms`（见 film/shots/manifest.json 的 notes）。
     * `view.draw(canvas)` 同步触发 Compose 的 `dispatchDraw`，配合 `@GraphicsMode(NATIVE)` 的真实 Skia
     * 即可拿到真实像素（与 Roborazzi 同一路径）。
     */
    private fun captureWindow(): Bitmap {
        val activity = compose.activity
        val view: View = activity.window.decorView
        val metrics = activity.resources.displayMetrics
        val width = view.width.takeIf { it > 0 } ?: metrics.widthPixels
        val height = view.height.takeIf { it > 0 } ?: metrics.heightPixels
        if (view.width != width || view.height != height) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, width, height)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    /**
     * 测试环境里窗口实际的系统栏 insets（真机上是状态栏/导航栏高度）。
     *
     * 记录下来是因为 Robolectric 不渲染 system UI、insets 通常为 0：真机截图顶部会比本组图多出状态栏、
     * 底部多出导航栏，逐像素比对时必须先按 App 自己绘制的内容对齐。
     */
    private fun systemWindowInsetsPx(): JSONObject {
        val bars = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
            ?.getInsets(WindowInsetsCompat.Type.systemBars())
        return JSONObject().put("top", bars?.top ?: 0).put("bottom", bars?.bottom ?: 0)
    }

    /** PNG 由 Skia 编码（Robolectric native graphics 下真实可用）。 */
    private fun writePng(bitmap: Bitmap, file: File): Pair<Int, Int> {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "Bitmap.compress 返回 false" }
        }
        return bitmap.width to bitmap.height
    }

    // ---------------------------------------------------------------- 假数据

    /**
     * 42 天连续打卡（含今天）+ 破戒/渴求历史。
     *
     * 破戒都排在 42 天连续段之前（今天是 2024-05-20，破戒落在 -42/-60/-75/-90 天），
     * 这样首页大数字是 42、而日志页仍有统计卡要用的破戒样本。
     */
    private fun seedHistory() = runBlocking {
        (41 downTo 0).forEach { offset ->
            val date = dayOffset(-offset)
            database.checkInDao().insert(
                CheckInEntity(
                    date = date.toString(),
                    note = "",
                    createdAt = noonOf(date).toEpochMilliseconds(),
                ),
            )
        }
        repository.syncMilestones()

        listOf(-42, -60, -75, -90).forEachIndexed { index, offset ->
            repository.recordRelapse(
                RelapseRecord(
                    occurredAt = noonOf(dayOffset(offset)),
                    situation = listOf("深夜独处", "出差住酒店", "和伴侣吵架后", "连续加班")[index],
                    emotions = listOf(
                        listOf("孤独", "疲惫"),
                        listOf("焦虑", "无聊"),
                        listOf("被拒", "愤怒"),
                        listOf("疲惫", "空虚"),
                    )[index],
                    triggers = listOf(
                        listOf("深夜独处", "刷手机"),
                        listOf("独处", "外出住店"),
                        listOf("情绪起落", "争吵"),
                        listOf("加班", "熬夜"),
                    )[index],
                    note = listOf(
                        "本来只是想放空一下，结果刷到很晚",
                        "换了环境，晚上没人管就松懈了",
                        "吵完架心里堵，想找个出口",
                        "这周连着加班，睡前完全没有抵抗力",
                    )[index],
                ),
            )
        }

        listOf(-1 to UrgeTool.URGE_SURFING, -3 to UrgeTool.DELAY_TOOL, -7 to UrgeTool.URGE_SURFING)
            .forEach { (offset, tool) ->
                repository.recordUrgeEpisode(
                    UrgeEpisodeRecord(
                        startedAt = dayOffset(offset).let { LocalDateTime(it, LocalTime(22, 40)).toInstant(UTC) },
                        durationSec = if (tool == UrgeTool.DELAY_TOOL) 600 else 240,
                        peakIntensity = 8,
                        endIntensity = 3,
                        tool = tool,
                    ),
                )
            }
    }

    private fun dayOffset(days: Int): LocalDate = LocalDate.fromEpochDays(TODAY.toEpochDays() + days)

    private fun noonOf(date: LocalDate): Instant = LocalDateTime(date, LocalTime(12, 0)).toInstant(UTC)

    private companion object {
        val TODAY: LocalDate = LocalDate(2024, 5, 20)
        val UTC: TimeZone = TimeZone.UTC
        const val SHOTS_DIR = "E:/Anchor/film/shots"
        const val CMD = "gradlew.bat :app:testDebugUnitTest"
        const val CLASS_FILTER = "--tests \"com.anchor.recovery.ui.FilmShotTest\""

        /** 无论成功与否都成立的结论；与脚本同源，避免 manifest 里的发现只活在对话里。 */
        val BASE_NOTES: List<String> = listOf(
            "出图手法：compose.onRoot().captureToImage() 在本环境不可用。它进入 " +
                "androidx.compose.ui.test.WindowCapture.forceRedraw，等 ViewTreeObserver.OnDrawListener 被真实绘制帧回调，" +
                "JVM 里没有硬件绘制帧，抛 androidx.compose.ui.test.junit4.ComposeTimeoutException: " +
                "Condition still not satisfied after 2000 ms。改用 activity.window.decorView.draw(Canvas(softwareBitmap)) " +
                "同步触发 Compose 的 dispatchDraw（与 Roborazzi 同一路径），10/10 张出图成功。",
            "系统栏不在画面里：Robolectric 不渲染 system UI，窗口 system window insets 为 0（见 systemWindowInsetsPx）。" +
                "真机截图顶部多出状态栏、底部多出导航栏高度，与真机比对时请按 App 自己绘制的顶栏/底栏内容对齐，" +
                "不要假设两张图的 y=0 是同一位置。",
            "字体差异：Robolectric 用平台默认字体，真机为厂商字体，字形宽度可能有细微差别（影响换行点）。",
            "phases 没有对应“五个阶段总览”页面：PhaseCard 只渲染当前天数所属的单个阶段（phaseFor(dayNumber)），" +
                "本图是 dayNumber=42 → 第 3/5 阶段。要看其余阶段改 dayNumber（1/15/42/75/120）即可，没有第二种界面形态。",
            "library 的分类筛选行在 393dp 宽下横向溢出（截图右侧被裁切），是真实布局的可横向滚动行，不是渲染缺陷。",
            "milestones 的徽章墙只能展示到本机假数据的天数：42 天连续打卡 → 已点亮 1 天/1 周/30 天，60 天与 90 天仍是未点亮态。",
        )

        /** 本轮已出的图；`@After` 统一写进 manifest.json。 */
        val shots = JSONArray()

        /** 出图失败或刻意跳过的屏幕及原因。 */
        val notes = JSONArray()

        /** 最近一次出图时的系统栏 insets；`@After` 写进 manifest.json 顶层。 */
        var windowInsetsPx: JSONObject? = null
    }
}

/** 固定时钟：断言与截图里的日期不随运行时刻变化。（不能叫 FixedClock：同包下 AnchorCoreFlowUiTest 已有同名私有类，重名会撞 JVM 类名。） */
private class ShotClock(
    private val today: LocalDate,
    override val timeZone: TimeZone = TimeZone.UTC,
) : Clock {

    override fun now(): Instant = LocalDateTime(today, LocalTime(12, 0)).toInstant(timeZone)
}
