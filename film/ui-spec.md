# 复刻规格：磐石界面（供 comp.html 用，全部来自源码实测）

帧：**393 × 852 dp**（手机竖屏）。复刻根 `#rep` 的底色 = `#F5F2EE`（app 自己的 groupedBackground，
与 look 的 ground 同色 → 纸面上看不到「屏幕边界」，组件直接落在纸面上）。

## 字体

`app/src/main/res/font/harmonyos_sans_sc_{regular,medium,bold}.ttf`（HarmonyOS Sans SC 400/500/700；
Compose 里 `medium` 声明为 W600）。→ 拷进 `film/fonts/`，@font-face 三个字重。

## 字阶 AnchorType（`app/src/main/java/com/anchor/recovery/ui/theme/`）

| 角色 | px/lh | 字重 |
|---|---|---|
| largeTitle | 34/41 | Bold |
| title1 | 28/34 | Bold |
| title2 | 22/28 | Bold |
| title3 | 20/25 | SemiBold |
| headline | 17/22 | SemiBold |
| body | 17/22 | Regular |
| callout | 16/21 | Regular |
| subheadline | 15/20 | Regular |
| subheadlineSemibold | 15/20 | SemiBold |
| footnote | 13/18 | Regular |
| caption1 | 12/16 | Regular |
| caption2 | 11/13 | Regular |

## 颜色

groupedBackground `#F5F2EE` · card `#FFFFFF` · tint(石青) `#2E6F73` · sand `#8C6D46` ·
label `#000000` · labelSecondary `#6C6C70` · labelTertiary `#8E8E93` · separator ≈30% 黑 ·
opaqueSeparator `#C6C6C8` · fill `#787880` @16% · success `#34C759` · warning `#FF9500` · danger `#FF3B30`

## 组件几何（`ui/components/` 实测）

- **分组卡 `AnchorListGroup`**：左右各 16 dp 外边距（宽 361 dp）、圆角 10、白底、内部内容裁剪。
- **节标题 `AnchorSectionHeader`**：footnote 13/次级灰，padding start16 end16 top20 bottom6。
- **细分割线 `AnchorHairline`**：高 0.5 dp，左侧内缩 16 dp（跨整组传 0）。
- **行 `AnchorListItem`**：minHeight 44，padding h16 v11；标题 body17 label；
  副标题 subheadline15 次级灰 `padding-top 2`；右侧值 body17 次级灰（可点击行才有 chevron 20 dp + 6 间距）；
  leading icon 22 dp + 12 间距。
- **按钮 `AnchorButton`**：高 **50**、圆角 **12**、文本 headline 17/22 SemiBold、左右内边距 16。
  `Filled`=tint 底+白字 · `Tinted`=tint@14% 底+tint 字 · `Plain`=透明底+tint 字。
  禁用态 alpha 0.4。
- **徽章 `Badge`（里程碑）**：**56×56**、圆角 **10**；达成 = tint 底 + 白字（天数 title2 22 + caption1 11「天」）；
  未达成 = fill 底 + labelTertiary 字。
- **里程碑行**：padding h16 v12；徽章 + 12 间距 + 标题 body17 + 2 间距 + 状态行 footnote13
  （达成 → tint 色「6月1日达成 / … 累计 3 次」；未达成 → 次级灰「还差 7 天」）。行间 hairline。
- **可信度徽章 `CredibilityBadge`**：圆角 4、caption1 11、padding h6 v2。
- **来源 chip `SourceChips`**：footnote13 标签「来源」+ 圆角 6 chip（caption1 11，padding h6 v2）。
- **免责卡 `DisclaimerCard`**：圆角 10、padding 14、subheadlineSemibold 15 标题 + footnote13 正文。
- **文章行 `ArticleRow`**：padding h16 v12、行距 6；第一行 = 可信度徽章 + 日期 caption1 次级灰；
  标题 headline17；摘要 subheadline15（最多 3 行）次级灰；标签 LazyRow 间距 6。
- **日志行 `TimelineRow`**：每条目一张独立分组卡，内 padding 16、行距 6；
  首行 = 标签 subheadlineSemibold 15 + 右侧时间 footnote13 labelTertiary；详情行 footnote13 次级灰；
  可选强调 chip 圆角 6（footnote13 padding h8 v3）。
- **进度条**：`LinearProgressIndicator`，色 tint，轨道 fill，`padding(top 6)`。
- **顶栏 `PublishAnchorNavBar`**：首页内联标题「磐石」常显（`hasLargeTitle = false`）；
  其余页大标题 `AnchorLargeTitle`（largeTitle 34）+ 内联标题收缩联动。
- **底部 Tab（`AnchorTabBar`）**：首页 / 工具 / 知识库 / 日志。

## 首页版式（`ui/home/HomeScreen.kt`，自上而下）

1. `StreakCard`（padding-top 8，卡内 padding 16、行距 6）
   1.1「当前连续记录」footnote13 次级灰
   1.2 Row 底对齐：大数字 largeTitle34 **tint 色** + 「 天」subheadline15 次级灰 `padding-bottom 6`
   1.3「历史最长 N 天 · 累计记录 M 天」footnote13 次级灰
   1.4（可选）「最近一次破戒：…」footnote13
   1.5 进度条（padding-top 6）
   1.6「90 天进度 41% · 下一个里程碑：第 2 个月」footnote13
2. 节标题「今天」→ `TodayCard`（卡内 padding 16、行距 12）
   2.1「还没打卡，一天一次。」body17
   2.2 `AnchorButton` Filled 通栏「打卡」
   2.3 `AnchorButton` Plain 通栏「打卡记录与感受」
   2.4（可选）消息 footnote13 tint
3. 节标题「现在有点难熬？」→ `QuickToolCard`（padding 16、行距 12）
   3.1「先做一次冲浪练习，或用十分钟延时把决定往后放。」body17
   3.2 Row 间距 8：按钮 Filled「渴求冲浪」+ 按钮 **Tinted**「十分钟延时」（各 weight 1f → 宽度均分）
   3.3「90 天是社群参照，不是医学判定。」footnote13 次级灰
4. `PhaseCard`（`padding(top 20)`，卡内 padding 16、行距 10）
   4.1 Row 两端对齐：「当前阶段：修复期」headline17 + 右「3/5」footnote13 次级灰
   4.2「第 30–59 天 · 连续 37 天」footnote13 **tint**
   4.3 headline body17
   4.4 NoteSection「可能会经历」：subheadlineSemibold15 tint + 每条 note body17 + 来源 chips
   4.5 NoteSection「这一阶段可以试试」同上
   4.6（仅 急性期/重连期 有）警示条：danger@12% 底、圆角 12、footnote13、padding 12
   4.7「本 App 只提供自助记录与科普，不能替代诊疗。若痛苦持续加重，请咨询专业医生。」footnote13 次级灰
5. 节标题「知识库」→ `LibraryCard`（padding 16、行距 12）
   5.1「71 篇离线文章，每篇标注来源与可信度。」body17
   5.2 按钮 Tinted 通栏「按分类浏览」

## 五个阶段（`core/src/main/kotlin/com/anchor/recovery/core/phase/WithdrawalPhase.kt`，闭区间）

| # | id | 名称 | 天数 |
|---|---|---|---|
| 1/5 | ACUTE | 急性期 | 0–7 |
| 2/5 | FLUCTUATION | 波动期 | 8–29 |
| 3/5 | REPAIR | 修复期 | **30–59** |
| 4/5 | RECONNECT | 重连期 | 60–89 |
| 5/5 | CONSOLIDATION | 巩固期 | 90+ |

修复期文案（`app/src/main/res/values/strings_phases.xml`）：
- name 修复期 · headline「强度通常在下降，但波动仍在：这一阶段的关键是别把"还有渴求"当成失败。」
- expectation 1「闪回与偶发渴求仍会出现，常由压力、孤独、酒精等触发，但强度与频率一般较前几周下降。」（来源 cases-006）
- expectation 2「开始出现主观获益的自我报告：情绪更稳、精力更集中、对自己更有掌控感；这是社区自我报告，不构成医学结论。」（cases-008 / cases-007）
- expectation 3「典型体验是"大体平稳，但能感觉到有变化正在发生"。」
- coping 1「把每次成功抵抗闪回算作一次训练：抵抗闪回不是副作用，而是康复过程本身。」（cases-006）
- coping 2「继续写记录并回看：对戒色日志的定性分析显示，书写与同伴反馈是受访者最主要的应对资源。」
- coping 3「环境控制 + 问责伙伴：把高风险设备、时段、应用设为默认阻断，并留一个可以如实汇报的人。」
- 区间文案 `phase_day_range_closed` = 第 %1$d–%2$d 天 · `phase_help_seeking_notice` 见上 ·
  修复期**没有** caution。

## 里程碑徽章墙（`ui/milestones/MilestoneWallScreen.kt`）

大标题「里程碑」+ header note footnote13 + 白卡（行间 hairline）+ footer note footnote13。
本片用的 6 行（`milestone_days_title_*`：第 1 天 / 第 1 周 / 第 1 个月 / 第 2 个月 / 90 天重启；7/14/30/60/90 各一枚）：

| 徽章 | 标题 | 状态 | 状态行 |
|---|---|---|---|
| 1 天 | 第 1 天 | 已达成 | tint「5 月 2 日达成」 |
| 7 天 | 第 1 周 | 已达成 | tint「5 月 8 日达成」 |
| 14 天 | 第 2 周 | 已达成 | tint「5 月 15 日达成」 |
| 30 天 | 第 1 个月 | 已达成 | tint「5 月 31 日达成」 |
| 60 天 | 第 2 个月 | 未达成 | 次级灰「还差 23 天」 |
| 90 天 | 90 天重启 | 未达成 | 次级灰「还差 53 天」 |

header note：「徽章按连续打卡天数点亮。断签或破戒后重新开始，已经点亮的徽章会保留。」
footer note：「里程碑只表示你在这里记录了多少天，不代表治疗结果或"痊愈"。」

## 知识库（`ui/library/`）

- `library_category_all_count` = 全部 71；分类计数 science 12 / authority 11 / medical 12 /
  methods 12 / cases 12 / statistics 12（合计 71，实测 `app/src/main/assets/content/articles.json`）。
- 本片用的三条**真实**文章（标题照抄，不改写）：
  1. credibility 高 · date 2016 · 摘要「经典综述，提出多巴胺介导的是对奖励的'想要'（wanting）而非'喜欢'（liking）……」
     title「多巴胺奖励回路：'想要'与'喜欢'的分离及激励敏感化理论」tags 神经科学/多巴胺/奖励回路/激励敏感化
  2. credibility 高 · date 2015 · summary「被广泛引用的综述（311 篇参考文献）……」
     title「Love 等 2015：《网络色情成瘾的神经科学：综述与更新》」tags 神经科学/综述/行为成瘾/网络色情
  3. credibility 高 · date 2013 · summary「动物研究（雄鼠）：性经验使伏隔核 ΔFosB 持续升高……」
     title「Pitchers 等 2013：ΔFosB——自然奖励与药物奖励共同的神经可塑性分子」
- 免责正文（`library_disclaimer_content`）：「本内容整理自公开来源，仅供科普与自助参考，不构成诊断或治疗建议。」

## 本片的**一致性场景**（所有数字互相自洽，不许出现矛盾）

- 连续 37 天 · 历史最长 41 天 · 累计记录 63 天 · 90 天进度 **41%** · 下一个里程碑「第 2 个月」（60 天，还差 23 天）
- 当前阶段 = 修复期（30–59 天，3/5）
- 里程碑：1/7/14/30 天已点亮，60/90 未点亮
- 知识库 71 篇
- B7 破戒之后：连续 **0** 天、进度 0%、阶段回急性期 1/5，**但**历史最长 41 / 累计 63 不变、
  **已点亮的 1/7/14/30 徽章保持点亮**（这是真实规则，也是这一拍要讲的东西）

## 尚未读的屏幕（写对应 beat 之前必须读源码，不许凭印象画）

- `ui/tools/UrgeSurfingScreen.kt`（346 行）— B4 渴求冲浪
- `ui/tools/DelayToolScreen.kt`（363 行）— B5 十分钟延时
- `ui/tools/RelapseEditScreen.kt`（197 行）— B6 破戒记录
- `ui/home/CheckInScreen.kt`（173 行）— 打卡页（B3 可能用到）
