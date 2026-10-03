"""扫描 Kotlin 源码里「用户可见」的中文字面量（注释不算）。

用途：
1. P4（文案抽取入 strings.xml）的进度盘点与批次划分依据；
2. 抽取后的验收手段——本项目没有真机，靠「字面量清单为空/只剩记录在案的项」代替界面走查，
   补足 `磐石Anchor-交付说明.md` §4.4 的真机项。

判定规则：逐字符扫，跳过 `//` 行注释、`/* */` 块注释（含 KDoc），只统计
双引号字符串字面量（含三引号原始字符串）里出现中日韩统一表意文字（U+4E00–U+9FFF）的内容。

用法：
    py content-tools/scan_hardcoded_text.py            # 分组统计
    py content-tools/scan_hardcoded_text.py --list     # 附带逐条字面量
    py content-tools/scan_hardcoded_text.py --list app/src/main/java  # 只看某个文件/目录
"""

from __future__ import annotations

import argparse
import os
import re
import sys

CJK = re.compile("[\u4e00-\u9fff]")
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCES = ("app/src/main/java", "core/src/main/kotlin")

GROUPS = (
    ("app/ui 组件与页面", "app/src/main/java/com/anchor/recovery/ui/"),
    ("app/notify 通知", "app/src/main/java/com/anchor/recovery/notify/"),
    ("app 其它（应用层）", "app/src/main/java/com/anchor/recovery/"),
    ("core 展示文案", "core/src/main/kotlin/"),
)


def strip_comments_and_collect(path: str) -> list[tuple[int, str]]:
    """返回 [(行号, 字面量内容), ...]，只含带中文的字符串字面量。"""
    with open(path, encoding="utf-8") as handle:
        text = handle.read()

    found: list[tuple[int, str]] = []
    i = 0
    line = 1
    length = len(text)
    while i < length:
        char = text[i]

        if char == "\n":
            line += 1
            i += 1
            continue

        # 行注释
        if text.startswith("//", i):
            while i < length and text[i] != "\n":
                i += 1
            continue

        # 块注释（含 KDoc）
        if text.startswith("/*", i):
            i += 2
            while i < length and not text.startswith("*/", i):
                if text[i] == "\n":
                    line += 1
                i += 1
            i += 2
            continue

        # 原始字符串（三引号）
        if text.startswith('"""', i):
            start_line = line
            i += 3
            buffer: list[str] = []
            while i < length and not text.startswith('"""', i):
                buffer.append(text[i])
                if text[i] == "\n":
                    line += 1
                i += 1
            i += 3
            literal = "".join(buffer)
            if CJK.search(literal):
                found.append((start_line, literal.strip()))
            continue

        # 普通字符串（跳过转义字符）
        if char == '"':
            start_line = line
            i += 1
            buffer = []
            while i < length and text[i] != '"':
                if text[i] == "\\":
                    buffer.append(text[i])
                    i += 1
                    if i < length:
                        buffer.append(text[i])
                    i += 1
                    continue
                if text[i] == "\n":
                    break
                buffer.append(text[i])
                i += 1
            i += 1
            literal = "".join(buffer)
            if CJK.search(literal):
                found.append((start_line, literal))
            continue

        if char == "'":  # 字符字面量
            i += 1
            while i < length and text[i] != "'":
                i += 2 if text[i] == "\\" else 1
            i += 1
            continue

        i += 1

    return found


def group_of(relative_path: str) -> str:
    normalised = relative_path.replace(os.sep, "/")
    for name, prefix in GROUPS:
        if normalised.startswith(prefix):
            return name
    return "其它"


def collect_targets(roots: list[str]) -> list[str]:
    targets: list[str] = []
    for root in roots:
        absolute = os.path.join(ROOT, root)
        if os.path.isfile(absolute):
            targets.append(root)
            continue
        for dirpath, _, filenames in os.walk(absolute):
            for filename in filenames:
                if filename.endswith(".kt"):
                    targets.append(os.path.relpath(os.path.join(dirpath, filename), ROOT))
    return targets


def main() -> int:
    # Windows 控制台默认 GBK：直接 print 中文/emoji 会 UnicodeEncodeError 并中断输出。
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    parser = argparse.ArgumentParser()
    parser.add_argument("--list", action="store_true", help="逐条打印字面量")
    parser.add_argument("paths", nargs="*", help="只扫描指定文件/目录（相对仓库根）")
    args = parser.parse_args()

    per_file: dict[str, list[tuple[int, str]]] = {}
    for target in sorted(collect_targets(args.paths or list(SOURCES))):
        hits = strip_comments_and_collect(os.path.join(ROOT, target))
        if hits:
            per_file[target.replace(os.sep, "/")] = hits

    if args.list:
        for target, hits in sorted(per_file.items(), key=lambda item: -len(item[1])):
            print(f"== {target} ({len(hits)})")
            for line, literal in hits:
                shown = literal.replace("\n", "\\n")
                if len(shown) > 60:
                    shown = shown[:57] + "..."
                print(f"   {line:>4}  \"{shown}\"")

    per_group: dict[str, int] = {name: 0 for name, _ in GROUPS}
    per_group["其它"] = 0
    for target, hits in per_file.items():
        per_group[group_of(target)] += len(hits)

    print("---")
    for name, count in per_group.items():
        print(f"{count:5d}  {name}")
    print(f"{sum(per_group.values()):5d}  合计（字面量个数，注释不计）")
    print(f"{len(per_file):5d}  涉及文件数")
    return 0


if __name__ == "__main__":
    sys.exit(main())
