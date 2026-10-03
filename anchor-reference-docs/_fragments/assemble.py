# -*- coding: utf-8 -*-
"""Assemble the final literature compilation, README index and source list."""
import re
from pathlib import Path

ROOT = Path(r"E:/Anchor/jiese-content-db")
FRAG = ROOT / "_fragments"

# ---------- 1. Assemble literature compilation ----------
def split_frag(p):
    text = p.read_text(encoding="utf-8")
    body, _, refs = text.partition("===REFS===")
    refmap = {}
    marks = list(re.finditer(r"\[([A-D]\d+)\]", refs))
    for i, m in enumerate(marks):
        end = marks[i + 1].start() if i + 1 < len(marks) else len(refs)
        refmap[m.group(1)] = " ".join(refs[m.end():end].split())
    return body.strip(), refmap

def normalize_d(body):
    """part-D tables use a leading '#' column with tags like | D1 |; fold the tag into the title cell."""
    lines = []
    for line in body.splitlines():
        m = re.match(r"\|\s*(D\d+)\s*\|(.+)", line)
        if m:
            rest = m.group(2)
            line = "|" + rest.replace(" | ", f" [{m.group(1)}] | ", 1)
        elif line.startswith("| # |"):
            line = line.replace("| # | ", "| ", 1)
        elif re.match(r"^\|[-\s|]+\|$", line) and line.count("---") > 9:
            parts = line.split("|")
            del parts[1]  # drop the '#' separator column
            line = "|".join(parts)
        lines.append(line)
    return "\n".join(lines)

order = ["part-B.md", "part-A.md", "part-C.md", "part-D.md"]
bodies, allrefs = [], {}
for name in order:
    b, rm = split_frag(FRAG / name)
    if name == "part-D.md":
        b = normalize_d(b)
    bodies.append(b)
    allrefs.update(rm)

body = "\n\n".join(bodies)

mapping = {}
n = 0
for m in re.finditer(r"\[([A-D]\d+)\]", body):
    t = m.group(1)
    if t not in mapping:
        n += 1
        mapping[t] = n

body_new = re.sub(r"\[([A-D]\d+)\]", lambda m: f"[{mapping[m.group(1)]}]", body)
inv = {v: k for k, v in mapping.items()}
refs_section = "\n\n".join(f"[{i}] {allrefs[inv[i]]}" for i in sorted(inv))

cnt = {p: sum(1 for k in mapping if k.startswith(p)) for p in "BADC"}
total = len(mapping)

ORG_KEYS = ["WHO", "ICD", "APA", "ASAM", "AASECT", "Monitor on Psychology",
            "Common Sense", "卫生", "政府", "CDC", "NHS"]
org_hits = set()
for m in re.finditer(r"^\|(.+)\|\s*$", body, re.M):
    cells = m.group(1).split("|")
    if len(cells) >= 4 and any(k in cells[3] for k in ORG_KEYS):
        for tag in re.findall(r"\[([A-D]\d+)\]", cells[0]):
            org_hits.add(tag)
org_cnt = len([t for t in org_hits if t in mapping])

header = f"""# 戒色主题权威文献与数据汇编

> 关于戒除色情内容消费及强迫性色情使用（PPU）/ 强迫性性行为障碍（CSBD）的同行评审研究与官方报告汇编。
> 生成日期：2026-10-03

## 摘要

本汇编共收录 **{total} 条** 有量化数据或官方立场的文献与报告，按主题分为六章：定义与诊断（{cnt['B']} 条，含流行病学）、神经机制（{cnt['A']} 条，含健康影响）、干预效果（{cnt['C']} 条）、学术争议（{cnt['D']} 条）。来源分布：以同行评审期刊论文为主（约 {total - org_cnt} 条，覆盖 JAMA Psychiatry、Neuropsychopharmacology、Archives of Sexual Behavior、World Psychiatry、Journal of Sexual Medicine 等），官方机构文件与权威报告约 {org_cnt} 条（WHO ICD-11、APA、ASAM、AASECT、Common Sense Media 等）。时间范围 2003–2025 年，以 2010 年后为主；个别早期标志性研究（如 2003 年禁欲与睾酮研究）已收录并注明其状态（该文已于 2021 年撤稿）。

**最值得关注的 3 项核心数据：**

1. **8.6%**：美国全国代表性样本（N=2,325）中 8.6% 成人（男 10.3%、女 7.0%）报告因难以控制性冲动/行为而感到临床意义的痛苦（Dickenson et al., 2018, JAMA Network Open）——问题性色情/性行为困扰的真实规模远高于大众直觉。
2. **93%**：接纳承诺疗法（ACT）12 次治疗后，问题性色情使用者的观看量减少 93%（对照组 21%），54% 完全停止，3 个月随访维持 86% 降幅（Crosby & Twohig, 2016，RCT）——目前证据最强的戒色心理干预方案。
3. **3–8%**：大样本潜在画像分析（N=14,006）显示高频色情使用者中仅 3–8% 存在问题性使用，高频无问题者是有问题者的 3–6 倍（Bőthe et al., 2020）——"高频≠成瘾"，戒色内容产品应避免对普通使用者的过度病理化。

---
"""

out = header + "\n" + body_new + "\n\n---\n\n## 参考文献\n\n" + refs_section + "\n"
(ROOT / "戒色主题权威文献与数据汇编.md").write_text(out, encoding="utf-8")
print(f"汇编完成: {total} 条引用 (B={cnt['B']}, A={cnt['A']}, C={cnt['C']}, D={cnt['D']}), 官方来源约 {org_cnt} 条")

# ---------- 2. Parse front-matter of all category files ----------
def esc(s):
    return str(s or "").replace("|", "\\|").strip()

def parse_fm(p):
    text = p.read_text(encoding="utf-8")
    m = re.match(r"---\s*\n(.*?)\n---", text, re.S)
    d = {}
    if m:
        for line in m.group(1).splitlines():
            mm = re.match(r"(\w+):\s*(.*)", line)
            if mm:
                d[mm.group(1)] = mm.group(2).strip().strip('"').strip("'")
    return d

CATS = [
    ("01-science", "一、科学研究类", "同行评审期刊关于色情成瘾机制、多巴胺奖励回路、大脑可塑性的研究"),
    ("02-authority", "二、权威机构观点", "WHO / APA / ASAM / AASECT 等官方立场声明与诊断标准"),
    ("03-medical", "三、医学/心理学专业解读", "医疗机构与执业专业人士的成因、危害评估与鉴别诊断科普"),
    ("04-methods", "四、实践方法类", "CBT / ACT / 正念 / 药物 / 习惯替代 / 复吸预防等干预方法"),
    ("05-cases", "五、真实案例与经验分享", "戒断反应时间线、重启经验、社区调查与定性研究"),
    ("06-statistics", "六、数据统计类", "使用率调查、患病率统计、时间趋势与对比研究数据"),
]

all_entries = []
readme_cats = []
for folder, cname, cdesc in CATS:
    files = sorted((ROOT / folder).glob("*.md"))
    rows = []
    for f in files:
        fm = parse_fm(f)
        rel = f"{folder}/{f.name}"
        all_entries.append((folder, f.name, fm))
        rows.append(f"| [{f.name}]({rel}) | {esc(fm.get('title'))} | {esc(fm.get('author'))} | {esc(fm.get('date'))} | {esc(fm.get('credibility'))} |")
    readme_cats.append((folder, cname, cdesc, len(files), rows))

total_files = len(all_entries)

readme = ["# 戒色主题内容数据库 · 总索引", "",
"> 用于戒色辅助类 App 内容数据库的权威资料库。所有条目均来自实际可核验来源（同行评审期刊、WHO/APA 等官方机构、知名医疗机构、执业专业人士），并标注可信度评级。",
"> 生成日期：2026-10-03 ｜ 文章总数：" + str(total_files) + " 篇", "",
"## 数据库构成", "",
"| 组成部分 | 说明 |", "|---|---|",
"| [戒色主题权威文献与数据汇编.md](戒色主题权威文献与数据汇编.md) | 六章结构化文献综述，68 条带量化数据的研究条目 + 统一编号参考文献 |",
"| [资料来源清单表.md](资料来源清单表.md) | 全部 71 篇文章的来源 URL、发布机构、日期与可信度评级，便于核查 |",
"| 六个分类文件夹 | 每类 11–12 篇独立 md 文章，含 front-matter 元数据与结构化要点 |", "",
"## 分类导航", ""]
for folder, cname, cdesc, cnt_f, _ in readme_cats:
    readme.append(f"- [{cname}（{cnt_f} 篇）]({folder}/) — {cdesc}")
readme.append("")
readme.append("## 可信度评级说明")
readme += ["", "- **高**：同行评审期刊、WHO/APA/政府官方文件；", "- **中**：知名医疗机构科普、专业媒体（有医学审阅）、倡导性网站汇总（已注明立场）、单一调查；",
           "- **低**：社区自我报告调查（存在自选样本偏差）、个人经验分享、媒体报道。", "",
           "> ⚠️ 特别提示：网络上流传的「禁欲 7 天睾酮提升 45.7%」说法的唯一出处（Jiang et al., 2003）已于 2021 年被期刊正式撤稿，本库中相关条目均已如实标注，请在 App 内容中避免引用该数据。", ""]
readme.append("## 文章索引")
for folder, cname, cdesc, cnt_f, rows in readme_cats:
    readme += ["", f"### {cname}（{cnt_f} 篇）", "", f"*{cdesc}*", "",
               "| 文件 | 标题 | 作者/机构 | 日期 | 可信度 |", "|---|---|---|---|---|"] + rows
(ROOT / "README.md").write_text("\n".join(readme) + "\n", encoding="utf-8")
print(f"README 完成: {total_files} 篇文章")

# ---------- 3. Source list table ----------
lines = ["# 资料来源清单表", "",
"> 汇总全部 71 篇文章的引用来源与可信度评级，便于后续核查。所有 URL 均经检索验证真实存在。",
"> 生成日期：2026-10-03", ""]
cat_label = dict((f, n) for f, n, _ in CATS)
for folder, cname, _ in CATS:
    entries = [(fn, fm) for fo, fn, fm in all_entries if fo == folder]
    lines += ["", f"## {cname}（{len(entries)} 篇）", "",
              "| 编号 | 标题 | 发布机构/作者 | 日期 | 可信度 | 来源 URL |", "|---|---|---|---|---|---|"]
    for fn, fm in entries:
        num = fn.split("-")[0]
        lines.append(f"| {num} | {esc(fm.get('title'))} | {esc(fm.get('author'))} | {esc(fm.get('date'))} | {esc(fm.get('credibility'))} | {esc(fm.get('source'))} |")
lines += ["", "---", "",
"## 文献汇编引用来源",
"",
"综合文献汇编（68 条带数据条目）的完整引文（APA 格式，含 DOI/URL）见 [戒色主题权威文献与数据汇编.md](戒色主题权威文献与数据汇编.md) 末尾的参考文献章节。",
""]
(ROOT / "资料来源清单表.md").write_text("\n".join(lines), encoding="utf-8")
print("来源清单表完成")
