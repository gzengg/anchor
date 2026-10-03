# 磐石（Anchor）交付说明

版本：0.2.0（versionCode 2） · 包名：`com.anchor.recovery` · 交付日期：本次会话当日
验收依据：`磐石Anchor-开发执行提示词.md`（v0.1.0，第八节交付物要求逐条对应在下方五节）
与 `磐石Anchor-第二版开发计划.md`（v0.2.0 的 P0–P4 验收总表）

---

## 1. 实际依赖版本表

全部构建均在 **`--offline`** 下完成（无外网拉取）。版本取自 `gradle/libs.versions.toml`
与 `./gradlew --version`，解析结果用 `:app:dependencyInsight` 核对。

| 类别 | 组件 | 版本 |
|---|---|---|
| 构建 | Gradle Wrapper | **8.13**（提示词自查清单写的是 8.12；本机缓存里可用的是 8.13，故用 8.13） |
| 构建 | Android Gradle Plugin | 8.12.1 |
| 构建 | Kotlin / Compose 编译器插件 | 2.2.20 |
| 构建 | KSP | 2.2.20-2.0.2 |
| 构建 | JDK（Launcher / Daemon） | 20.0.1（Oracle），daemon 由 `gradle.properties` 的 `org.gradle.java.home=E:/JAVA/20` 固定 |
| Android | compileSdk / targetSdk / minSdk | 35 / 35 / 26 |
| Android | Build Tools | 35.0.0 |
| Compose | BOM | 2025.01.01 |
| Compose | material3（由 BOM 约束） | 1.3.1 |
| AndroidX | core-ktx | 1.15.0 |
| AndroidX | activity-compose | 1.9.3 |
| AndroidX | lifecycle（runtime-ktx / viewmodel-compose） | 2.8.7 |
| AndroidX | navigation-compose | 2.8.5 |
| AndroidX | room（runtime / ktx / compiler，走 KSP） | 2.7.1 |
| AndroidX | room-testing（迁移测试，仅测试源集） | 2.7.1 |
| 加密 | sqlcipher-android（SQLCipher 4.13.0 的 Android 绑定，含 JNI） | 4.13.0 |
| AndroidX | work-runtime-ktx | 2.10.0 |
| AndroidX | datastore-preferences | 1.1.7 |
| AndroidX | browser（CustomTabs） | 1.8.0 |
| 资源 | 打包字体 `res/font/harmonyos_sans_sc_{regular,medium,bold}.ttf`（HarmonyOS Sans SC 子集） | 字体文件 Version 1.0；子集字符集 = GB2312 6763 字 + 应用实际用到的字符 + 常用符号（见 §3.1） |
| kotlinx | coroutines-core / -android / -test | 1.10.2 |
| kotlinx | serialization-json | 1.8.1 |
| kotlinx | datetime | 0.6.1 |
| 测试 | kotlin-test-junit5 / junit-jupiter | 2.2.20 / 5.11.4（`:core`） |
| 测试 | junit4 / robolectric / androidx-test | 4.13.2 / 4.14.1 / core 1.6.1 · ext-junit 1.2.1（`:app`） |
| 测试 | compose ui-test-junit4 / ui-test-manifest（`:app` 的 Compose UI 测试跑在 JVM/Robolectric 上） | 由 Compose BOM 2025.01.01 约束 |
| 测试 | work-testing（提醒排程单测） | 2.10.0 |
| 测试 | androidx-test:runner（androidTest 运行时） | 1.6.2 |

未引入的第三方库（刻意的取舍）：无 DI 框架（手写 `AnchorApplication` 装配）、无图片加载库、
无 `core-splashscreen`（Android 12+ 用系统默认启动画面）、无 Cupertino/第三方 iOS 风格组件库
（P1 的 iOS 视觉全部自研在 `ui/theme` + `ui/components`）。
字体也不走依赖：3 个 TTF 直接放 `res/font/`，由 `ui/theme/AnchorFont.kt` 声明为一个 FontFamily（见 §3.1）。
权限：`SCHEDULE_EXACT_ALARM` 自 v0.2.0（P3-1）入 Manifest，但只在用户于设置页显式开启「准点提醒」
且拿到系统授权后才用（默认关，未授权/被收回自动回落 inexact，见 §5 限制 3）。

---

## 2. 最终测试统计（已去重）

统计来源：`test-results/**/TEST-*.xml` 里 `tests= / failures= / errors= / skipped=` 的累加值。

| 模块 | 测试类数 | 用例数 | 失败 | 错误 | 跳过 |
|---|---|---|---|---|---|
| `:core`（jvm library，单变体） | 22 | **162** | 0 | 0 | 0 |
| `:app`（Android 单元测试，debug 变体） | 21 | **83** | 0 | 0 | 0 |
| 合计（去重后） | 43 | **245** | 0 | 0 | 0 |

（v0.1.0 的对应数字是 30 类 / 184 用例：`:core` 151 + `:app` 33；v0.2.0 新增了加密/导入/迁移、
主题、里程碑、提醒策略、以及 P4 的 6 个文案映射测试类；v0.2.1 新增 `AnchorFontTest`（3 例）与
`AnchorSegmentedControlTest`（1 例，仅 debug 变体）。）

去重说明：`:app:test` 会分别跑 `testDebugUnitTest` 与 `testReleaseUnitTest`，两个变体是同一份源码
（release 变体 75 个用例）；上表只取 debug 变体的 83 个。另用源码 `@Test` 计数交叉核对
（`core/src/test` 162 + `app/src/test` 75 + `app/src/testDebug` 8 = 245），与 XML 一致。
`src/testDebug` 里的 8 例只跑在 debug 变体：迁移测试 `AppDatabaseMigrationTest` 7 例
（release 变体不打包 schema 资产）+ 段控件布局回归 `AnchorSegmentedControlTest` 1 例
（依赖只给 debug 变体的 `ui-test-manifest`，release 下会因找不到宿主 Activity 而报错）。

测试分布（可测逻辑全部落在 `:core`，UI 层保持哑渲染）：

- `:core`（22 类）：`StreakCalculator` / `CheckInPolicy` / `RebootFramework`、`WithdrawalPhaseResolver` /
  `WithdrawalPhaseContent`、`CsbdScorer` / `MoralIncongruenceScorer`、`UrgeSurfingSession` / `DelayTool` /
  `BreathingPattern`、`TriggerAnalyzer`、`ContentIndex` / `MarkdownLite`、`DataExporter` / `DataImporter`
  （导出导入往返、版本拒绝、坏字段拒绝、重复日期拒绝）、`Disclaimer`、`ReminderMessages` /
  `ReminderScheduling` / `ReminderTimeCalculator`、`MilestoneAchievements`、`DatabaseSecurity`。
- `:app`（21 类）：4 张表的 Room DAO / 实体映射 / 仓库（Robolectric）+ 导出 JSON 装配 + 导入整体替换
  端到端（导出→清空→导入→再导出逐字节一致）+ Room schema 迁移骨架（`src/testDebug`）、
  `AnchorThemeColorsTest`（明暗配色）、`AnchorFontTest`（v0.2.1 打包字体接线）、
  `AnchorSegmentedControlTest`（v0.2.1 段控件高度回归，仅 debug 变体）、`AppInfoTest`，
  以及 v0.2.0 新增的 6 个文案映射测试
  （`CoreTextTest` / `PhaseTextTest` / `QuestionnaireTextTest` / `ToolsJournalTextTest` /
  `LegalContentTextTest` / `MilestoneCopyComplianceTest`）、`ReminderCopyComplianceTest`、
  `AnchorCoreFlowUiTest`（Compose UI 测试跑在 JVM/Robolectric 上，无真机也能进门禁）。

v0.2.0 的设备端用例（`app/src/androidTest`，**本次未执行——本机无真机，按用户要求也不启模拟器**）：
`SqlCipherAvailabilityTest` 验 libsqlcipher 能加载、库文件不是明文、错口令打不开、Keystore 口令稳定。

### 2.1 P4 文案抽取后的残留清单（逐条核对过，全部有意保留）

扫描工具：`py content-tools/scan_hardcoded_text.py [--list] <path>`（注释不计，只统计 Kotlin 字符串字面量）。
P4 收尾后：`app/src/main/java` 的界面与通知文案 **0 处**（一个有 21 处，见下），
`core/src/main/kotlin` 从 83 处降到 **58 处**，全仓合计 79 处 / 13 个文件。

界面里已经一中文不剩；剩下的 79 处按「为什么不能进 `strings.xml`」分四类：

1. **持久化数据值（18 + 3 + 3 + 3 = 27 处）**：会写进库/文件并跨设备流转，
   资源化会让「同一份导出里的值」随界面语言漂移，且要把 `Context` 拖进只做数据的层。
   - `core/.../relapse/TriggerAnalyzer.kt` 18 处（`RelapseTags.emotions/triggers` = 情绪/触发因素选项，
     既是写出也是匹配键）
   - `app/.../data/settings/AnchorSettings.kt` 3 处（`DEFAULT_PROMPTS` 默认提示语，有 KDoc 说明）
   - `core/.../content/Credibility.kt` 3 处（`高/中/低`，是 `articles.json` 里的数据键，
     显示走 `:app` 的 `credibilityLabelRes` 映射）
   - `core/.../export/DataExporter.kt` 1 处（`countsLine` 条数文案，导出文件与设置页共用同一份措辞）
2. **合规词表（11 处）**：`core/.../notify/ReminderMessages.kt` 的敏感词/夸大疗效词黑名单，
   用于校验通知文案，本身不展示。
3. **诊断与日志（28 处）**：`app/.../data/db/AnchorDatabaseFactory.kt` 10 + 
   `.../data/db/crypto/DatabasePassphraseStore.kt` 8（加密库迁移/Keystore 日志与异常）+ 
   `core/.../content/ContentIndex.kt` 10（内容资产解析失败详情，会作为「内容加载失败：%1$s」的
   参数透出，属技术细节）。
4. **开发者断言（12 处）**：`require`/`check` 消息，仅开发期会看到——
   `core/.../phase/WithdrawalPhase.kt` 6、`.../assessment/CsbdScorer.kt` 3、
   `.../assessment/MoralIncongruenceScorer.kt` 3、`.../notify/ReminderTimeCalculator.kt` 2。
   另有 `core/.../AnchorCore.kt` 1 处（`APP_DISPLAY_NAME = "磐石"` 品牌常量，有单测断言；
   界面侧已改为 `stringResource(R.string.app_name)`，不再依赖它渲染）。

导入失败文案已经不再把中文塞在 `:core`：时间字段解析失败时 core 只回
`ImportResult.Rejected(ImportRejection.INVALID_FIELD, args = (ImportField, 原始文本))`，
`ImportField` 是枚举（`CHECK_IN_DATE/CHECK_IN_TIME/RELAPSE_TIME/URGE_STARTED_AT/ASSESSMENT_TAKEN_AT`），
中文整句在 `values/strings_core.xml`，由 `:app` 的 `importRejectionText(rejected): Pair<Int, List<Any>>`
按字段选句（选句只用到资源 id，不需要 `Context`，所以 `SettingsViewModel` 仍是纯 `ViewModel`）。

阶段门禁命令（P0–P4 每阶段收尾与本次交付收尾都跑过，按改动范围增量执行，不做全量 `clean`）：

```bash
./gradlew :core:test :app:test :app:assembleDebug --offline
```

P4 收尾那次：`BUILD SUCCESSFUL in 53s`（`:core:test :app:test`，含新增的 2 个导入字段结构化断言），
随后 `:app:assembleDebug` 重新出包。v0.2.1（字体 + 段控件修复）收尾那次：同一条命令 `BUILD SUCCESSFUL`
（`:core` 162 + `:app` debug 83 / release 75 用例全绿，`:app:assembleDebug` 出包 46,629,277 字节）；
本次只改了 `:app` 的源码与测试，所以 `:core:test` 与多数任务为 `UP-TO-DATE`。

---

## 3. APK

```
E:\Anchor\app\build\outputs\apk\debug\app-debug.apk      （debug，46,629,277 字节 / 约 44.5 MB）
```

该 APK 由本次交付提交的源码树在 `--offline` 下构建，`aapt2 dump badging` 核对为
`versionCode=3`、`versionName=0.2.1`、`minSdk=26`、`compileSdk=35`、应用名「磐石」，产物与提交内容一致。

体积构成（v0.2.1）：SQLCipher 的 4 个 ABI 原生库合计 22.5 MB（arm64-v8a 6.3 / armeabi-v7a 3.9 /
x86 5.3 / x86_64 7.0 MB），新打包的 3 个子集字体共 4.92 MB（`res/font/` 下以未压缩方式存储，
便于系统直接 mmap；这也是从 v0.2.0 的 43.66 MB 涨到 46.63 MB 的全部原因）。
若要瘦身，可用 `abiFilters` 只留 arm64-v8a，或改为 App Bundle（未做，属发布决策，见 §5 建议）。

安装：`adb install -r app\build\outputs\apk\debug\app-debug.apk`（需 JDK20 + platform-tools）。
覆盖安装：v0.2.0（versionCode 2）与 v0.1.0（versionCode 1）都能直接覆盖到 versionCode 3；
从 v0.1.0 直接升上来时**首次启动会把明文库一次性迁移为加密库**
（迁移前建议先在设置页导出一次 JSON 备用，见 §4.2）。
未产出 release 包（未配置签名，提示词也未要求）。

### 3.1 打包字体（v0.2.1）

| 项 | 内容 |
|---|---|
| 字体 | HarmonyOS Sans SC（Huawei Device Co., Ltd，字库：汉仪），字体文件 Version 1.0 |
| 文件 | `app/src/main/res/font/harmonyos_sans_sc_{regular,medium,bold}.ttf`，1.63 / 1.64 / 1.65 MB |
| 许可 | 《HarmonyOS Sans Fonts License Agreement》，免费可用于商业产品；通告全文见 `app/src/main/assets/licenses/HarmonyOS-Sans.txt` |
| 声明位置 | `ui/theme/AnchorFont.kt`（FontFamily）、`ui/theme/AnchorType.kt`（13 个槽位）、`ui/theme/Theme.kt`（M3 Typography 的 15 个槽位 + `LocalTextStyle` 兜底） |
| 回归测试 | `app/src/test/java/com/anchor/recovery/ui/theme/AnchorFontTest.kt`（3 例） |

为什么不是 SF Pro / 苹方：两者都是 Apple 专有字体，不能随应用再分发，所以 v0.1 只能用系统默认
字体，结果在小米（MiSans）、一加（OPPO Sans）这类 ROM 上字形随机器变。HarmonyOS Sans SC 是当前
免费可商用字体里与苹方观感最接近的一款（同为现代中文黑体、字面比例接近），且不是任何非华为机型
的系统默认字体，因此在三方机型上字形一致。

体积控制：三个字重的全量字体共 23.3 MB，直接打包不可接受。用 fontTools（`pyftsubset`）子集化到
GB2312 全部 6763 个常用汉字 + 仓库实际出现的全部字符 + ASCII/标点/箭头/几何等符号区间，压到
4.92 MB（APK 里以未压缩方式存储）。实测子集只缺 2 个字符：`↔`（只出现在注释里）与 `◦`
（文章大纲的二级项目符号，`ui/library/LibraryComponents.kt:204`）——字体本身没有这两个字形，
会由 Android 系统字体 fallback 渲染，不会显示方框。

字重映射：HarmonyOS Sans SC 只有 400/500/700 三档，没有 600。`medium.ttf` 在 FontFamily 里被声明为
W600，这样一个文件同时接住 `AnchorType` 的 SemiBold 槽位与 M3 里大量 W500 槽位（Compose 的选型规则
是「取不低于请求值的最近字重」）；`regular.ttf` 为 W400、`bold.ttf` 为 W700。

### 3.2 v0.2.1 修的两个问题（对应真机反馈）

| 反馈 | 根因 | 处理 |
|---|---|---|
| 字体不好看 | v0.2.0 用系统默认字体，字形随 ROM 变 | 打包 HarmonyOS Sans SC 子集并全站归一（§3.1） |
| 知识库/日志页整块空白、看不到列表 | `AnchorSegmentedControl` 的选中块用 `fillMaxHeight()` 铺底；控件被放进 `Column` 时这条约束解析成「剩余高度」，于是段控件长到整页，它下面的 `LazyColumn(weight(1f))` 被挤成 0 高 | 选中块改为固定 32dp（轨道总高 36dp）；加回归测试 `AnchorSegmentedControlTest`（用旧写法重跑该测试会在 `AnchorSegmentedControlTest.kt:50` 断言失败，已实测） |

提交：`c00074a`。

---

## 4. 真机手动验证清单（JVM 测试覆盖不到的部分，尚未在真机执行）

> 本机无模拟器/真机，以下项**只完成了代码层 review，未经运行验证**，交付后需按此清单实测。

### 4.1 逐页 insets 检查清单

布局原则：`MainActivity` 里 `enableEdgeToEdge()`；全局只有 `AnchorApp` 一个 `Scaffold`，
`TopAppBar`/`NavigationBar` 各自消费系统栏 insets，`NavHost` 容器拿到的是已经扣掉系统栏的
`padding`、并追加 `imePadding()`（键盘）——因此页面自身不再重复加 insets。

| 页面 | 顶/底栏 | insets 处理 | 需实测 |
|---|---|---|---|
| 首页 | TopAppBar + 底栏 | Scaffold padding | 状态栏与顶栏是否重叠、底栏与手势条间距 |
| 今日打卡 | TopAppBar + 返回 | Scaffold padding + imePadding | 备注输入框被键盘顶起后是否可见 |
| 工具 | TopAppBar + 底栏 | Scaffold padding | 同首页 |
| 知识库（列表/分类/检索） | TopAppBar + 底栏 | Scaffold padding | 列表最后一项是否被底栏遮住 |
| 阅读器 | TopAppBar + 返回 | Scaffold padding | 长文滚动到最后一行是否可见；「查看原始来源」按钮位置 |
| 日志（时间线 + 统计卡） | TopAppBar + 底栏 | Scaffold padding | 过滤条与底栏间距；统计卡默认收起，点「展开」后四列时段条、Top 榜、免责口径都完整可见 |
| 渴求冲浪 / 十分钟延时 | TopAppBar + 返回 | Scaffold padding + imePadding | 计时器数字在横屏/大字体下是否溢出 |
| 破戒记录表单 | TopAppBar + 返回 | Scaffold padding + imePadding | 多行输入 + 键盘同时出现时的滚动 |
| 问卷（hub/intro/答题/结果） | TopAppBar + 返回 | Scaffold padding | 长题项滚动、底部「上一题/下一题」是否可点 |
| 设置 | TopAppBar + 返回 | Scaffold padding + imePadding | 提示语输入框被键盘顶起；弹窗在横屏下是否完整 |
| 免责声明门禁（首启/声明升级） | 无栏（整屏替换导航图） | `safeDrawingPadding()` 自行消费 | 状态栏/手势条两侧留白；超长文案滚动到底部按钮可点 |

补充实测项：三键导航 vs 手势导航、横屏、系统字体放大到最大、分屏、
深色模式下同样过一遍（`values-night` 主题与 Compose 深色配色）。

列表页整页空白的回归项（v0.2.1 修复）：知识库与日志页的段控件只占一行（36dp 高），
它下面应立刻出现计数文案与文章/记录列表；若又看到「段控件撑满整页」，根因与修法见 §3.2。

### 4.2 通知与提醒

- Android 13+：开启提醒时是否弹出 `POST_NOTIFICATIONS` 授权；拒绝后提醒开关**不应**被打开，
  并且 App 其余功能不受影响（此处逻辑已单测/代码审查，授权弹窗本身需真机确认）。
- 通知是否按时到达（inexact 周期任务，系统可能延后）；锁屏预览文案是否中性
  （`:core` 的 `ReminderMessages.containsSensitiveWording` 有敏感词表单测兜底）。
- 通知渠道名应为「每日记录提醒」，渠道描述「每天提醒你记录一次。可在设置里关闭。」。
- 关掉提醒开关后：已排程任务是否被取消（`cancelUniqueWork`）；已存在的历史通知不必撤回。
- 重启设备后周期任务是否仍然存活（WorkManager 持久化）。
- 改系统时区/夏令时切换后，下一次提醒时间是否跟随（`ReminderTimeCalculator` 用 `Clock.timeZone`；
  两条路径都经由它计算，精确路径在每次触发后重算下一天）。
- **精确提醒（v2 / P3-1，默认关）**：设置页打开「准点提醒」时应拉起系统「闹钟与提醒」授权页
  （`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`）；授权后到点误差应为秒级（对比 inexact 的小时级延后）。
- 精确提醒在系统页面里拒绝/日后收回时：开关应自动回到关（`MainActivity` 自愈，判定用 `:core`
  `ReminderScheduling.shouldFallBack`），并提示「仍使用普通提醒」——不能出现「开关开着但不会响」。
- 精确提醒下重启设备：`ExactReminderReceiver` 需收到 `BOOT_COMPLETED` 并重排（闹钟本身不持久化）；
  同时确认 `inexact` 周期任务不会因两条路径共存而产生一天两条通知（`cancelUniqueWork` 是否生效）。
- 省电模式 / 后台限制下精确闹钟是否仍触发（`setExactAndAllowWhileIdle` 可穿透 Doze）；国产 ROM 单独确认。
- 开着精确提醒时改提醒时间：闹钟应立刻改到新时间（同一个 `PendingIntent` 覆盖旧闹钟），不需重启 App。

### 4.3 其它

- CustomTabs：文章内「查看原始来源」是否能拉起系统浏览器；确认 App 自身无 `INTERNET` 权限
  （Manifest 未申请，可 `aapt dump permissions` 核对）。
- 启动图标：圆形/方形/圆角遮罩下锚形是否完整（自适应图标前景按 22% inset 收进安全区）；
  Android 13+ 主题图标（monochrome）跟随壁纸配色。
- 启动画面：冷启动首帧是 `@color/anchor_background`（中性色，不是品牌石青），无白屏闪烁；
  启动画面退出后应立刻变成主题底色；深色模式下不刺眼。
- 固定配色（v2 起已移除动态取色，见 §5 限制 8）：Android 12+ 与 11 及以下都应为品牌石青 + 暖沙，
  换壁纸不应改变页面/卡片/底栏配色；深色模式下日志里「破戒」标注应为深底浅字
  （`TimelineScreen.kt` 的 accent 用 `errorContainer`/`surfaceVariant`）。
- 数据导出：导出 JSON 落到用户选择的位置（如 Downloads），内容与页面上显示的条数一致；
  取消选择器时不应留下空文件。
- 一键清空：清空后首页/日志归零，提醒时间与提示语等设置保留。
- 免责声明版本升级：把 `Disclaimer.VERSION` 调大后，老用户重进 App 应再次看到门禁页。
- TalkBack：表单、按钮的 `contentDescription` 是否齐全；逐项核查见 §4.4。

### 4.4 TalkBack 与语义核查（P3-2）

**代码层已逐项核对（无需真机即可确认）**

| 组件 | 语义 | 位置 |
|---|---|---|
| 主/次按钮 | `Role.Button` + 文案；内容刻意不用 `fillMaxSize`（否则会向父级声明「占满整行」，把同排兄弟挤成 0 宽） | `AnchorControls.kt` 的 `AnchorButton` |
| 图标按钮 | `Role.Button` + **必填** `contentDescription` | `AnchorIconButton`（各页设置入口） |
| 开关 | `toggleable(role = Role.Switch)`，读屏播报开/关 | `AnchorSwitch`（提醒、准点提醒） |
| 分段控件 | `selectableGroup` + `selectable(role = Role.RadioButton)` | `AnchorSegmentedControl`（知识库分类、日志过滤） |
| 底栏标签 | `selectableGroup` + `selected` 语义 + `Role.Tab`；图标 `contentDescription = null`，意义由标签文字承载（避免重复朗读） | `AnchorTabBar` |
| 可点列表行 | `mergeDescendants = true` + `Role.Button`，标题/副标题/值合并成一句 | `AnchorList.kt` 的 `AnchorListItem` |
| 顶栏返回 | `Role.Button` + `contentDescription = "返回"` | `AnchorNavBar` |
| 表单输入 | Material3 `OutlinedTextField` + `label`（读屏播报标签与已输入内容） | 打卡备注、破戒表单、提示语 |
| 纯装饰图形 | 无语义、不参与朗读（数值由相邻文字承担）：时段分布条 | `InsightCard.kt` 的 `DistributionBar` |

**需真机逐项实测（本机无设备，列入待实测）**

- 首页：焦点顺序应为「连续天数 → 状态卡 → 主按钮 → 工具入口 → 知识库文章」；
  大数字卡不应只读出裸露数字而没有上下文。
- 打卡页：备注框（含 `label`）→ 保存打卡 → 撤销今天（未打卡时「撤销今天」应播报为不可用，而不是读不到）。
- 破戒表单：情绪/触发源/情境标签（多选）的「已选中」播报、备注框、保存按钮；
  保存被拒时错误文案应被自动朗读。
- 渴求冲浪 / 十分钟延时：计时数字频繁更新时不应反复抢焦点；结束时是否有可感知提示（震动 + 文案）。
- 问卷（CSBD / 道德不一致）：第 N / 共 M 题的播报、选项（`RadioButton` 语义）的选中态、
  「下一题」未作答时的不可用态。
- 徽章墙：未点亮徽章的「还差 N 天」与已点亮的达成日期都应可读，不依赖颜色区分。
- 日志 / 统计：过滤分段控件的选中态、统计卡「展开/收起」的展开态播报；
  时段分布四列（「上午 3 次」这类文本已承担数值，柱体无读音是预期行为）。
- 设置：提醒开关 → 时间步进（−5/+5 分钟）→ 准点提醒 → 提示语输入框 → 导出/导入/清空按钮的先后顺序；
  弹窗（清空确认、导入确认）应自动聚焦到标题并把焦点限制在弹窗内。
- 全局：开启 TalkBack 后用底栏切页，焦点应回到新页面顶部标题；
  禁用按钮应播报为「不可用」，而不是完全读不到。

### 4.5 v2 新增能力（加密库 / 导入恢复 / 文案）

**加密库与就地升级（P0）**——这是本版唯一可能丢数据的地方，务必先做备份再测：

- 先在 v0.1.0 上造数据（几次打卡 + 一条破戒 + 一次问卷），**先用设置页导出一次 JSON 存到别处**，
  再覆盖安装 v0.2.0（同签名升级，`versionCode` 1→2）。
- 升级首次启动：老数据应原样在（条数与升级前一致）；启动时间可能稍长（一次性迁移），但不应 ANR。
- 库文件已加密：用 `adb shell run-as com.anchor.recovery ls -l databases/` 看到 `anchor.db`，
  再用 `sqlite3` 打它应报“file is not a database”（明文能直接打开则说明迁移没生效）。
- 失败回退路径（代码层已测，真机确认现场行为）：迁移失败时必须**保留明文旧库**并提示一次，
  不允许出现“库没了但 App 照常启动到空数据”；此时导出仍应可用（数据库能打开）。
- 换机/清数据：卸载重装后 Keystore 密钥消失，老库不可解密——App 应能正常空库启动（不崩），
  然后用上面导出的 JSON 导入恢复，数据应与导出前一致（含问卷原始作答与渴求事件）。

**导入恢复（P0-3）**：

- 设置页「导入」拉起系统文件选择器（SAF），选到导出 JSON 后弹确认框；取消不应改数据。
- 导入后条数与文件内一致，且**合并而不是覆盖**已有记录（重复日期按拒绝规则处理，见下）。
- 拒绝路径的文案必须完整：手工把 JSON 里的一个时间改成「昨天下午」再导入，
  提示应为「文件里的打卡时间读不出来（昨天下午）」，**不得出现裸露的 `%1$s` 或空白提示**；
  把 JSON 改坏（缺字段/非法值）则为「文件里的记录读不出来（…）」；
  同一日期出现两次则为「文件里有重复的打卡日期（…），为免丢数据已拒绝导入」。
- 被拒绝后库里数据与导入前一致（拒绝是整份拒绝，不是部分导入）。

**文案抽取走查（P4）**：

- 逐页扫一遍：不应出现裸资源名（`R.string.xxx`）、`%1$s` 占位符、空标题或错位断行；
  特别看阶段卡（首页/日志）、问卷题干与结果、免责声明全文、导入/导出提示、通知文案。
- 系统语言切到 English（或任何非中文）时：界面文案应**保持中文**（本版不做翻译，
  未提供 `values-en/` 即为预期，不是 bug），且不应因缺资源而回退成英文默认值。
- 大字体（设置里拉到最大）与横屏下：新增的阶段卡与问卷选项不应截断（属 §4.1 同一轮走查）。

### 4.6 字体走查（v0.2.1）

- 同一页面与 v0.2.0 对比：中文、数字、英文应整体换字形（若你的机型是华为/荣耀，系统默认字体本就
  是 HarmonyOS Sans，观感变化会很小，属预期而非没生效）。
- 字重层次：「磐石」大标题（Bold）、卡片与行标题（Medium）、正文（Regular）应能看出三级差异，
  而不是只有粗细两种。
- 标点与符号：中文引号「」、省略号……、破折号——、箭头 →↑↓ 应与正文同一字体风格；文章大纲的
  二级项目符号「◦」由系统字体渲染，允许略有差异（见 §3.1）。
- 生僻字：在打卡备注或标签里输入「甪」「龘」这类 GB2312 之外的字，应由系统字体渲染、不出现方框。
- 与 §4.1 同一轮走查：大字体拉到最大、横屏下，字距变化后不应出现截断或重叠。

---

## 5. 已知限制与后续迭代建议

**限制（当前版本的真实边界）**

1. **数据库已加密，设置项仍是明文偏好文件**：Room 库由 SQLCipher 加密（口令经 Android Keystore
   包裹后存应用私有目录，见 `DatabasePassphraseStore`），被解锁/root 后拿到库文件也打不开；
   但 DataStore（`anchor_settings.preferences_pb`：提醒时间、提示语等）未加密，仍可读。
   设置页「隐私说明」已如实告知（`Disclaimer.PRIVACY_PARAGRAPH`）。
2. **密钥丢失即数据不可读**：清除应用数据、卸载重装或换机后 Keystore 口令消失，加密库无法恢复；
   唯一恢复路径是用户自留的导出 JSON（导出文件本身保持明文，由用户显式持有）。
3. **提醒仍不保证准点**：默认仍是 inexact `PeriodicWorkRequest`（24h）；精确提醒需用户在设置页
   显式开启并授予「闹钟与提醒」权限（`SCHEDULE_EXACT_ALARM`），权限被系统收回后自动回落 inexact。
   省电策略与国产 ROM 后台管控仍可能压制精确闹钟，界面文案不做「准点」承诺。
4. **两份问卷为自撰题项**：F7 的 19 题（五维自评）与 F8 的 12 题（道德冲突）是按公开维度描述
   自行撰写的题项，**不是任何官方量表的原文**，结果仅作自我参考，页面与结果页均标注了免责说明。
5. **UI/通知/CustomTabs 未在真机验证**：本机无真机、按用户要求不启模拟器，第 4 节清单全部待实测；
   其中 v2 新增待实测项：明文库→加密库迁移、导入恢复、精确提醒、iOS 风格改版后的逐页 insets 与焦点顺序。
6. **仅中文**：文案已全量抽入 `res/values/strings_*.xml`（P4），但本版只抽不译，没有第二语言资源；
   知识库 71 篇也全部为中文。
7. **构建环境绑本机**：`gradle.properties` 里写死 `org.gradle.java.home=E:/JAVA/20`，
   换机器需要改这一行；离线构建依赖本机 Gradle 缓存。
8. **iOS 风格只覆盖应用内视觉语言，不含系统组件**：v2 起用固定品牌色板（动态取色已移除，
   配色不再随壁纸变化，但启动图标仍可跟随系统「主题图标」）；返回手势、权限弹窗、
   日期/文件选择器保持 Android 原生；大标题收缩、分段控件滑动手感等细节为「风格神似」，
   不追求像素级复刻。
9. **字体是打包的子集，不是完整字体**：三个字重共 4.92 MB（全量 23.3 MB），字符集为
   GB2312 6763 字 + 应用实际用到的字符 + 常用符号；子集外的生僻字、`↔`、`◦` 由系统字体
   fallback 渲染（不显示方框，但字形风格可能与正文略有出入）。日后若文案新增冷僻字，
   需重跑一次子集化（做法见 §3.1），否则该字会自动落到系统字体上。

**迭代建议（v0.2.0 之后，按收益排序）**

> v0.1.0 的 7 条建议在 v0.2.0 已全部落地：加密（1）、导入恢复（2）、schema 导出 + 迁移测试（3）、
> 里程碑徽章墙（4）、可选精确提醒（5）、Compose UI 测试与 TalkBack 清单（6）、文案抽取（7）。

1. **加密导出**：导出 JSON 目前明文（由用户自持）；可加带口令的加密导出，本版刻意留待议。
2. **DataStore 加密**：把设置项也包进加密存储，消掉限制 1 的后半段。
3. **i18n 实际翻译**：P4 已铺好 `strings.xml` 基建，补 `values-en` 等语言包即可，不需要再动界面代码。
4. **release 签名与上架**：配置签名、Play 数据安全表单（需声明「数据仅存本地、无网络权限」）、
   隐私声明与 `Disclaimer.VERSION` 的升级流程。
5. **备份策略**：`allowBackup="false"` 是有意为之（避免明文上云）；若要提升换机便利性，
   应做加密备份或强化导出/导入引导，而不是直接打开系统自动备份。
6. **统计趋势**：日志页统计卡目前是累计口径，可加周/月趋势（数据已在库里，纯新增视图）。
7. **DI**：仍无 DI 框架（手写 `AnchorApplication` 装配）；页面与 ViewModel 数量继续增长后可考虑引入。

---

## 附：仓库状态

- 分支 `main`；v0.1.0 交付提交为 `500eb48`（其后两次真机反馈修复 `40fe996` 动态取色、`469a0a8` 统计卡瘦身），
  v0.2.0 按阶段提交：`bf8695b`（第二版计划）→ `425ef34`（P0 加密与 schema 基建）→ `cc2d46b`（P0-3 导入）→
  `315a251`（P1-1 主题与组件）→ `ede4ef0`（P2 达成判定下沉）→ `4d69134`（P2 徽章墙）→ `2104e88`（P1-2 逐页改造）→
  `7dbcb7f`（P3-1 精确提醒）→ `9f7b245`（P3-2 UI 测试）→ `a954639`（P4 文案批次 1-5）→ P4 收尾 `5cec05d`…`acff0a9`。
- v0.2.1（真机反馈修复）：`ab1de2e`（打包 HarmonyOS Sans SC，全站字体归一，versionCode 3 / versionName 0.2.1）、
  `2bc513b`（交付说明同步）、`c00074a`（段控件撑满整页导致列表区空白的修复 + 回归测试）。
- **未配置任何 remote**，因此没有执行 `git push`；需要远端时自行 `git remote add` 后再推。
- `.gitignore` 覆盖 `build/`、`local.properties`、`*.apk`、`.gradle/`、`.kotlin/` 等；
  `gradle/wrapper/gradle-wrapper.jar` 已入库（提示词要求）。
- 内容源 `anchor-reference-docs/**`（79 个文件）与产物 `app/src/main/assets/content/*.json` 均已入库，
  可复现：`py content-tools/build_content.py` 会重新生成并逐项校验（数量、分类、字段、来源链接）。
