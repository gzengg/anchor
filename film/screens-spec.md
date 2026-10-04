# 磐石 Anchor · 4 个界面复刻规格（实测级）

> 来源：源码原文。所有 dp / 文案 / 枚举 / 秒数逐字取自源码；未读到的写「未确认」，不猜。
> 每节顺序：顶栏 → 自上而下版式 → 全部字符串 → 数字/枚举域 → 状态机 → 图表判定。

## 全局共享（4 个界面全部适用）

- 页面根容器：`Column` + `fillMaxSize()` + `background(AnchorTheme.colors.groupedBackground)` + `verticalScroll(scrollState)`。
- 滚动状态交给全局顶栏：`PublishAnchorNavBar(scrollState)`（`hasLargeTitle` 默认 `true`）。
- 顶栏 `AnchorNavBar`（全局 Scaffold 的 `topBar`，app/src/main/java/com/anchor/recovery/ui/AnchorApp.kt:140）：
  `Column(fillMaxWidth().background(colors.cardBackground).windowInsetsPadding(WindowInsets.statusBars))`；
  内含 `Box(fillMaxWidth().height(44.dp))`：
  返回键 `align(CenterStart).padding(start = 4.dp).size(44.dp)`，图标 `Icons.AutoMirrored.Filled.KeyboardArrowLeft`，
  `size(28.dp)`，`tint = colors.tint`，`contentDescription = R.string.nav_back`（**返回**）；
  标题 `AnchorType.headline`、`colors.label`、`textAlign = Center`、`maxLines = 1`、`padding(horizontal = 56.dp)`、`alpha = titleAlpha()`；
  底部 `AnchorHairline(inset = 0.dp)`（高 `Dp.Hairline` = 0.5dp，色 `colors.separator`）。
- 内联标题透明度：`(scrollState.value / 44.dp).coerceIn(0f, 1f)`。即**进入页面时内联标题 alpha=0（不可见）**，滚动 44dp 后完全淡入。
- 这 4 个路由都**不是**底部 Tab（`bottomTabs = listOf(HOME, TOOLS, LIBRARY, JOURNAL)`，AnchorRoutes.kt:32），
  所以 4 个界面**都有返回按钮**、**都没有底部 Tab 栏**。
- 大标题 `AnchorLargeTitle`（AnchorNavBar.kt）：`AnchorType.largeTitle`（34sp / 行高 41sp / Bold / letterSpacing 0.37sp）、
  `colors.label`、`maxLines = 2`、`ellipsis`、`padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)`。
- 分组卡片 `AnchorListGroup`：`fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(10.dp)).background(colors.cardBackground)`；
  卡片内水平 16dp 由调用方自己加（多数是 `.padding(16.dp)`）。
- 分组表头 `AnchorSectionHeader`：`AnchorType.footnote`（13sp/18sp/Regular）、`colors.labelSecondary`、
  `padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)`。
- 列表行 `AnchorListItem`：`Row(fillMaxWidth().defaultMinSize(minHeight = 44.dp).padding(horizontal = 16.dp, vertical = 11.dp))`；
  标题 `AnchorType.body` + `colors.label`；副标题 `AnchorType.subheadline` + `colors.labelSecondary` + `padding(top = 2.dp)`；
  `>` 图标 `size(20.dp)` + `colors.labelTertiary`。
- 细分割线 `AnchorHairline`：高 0.5dp，色 `colors.separator`，默认左内缩 `inset = 16.dp`。
- 按钮 `AnchorButton`：高 `50.dp`、圆角 `RoundedCornerShape(12.dp)`、文字 `AnchorType.headline`（17sp/22sp/SemiBold）、
  `padding(horizontal = 16.dp)`。
  `Filled` = 底 `colors.tint` + 文字 `Color.White`；
  `Tinted` = 底 `colors.tint.copy(alpha = 0.14f)` + 文字 `colors.tint`；
  `Plain` = 底透明 + 文字 `colors.tint`；`enabled = false` 时整体 `alpha = 0.4f`。
- 颜色角色取值（浅色，AnchorColors.kt）：`groupedBackground = #F5F2EE`，`cardBackground = #FFFFFF`，
  `separator = #4A3C3C43`（叠加色），`opaqueSeparator = #C6C6C8`，`label = #000000`，`labelSecondary = #6C6C70`，
  `labelTertiary = #8E8E93`，`fill = #29787880`（叠加色），`fillStrong = #33787880`（叠加色），`tint = #2E6F73`（石青）。
- 4 个界面**都没有 `LazyColumn` / `LazyRow`**，只有 `Column` + `verticalScroll`；也都没有使用 `CredibilityBadge`。

---

## 1. 渴求冲浪 F3 — `app/src/main/java/com/anchor/recovery/ui/tools/UrgeSurfingScreen.kt`（约 346 行）

标题：`AnchorLargeTitle(R.string.urge_title)` = **渴求冲浪**；内联标题 `route_title_urge_surfing` = **渴求冲浪**；有返回按钮。

### 1.1 自上而下版式

根 `Column(spacedBy(16.dp), padding(bottom = 24.dp))`，元素间距 **16dp**：

1. `AnchorLargeTitle` = **渴求冲浪**（34sp Bold）。
2. `StageHeader`：`Column(fillMaxWidth().padding(horizontal = 16.dp), spacedBy(6.dp))`，两行：
   - 行 A：文本 `"${state.stage.ordinal + 1}/6 · $stageLabel"`，`AnchorType.footnoteSemibold`，`colors.tint`。
   - 行 B：`urgeSurfingStageHintRes(stage)` 提示句，`AnchorType.body`，`colors.label`。
3. 按阶段渲染主体（见 1.1.1）。
4. 页脚 `Text(R.string.urge_footnote)`，`AnchorType.footnote`，`colors.labelSecondary`，`padding(horizontal = 16.dp)`。

#### 1.1.1 各阶段主体

- **INTRO**：`AnchorListGroup { Column(fillMaxWidth().padding(16.dp), spacedBy(16.dp)) }`
  - `IntensityPicker`（默认 label = `urge_intensity_label`）
  - `AnchorButton(urge_start_button, Filled, fillMaxWidth())`
- **RATE_AGAIN**：同结构
  - `IntensityPicker(label = R.string.urge_intensity_label)`
  - `AnchorButton(urge_finish_button, Filled, fillMaxWidth())`
- **BREATHE / OBSERVE / RIDE**（源码 `else` 分支）：
  1. `TimerCard` = `AnchorListGroup { Column(fillMaxWidth().padding(16.dp), spacedBy(10.dp)) }`：
     - `Text(formatSeconds(remainingSec))`：`AnchorType.largeTitle`（34sp Bold），`colors.label`。格式 `"%d:%02d"` → `1:00` / `0:30` / `2:00`。
     - `LinearProgressIndicator(progress = { stageProgress }, fillMaxWidth(), color = colors.tint, trackColor = colors.fillStrong)`。
     - `Text(pluralStringResource(R.plurals.urge_elapsed_seconds, elapsedSecTotal, elapsedSecTotal))`：`AnchorType.footnote`，`colors.labelSecondary`。
  2. **仅 BREATHE 阶段**追加 `BreathingHint` = `AnchorListGroup { Column(fillMaxWidth().padding(16.dp), spacedBy(4.dp)) }`：
     - 呼吸相位词 `AnchorType.title3`（20sp/25sp/SemiBold），`colors.label`。
     - `pluralStringResource(R.plurals.urge_breathing_hold, cue.remainingSec, cue.remainingSec)`，`AnchorType.footnote`，`colors.labelSecondary`。
  3. `Column(fillMaxWidth().padding(horizontal = 16.dp), spacedBy(12.dp))`：
     - `AnchorButton(urge_skip_button, Filled, fillMaxWidth())`
     - `AnchorButton(urge_stop_button, Plain, fillMaxWidth())`
- **DONE**：
  1. `ResultCard` = `AnchorListGroup { Column(fillMaxWidth().padding(16.dp), spacedBy(6.dp)) }`，三行：
     - `urge_result_partial` 或 `urge_result_finished`：`AnchorType.headline`，`colors.label`。
     - 拼接串：`pluralStringResource(urge_result_summary, elapsedSecTotal, elapsedSecTotal, peakIntensity?.toString() ?: "-")`，再拼（若 `endIntensity != null`）`stringResource(urge_result_end_intensity, endIntensity)`。`AnchorType.body`，`colors.label`。
     - 第三行（`state.delta != null` 时）三选一：`urge_delta_down(delta)` / `urge_delta_same` / `urge_delta_up`；否则 `urge_no_end_score`。`AnchorType.body`，`colors.label`。
  2. `AnchorButton(urge_back_to_tools_button, Filled, fillMaxWidth().padding(horizontal = 16.dp))` —— **不在分组卡片内**。

#### 1.1.2 IntensityPicker 组件

`Column(spacedBy(4.dp))`：
- 第一行 `Row(fillMaxWidth(), SpaceBetween, CenterVertically)`：标签 `AnchorType.subheadlineSemibold`（15sp SemiBold）+ `colors.label`；数值 `"$intensity / 10"`，`AnchorType.headline`，`colors.label`。
- 第二行 `Slider(value = intensity.toFloat(), onValueChange = { onIntensityChange(it.toInt().coerceIn(1, 10)) }, valueRange = 1f..10f, steps = 8, colors = SliderDefaults.colors(thumbColor = tint, activeTrackColor = tint, inactiveTrackColor = fillStrong))`。

### 1.2 全部字符串（资源 key = 中文原文）

- `urge_title` = **渴求冲浪**
- `urge_start_button` = **开始**
- `urge_intensity_label` = **现在的渴求强度**
- `urge_finish_button` = **完成这一轮**
- `urge_back_to_tools_button` = **回到工具页**
- `urge_skip_button` = **跳过这一步**
- `urge_stop_button` = **先到这里**
- `urge_footnote` = **自助工具，不构成医疗建议；若痛苦持续加重，请咨询医生。**
- `urge_elapsed_seconds`（plurals other）= **已持续 %1$d 秒**
- `urge_breathing_hold`（plurals other）= **保持 %1$d 秒 · 吸气 4 / 停 2 / 呼气 6**
- `urge_result_partial` = **这一轮记作部分完成**
- `urge_result_finished` = **这一轮结束了**
- `urge_result_summary`（plurals other）= **用时 %1$d 秒 · 峰值 %2$s/10**
- `urge_result_end_intensity` = **" · 结束 %1$d/10"**（资源值首尾各含空格，含前导 `·`）
- `urge_delta_down` = **下降 %1$d 分：它自己退下去了。**
- `urge_delta_same` = **强度没变，但也没有继续升高。**
- `urge_delta_up` = **比开始时更高。冲浪不保证每次都下降，你已经做到不立刻行动。**
- `urge_no_end_score` = **没有结束评分，这次不算完整一轮，但记录已保存。**
- 阶段名：`urge_stage_label_intro` = **开始**；`urge_stage_label_breathe` = **呼吸引导**；`urge_stage_label_observe` = **观察渴求**；`urge_stage_label_ride` = **等待它过去**；`urge_stage_label_rate_again` = **再次评分**；`urge_stage_label_done` = **结束**
- 阶段提示：
  - `urge_stage_hint_intro` = **渴求像一道波浪：会自己升高，也会自己退下。接下来几分钟先不动手，只观察它。**
  - `urge_stage_hint_breathe` = **把注意力放在呼吸上：吸气 4 秒，停 2 秒，呼气 6 秒。走神了就回到呼吸。**
  - `urge_stage_hint_observe` = **像旁观者一样描述它：在身体哪个位置？紧、热还是空？不下判断。**
  - `urge_stage_hint_ride` = **不对抗，也不顺从。让它在这里，你只是陪着它变弱。**
  - `urge_stage_hint_rate_again` = **再给渴求打一次分，看是否变化。**
  - `urge_stage_hint_done` = **无论分数有没有下降，你都完成了一次“不立刻行动”。**
- 呼吸相位：`breathing_phase_in` = **吸气**；`breathing_phase_hold` = **停**；`breathing_phase_out` = **呼气**
- 返回 contentDescription：`nav_back` = **返回**

### 1.3 数字 / 枚举域

- 滑块：`valueRange = 1f..10f`，`steps = 8`（1..10 共 10 档整数）；默认 `intensity = 5`（`mutableIntStateOf(5)`）；变更时 `toInt().coerceIn(1, 10)`；显示 `"$intensity / 10"`。
- 核心钳制（UrgeSurfingSession.kt）：`MIN_INTENSITY = 1`，`MAX_INTENSITY = 10`，`clamp(i) = i.coerceIn(1, 10)`。
- **阶段共 6 步**，顺序固定：`INTRO → BREATHE → OBSERVE → RIDE → RATE_AGAIN → DONE`（ordinal 0..5）。
- 阶段时长（`UrgeSurfingSession.DEFAULT_DURATIONS`）：`INTRO = 0`（等用户）、`BREATHE = 60` 秒、`OBSERVE = 30` 秒、`RIDE = 120` 秒、`RATE_AGAIN = 0`（等用户）、`DONE = 0`；计时阶段合计 **210 秒（3:30）**。
- 呼吸节奏（BreathingPattern.kt）：`IN_SEC = 4`，`HOLD_SEC = 2`，`OUT_SEC = 6`，`CYCLE_SEC = 12`。`cue(elapsedSec)`：`position = elapsedSec % 12`；`0..3 → IN(剩余 4-position)`；`4..5 → HOLD(剩余 6-position)`；`6..11 → OUT(剩余 12-position)`。即**吸气 4 / 停 2 / 呼气 6**。
- 步骤指示器画法：**不是圆点**。阶段用文字 `"${ordinal+1}/6 · 阶段名"`（如 `1/6 · 开始`）；阶段内倒计时用一条 M3 `LinearProgressIndicator`（`fillMaxWidth()`，`progress = elapsedSecInStage / duration`，色 `tint`，轨道 `fillStrong`）。进度条高度源码未指定（M3 默认，**未确认**）。
- 推进：每秒 `delay(1_000)` 调一次；除 `RATE_AGAIN` 外自动推进；`skipStage` 直接 `stage.next` 且 `elapsedSecInStage = 0`。
- 结果导出 `result(state, startedAt)`：`peak = peakIntensity ?: initialIntensity ?: 1`，`end = endIntensity ?: peak`，`delta = peak - end`，`durationSec = elapsedSecTotal`，落库 `tool = URGE_SURFING`。

### 1.4 状态机

| 当前 | 用户动作 | 结果 |
|---|---|---|
| 首次进入 | — | `INTRO`：标题 `1/6 · 开始` + intro 提示；卡片内滑块默认 **5**，按钮 **开始**；内联标题不可见。 |
| `INTRO` | 点 **开始** | `start()`：`stage = BREATHE`，`elapsedSecInStage = 0`，`initialIntensity = clamp(intensity)`，`peakIntensity` 同值。 |
| `BREATHE`（60s） | 每秒自动 | 倒计时 `1:00 → 0:00`，进度 0→1，呼吸卡显示吸气/停/呼气 + 保持秒数。 |
| `BREATHE` | 点 **跳过这一步** | 进入 `OBSERVE`（`elapsedSecInStage = 0`），呼吸卡消失。 |
| `OBSERVE`（30s） | 自动 / 跳过 | 进入 `RIDE`。 |
| `RIDE`（120s） | 自动 / 跳过 | 进入 `RATE_AGAIN`：倒计时卡消失，出现评分卡片。 |
| `RATE_AGAIN` | 拖滑块 | 只改本地 `intensity`（不自动推进）。 |
| `RATE_AGAIN` | 点 **完成这一轮** | `finish()`：`stage = DONE`，`endIntensity = clamp`，`peakIntensity = max(peak, end)`。 |
| 任意计时阶段 | 点 **先到这里** | `abandon()`：`stage = DONE`，`abandoned = true`（保留已记录评分）。 |
| 进入 `DONE` | 自动（`LaunchedEffect(stage)`） | `viewModel.save(session.result(...))` 写库。 |
| `DONE` | 点 **回到工具页** | `onDone()` 回工具页。 |

首次可见 = 只有一个滑块 + 开始按钮；`DONE` = 结果卡（3 行）+ 一个全宽按钮，**没有**滑块。
注意：`DONE` 阶段 `StageHeader` 仍显示 `6/6 · 结束` + `urge_stage_hint_done`。

### 1.5 图表判定

- **无曲线 / 无圆环**。唯一可视化是 `TimerCard` 里的 M3 `LinearProgressIndicator`（当阶段进度条），数据 `stageProgress = elapsedSecInStage / duration`（coerce 0f..1f），色 `tint`，轨道 `fillStrong`，宽 `fillMaxWidth()`。
- 呼吸只有文字，没有动画圆。

---

## 2. 十分钟延时 F4 — `app/src/main/java/com/anchor/recovery/ui/tools/DelayToolScreen.kt`（约 363 行）

标题：`AnchorLargeTitle(R.string.delay_title)` = **十分钟延时**；内联标题 `route_title_delay_tool` = **十分钟延时**；有返回按钮。

### 2.1 自上而下版式

根 `Column(spacedBy(16.dp), padding(bottom = 24.dp))`：

1. `AnchorLargeTitle` = **十分钟延时**。
2. 4 选 1 的状态卡（见下）。
3. 页脚 `Text(R.string.delay_footnote)`：`AnchorType.footnote`，`colors.labelSecondary`，`padding(horizontal = 16.dp)`。

**StartCard** = `AnchorListGroup { Column(fillMaxWidth().padding(16.dp), spacedBy(10.dp)) }`：
- `delay_start_heading`：`AnchorType.title3`，`colors.label`。
- `delay_start_hint`：`AnchorType.body`，`colors.label`。
- `IntensityRow(label = delay_initial_intensity_label, intensity = initialIntensity, …)`。
- `AnchorButton(delay_start_button, Filled, fillMaxWidth())`。

**RunningCard** = `AnchorListGroup { Column(fillMaxWidth().padding(16.dp), spacedBy(12.dp)) }`：
- `Text(uiState.remainingLabel)`：`AnchorType.largeTitle`，`colors.label`；格式 `"%02d:%02d"` → 初始 **10:00**。
- `LinearProgressIndicator(progress = { uiState.progress }, fillMaxWidth(), color = tint, trackColor = fillStrong)`。
- `Row(fillMaxWidth(), spacedBy(12.dp), CenterVertically)`：
  - 呼吸相位 `breathingPhaseLabelRes(cue.phase)`：`AnchorType.title3`，`colors.label`。
  - `pluralStringResource(R.plurals.delay_breathing_hold, cue.remainingSec, cue.remainingSec)`：`AnchorType.footnote`，`colors.labelSecondary`。
- 若当前 prompt 非空：
  - `Text("“$prompt”")`（源码 `"\u201C$prompt\u201D"`，前后是中文弯引号 **“ ”**）：`AnchorType.body`，`colors.label`。
  - `pluralStringResource(R.plurals.delay_prompt_rotate_hint, 45, 45)`：`AnchorType.footnote`，`colors.labelSecondary`。
- `AnchorButton(delay_stop_button, Tinted, fillMaxWidth())`。

**OutcomeCard** = `AnchorListGroup { Column(fillMaxWidth().padding(16.dp), spacedBy(12.dp)) }`：
- `delay_done_heading`：`AnchorType.title3`，`colors.label`。
- `delay_done_question`：`AnchorType.body`，`colors.label`。
- `Row(fillMaxWidth(), spacedBy(12.dp))`：
  - `AnchorButton(delay_answer_passed, Filled, weight(1f))` = **过去了**。
  - `AnchorButton(delay_answer_still, Tinted, weight(1f))` = **还在**。
- `IntensityRow(label = delay_end_intensity_label, intensity = endIntensity, …)`。
- `Text(stringResource(delay_intensity_compare, initialIntensity, endIntensity))`：`AnchorType.footnote`，`colors.labelSecondary`。
- `AnchorButton(text = if (answered == null) delay_answer_prompt_button else delay_save_button, enabled = answered != null, fillMaxWidth())`。

**SavedCard** = `AnchorListGroup { Column(fillMaxWidth().padding(16.dp), spacedBy(10.dp)) }`：
- `delay_saved_heading`：`AnchorType.headline`，`colors.label`。
- `delay_saved_hint`：`AnchorType.body`，`colors.label`。
- `AnchorButton(delay_back_to_tools_button, Filled, fillMaxWidth())`。

`IntensityRow` 与 F3 的 `IntensityPicker` **几何完全一致**（`Column(spacedBy(4.dp))` + 标签行 `Row(fillMaxWidth(), SpaceBetween, CenterVertically)` + `Slider(valueRange = 1f..10f, steps = 8, thumb = tint, active = tint, inactive = fillStrong)`），只有标签文案不同。

### 2.2 全部字符串

- `delay_title` = **十分钟延时**
- `delay_footnote` = **不替你决定做什么，只是把决定推迟十分钟。不构成医疗建议。**
- `delay_start_heading` = **把决定往后放十分钟**
- `delay_start_hint` = **这十分钟里只做两件事：跟着呼吸，看看自己写下的理由。**
- `delay_initial_intensity_label` = **开始前的渴求强度**
- `delay_start_button` = **开始计时**
- `delay_breathing_hold`（plurals other）= **保持 %1$d 秒（吸气 4 / 停 2 / 呼气 6）**
- `delay_prompt_rotate_hint`（plurals other）= **每 %1$d 秒换一条，可在设置里改成自己的话。**
- `delay_stop_button` = **结束计时**
- `delay_done_heading` = **十分钟到了**
- `delay_done_question` = **刚才那股冲动，现在过去了吗？如实答就好。**
- `delay_answer_passed` = **过去了**
- `delay_answer_still` = **还在**
- `delay_end_intensity_label` = **现在的渴求强度**
- `delay_intensity_compare` = **开始前 %1$d / 10 → 现在 %2$d / 10**
- `delay_answer_prompt_button` = **先回答问题**
- `delay_save_button` = **保存这次记录**
- `delay_saved_heading` = **已记录**
- `delay_saved_hint` = **你刚把行动推迟了十分钟，这条记录会出现在日志页。**
- `delay_back_to_tools_button` = **回到工具页**
- 呼吸相位：`breathing_phase_in` = **吸气**；`breathing_phase_hold` = **停**；`breathing_phase_out` = **呼气**
- 返回 contentDescription：`nav_back` = **返回**

### 2.3 数字 / 枚举域

- 倒计时长度：`DelayTool.DEFAULT_DURATION_SEC = 10 * 60` = **600 秒**；`remainingLabel` 格式 `"%02d:%02d"`，起点 **10:00**。
- 进度：`progress = elapsedSec / durationSec`（coerce 0f..1f）；`isComplete = elapsedSec >= durationSec`；`elapsedSec` / `remainingSec` 均 `coerceAtLeast(0)`。
- 滑块：**开始前**默认 `initialIntensity = 6`；**结束时**默认 `endIntensity = 3`；范围 `1f..10f`，`steps = 8`。
- 答题副作用：点 **过去了** → `answered = true`，`endIntensity = 3`；点 **还在** → `answered = false`，`endIntensity = 8`。（之后仍可拖滑块改 `endIntensity`。）
- 提示语轮播（理由卡）：
  - 条数：默认 **3 条**（`AnchorSettingsSnapshot.DEFAULT_PROMPTS`；用户在设置里写过就用自写的，`effectivePrompts = motivationPrompts.ifEmpty { DEFAULT_PROMPTS }`）。
  - **全部原文（AnchorSettings.kt:43-48）**：
    1. **先不做决定，十分钟之后再看这件事。**
    2. **我想要的是自己说了算，而不是被这一分钟牵着走。**
    3. **这一条记录是写给我自己的，不是写给任何人看的。**
  - 轮换间隔：`DelayTool.PROMPT_ROTATE_SEC = 45`，即 **每 45 秒换一条**；`promptIndex = (elapsedSec / 45) % promptCount`（0–44s 第 1 条，45–89s 第 2 条，90–134s 第 3 条，135s 回到第 1 条，循环）。
  - 呼吸节奏沿用 `BreathingPattern.cue(uiState.elapsedSec)`：吸气 4 / 停 2 / 呼气 6。
- 落库（`recordOutcome`）：`peakIntensity = maxOf(initialIntensity, endIntensity)`，`endIntensity` 用滑块值，`durationSec = 已用秒数`，`tool = DELAY_TOOL`。

### 2.4 状态机

| 当前 | 用户动作 | 结果 |
|---|---|---|
| 首次进入 | — | `started = false` → **StartCard**：标题 + 提示 + 滑块默认 **6** + 按钮 **开始计时**。 |
| StartCard | 点 **开始计时** | `viewModel.start()` → `currentSession = tool.start(600)`；`started = true`，`tick = 0` → **RunningCard**，倒计时 **10:00**。 |
| RunningCard | 每秒 | `delay(1_000)` → 重算 `uiState`（倒计时递减、进度 0→1、呼吸相位循环、每 45s 换提示语）。 |
| RunningCard | 点 **结束计时** | `viewModel.reset()` → `currentSession = null`，`started = false` → 回到 **StartCard**（滑块回默认 6）。 |
| RunningCard | 倒计时到 0 | `uiState.completed = true` → **OutcomeCard**（「十分钟到了」）。 |
| OutcomeCard | 点 **过去了 / 还在** | `answered` 置 true/false；`endIntensity` 变 3 / 8；保存按钮从「先回答问题」（禁用）变为「保存这次记录」（启用）。 |
| OutcomeCard | 点 **保存这次记录** | `recordOutcome(...)` 写库 → `saved = true` → **SavedCard**。 |
| SavedCard | 点 **回到工具页** | `onDone()`。 |

首次可见 = StartCard；操作后依次 RunningCard → OutcomeCard → SavedCard，四卡互斥。

### 2.5 图表判定

- **无曲线 / 无圆环**。可视化只有一条 M3 `LinearProgressIndicator`（`fillMaxWidth()`，`progress = uiState.progress`，色 `tint`，轨道 `fillStrong`），数据 `DelayTool.progress(session) = elapsedSec / 600`。高度源码未指定（**未确认**）。

---

## 3. 破戒记录 F5 — `app/src/main/java/com/anchor/recovery/ui/tools/RelapseEditScreen.kt`（约 197 行）

标题：`AnchorLargeTitle(R.string.relapse_title)` = **记录一次破戒**；内联标题 `route_title_relapse_edit` = **记录一次破戒**；有返回按钮。

### 3.1 自上而下版式

根 `Column(spacedBy(16.dp), padding(bottom = 24.dp))`：

1. `AnchorLargeTitle` = **记录一次破戒**。
2. `Text(R.string.relapse_hint)`：`AnchorType.body`，`colors.label`，`padding(horizontal = 16.dp)`。
3. `AnchorListGroup { OutlinedTextField(situation) .fillMaxWidth().padding(16.dp) }`
   - `label = R.string.relapse_situation_label`，`minLines = 2`。
   - 颜色：`focusedBorderColor = tint`，`unfocusedBorderColor = separator`，`focusedLabelColor = tint`，`unfocusedLabelColor = labelSecondary`，`cursorColor = tint`。
4. `TagSection(title = relapse_emotions_title, options = RelapseTags.emotions, …)`：
   - `AnchorSectionHeader` 表头（自带 top 20 / bottom 6）。
   - `AnchorListGroup { FlowRow(fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) }`；每项一个 M3 `FilterChip`：`FilterChipDefaults.filterChipColors(containerColor = colors.fill, labelColor = colors.label, selectedContainerColor = colors.tint, selectedLabelColor = Color.White)`；未传 `shape` / 尺寸 → M3 默认（圆角与高度**未确认**）。
5. `TagSection(title = relapse_triggers_title, options = RelapseTags.triggers, …)`：同上。
6. `AnchorListGroup { OutlinedTextField(note) .fillMaxWidth().padding(16.dp) }`
   - `label = R.string.relapse_note_label`，`minLines = 3`，颜色同第 3 项。
7. **仅当 `state.messageRes != null`**：`AnchorListGroup { Column(fillMaxWidth().padding(16.dp), spacedBy(10.dp)) }`：
   - `Text(stringResource(messageRes))`：`AnchorType.body`，`colors.label`。
   - **仅当 `state.saved == true`**：`AnchorButton(relapse_open_journal_button, Filled, fillMaxWidth())` → `onSaved()`。
8. `AnchorListGroup { AnchorButton(relapse_save_button, Filled, enabled = state.canSave, fillMaxWidth().padding(16.dp)) }` —— **该分组卡片始终存在**，保存后仍在。
9. 页脚 `Text(R.string.relapse_footnote)`：`AnchorType.footnote`，`colors.labelSecondary`，`padding(horizontal = 16.dp)`。

无 `LazyColumn`；标签用 `FlowRow` 自动换行。
**两个 `OutlinedTextField` 都只有浮动 `label`、没有 `placeholder`**（源码未传 `placeholder`）——输入框内灰色字是 label，不要自造 placeholder 文案。

### 3.2 全部字符串

- `relapse_title` = **记录一次破戒**
- `relapse_hint` = **如实记录就好，破戒很常见。保存后连续天数从今天重新算。**
- `relapse_situation_label` = **情境（在哪、和谁、在做什么）**
- `relapse_emotions_title` = **情绪（可多选）**
- `relapse_triggers_title` = **触发源（可多选）**
- `relapse_note_label` = **备注（当时在想什么）**
- `relapse_open_journal_button` = **去看日志与统计**
- `relapse_save_button` = **保存记录**
- `relapse_footnote` = **标签由你自己选，统计只说明记录里出现过多少次，不代表因果关系。**
- `relapse_message_incomplete` = **至少填一项：情境、情绪、触发源或备注。**
- `relapse_message_saved` = **已记录。写下它的价值是让你看清规律，而不是给自己下判断。**
- 返回 contentDescription：`nav_back` = **返回**

### 3.3 表单字段与枚举域（逐条照抄）

标签**硬编码在 Kotlin 里，不在 strings.xml / arrays.xml**：
`core/src/main/kotlin/com/anchor/recovery/core/relapse/TriggerAnalyzer.kt:10-33`，`object RelapseTags`，两者都是 `List<String>`，列表顺序即界面顺序。

- 情绪 `RelapseTags.emotions`，共 **9 条**（顺序）：
  1. **焦虑**
  2. **烦躁**
  3. **孤独**
  4. **无聊**
  5. **低落**
  6. **压力大**
  7. **疲惫**
  8. **愤怒**
  9. **兴奋**
- 触发源 `RelapseTags.triggers`，共 **9 条**（顺序）：
  1. **深夜独处**
  2. **压力事件**
  3. **社交媒体**
  4. **饮酒**
  5. **闲暇无聊**
  6. **情绪低落**
  7. **失眠**
  8. **看到相关内容**
  9. **社交场合**

表单字段：情境（多行，`minLines = 2`，label `情境（在哪、和谁、在做什么）`）；情绪（多选 chips）；触发源（多选 chips）；备注（`minLines = 3`，label `备注（当时在想什么）`）。
**均无 placeholder、无字数上限、无默认值。**

保存按钮：**保存记录**（`relapse_save_button`），`enabled = state.canSave = emotions.isNotEmpty() || triggers.isNotEmpty() || situation.isNotBlank() || note.isNotBlank()`。
保存后的提示：消息卡显示 **已记录。写下它的价值是让你看清规律，而不是给自己下判断。**（`relapse_message_saved`）+ 一个 **去看日志与统计** 按钮。
保存成功后表单被重置为 `RelapseFormState(saved = true, messageRes = relapse_message_saved)` —— 即清空情境/情绪/触发源/备注，`canSave` 变 false，保存按钮转禁用态。
落库内容：`occurredAt = Instant.fromEpochMilliseconds(repository.nowMillis())`，`situation = situation.trim()`，`note = note.trim()`，`emotions` / `triggers` 转 List（顺序 = 集合迭代序，非界面顺序，**未确认**稳定排序）。

### 3.4 状态机

| 当前 | 用户动作 | 结果 |
|---|---|---|
| 首次进入 | — | 空表单：情境空、情绪未选、触发源未选、备注空；无消息卡；**保存记录按钮为禁用态**（`canSave = false`，`alpha = 0.4f`）。 |
| 任意输入 | 输入情境 / 备注 | `situation` / `note` 更新（不 trim，trim 只在保存时做）。 |
| 任意输入 | 点某个情绪 chip | 该值在 `emotions` 集合 toggle（选中 = 底 `tint` + 白字；未选 = 底 `fill` + 字 `label`）。 |
| 任意输入 | 点某个触发源 chip | 同上，作用于 `triggers`。 |
| 填了任意一项后 | 点 **保存记录** | 写库（见 3.3）；成功后重置 → 消息卡出现（已记录…）+「去看日志与统计」按钮。 |
| 一项都没填 | 点 **保存记录**（禁用） | 正常点不动；ViewModel 内仍有兜底：`if (!canSave) messageRes = relapse_message_incomplete`，显示 **至少填一项：情境、情绪、触发源或备注。**（不出现日志按钮）。 |
| 已保存 | 点 **去看日志与统计** | `onSaved()` 跳转日志/统计页。 |

首次可见 vs 操作后差异：首次无消息卡、保存禁用；填写后保存启用；保存后出现带按钮的消息卡、表单清空、保存再次禁用。

### 3.5 图表判定

- **无图表**（无曲线、无圆环、无进度条）。只有文本框、FilterChip 标签、按钮。触发因素统计（`TriggerAnalyzer`，Top-N / 时段分布 / 间隔中位数）属于日志页，不在本文件。

---

## 4. 今日打卡 F1 — `app/src/main/java/com/anchor/recovery/ui/home/CheckInScreen.kt`（约 173 行）

标题：`AnchorLargeTitle(R.string.checkin_title)` = **今日打卡**；内联标题 `route_title_check_in` = **今日打卡**；有返回按钮。

### 4.1 自上而下版式

根 `Column(fillMaxSize().padding(bottom = 24.dp))`，**没有 `spacedBy`**，间距全靠各元素 `padding`：

1. `AnchorLargeTitle` = **今日打卡**（自带 start/end 16、top 8、bottom 8）。
2. `TodayStatusCard(modifier = Modifier.padding(top = 8.dp))` = `AnchorListGroup { Column(fillMaxWidth().padding(16.dp), spacedBy(4.dp)) }`，三行：
   - `Text(state.today.toString())`：`AnchorType.headline`，`colors.label`。**是 `LocalDate` 的 ISO 文本（如 `2026-07-15`），非本地化日期、不含星期。**
   - 状态：`checkin_status_checked_in` 或 `checkin_status_not_checked_in`：`AnchorType.body`，`colors.label`。
   - `checkin_only_today_notice`：`AnchorType.footnote`，`colors.labelSecondary`。
3. `AnchorListGroup(modifier = Modifier.padding(top = 16.dp)) { Column(fillMaxWidth().padding(16.dp), spacedBy(12.dp)) }`：
   - `OutlinedTextField(note)`：`label = R.string.checkin_note_label`，`minLines = 3`，`enabled = !state.todayCheckedIn`，`fillMaxWidth()`。
     颜色：`focusedBorderColor = tint`，`unfocusedBorderColor = opaqueSeparator`，`disabledBorderColor = separator`，`focusedTextColor = label`，`unfocusedTextColor = label`，`disabledTextColor = labelTertiary`，`cursorColor = tint`，`focusedLabelColor = tint`，`unfocusedLabelColor = labelSecondary`，`disabledLabelColor = labelTertiary`。
     **无 `placeholder`**，灰色字是 label。
   - `Row(horizontalArrangement = Arrangement.spacedBy(8.dp))`：
     - `AnchorButton(text = if (todayCheckedIn) checkin_checked_in_button else checkin_save_button, onClick = save, enabled = !todayCheckedIn, modifier = Modifier.weight(1f))` —— **Filled**，占满剩余宽度。
     - `AnchorButton(checkin_undo_button, onClick = undoToday, style = Tinted, enabled = todayCheckedIn)` —— **Tinted**，无 modifier → 宽度按内容 + 左右 16dp。
   - `state.messageRes?.let { Text(stringResource(messageRes, *messageArgs)) }`：`AnchorType.footnote`，`colors.tint`。
4. `AnchorSectionHeader(pluralStringResource(R.plurals.checkin_recent_count, recent.size, recent.size))`（自带 top 20 / bottom 6）。
5. `AnchorListGroup { … }`：
   - 若 `state.recent.isEmpty()`：`Text(checkin_empty)`，`AnchorType.body`，`colors.labelSecondary`，`fillMaxWidth().padding(16.dp)`。
   - 否则 `recent.forEachIndexed { index, record -> … }`：
     - `index > 0` 时先插 `AnchorHairline()`（默认 `inset = 16.dp`）。
     - `AnchorListItem(title = record.date.toString(), subtitle = record.note.takeIf { it.isNotBlank() })`。
       **无 `value`、无 `showChevron`、无 `leadingIcon`、`onClick = null`（纯展示行）**；行高下限 44dp，垂直 padding 11dp。

### 4.2 全部字符串

- `checkin_title` = **今日打卡**
- `checkin_note_label` = **今天的感受 / 触发情境（可留空）**
- `checkin_checked_in_button` = **今天已打卡**
- `checkin_save_button` = **保存打卡**
- `checkin_undo_button` = **撤销今天**
- `checkin_recent_count`（plurals other）= **最近 %1$d 次打卡**
- `checkin_empty` = **还没有打卡记录。**
- `checkin_status_checked_in` = **状态：已打卡**
- `checkin_status_not_checked_in` = **状态：今天还没打卡**
- `checkin_only_today_notice` = **只能为今天打卡：漏一天就断签，补打会让连续天数失真。**
- 消息（strings_settings.xml，通过 `UiMessage` 下发）：
  - `msg_check_in_recorded` = **已记录今天。**
  - `msg_check_in_duplicate` = **今天已经记录过了。**
  - `msg_check_in_undone` = **已撤销今天的打卡。**
  - `checkin_reject_future_date` = **不能为未来日期打卡**
  - `checkin_reject_not_today` = **只能为今天打卡，不支持补打卡**
- 返回 contentDescription：`nav_back` = **返回**


### 4.3 数字 / 枚举域

- 最近记录条数上限：`RECENT_LIMIT = 14`（`app/src/main/java/com/anchor/recovery/ui/AnchorViewModels.kt` 的 `CheckInViewModel` `private companion object`）；列表 `checkIns.sortedByDescending { it.date }.take(14)`；表头数字 = `state.recent.size`（**是当前列表长度，不是历史累计打卡数**）。
- 无强度评分、无滑块、无枚举标签。
- **日期 / 周视图 / 连续天数的可视元素：没有。** 界面只有一行 `LocalDate.toString()`（ISO `yyyy-MM-dd`）、一句状态文本、一句「只能为今天打卡」说明；**没有日历、没有周视图、没有连续天数数值、没有进度环**。连续天数只在首页 `HomeScreen` 出现，不在本页。
- 「一天一次」由提示句 `checkin_only_today_notice` 表达；代码层面由 `core/src/main/kotlin/com/anchor/recovery/core/streak/CheckInPolicy.kt:36-48` 判定，原因只有两种：`FUTURE_DATE`、`NOT_TODAY`（另有 `AlreadyCheckedIn`）。

### 4.4 状态机

| 当前 | 用户动作 | 结果 |
|---|---|---|
| 首次进入（未打卡） | — | `todayCheckedIn = false`，`note = ""`，`recent = []`：状态行 = **状态：今天还没打卡**；输入框**可编辑**；左按钮 = **保存打卡**（Filled，启用，weight 1）；右按钮 = **撤销今天**（Tinted，**禁用**，alpha 0.4）；最近区表头 = **最近 0 次打卡**，卡内 = **还没有打卡记录。**；无消息行。 |
| 未打卡 | 输入备注 | `viewModel.updateNote` 更新（可留空，无上限）。 |
| 未打卡 | 点 **保存打卡** | `repository.checkInToday(note)`：`Allowed` → 清空 `note`，`messageRes = msg_check_in_recorded`；`AlreadyCheckedIn` → `msg_check_in_duplicate`；`Rejected` → `checkin_rejectionRes(reason)`。 |
| 保存成功（已打卡） | — | 状态行 = **状态：已打卡**；输入框 `enabled = false`（边框/文字/标签转 `separator` / `labelTertiary`）；左按钮文案 = **今天已打卡** 且**禁用**；右按钮 **撤销今天** 变**启用**；最近列表首行出现今天记录（title = `yyyy-MM-dd`，subtitle = 备注，仅备注非空时显示）；下方显示 tint 色脚注消息。 |
| 已打卡 | 点 **撤销今天** | `repository.removeCheckIn(repository.today())` → `messageRes = msg_check_in_undone` → 回到「未打卡」可视态（记录从 `recent` 移除，按钮/输入框复位）。 |

首次可见 vs 操作后差异：变化项 = 状态行文案 / 输入框启用态 / 两个按钮的启用与文案 / 最近列表内容 / 是否有消息行。

### 4.5 图表判定

- **无图表**：没有曲线、没有圆环、没有进度条、没有 calendar / week strip。纯粹是文本行 + 输入框 + 两个按钮 + 分组列表。

---

## 附：复刻时最容易搞错的点（汇总）

1. **F3 的步骤指示器是文字 `"N/6 · 阶段名"`，不是圆点**；阶段内才有一条 `LinearProgressIndicator`。`DONE` 显示 `6/6 · 结束`。
2. **F3 的页脚与结果按钮的容器不同**：结果按钮 `fillMaxWidth().padding(horizontal = 16.dp)` **不在** `AnchorListGroup` 内；其余阶段的按钮都在卡片内。
3. **F4 有 4 个互斥卡片**（开始 / 计时 / 结果 / 已记录）；答题按钮会写 `endIntensity`（过去了→3，还在→8），但滑块仍可再改。
4. **F4 理由卡默认 3 条、每 45 秒轮换**，文案是设置页数据的默认值（不是 strings.xml），原文见 2.3。
5. **F5 的情绪 / 触发源列表硬编码在 `TriggerAnalyzer.kt` 的 `object RelapseTags`**（各 9 条，顺序即界面顺序），不在 arrays.xml。
6. **F5 与 F1 的输入框用的是浮动 `label`，源码没有 `placeholder`**；不要自造 placeholder 文案。
7. **F1 没有连续天数 / 周视图**；表头数字是「当前列表条数」（上限 14），不是累计打卡数。
8. **F4 的理由卡用中文弯引号 `“ ”`**，左右成对出现在同一行。
9. 四个页面文案全部来自资源；**唯一硬编码中文是 F5 的两个标签列表**（属 core 数据）。
10. **4 个页面都没有 `LazyColumn`/`LazyRow`**，全部 `Column` + `verticalScroll`。
