# 磐石（Anchor）App 开发执行提示词

> 使用方式：将下方「提示词正文」完整复制给任意具备代码执行能力的 AI Agent（如 WorkBuddy / Cursor / Claude Code 等）执行。提示词已包含全部背景、决策、环境约束与验收标准，执行方无需额外询问即可开工。

---

## 提示词正文（从此处开始复制）

# 角色与任务

你是一名资深 Android 工程师，负责从零实现「磐石（Anchor）」——一款基于循证内容的戒色（戒除色情内容依赖）辅助 Android App 的 MVP。请严格按照本文档执行，不要自行变更已确定的技术决策；如遇文档未覆盖的决策点，按「最小惊讶原则」选择业界主流方案并在交付说明中注明。

# 一、项目背景

- 工作目录：`E:\Anchor\`（Windows）。该目录下已有：
  - `anchor-reference-docs/`：内容素材库，含 6 个分类文件夹（01-science、02-authority、03-medical、04-methods、05-cases、06-statistics）共 **71 篇中文 Markdown 文章**。每篇带 YAML front-matter，字段固定为：`title`、`source`（URL）、`author`、`date`、`category`、`tags`、`credibility`（高/中/低）、`summary`。另有 README.md、文献汇编、来源清单表（仅参考，不入 App）。
  - `磐石Anchor-MVP开发计划.md`：本计划的决策依据（可读参考）。
- App 定位：**自助记录与科普工具**（非医疗诊断工具），中文 UI，目标用户为希望戒除色情内容消费的中文用户。
- 应用 ID / 根包名：`com.anchor.recovery`，App 显示名「磐石」。

# 二、已确定的技术决策（不得变更）

1. **技术栈**：Kotlin 2.2.20 + Jetpack Compose（使用 `org.jetbrains.kotlin.plugin.compose` K2 插件，版本与 Kotlin 相同），原生 Android；compileSdk 35 / targetSdk 35 / minSdk 26；buildTools 35.0.0。
2. **数据**：纯本地离线——Room（SQLite）+ Preferences DataStore。**无账号体系、无后端、默认不申请 INTERNET 权限**（仅阅读器中用户主动点击"查看原始来源"时通过 CustomTabs 调起浏览器）。
3. **内容**：71 篇 md 经 Python 脚本一次性转换为 JSON，打包进 `app/src/main/assets/content/`，App 内置离线阅读器。
4. **架构**：Gradle 多模块——
   - `:core`：**纯 Kotlin JVM 模块**（`kotlin("jvm")`），**严禁任何 android.* 依赖**。全部业务逻辑、算法、问卷计分、日期计算都在此模块，用 JUnit5（kotlin-test）做 JVM 单元测试。
   - `:app`：Android 模块，只做 UI（Compose）与系统 API 适配（Room、DataStore、WorkManager、通知），业务逻辑一律委托 `:core`。
   - `content-tools/`：Python 脚本目录（非 Gradle 模块），仅含内容转换脚本。
5. **构建环境**（已实测，必须使用缓存版本以保证离线可构建）：
   - Gradle **8.12**（本机 `~/.gradle/wrapper/dists/gradle-8.12-all` 已缓存），用 `gradle wrapper --gradle-version 8.12 --no-validate-url` 生成 wrapper（本机 `gradle` 命令不在 PATH 时，直接调用缓存目录下的 `gradle-8.12/bin/gradle`）；
   - AGP **8.12.1**、Kotlin **2.2.20**；
   - JDK：PATH 上的 JDK 20（`java version 20.0.1`），**不要**指向 Android Studio 的 JBR 25；
   - Android SDK：`%LOCALAPPDATA%\Android\Sdk`（platforms android-35 已装），写入 `local.properties` 的 `sdk.dir`（该文件不入 git）；
   - 首次构建用 `--offline` 验证依赖全部命中本机缓存（`~/.gradle/caches/modules-2`）；Compose BOM、Room、WorkManager、Navigation-Compose、kotlinx-serialization 等版本若缓存缺失，换成缓存中已有的最近版本，并在交付说明中列出实际版本表。
6. **测试纪律（硬性）**：每个阶段完成后必须执行 `./gradlew clean :core:test :app:test :app:assembleDebug` 且 **BUILD SUCCESSFUL**；**禁止**删除、跳过、注释测试或降低断言标准来让构建变绿；测试失败必须修复根因。

# 三、MVP 功能范围（8 个功能，编号即优先级）

| 编号 | 功能 | 说明 |
|---|---|---|
| F1 | 戒断打卡与 streak | 自然日（本地时区）每日打卡，连续天数计数；记录"复吸"则 streak 清零重计；显示当前 streak 与历史最长；对接 90 天重启里程碑（1/7/30/60/90 天） |
| F2 | 戒断反应时间线向导 | 按戒断天数划分 5 阶段（0–7、8–29、30–59、60–89、90+），每阶段展示预期反应与应对建议，内容提炼自 `anchor-reference-docs/05-cases/`，每条建议标注来源文章 id |
| F3 | 渴求冲浪引导 | 基于正念 urge surfing 的计时引导流程：引入→呼吸引导→渴求评分(1-10)→观察等待（计时）→再次评分→完成，记录渴求下降值 |
| F4 | 10 分钟延迟工具 | 冲动时启动 10 分钟倒计时 + 呼吸引导 + 用户预设的"戒断理由/人生价值"提示语轮播；结束后询问"渴求是否过去"并记录 |
| F5 | 复吸/触发因素日志 | 记录每次复吸的时间、情境、情绪（多选）、触发源（多选）、备注；提供情绪 Top-N、触发源 Top-N、时段分布、复吸间隔中位数的基础统计 |
| F6 | 循证知识库 | 71 篇文章离线浏览：六分类筛选、列表（显示可信度徽章：高=绿/中=黄/低=灰）、阅读器（极简 Markdown 渲染：标题/列表/加粗/引用/表格尽量保留）、来源链接（CustomTabs）、每页底部免责声明卡 |
| F7 | CSBD 自我筛查问卷 | 参照 WHO ICD-11 CSBD 标准与 CSBD-19 量表维度（控制失控/情绪应对/强迫性/负面后果/尝试失败）**自行撰写** 19 道中文题（不可逐字搬运原版量表，有版权），每题 0–4 分；结果分 4 级（<20 低风险 / 20–39 需关注 / 40–55 高风险 / ≥56 强烈建议就医），阈值标注为"自研参考阈值"；结果页含显著免责声明与就医引导 |
| F8 | 道德冲突 vs 真实问题测评 | 基于 Grubbs 道德不一致（moral incongruence）研究：12 题（行为问题 6 + 道德冲突 6，各 0–4 分），输出四象限解读（行为高/低 × 道德高/低），核心文案避免过度病理化（如"价值观冲突不等于成瘾"） |

# 四、项目结构与关键实现要求

## 4.1 目录结构

```
E:\Anchor\
├── settings.gradle.kts            include(":core", ":app")
├── build.gradle.kts               仅插件版本声明，apply false
├── gradle.properties              android.useAndroidX=true 等
├── gradle/libs.versions.toml      Version Catalog 统一管理依赖
├── local.properties               sdk.dir（gitignore）
├── .gitignore                     build/ .gradle/ .kotlin/ local.properties *.apk
├── core/
│   ├── build.gradle.kts           kotlin("jvm") 2.2.20 + kotlinx-datetime + kotlin-test(JUnit5)
│   └── src/{main,test}/kotlin/com/anchor/recovery/core/...
├── app/
│   ├── build.gradle.kts           com.android.application + compose
│   └── src/main/{java/com/anchor/recovery/..., res/..., assets/content/}
└── content-tools/
    └── build_content.py           md → JSON 转换脚本（Python 3.13，零第三方依赖优先）
```

## 4.2 `:core` 模块（包 `com.anchor.recovery.core`）

必须实现以下类，并配齐 JVM 测试：

- `clock/Clock.kt`：接口 + `SystemClock`，测试注入 FakeClock。**所有日期/时间逻辑必须通过 Clock 获取当前时间**。
- `streak/StreakCalculator.kt`：输入有序打卡列表 + 复吸列表 + Clock，输出 `StreakState(currentDays, longestDays, ...)`。规则：按自然日去重；漏打一天即断；最近一次复吸之后重新累计。测试至少覆盖：空历史、连续打卡、漏打断签、同日重复打卡、当天复吸清零、昨天复吸今天起算、跨月边界、23:59/00:01 时区日界、最长 streak 跨重置追踪。
- `streak/RebootFramework.kt`：90 天里程碑定义（1/7/30/60/90）。
- `timeline/WithdrawalPhaseResolver.kt`：戒断天数 → 五阶段枚举；阶段内容 `PhaseContent`（预期反应/应对建议/来源文章 id 列表）作为 core 常量数据。测试覆盖各边界天数与负数异常。
- `urge/UrgeSurfingSession.kt`：状态机 `INTRO → BREATHE → OBSERVE → RIDE → RATE_AGAIN → DONE`，记录峰值/结束评分并计算 delta；支持中途放弃记为部分完成。测试：完整流程、中途放弃、评分钳制 1-10。
- `urge/DelayTool.kt`：10 分钟倒计时纯逻辑（剩余时间计算、完成判定），测试注入 Clock。
- `relapse/TriggerAnalyzer.kt`：情绪/触发源频次 Top-N、时段分布（上午/下午/晚上/深夜）、复吸间隔中位数。测试：排序、空日志、奇偶中位数。
- `assessment/CsbdQuestionnaire.kt` + `CsbdScorer.kt`：19 题 5 维，每题 0–4；总分分级（<20/20–39/40–55/≥56）+ 维度小计。测试：全 0、满分、各分级边界（19/20、39/40、55/56）、维度小计之和=总分、缺答抛异常。
- `assessment/MoralIncongruenceScale.kt` + `MoralIncongruenceScorer.kt`：行为 6 题 + 道德 6 题，各维度 ≥12 为高（自研阈值需注明），输出四象限。测试：四象限各一例 + 边界值。
- `content/Article.kt`、`Category.kt`、`ContentIndex.kt`：与 JSON 结构对应的 kotlinx-serialization 模型；`Credibility` 枚举（HIGH/MEDIUM/LOW），解析时对未知值兜底为 LOW 而非崩溃。
- `notify/ReminderTimeCalculator.kt`：根据用户设定时间与当前时间计算下次提醒触发延迟（供 WorkManager 使用），纯逻辑可测试。

## 4.3 `:app` 模块（包 `com.anchor.recovery`）

- **导航**：单 Activity + Navigation-Compose，底部 4 Tab：首页(`home`)/工具(`tools`)/知识库(`library`)/日志(`journal`)。二级路由：`checkin`、`urge_surfing`、`delay_tool`、`relapse_edit`、`assessment_hub → csbd_intro → csbd_quiz → csbd_result`、`moral_intro → moral_quiz → moral_result`、`article/{articleId}`、`settings`、首次启动 `onboarding`。
- **首页**：streak 大数字卡片（当前/最长）、今日打卡按钮（已打卡则置灰显示已打卡）、当前戒断阶段卡片（F2 摘要）、F3/F4 快捷入口。
- **Room**（`AppDatabase` v1，exportSchema=false）4 个实体：
  - `check_in`（id 自增、date 字符串 ISO yyyy-MM-dd **唯一索引**、note、createdAt 毫秒）
  - `relapse`（id、occurredAt、situation、emotions 逗号分隔、triggers 逗号分隔、note）
  - `urge_episode`（id、startedAt、durationSec、peakIntensity、endIntensity、tool 枚举名）
  - `assessment_result`（id、type=CSBD/MORAL、takenAt、totalScore、level、answersJson）
  DAO 返回 Flow；Repository 负责 Entity ↔ core 模型映射，映射函数写 JVM 测试；DAO 用 Room in-memory（room-testing）写增删查测试。
- **DataStore**（`anchor_settings`）：`reminder_enabled`、`reminder_time`（HH:mm）、`onboarding_done`、`disclaimer_ack_version`、用户"戒断理由/价值提示语"列表（供 F4 轮播）。
- **提醒**：WorkManager PeriodicWorkRequest（24h，inexact，**不申请 SCHEDULE_EXACT_ALARM**），channel `daily_checkin`，Worker 内用 `:core` 计算 streak 组装文案；Android 13+ 运行时申请 `POST_NOTIFICATIONS`。
- **edge-to-edge（targetSdk 35 强制）**：`MainActivity` 调 `enableEdgeToEdge()`；根布局用 `Scaffold`（自动消费 insets）；TopAppBar 用官方默认 windowInsets；全屏滚动页用 `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`。逐页检查顶部/底部遮挡。
- **知识库阅读器**：自写极简 Markdown 渲染（`##`/`###` 标题、`-`/`1.` 列表、`**粗体**`、`>` 引用、表格降级为等宽文本），`SelectionContainer` 可选中复制；顶部可信度徽章 + 作者/日期；底部"查看原始来源"（CustomTabs）+ 免责声明卡「本文内容为科普信息，不构成医疗建议」。

## 4.4 内容管线

`content-tools/build_content.py`（用本机 Python 运行一次，输出入 git）：
- 扫描 `anchor-reference-docs/0*/`，解析 front-matter（手写极简解析，字段固定，不依赖 pyyaml；遇异常行 fail-fast 报错文件名与行号）；
- **校验（assert，作为内容测试）**：文件总数 == 71；七字段齐全；credibility ∈ {高,中,低}；category 与目录名一致；source 为 http(s) URL；
- 输出 `app/src/main/assets/content/articles.json`（全量，含 bodyMarkdown 原文）与 `index.json`（分类→文章 id 列表与计数）；
- `:app` 增加构建期校验 task：assets 中两个 JSON 存在且 articles.json 数组长度 == 71，否则构建失败。

# 五、合规与文案要求（严格执行）

1. App 首次启动 Onboarding：整体免责声明（本 App 为自助记录与科普工具，不是医疗器械，不提供诊断；数据仅存本机），须勾选同意方可进入（DataStore 记录版本号，声明更新后强制重读）。
2. F7/F8 问卷 intro 页：独立免责声明 + "本问卷为自评参考工具，非临床诊断" + 勾选确认；结果页措辞用"建议咨询/考虑就诊"，**禁止**出现"你患有/你被诊断为"。
3. F7 题项须为参照维度自撰，页内注明"本问卷非 CSBD-19 原版，未经验证"。
4. 全 App 禁止出现未经证实的功效宣称（如"戒色提升睾酮""治疗/治愈成瘾"）；「禁欲 7 天睾酮提升 45.7%」说法来源已撤稿，严禁引用。
5. 通知文案不得暴露敏感用途（用"今天也要记录哦"这类中性文案）。

# 六、分阶段执行（S0–S7，按序执行，每阶段验收通过再进入下一阶段）

| 阶段 | 产出物 | 验收标准 |
|---|---|---|
| **S0** 骨架 | settings/build 脚本、libs.versions.toml、:core 与 :app 空模块、空 MainActivity（显示"磐石"二字）、gradle wrapper | `--offline` 下 `./gradlew :app:assembleDebug` 成功 + 一个 smoke test 通过 |
| **S1** 内容管线 | build_content.py 运行成功、articles.json/index.json 入库、core 的 ContentIndex 模型、构建期校验 task | ContentIndexTest（解析样例、71 篇计数、字段完整性）通过 |
| **S2** Streak 核心 | StreakCalculator/CheckInPolicy/RebootFramework/Clock | StreakCalculatorTest 全部用例通过 |
| **S3** 数据层+首页 | Room 4 实体/DAO/Repository、Home/CheckIn 屏、导航骨架、edge-to-edge | Repository 映射测试 + Room in-memory DAO 测试通过；assembleDebug 绿 |
| **S4** F2+F6 | WithdrawalPhaseResolver + 五阶段文案（标注来源文章 id）；知识库列表/分类/阅读器/徽章 | WithdrawalPhaseResolverTest、ContentRepositoryTest 通过 |
| **S5** F3+F4+F5 | UrgeSurfingSession、DelayTool、复吸/渴求记录表单与日志页、TriggerAnalyzer 统计卡 | 三个 core 测试类通过；计时 UI 用 LaunchedEffect，逻辑全在 core |
| **S6** F7+F8 | 两套题项（core 常量）、CsbdScorer、MoralIncongruenceScorer、问卷流程 UI、结果页、免责声明流程 | 两个 ScorerTest 全部边界用例通过 |
| **S7** 打磨发布 | WorkManager 提醒+运行时权限、Onboarding、设置页（数据导出 JSON/一键清空）、启动图标与 splash、逐页 insets 人工检查清单 | ReminderTimeCalculatorTest 通过；最终 `./gradlew clean :core:test :app:test :app:assembleDebug` 全绿；统计并报告真实测试用例数 |

# 七、风险与注意事项

1. **离线构建是最高风险**：S0 先用 `--offline` 验证；缺哪个依赖版本就换缓存里有的（Compose BOM / Room / WorkManager / Navigation / kotlinx-serialization / kotlinx-datetime），并在交付说明中给出最终版本表。
2. **JDK**：用 PATH 上的 JDK 20；若 Kotlin daemon 报错，fallback 为安装 JDK 17 并在 `gradle.properties` 显式 `org.gradle.java.home`（路径用正斜杠）。
3. 无模拟器/真机：**所有可测逻辑必须沉淀在 :core**（含日期、计分、通知时间计算）；UI 层保持"哑渲染"，ViewModel 只做状态转发。
4. Room 数据本地明文存储，在设置页隐私说明中如实告知用户。
5. `local.properties`、`*.apk`、`build/` 等必须 gitignore；`gradle-wrapper.jar` 必须入库。
6. 完成后 `git init`（分支 main，仓库级 user.name/email）并提交；如需分批提交，按阶段提交。

# 八、交付物与汇报要求

全部阶段完成后输出交付说明，包含：
1. 实际依赖版本表（Gradle/AGP/Kotlin/Compose BOM/Room/WorkManager 等）；
2. 最终测试统计：`:core` 与 `:app` 各自测试类数与用例数（用 `test-results/**/TEST-*.xml` 统计真实用例数，注意 app 模块 debug+release 变体会重复计数，需去重）；
3. APK 路径（`app/build/outputs/apk/debug/`）；
4. 真机手动验证清单（edge-to-edge 遮挡、通知到达、CustomTabs 跳转、深色模式等无法 JVM 测试的项）；
5. 已知限制与后续迭代建议（如 SQLCipher 加密、数据云同步、里程碑徽章等）。

现在请从 **S0** 开始执行。每完成一个阶段，简要汇报该阶段产出与测试结果，然后继续下一阶段，直至 S7 完成。

## 提示词正文（复制到此为止）

---

## 附：执行前检查清单（交给 Agent 前自查）

- [ ] 目标 Agent 具备 Windows 文件读写与命令执行能力
- [ ] 工作目录 `E:\Anchor\` 可被访问，且 `anchor-reference-docs/` 内容完整（71 篇）
- [ ] 本机 Gradle 8.12 / AGP 8.12.1 / Kotlin 2.2.20 缓存未被清理
- [ ] 如需在全新机器执行：先按提示词第五节「构建环境」实测并替换为该机实际缓存版本
