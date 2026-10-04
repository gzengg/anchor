#!/usr/bin/env python3
"""vo.py — 中文配音：逐句合成（edge-tts）→ 真实时长 → plan.json / subs.json / vo.wav

    py tools/vo.py synth --voice zh-CN-YunyangNeural --rate=-5
    py tools/vo.py audition --voice zh-CN-YunyangNeural --out vo/audition_yunyang.m4a
    py tools/vo.py build                     # 用 plan 拼出 vo/vo.wav（48k mono）

lines.txt 一行 = 一句旁白。edge-tts 的 zh-CN 只给 SentenceBoundary（按。，切开），
所以句内的字级时间按字符数在句子时长里按比例分；句子级的锚点是实测的。
"""
import argparse, asyncio, json, os, pathlib, re, subprocess, sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
VO = ROOT / "vo"
SR = 48000
LEAD = 1.0          # 第一句之前
BREATH = 0.40       # 句间呼吸
GAPS = {7: 2.4}     # 第 N 句之前多留一段（画面自己走）


def ffprobe_dur(p):
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration",
                          "-of", "default=nw=1:nk=1", str(p)], capture_output=True, text=True).stdout.strip()
    return float(out)


def to_wav(mp3, wav):
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", str(mp3), "-ac", "1", "-ar", str(SR), str(wav)], check=True)


async def _synth(text, voice, rate, mp3):
    import edge_tts
    c = edge_tts.Communicate(text, voice, rate=rate)
    spans, data = [], bytearray()
    async for ch in c.stream():
        if ch["type"] == "audio":
            data += ch["data"]
        elif ch["type"] == "SentenceBoundary":
            spans.append({"t": ch["offset"] / 1e7, "d": ch["duration"] / 1e7, "text": ch["text"]})
    mp3.write_bytes(bytes(data))
    return spans


def split_chunks(text, max_cjk):
    """按标点切块，块长 ≤ max_cjk（CJK 计 1）。"""
    parts = re.findall(r"[^。，、；：！？…—]+[。，、；：！？…—]*", text) or [text]
    out, cur = [], ""
    for p in parts:
        if cur and len(cur) + len(p) > max_cjk:
            out.append(cur); cur = p
        else:
            cur += p
    if cur:
        out.append(cur)
    return [c for c in (x.strip() for x in out) if c]


def cmd_synth(a):
    base = VO / a.voice
    base.mkdir(parents=True, exist_ok=True)
    lines = [l.strip() for l in (VO / "lines.txt").read_text(encoding="utf-8").splitlines() if l.strip()]
    recs = []
    for i, text in enumerate(lines, 1):
        mp3, wav = base / f"t{i}.mp3", base / f"t{i}.wav"
        spans = asyncio.run(_synth(text, a.voice, a.rate, mp3))
        to_wav(mp3, wav)
        recs.append({"i": i, "text": text, "dur": round(ffprobe_dur(wav), 3),
                     "spans": [{"t": round(s["t"], 3), "d": round(s["d"], 3), "text": s["text"]} for s in spans]})
        print(f"  t{i}  {recs[-1]['dur']:6.2f}s  {len(spans)} sentence-spans  {text}")
    (base / "lines.json").write_text(json.dumps({"voice": a.voice, "rate": a.rate, "lines": recs},
                                                ensure_ascii=False, indent=2), encoding="utf-8")
    total = sum(r["dur"] for r in recs)
    print(f"speech {total:.2f}s · + lead {LEAD} + breaths {(len(recs)-1)*BREATH:.2f} + gaps {sum(GAPS.values())} "
          f"→ film ≈ {total + LEAD + (len(recs)-1)*BREATH + sum(GAPS.values()):.2f}s")


def cmd_plan(a):
    base = VO / a.voice
    d = json.loads((base / "lines.json").read_text(encoding="utf-8"))
    t = LEAD
    out = []
    for r in d["lines"]:
        if r["i"] in GAPS:
            t += GAPS[r["i"]]
        out.append({**r, "start": round(t, 3), "end": round(t + r["dur"], 3)})
        t += r["dur"] + (BREATH if r["i"] != d["lines"][-1]["i"] else 0)
    plan = {"voice": d["voice"], "lead": LEAD, "breath": BREATH, "gaps": GAPS,
            "total": round(t, 3), "lines": out}
    (base / "plan.json").write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"plan.json → {t:.2f}s, {len(out)} lines")
    for r in out:
        print(f"  {r['start']:6.2f} – {r['end']:6.2f}  {r['text']}")


def cmd_subs(a):
    base = VO / a.voice
    plan = json.loads((base / "plan.json").read_text(encoding="utf-8"))
    subs = []
    for r in plan["lines"]:
        spans = r["spans"] or [{"t": 0.0, "d": r["dur"], "text": r["text"]}]
        for s in spans:
            chunks = split_chunks(s["text"], a.max_cjk)
            n = sum(len(c) for c in chunks) or 1
            ct = r["start"] + s["t"]
            for c in chunks:
                d = s["d"] * len(c) / n
                subs.append([round(ct, 3), round(ct + d, 3), c])
                ct += d
    (base / "subs.json").write_text(json.dumps(subs, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"subs.json → {len(subs)} cues, last end {subs[-1][1]:.2f}s")
    for s in subs:
        print(f"  {s[0]:6.2f}–{s[1]:6.2f}  {s[2]}")


def cmd_build(a):
    """按 plan 把逐句 wav 摆到时间轴上 → vo/vo.wav（48k mono）。"""
    import numpy as np
    base = VO / a.voice
    plan = json.loads((base / "plan.json").read_text(encoding="utf-8"))
    total = plan["total"] + 1.2
    buf = np.zeros(int(total * SR), dtype=np.float32)
    for r in plan["lines"]:
        wav = subprocess.run(["ffmpeg", "-v", "error", "-i", str(base / f"t{r['i']}.wav"), "-f", "f32le",
                              "-ac", "1", "-ar", str(SR), "-"], capture_output=True).stdout
        x = np.frombuffer(wav, dtype=np.float32)
        s = int(r["start"] * SR)
        buf[s:s + len(x)] += x[:len(buf) - s]
    VO.mkdir(exist_ok=True)
    raw = VO / "_vo_raw.f32"
    raw.write_bytes(buf.tobytes())
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-f", "f32le", "-ar", str(SR), "-ac", "1", "-i", str(raw),
                    "-c:a", "pcm_s16le", str(VO / "vo.wav")], check=True)
    raw.unlink()
    print(f"vo/vo.wav → {ffprobe_dur(VO/'vo.wav'):.2f}s")


def cmd_audition(a):
    base = VO / a.voice
    base.mkdir(parents=True, exist_ok=True)
    lines = [l.strip() for l in (VO / "lines.txt").read_text(encoding="utf-8").splitlines() if l.strip()]
    parts = []
    for i, text in enumerate(lines, 1):
        mp3 = base / f"a{i}.mp3"
        asyncio.run(_synth(text, a.voice, a.rate, mp3))
        parts.append(mp3)
    lst = base / "_aud.txt"
    lst.write_text("".join(f"file '{p.as_posix()}'\n" for p in parts), encoding="utf-8")
    out = pathlib.Path(a.out)
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-f", "concat", "-safe", "0", "-i", str(lst),
                    "-c:a", "aac", "-b:a", "128k", str(out)], check=True)
    for p in parts:
        p.unlink()
    lst.unlink()
    print("wrote", out, f"{ffprobe_dur(out):.2f}s")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    sp = ap.add_subparsers(dest="cmd", required=True)
    for name, fn in (("synth", cmd_synth), ("plan", cmd_plan), ("subs", cmd_subs),
                     ("build", cmd_build), ("audition", cmd_audition)):
        p = sp.add_parser(name); p.set_defaults(fn=fn)
        p.add_argument("--voice", default="zh-CN-YunyangNeural")
        p.add_argument("--rate", default="-5%")
        if name == "subs":
            p.add_argument("--max-cjk", type=int, default=14)
        if name == "audition":
            p.add_argument("--out", required=True)
    a = ap.parse_args(); a.fn(a)
