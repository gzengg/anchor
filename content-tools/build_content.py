#!/usr/bin/env python
"""磐石（Anchor）内容管线：anchor-reference-docs/*.md -> app/src/main/assets/content/*.json

一次性离线脚本（Python 3.13，零第三方依赖）。运行：

    C:\\Users\\Administrator\\.workbuddy\\binaries\\python\\versions\\3.13.12\\python.exe content-tools/build_content.py

校验失败一律 fail-fast，错误信息包含文件名与行号；产物入 git。

设计取舍：
- front-matter 字段固定（title/source/author/date/category/tags/credibility/summary），
  因此手写极简解析器，不引入 pyyaml。
- 源数据里 credibility 存在第四种写法「中-高」。App 只支持三级徽章（高/中/低），
  按「保守降级」归一到「中」，同时把原始等级保留在 credibilityRaw 里以免丢失信息。
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SOURCE_DIR = REPO_ROOT / "anchor-reference-docs"
OUTPUT_DIR = REPO_ROOT / "app" / "src" / "main" / "assets" / "content"

EXPECTED_FILE_COUNT = 71

# 分类元数据：目录名 -> (分类 key, 中文显示名, 简介)
# 分类名固定 2 字：它同时是知识库页顶部分段控件的段标签（每段 88dp，见
# app/src/main/java/com/anchor/recovery/ui/library/LibraryScreen.kt 的 CATEGORY_SEGMENT_WIDTH）。
# 4 字标题 + 两位数计数需约 80dp，扣掉轨道与文字内边距后只剩 79.4dp，会被省略号截断。
CATEGORY_META: dict[str, tuple[str, str, str]] = {
    "01-science": ("science", "科研", "成瘾神经科学与行为机制研究"),
    "02-authority": ("authority", "机构", "WHO/APA 等官方分类与立场"),
    "03-medical": ("medical", "临床", "医疗机构的症状、诊断与治疗说明"),
    "04-methods": ("methods", "方法", "有实证支持的戒断与干预方法"),
    "05-cases": ("cases", "经验", "亲历者叙述与社群经验研究"),
    "06-statistics": ("statistics", "统计", "流行病学与人群调查数据"),
}

# 归一化规则：原始等级 -> 三级等级
CREDIBILITY_NORMALIZE: dict[str, str] = {
    "高": "高",
    "中": "中",
    "低": "低",
    "中-高": "中",
}

ALLOWED_CREDIBILITY_RAW = set(CREDIBILITY_NORMALIZE)
ALLOWED_CREDIBILITY = {"高", "中", "低"}

REQUIRED_FIELDS = (
    "title",
    "source",
    "author",
    "date",
    "category",
    "tags",
    "credibility",
    "summary",
)

FIELD_ORDER = REQUIRED_FIELDS
URL_RE = re.compile(r"^https?://\S+$")
DIR_RE = re.compile(r"^(\d{2})-([a-z]+)$")


class ContentError(Exception):
    """内容管线校验错误，消息中必须包含文件名与行号。"""


def _fail(path: Path, line_no: int | None, message: str) -> ContentError:
    where = path.name if line_no is None else f"{path.name}:{line_no}"
    return ContentError(f"[内容校验失败] {where} {message}")


def _unquote(path: Path, line_no: int, key: str, raw: str) -> str:
    """解析形如 "文本" 的标量字段，支持 \\" 与 \\\\ 转义。"""
    value = raw.strip()
    if len(value) < 2 or not (value.startswith('"') and value.endswith('"')):
        raise _fail(path, line_no, f"字段 {key} 必须用双引号包裹，实际为 {raw!r}")
    body = value[1:-1]
    out: list[str] = []
    i = 0
    while i < len(body):
        ch = body[i]
        if ch == "\\":
            if i + 1 >= len(body):
                raise _fail(path, line_no, f"字段 {key} 以孤立反斜杠结尾")
            nxt = body[i + 1]
            if nxt not in ('"', "\\"):
                raise _fail(path, line_no, f"字段 {key} 含非法转义 \\{nxt}")
            out.append(nxt)
            i += 2
            continue
        out.append(ch)
        i += 1
    text = "".join(out)
    if not text.strip():
        raise _fail(path, line_no, f"字段 {key} 为空")
    return text


def _parse_tags(path: Path, line_no: int, raw: str) -> list[str]:
    """解析 tags：源数据里同时存在 ["a", "b"] 与 [a, b] 两种写法。"""
    value = raw.strip()
    if not (value.startswith("[") and value.endswith("]")):
        raise _fail(path, line_no, f"字段 tags 必须是 [...] 数组，实际为 {raw!r}")
    inner = value[1:-1]
    items: list[str] = []
    current: list[str] = []
    in_string = False
    i = 0
    while i < len(inner):
        ch = inner[i]
        if ch == "\\":
            if i + 1 >= len(inner):
                raise _fail(path, line_no, "字段 tags 以孤立反斜杠结尾")
            current.append(inner[i + 1])
            i += 2
            continue
        if ch == '"':
            in_string = not in_string
            i += 1
            continue
        if ch == "," and not in_string:
            items.append("".join(current).strip())
            current = []
            i += 1
            continue
        current.append(ch)
        i += 1
    if in_string:
        raise _fail(path, line_no, "字段 tags 引号未闭合")
    items.append("".join(current).strip())

    cleaned = [item for item in items if item]
    if not cleaned:
        raise _fail(path, line_no, "字段 tags 解析后为空")
    if len(cleaned) != len(items):
        raise _fail(path, line_no, f"字段 tags 存在空项：{items!r}")
    return cleaned


def parse_front_matter(path: Path, text: str) -> tuple[dict[str, object], str]:
    """返回 (字段字典, 正文 Markdown)。字段缺失/重复/非法一律抛错。"""
    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    if normalized.startswith("\ufeff"):
        normalized = normalized[1:]
    if not normalized.startswith("---\n"):
        raise _fail(path, 1, "文件未以 '---' front-matter 开头")

    end = normalized.find("\n---\n", 3)
    if end < 0:
        raise _fail(path, 1, "front-matter 未用 '---' 正常闭合")
    block = normalized[4:end]
    body = normalized[end + 5:].strip()

    fields: dict[str, object] = {}
    for offset, raw_line in enumerate(block.split("\n")):
        line_no = offset + 2  # front-matter 从文件第 2 行开始
        line = raw_line.rstrip()
        if not line.strip():
            continue
        match = re.match(r"^([a-z_]+):\s*(.*)$", line)
        if not match:
            raise _fail(path, line_no, f"无法解析的行：{line!r}")
        key, raw_value = match.group(1), match.group(2)
        if key not in REQUIRED_FIELDS:
            raise _fail(path, line_no, f"未知字段 {key!r}")
        if key in fields:
            raise _fail(path, line_no, f"字段 {key!r} 重复定义")
        if key == "tags":
            fields[key] = _parse_tags(path, line_no, raw_value)
        else:
            fields[key] = _unquote(path, line_no, key, raw_value)

    missing = [key for key in FIELD_ORDER if key not in fields]
    if missing:
        raise _fail(path, None, f"缺少字段 {missing}")

    if not body:
        raise _fail(path, None, "正文为空")

    source = str(fields["source"])
    if not URL_RE.match(source):
        raise _fail(path, None, f"source 不是 http(s) URL：{source!r}")

    raw_credibility = str(fields["credibility"])
    if raw_credibility not in ALLOWED_CREDIBILITY_RAW:
        raise _fail(path, None, f"credibility 取值非法：{raw_credibility!r}")
    fields["credibility"] = CREDIBILITY_NORMALIZE[raw_credibility]
    if fields["credibility"] not in ALLOWED_CREDIBILITY:
        raise _fail(path, None, f"credibility 归一化后非法：{fields['credibility']!r}")
    fields["credibilityRaw"] = raw_credibility

    return fields, body


def collect_sources(source_dir: Path) -> list[tuple[Path, str, str]]:
    """返回 [(md 路径, 分类 key, 序号)]，按目录名与文件名稳定排序。"""
    collected: list[tuple[Path, str, str]] = []
    for dir_name, (category, _title, _desc) in CATEGORY_META.items():
        directory = source_dir / dir_name
        if not directory.is_dir():
            raise ContentError(f"[内容校验失败] 缺少内容目录：{directory}")
        files = sorted(p for p in directory.iterdir() if p.suffix == ".md")
        for path in files:
            match = re.match(r"^(\d{3})-", path.stem)
            if not match:
                raise _fail(path, None, "文件名未以三位数字序号开头")
            collected.append((path, category, match.group(1)))
    return collected


def build(
    source_dir: Path = SOURCE_DIR,
    expected_count: int = EXPECTED_FILE_COUNT,
) -> dict[str, object]:
    entries = collect_sources(source_dir)
    if len(entries) != expected_count:
        raise ContentError(
            f"[内容校验失败] 文章数量不匹配：期望 {expected_count}，实际 {len(entries)}"
        )

    dirs_seen = {p.parent.name for p, _c, _n in entries}
    unknown_dirs = dirs_seen - set(CATEGORY_META)
    if unknown_dirs:
        raise ContentError(f"[内容校验失败] 出现未登记的分类目录：{sorted(unknown_dirs)}")

    articles: list[dict[str, object]] = []
    ids: set[str] = set()
    per_category: dict[str, list[str]] = {category: [] for category, _t, _d in CATEGORY_META.values()}

    for path, category, number in entries:
        text = path.read_text(encoding="utf-8")
        fields, body = parse_front_matter(path, text)

        dir_name = path.parent.name
        expected_category = CATEGORY_META[dir_name][0]
        if fields["category"] != expected_category:
            raise _fail(
                path, None,
                f"category 与目录不一致：字段={fields['category']!r} 目录={dir_name!r}",
            )

        article_id = f"{category}-{number}"
        if article_id in ids:
            raise _fail(path, None, f"文章 id 重复：{article_id}")
        ids.add(article_id)
        per_category[category].append(article_id)

        articles.append({
            "id": article_id,
            "category": category,
            "categoryDir": dir_name,
            "title": fields["title"],
            "source": fields["source"],
            "author": fields["author"],
            "date": fields["date"],
            "tags": fields["tags"],
            "credibility": fields["credibility"],
            "credibilityRaw": fields["credibilityRaw"],
            "summary": fields["summary"],
            "bodyMarkdown": body,
        })

    for article in articles:
        if article["credibility"] not in ALLOWED_CREDIBILITY:
            raise ContentError(f"[内容校验失败] 归一化后仍存在非法 credibility：{article['id']}")

    categories = []
    for dir_name, (category, title, desc) in CATEGORY_META.items():
        categories.append({
            "key": category,
            "dir": dir_name,
            "title": title,
            "description": desc,
            "count": len(per_category[category]),
            "articleIds": per_category[category],
        })

    index = {
        "version": 1,
        "total": len(articles),
        "categories": categories,
    }
    return {"articles": articles, "index": index}


def write_outputs(payload: dict[str, object], output_dir: Path = OUTPUT_DIR) -> tuple[Path, Path]:
    output_dir.mkdir(parents=True, exist_ok=True)
    articles_path = output_dir / "articles.json"
    index_path = output_dir / "index.json"

    def dump(obj: object) -> str:
        return json.dumps(obj, ensure_ascii=False, indent=1, sort_keys=False) + "\n"

    articles_path.write_text(dump(payload["articles"]), encoding="utf-8", newline="\n")
    index_path.write_text(dump(payload["index"]), encoding="utf-8", newline="\n")
    return articles_path, index_path


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="磐石内容管线：Markdown -> JSON")
    parser.add_argument("--source", type=Path, default=SOURCE_DIR, help="内容源目录")
    parser.add_argument("--output", type=Path, default=OUTPUT_DIR, help="产物输出目录")
    parser.add_argument(
        "--expect-count", type=int, default=EXPECTED_FILE_COUNT, help="期望文章数"
    )
    args = parser.parse_args(argv)

    try:
        payload = build(args.source, args.expect_count)
    except ContentError as error:
        print(str(error), file=sys.stderr)
        return 1

    articles_path, index_path = write_outputs(payload, args.output)
    articles = payload["articles"]
    assert isinstance(articles, list)
    counts = ", ".join(
        f"{entry['key']}={entry['count']}"
        for entry in payload["index"]["categories"]  # type: ignore[index]
    )
    print(f"OK: {len(articles)} 篇文章 -> {articles_path}")
    print(f"OK: index -> {index_path}")
    print(f"    分类计数：{counts}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
