# 磐石（Anchor）戒色辅助 Android App · MVP 开发计划

> 执行第 0 步：先将本计划文档保存为 `E:\Anchor\磐石Anchor-MVP开发计划.md`（用户要求计划文档存放在 E:\Anchor 目录下）。

## 已确认决策
- **技术栈**：Kotlin 2.2.20 + Jetpack Compose，原生 Android（compileSdk/targetSdk 35，minSdk 26）
- **数据**：纯本地离线 — Room + DataStore，无账号、无后端、默认无网络权限
- **内容**：anchor-reference-docs 的 71 篇 md → JSON 打包进 assets，内置离线阅读器
- **MVP 8 功能**：F1 打卡 streak｜F2 戒断时间线向导｜F3 渴求冲浪引导｜F4 10 分钟延迟工具｜F5 复吸/触发日志｜F6 循证知识库｜F7 CSBD 自评问卷｜F8 道德冲突测评

## 本机环境（已实测）
- Gradle 8.12（缓存）+ AGP 8.12.1 + Kotlin 2.2.20（Compose K2 插件同版本）
- JDK 20（PATH）；SDK：`%LOCALAPPDATA%\Android\Sdk`（platforms 35 在）
- 无模拟器/真机 → 验证 = `:core`/`:app` JVM 单测 + `assembleDebug`
- wrapper 离线生成：`gradle wrapper --gradle-version 8.12 --no-validate-url`

## 项目结构（E:\Anchor\ 下新建，与 anchor-reference-docs 平级）
```
settings.gradle.kts / build.gradle.kts / gradle/libs.versions.toml / local.properties(sdk.dir)
core/    纯 Kotlin JVM（零 Android 依赖）：全部业务逻辑
app/     Android：Compose UI + Room + DataStore + WorkManager
content-tools/build_content.py   md→assets JSON（一次性离线脚本，Python 3.13）
```
- 应用 ID：`com.anchor.recovery`
- `:core` 包：`streak`（StreakCalculator/CheckInPolicy/RebootFramework）、`timeline`（WithdrawalPhaseResolver，阶段 0-7/8-29/30-59/60-89/90+）、`urge`（UrgeSurfingSession 状态机、DelayTool）、`relapse`（TriggerAnalyzer）、`assessment`（CsbdScorer 19题5维 0-4分；MoralIncongruenceScorer 行为6题+道德6题→四象限）、`content`（Article/Category/ContentIndex）、`clock`（可注入 Clock）
- `:app` 包：`ui`（home/tools/library/journal 四 Tab + checkin、urge_surfing、delay_tool、relapse_edit、assessment 流程、article/{id}、settings）、`data`（Room 4 实体：check_in(date 唯一索引)/relapse/urge_episode/assessment_result + DAO + Repository 映射）、`content/ContentRepository`（assets JSON + kotlinx.serialization）、`notify`（WorkManager 每日打卡提醒，inexact，Android 13+ 运行时申请 POST_NOTIFICATIONS）

## 关键设计要点
1. **streak 规则**：自然日（本地时区）打卡去重，漏打断签，Relapse 清零重计；Clock 可注入以便测试时区/月界用例
2. **内容管线**：`build_content.py` 手写 front-matter 解析（七字段校验 + 71 篇计数 assert）→ `articles.json`/`index.json` 入 git；阅读器自写极简 md 渲染 + 可信度徽章（高绿/中黄/低灰）+ CustomTabs 打开来源链接 + 底部免责声明卡
3. **F7/F8 合规**：CSBD-19 原版题项有版权 → 参照维度自撰中文题并注明"非原版、未经验证"；结果页三分级/四象限 + 显著免责声明（非诊断）+ 就医引导（措辞"建议咨询/考虑就诊"，禁止"你患有"）；首次测评前强制勾选免责（DataStore 记录确认版本）
4. **edge-to-edge（targetSdk 35）**：`enableEdgeToEdge()` + Scaffold/safeDrawing insets，TopAppBar 用官方 insets 默认值
5. **测试纪律**：所有可测逻辑沉淀 :core（含通知触发时间计算）；每阶段 `./gradlew clean :core:test :app:test :app:assembleDebug` 必须绿；禁止删/跳测试

## 分阶段实施（8 阶段）
| 阶段 | 产出 | 验收 |
|---|---|---|
| S0 骨架 | 双模块空壳 + wrapper + 空 MainActivity APK（`--offline` 验证依赖缓存，缺则换缓存版本） | assembleDebug 绿 + smoke test |
| S1 内容管线 | build_content.py + articles/index.json + core ContentIndex | ContentIndexTest（71篇计数/字段完整） |
| S2 Streak 核心 | StreakCalculator/CheckInPolicy/RebootFramework + Clock | StreakCalculatorTest 9 用例（时区/月界/复吸重置/最长记录） |
| S3 数据层+首页 | Room 4实体/DAO/Repository + Home/CheckIn 屏 + 导航 + edge-to-edge | Repository 映射测试 + Room in-memory DAO 测试 |
| S4 F2+F6 | WithdrawalPhaseResolver + 阶段文案（引 05-cases 来源）；知识库列表/阅读器/徽章 | PhaseResolverTest + ContentRepositoryTest |
| S5 F3+F4+F5 | 渴求冲浪状态机、延迟工具、日志表单+TriggerAnalyzer 统计卡 | UrgeSurfingSessionTest/DelayToolTest/TriggerAnalyzerTest |
| S6 F7+F8 | 两问卷题项(core常量)+两 Scorer+流程 UI+结果页+免责声明 | 两个 ScorerTest 全边界用例 |
| S7 打磨发布 | WorkManager 提醒+运行时权限、Onboarding+免责流程、设置页(导出JSON/清空)、图标 splash、逐页 insets 人工检查清单 | ReminderScheduling 纯逻辑测试 + 全量验收 |

## 风险
- 离线构建依赖版本必须全部命中缓存（S0 首先验证，最高风险）
- JDK 不要用 JBR 25；Kotlin daemon 若与 JDK 20 不兼容则装 JDK 17 fallback
- 上架合规：定位"自助记录与科普工具"，禁"治疗/治愈"宣称；撤稿研究（Jiang 2003 睾酮）等伪科学数据严禁入文案
- 隐私：Room 本地明文，隐私声明如实告知；通知文案注意不泄露敏感用途
- 内容更新需重跑脚本发版；credibility 渲染对未知值兜底

## 参考文件
- 内容源：`E:\Anchor\anchor-reference-docs\`（01~06 共 71 篇 md）
- 本机构建约定：见本机的 Android 构建技能说明（该文档不在仓库内）
