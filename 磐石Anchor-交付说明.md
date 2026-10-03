# 磐石（Anchor）交付说明

版本：0.1.0（versionCode 1） · 包名：`com.anchor.recovery` · 交付日期：本次会话当日
验收依据：`磐石Anchor-开发执行提示词.md`（第八节交付物要求逐条对应在下方五节）

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
| AndroidX | work-runtime-ktx | 2.10.0 |
| AndroidX | datastore-preferences | 1.1.7 |
| AndroidX | browser（CustomTabs） | 1.8.0 |
| kotlinx | coroutines-core / -android / -test | 1.10.2 |
| kotlinx | serialization-json | 1.8.1 |
| kotlinx | datetime | 0.6.1 |
| 测试 | kotlin-test-junit5 / junit-jupiter | 2.2.20 / 5.11.4（`:core`） |
| 测试 | junit4 / robolectric / androidx-test | 4.13.2 / 4.14.1 / core 1.6.1 · ext-junit 1.2.1（`:app`） |

未引入的第三方库（刻意的取舍）：无 DI 框架（手写 `AnchorApplication` 装配）、无图片加载库、
无 `core-splashscreen`（Android 12+ 用系统默认启动画面）、无 `SCHEDULE_EXACT_ALARM`
（提醒用 WorkManager 的 inexact 周期任务）。

---

## 2. 最终测试统计（已去重）

统计来源：`test-results/**/TEST-*.xml` 里 `tests= / failures= / errors= / skipped=` 的累加值。

| 模块 | 测试类数 | 用例数 | 失败 | 错误 | 跳过 |
|---|---|---|---|---|---|
| `:core`（jvm library，单变体） | 18 | **135** | 0 | 0 | 0 |
| `:app`（Android library 单元测试，debug 变体） | 8 | **29** | 0 | 0 | 0 |
| 合计（去重后） | 26 | **164** | 0 | 0 | 0 |

去重说明：`:app:test` 会分别跑 `testDebugUnitTest` 与 `testReleaseUnitTest`，两个变体是同一份源码，
按 XML 直接相加会得到 58 个用例；上表只取 debug 变体的 29 个。另用源码 `@Test` 计数交叉核对
（`:core` 135、`:app` 29），与 XML 一致。

测试分布（可测逻辑全部落在 `:core`，UI 层保持哑渲染）：

- `:core`：`StreakCalculator` / `CheckInPolicy` / `RebootFramework`、`WithdrawalPhaseResolver` +
  阶段文案、`CsbdScorer` / `MoralIncongruenceScorer`、`UrgeSurfingSession` / `DelayTool` /
  `BreathingPattern`、`TriggerAnalyzer`、`ContentIndex` / `MarkdownLite`、`DataExporter`、
  `Disclaimer`、`ReminderMessages` / `ReminderTimeCalculator`。
- `:app`：Room DAO / 实体映射 / 仓库（Robolectric）+ 导出 JSON 装配。

阶段门禁命令（每个阶段收尾都跑过，最后一次为 S7 完成后的全量门禁）：

```bash
./gradlew clean :core:test :app:test :app:assembleDebug --offline
```

最后一次结果：`BUILD SUCCESSFUL in 54s`（83 tasks，65 executed / 18 from cache），
`:core` 135 用例、`:app` 29 用例（×2 变体）全绿，APK 正常产出。

提交前又改了 `ReminderTimeCalculator.nextReminder`（去掉 `var`，改早返回）与 `NavHost` 的
`imePadding`/`consumeWindowInsets`，因此补跑了增量门禁 `:core:test :app:test :app:assembleDebug`
（22s，全绿）并据其重建 APK。

---

## 3. APK

```
E:\Anchor\app\build\outputs\apk\debug\app-debug.apk      （debug，18,988,885 字节）
```

该 APK 由本次交付提交的源码树在 `--offline` 下构建，产物与提交内容一致。

安装：`adb install -r app\build\outputs\apk\debug\app-debug.apk`（需 JDK20 + platform-tools）。
未产出 release 包（未配置签名，提示词也未要求）。

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

### 4.2 通知与提醒

- Android 13+：开启提醒时是否弹出 `POST_NOTIFICATIONS` 授权；拒绝后提醒开关**不应**被打开，
  并且 App 其余功能不受影响（此处逻辑已单测/代码审查，授权弹窗本身需真机确认）。
- 通知是否按时到达（inexact 周期任务，系统可能延后）；锁屏预览文案是否中性
  （`:core` 的 `ReminderMessages.containsSensitiveWording` 有敏感词表单测兜底）。
- 通知渠道名应为「每日记录提醒」，渠道描述「每天提醒你记录一次。可在设置里关闭。」。
- 关掉提醒开关后：已排程任务是否被取消（`cancelUniqueWork`）；已存在的历史通知不必撤回。
- 重启设备后周期任务是否仍然存活（WorkManager 持久化）。
- 改系统时区/夏令时切换后，下一次提醒时间是否跟随（`ReminderTimeCalculator` 用 `Clock.timeZone`）。

### 4.3 其它

- CustomTabs：文章内「查看原始来源」是否能拉起系统浏览器；确认 App 自身无 `INTERNET` 权限
  （Manifest 未申请，可 `aapt dump permissions` 核对）。
- 启动图标：圆形/方形/圆角遮罩下锚形是否完整（自适应图标前景按 22% inset 收进安全区）；
  Android 13+ 主题图标（monochrome）跟随壁纸配色。
- 启动画面：冷启动首帧是 `@color/anchor_background`（中性色，不是品牌石青），无白屏闪烁；
  启动画面退出后应立刻变成动态取色算出的页面底色；深色模式下不刺眼。
- 动态取色（Android 12+）：换一张明显带色的壁纸，页面/卡片/底栏配色应随之变化；
  Android 8.0～11 应稳定为品牌石青 + 暖沙；深色模式下日志里「破戒」标注应为深底浅字
  （不再是写死的浅粉底，`TimelineScreen.kt` 的 accent 已改用 `errorContainer`/`surfaceVariant`）。
- 数据导出：导出 JSON 落到用户选择的位置（如 Downloads），内容与页面上显示的条数一致；
  取消选择器时不应留下空文件。
- 一键清空：清空后首页/日志归零，提醒时间与提示语等设置保留。
- 免责声明版本升级：把 `Disclaimer.VERSION` 调大后，老用户重进 App 应再次看到门禁页。
- TalkBack：表单、按钮的 `contentDescription` 是否齐全（当前只给图标补齐了描述，未做朗读实测）。

---

## 5. 已知限制与后续迭代建议

**限制（当前版本的真实边界）**

1. **数据明文存储**：Room 数据库与 DataStore 都在应用私有目录，未加密；设备被解锁/root 后可读。
   设置页「隐私说明」已如实告知（`Disclaimer.PRIVACY_PARAGRAPH`）。
2. **无备份与同步**：`allowBackup="false"`，无账号体系、无云同步；数据只靠用户手动导出 JSON。
3. **提醒不精确**：使用 inexact `PeriodicWorkRequest`（24h），不申请 `SCHEDULE_EXACT_ALARM`，
   系统省电策略下可能延后，也不保证「每天准点」。
4. **两份问卷为自撰题项**：F7 的 19 题（五维自评）与 F8 的 12 题（道德冲突）是按公开维度描述
   自行撰写的题项，**不是任何官方量表的原文**，结果仅作自我参考，页面与结果页均标注了免责说明。
5. **UI/通知/CustomTabs 未在真机验证**：无模拟器与真机，第 4 节的清单全部待实测。
6. **仅中文**：无 i18n 资源；知识库 71 篇也全部为中文。
7. **构建环境绑本机**：`gradle.properties` 里写死 `org.gradle.java.home=E:/JAVA/20`，
   换机器需要改这一行；离线构建依赖本机 Gradle 缓存。
8. **动态取色使配色随壁纸变化**（Android 12+）：页面与卡片底色取自壁纸，所以
   ① 系统启动画面是进程启动前解析的静态值，冷启动首帧与 Compose 首帧可能有极轻微的色调差
   （静态值只能选中性色贴近，进程起来后 MainActivity 会立刻把窗口底色改成方案色）；
   ② 启动图标底色固定为品牌石青，需跟随壁纸时由用户开启系统「主题图标」；
   ③ 视觉验收/截图不再与设备无关（Android 11 及以下为固定品牌色板）。

**迭代建议（按收益排序）**

1. **SQLCipher 或 EncryptedFile** 包裹 Room + DataStore，把「明文」这条限制消掉（最高收益）。
2. **导出后再导入**：目前只能导出 JSON，没有导入/恢复路径；加一个带 schema 版本的导入校验。
3. **Room schema 导出**（`room.schemaLocation`）+ 迁移测试，为后续加表/改列兜底。
4. **里程碑可视化**：`:core` 已有 1/7/30/60/90 里程碑与进度计算，UI 只有进度环；
   可做徽章墙与达成记录（需要新增一张表）。
5. **可选的本地提醒精确化**：让用户显式选择「精确提醒」（申请 `SCHEDULE_EXACT_ALARM`）。
6. **无障碍与字号**：把 `:app` 的 Compose UI 测试跑起来（`ui-test-junit4` 已配依赖但未使用），
   补上大字号与 TalkBack 的回归。
7. **多语言**：抽出 `strings.xml` 里散落的文案（当前部分文案直接在 Compose 里硬编码），再做翻译。

---

## 附：仓库状态

- 分支 `main`，按阶段提交：S0 `cfce6de`、S1 `5d57a64`、S2 `25d542e`、S3 `5f44d90`、
  S4 `49d45ab`、S5 `21d05a9`、S6 `6837c0a`，S7 为本次最后一个提交。
- **未配置任何 remote**，因此没有执行 `git push`；需要远端时自行 `git remote add` 后再推。
- `.gitignore` 覆盖 `build/`、`local.properties`、`*.apk`、`.gradle/`、`.kotlin/` 等；
  `gradle/wrapper/gradle-wrapper.jar` 已入库（提示词要求）。
- 内容源 `anchor-reference-docs/**`（79 个文件）与产物 `app/src/main/assets/content/*.json` 均已入库，
  可复现：`py content-tools/build_content.py` 会重新生成并逐项校验（数量、分类、字段、来源链接）。
