# 磐石 · 发布片（49.0 s · 1920×1080 草稿 → 3840×2160 终版）

onetake pipeline（skill 在 `C:\Users\Administrator\.pi\agent\skills\onetake`）。
本片 = `E:\Anchor\film\`，与 Gradle 模块平级，不改 app 源码。

## 本机硬约束

- Python 必须 `py`（`python` 是 WindowsApps 占位符，退出码 49）。
- **所有 skill 脚本必须带 `PYTHONUTF8=1`**：本机默认编码 GBK，`look.py` 读 `looks/looks.json`
  会抛 `UnicodeDecodeError: 'gbk' codec can't decode byte 0x94 in position 426`。
- `python3` / `yt-dlp` 拉 YouTube 不通；apple.com、pixabay 可达。
- 禁自行启动模拟器/虚拟机/模型推理 → 界面一律 HTML 复刻，不录屏。
- `decompress --toFile` 路径只接受 `C:\Users\ADMINI~1\AppData\Local\Temp`、`~/.cache/opencode`、`~/.cache/pi`。

## §1 参考片实测（量出来的，不是感觉）

```
ios-hero.mp4  13.58 s  still 38%  low 61.3%  longest quiet 1.42 s
  cuts: 1.83 1.92 2.00 2.08 2.17 | 4.0 7.0 7.67 8.5 | 10.0 10.5 10.58 10.67 10.75 10.83
  bursts: [1.83–2.33] [10.0–11.33]
  motion: peak 133 px/frame @30 · 6 moves · median t80 0.55 · soft 55% of travel · smear 1.03（sharp，无快门模糊）
  per-move t80: 0.80 0.55 0.67 0.57 0.54 0.42
ios-large.mp4 4.50 s  still 42.6%  low 55.6%  longest quiet 1.75 s
  cuts: []（4.5 秒一次硬切都没有）  bursts: [0.67–1.08] [3.33–4.0]
  motion: peak 44 px/frame @30 · 3 moves · median t80 0.68 · soft 100% · smear 0.91
```

来源：`C:\Users\ADMINI~1\AppData\Local\Temp\ref\ios-hero.mp4` / `ios-large.mp4`（Apple 站点素材，
只用于量节奏，不能进成片）+ `hero/summary.json`、`large/summary.json`（analyze_ref.py 产物）。

**本片节奏目标**：still 36–42%（实际会掉到 ~20%，因为字幕在跑，如实报）· 镜头长 0.25 s–6.0 s（≥4×）·
只有两个硬切 burst · ≥1 段 ≥2.5 s 无旁白 + 画面全静 · median t80 0.55–0.68（软 S 为主），
burst 内用 t80≈0.23 的 snap · 峰值 ≤130 px/frame 且**开快门**（参考片选全锐利，不搬）。

**对比参照（必须与它们都不同）**
- `cases/ecoach-30s`：概念「那块玻璃」· 舞台 = 冷白台面 + 左侧竖屏手机 + 右侧留白 ·
  look = 冷蓝灰 ground `#EEF1F6` / ink `#0D1420` / accent `#12B4CC` / display Archivo 600 ·
  verify: median t80 0.69, soft 66%, peak 198 px/frame。
- 参考片：冷白台面 + 深色手机硬件。

## §2 概念（用户选定 AC）

**AC**：产品本色打底（概念 A「一个数字在长大」的主体 —— 连续天数那个数字从不出画），
关键两拍用概念 C 的机制（镜头穿过被点的那个控件，潜在它背后真正发生的事）。

- **舞台**：暖白纸面（infinite plane），**不出设备外壳**（app 是 Android，套 iPhone 框是撒谎），
  app 自己的 iOS 组件（分组卡 / 导航标题 / 药丸按钮 / 细分割线）直接落在纸面上，镜头在纸面上推/潜。
- **承接物**：天数数字（B1→B8 同一个节点不重建）+ 下潜轴（B7、以及打卡那次）。
- **排除了什么**：除两个 burst 外无硬切；不出现平铺整屏截图当缩略图；不套手机壳；不用粒子。

### look：`anchor-paper`（产品本色）

```
ground #F5F2EE   deep #E7E4E0   card #FFFFFF   line #C6C6C8
mute   #6C6C70   ink  #000000   accent #2E6F73（石青）   accent2 #8C6D46（暖沙）
display Inter 700 / text Inter 500 / mono Geist Mono 400
```
全部取自 `E:\Anchor\app\src\main\java\com\anchor\recovery\ui\theme\AnchorColors.kt`
（`LightAnchorColors`：groupedBackground `#F5F2EE`、cardBackground `#FFFFFF`、opaqueSeparator `#C6C6C8`、
label `#000000`、labelSecondary `#6C6C70`、tint = `AnchorTeal #2E6F73`）与 `AnchorSand #8C6D46`。
对比度（`look.py check` PASS）：ink 18.82:1 · mute 4.69:1 · accent 5.17:1 · accent2 4.29:1。
look 文件：`E:\Anchor\film\_look\c1\look.json`（另两套候选 c2 品牌深水 / c3 石墨工作台 保留在 `_look/`）。
三套对比图：`E:\Anchor\film\_look\looks_compare.png`。

### 界面复刻的真实来源（`E:\Anchor\app\src\main\res\values\`）

字体：`app/src/main/res/font/harmonyos_sans_sc_{regular,medium,bold}.ttf`（HarmonyOS Sans SC，
免费商用，三个字重 400/500/700；Compose 里 medium 声明为 W600）。

字阶（`AnchorType`，照 HIG）：largeTitle 34/41 Bold · title1 28/34 Bold · title2 22/28 Bold ·
title3 20/25 SemiBold · headline 17/22 SemiBold · body 17/22 · callout 16/21 · subheadline 15/20 ·
footnote 13/18 · caption1 12/16 · caption2 11/13。

结构色：groupedBackground `#F5F2EE` · cardBackground `#FFFFFF` · separator `0x4A3C3C43`（≈30% 黑）·
opaqueSeparator `#C6C6C8` · label `#000000` · labelSecondary `#6C6C70` · labelTertiary `#8E8E93` ·
fill `0x29787880` · success `#34C759` · warning `#FF9500` · danger `#FF3B30`。圆角：卡 10dp、
大标题分组卡 10dp、弹窗 14dp、底部面板 24dp（顶两角）。iOS 卡片是平的，`surfaceTint` transparent。

底部 Tab：首页 / 工具 / 知识库 / 日志。顶栏标题：首页 =「磐石」，其余 =「工具」「知识库」「日志」。

**本片用到的真实文案（原样，不改写、不造数字）**

- `home_streak_current_label` 当前连续记录 · `home_streak_day_unit` " 天" ·
  `home_streak_history` 历史最长 %1$d 天 · 累计记录 %2$d 天
- `home_today_not_checked_in_message` 还没打卡，一天一次。 · `home_today_check_in_button` 打卡
- `home_progress_percent` 90 天进度 %1$d%% · `home_progress_next_milestone` " · 下一个里程碑：%1$s"
- `checkin_only_today_notice` 只能为今天打卡：漏一天就断签，补打会让连续天数失真。
- `checkin_title` 今日打卡 · `checkin_note_label` 今天的感受 / 触发情境（可留空）
- `tools_urge_surfing_title` 渴求冲浪 · `tools_urge_surfing_subtitle` 把一次冲动拆成几步：说明 → 呼吸 → 观察 → 等待 → 再评分。
- `urge_intensity_label` 现在的渴求强度 · `urge_elapsed_seconds` 已持续 %1$d 秒 ·
  `urge_breathing_hold` 保持 %1$d 秒 · 吸气 4 / 停 2 / 呼气 6 ·
  `urge_result_summary` 用时 %1$d 秒 · 峰值 %2$s/10 · `urge_delta_down` 下降 %1$d 分：它自己退下去了。
- `tools_delay_tool_title` 十分钟延时 · `tools_delay_tool_subtitle` 先不做决定，把选择往后放十分钟，计时结束再评估。
- `delay_start_heading` 把决定往后放十分钟 · `delay_prompt_rotate_hint` 每 %1$d 秒换一条，可在设置里改成自己的话。
- `delay_intensity_compare` 开始前 %1$d / 10 → 现在 %2$d / 10 · `delay_answer_passed` 过去了 · `delay_answer_still` 还在
- `relapse_hint` 如实记录就好，破戒很常见。保存后连续天数从今天重新算。 ·
  `relapse_situation_label` 情境（在哪、和谁、在做什么）· `relapse_emotions_title` 情绪（可多选）·
  `relapse_triggers_title` 触发源（可多选）
- `tools_relapse_title` 记录一次破戒 · `tools_milestone_subtitle` 7 / 14 / 30 / 60 / 90 天各一枚；破戒后重新开始，之前的徽章会保留。
- `milestone_title` 里程碑 · `milestone_header_note` 徽章按连续打卡天数点亮。断签或破戒后重新开始，已经点亮的徽章会保留。 ·
  `milestone_days_title_1/7/30/60/90` 第 1 天 / 第 1 周 / 第 1 个月 / 第 2 个月 / 90 天重启 ·
  `milestone_footer_note` 里程碑只表示你在这里记录了多少天，不代表治疗结果或「痊愈」。
- 五个阶段（`strings_phases.xml`）：急性期 / 波动期（第 2–4 周常被社区描述为"最艰难"的一段）/
  修复期 / 重连期 / 巩固期（90 天不是终点线，而是把新习惯固定下来的起点）·
  `phase_expectations_title` 可能会经历（自我报告归纳，不是诊断）· `phase_coping_title` 这一阶段可以试试
- `home_library_hint` 71 篇离线文章，每篇标注来源与可信度。 · `library_credibility` 可信度 %1$s ·
  `library_source_label` 来源 · `library_category_all_count` 全部 %1$d ·
  `library_disclaimer_content` 本内容整理自公开来源，仅供科普与自助参考，不构成诊断或治疗建议。
- `home_quick_tool_footnote` 90 天是社群参照，不是医学判定。
- `onboarding_offline_notice` 完全离线：数据只在这台手机上。
- `journal_accent_urge` 渴求平均 3–5 分钟达峰后自行下降 ·
  `journal_detail_intensity` 峰值强度 %1$d/10 → 结束时 %2$d/10 ·
  `journal_label_check_in` 打卡 · `journal_label_check_in_with_note` 打卡 · 有备注 ·
  `journal_filter_check_in/urge/relapse` 打卡 / 渴求 / 破戒

## §3 节拍表（真实 VO 时长：10 句，语音 A）

语音：`zh-CN-YunyangNeural`，`--rate=-5%`，speech 40.39 s + lead 1.0 + breath 0.40×9 + gap 2.4 = **47.39 s**，
片长 **49.0 s**。`vo/<voice>/plan.json` 是唯一真源，改 lines.txt 后重跑 `tools/vo.py synth/plan/subs/build`。

```
VO 线路（实测，plan.json）                画面 beat                                        承接物
0.0–1.0                                 安静起手：暖白纸面，一条细线上一枚刻度
1.00–5.34  磐石。完全离线，不联网，不上传。 「磐石」长出 → 三句依次落 → 镜头前推          刻度点
5.74–9.44  每天打卡一次，它替你数连续多少天。 点变按压点 → 打卡按下 → 数字 0→37           数字 37
9.84–13.46 漏一天就断签。它不给补打。      37 拆成 7 个日期格 → 空格被点 → 弹回 spring     空格
13.86–19.89 冲动来的时候…等它自己退。      空格展开成时间轴 → 曲线升起 → 峰值 7/10 →       曲线
                                          五步刻度点亮 → 下降 3 分
20.29–23.36 或者，把决定往后放十分钟。     同一条轴跑 10:00 + 理由轮换 + 过去了/还在      时间刻度
23.76–26.81 真的发生了，就如实记下来。     ★burst：标签墙 8 硬切×0.25 s（情绪/触发源）    标签
26.81–29.61 （无旁白 · 静默段 2.80 s）     曲线落到 0 → ★DIVE 1 穿过那个 0 → 徽章墙，        徽章墙
                                          一枚徽章灰→亮，画面全静 1.4 s
29.61–33.93 天数从今天重新算，已经点亮的徽章留着。 徽章墙缩成比例尺，天数 0→7            比例尺
34.33–38.22 五个阶段，每一段都写清会经历什么。 比例尺展开成 5 段轴，当前滑到「重连期」    轴右端
38.62–43.06 七十一篇离线文章，每篇标了来源和可信度。 轴右端展开成列表，数字翻滚，来源 chip  一张卡
43.46–47.39 九十天，是社群参照，不是医学判定。 卡放大成整屏大字，后段停住                 字
47.39–49.00 －                            片尾：磐石 + 完全离线：数据只在这台手机上。      －
```

- 字幕：**烧进画面**（用户选），底部一行，`subs.json` 19 条，`--max-cjk 14`。
- 两处画面全静：26.81–28.2（徽章墙，无字幕重叠）与 48.4–49.0。
- 微交互用库里 `swiftSpring(tau, kind, {duration, bounce})`（= SwiftUI `Spring(duration:bounce:)`，
  ζ=1−bounce，ω=2π/d；`.smooth` b=0 / `.snappy` .15 / `.bouncy` .3）。

## 文件

```
E:\Anchor\film\
  DESIGN.md                  本文件
  vo\lines.txt               10 句旁白（真源）
  vo\audition_A_yunyang.m4a  选定   vo\audition_B_xiaoxiao.m4a  备选
  vo\zh-CN-YunyangNeural\    t<i>.mp3/.wav · lines.json · plan.json · subs.json
  vo\vo.wav                  拼好的 48k mono 旁白
  tools\vo.py                合成/plan/subs/build/audition
  _look\                     三套候选 look + 对比图（保留，不删）
```

## 尚未做

- §4 复刻 Anchor UI（HTML，`set(state)` 纯函数写状态 + `lib/ui_kit.js` 量高亮）
- §5 `comp.html`（抄 templates/comp.html 的契约与 helpers，beat 自己写）
- §7 音效/音乐（`sfx_palette.py` 或 `score.py`；音乐要 royalty-free 并记录许可）
- §6 1080p30 草稿 → §8 `verify_promo.py` 验收 → 4K60 终版
- 交付时须说明：静止占比 ~20%（低于参考 38%）、音乐许可、4K60 渲染耗时
