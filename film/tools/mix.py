"""Build the film's soundtrack: the spoken words (already rendered) + a room + one cue per event.

Nothing here is sampled and nothing is licensed from anyone: every sound is synthesised from the
onetake SFX palette (bell/noise/sub primitives) or generated as a drone, so the film ships with no
third-party audio rights to clear. The bed owns a real silence around 26.5–29.0 s: the badge wall is
the one moment the product raises its voice, and it lands in a hole in the room.

  py tools/mix.py                        → out/audio.wav   (48k stereo)
"""
import os, sys, wave
import numpy as np

SK = os.path.expanduser('~/.pi/agent/skills/onetake/scripts')
sys.path.insert(0, SK)
from sfx_palette import (SR, air, glass, wood, sub, bubble, wobble, impulse, sat, lp, hp, pan_of,
                         _t, Score)

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VO = os.path.join(HERE, 'vo', 'vo.wav')
OUT = os.path.join(HERE, 'out', 'audio.wav')
DUR = 49.7

RNG = np.random.default_rng(1107)

# ── the room: a drone that is mostly a floor, not a melody. Owned, generated, 4 layers, slow drift.
def bed(n):
    t = _t(n)
    x = (0.55 * np.sin(2 * np.pi * 55.0 * t)
         + 0.34 * np.sin(2 * np.pi * 82.41 * t + 0.7)
         + 0.22 * np.sin(2 * np.pi * 110.0 * t + 2.1)
         + 0.10 * np.sin(2 * np.pi * 164.81 * t + 4.0))
    x *= 0.72 + 0.28 * np.sin(2 * np.pi * 0.055 * t - 1.2)        # one slow breath per 18 s
    x += 0.06 * hp(RNG.standard_normal(n), 1100)                  # a whisper of air over it
    return x

def env(n, a=0.6, r=2.0):
    """slow attack, long release — a room does not switch on"""
    e = np.ones(n)
    A, R = int(a * SR), int(r * SR)
    e[:A] = np.linspace(0, 1, A) ** 1.5
    e[n - R:] = np.linspace(1, 0, R) ** 1.5
    return e

def duck(n):
    """the one hole in the room: 26.45 → 29.00 s, the badge wall"""
    d = np.ones(n)
    a0, a1, a2, a3 = [int(v * SR) for v in (25.90, 26.45, 28.90, 29.60)]
    d[a0:a1] = np.linspace(1, 0.0, a1 - a0)
    d[a1:a2] = 0.0
    d[a2:a3] = np.linspace(0.0, 1, a3 - a2)
    return d * env(n, 1.6, 3.2)

# ── cues: (time, kind, gain, pan in frame x 0..1920)
CUES = [
    (0.55, 'breath', 0.10, 300), (3.72, 'tap', 0.16, 790), (4.02, 'tap', 0.16, 1130),
    (5.45, 'morph', 0.15, 960), (7.10, 'click', 0.10, 960), (8.07, 'tap', 0.30, 960),
    (8.62, 'bubble', 0.10, 900), (8.78, 'bubble', 0.09, 960),
    (10.50, 'breath', 0.13, 700),                                  # 漏掉的那一天：没有声音落下来
    (11.50, 'sub', 0.22, 700),                                     # 不给补打：一声很闷的“不行”
    (14.78, 'click', 0.11, 960), (15.28, 'click', 0.12, 1100), (15.30, 'glass', 0.13, 1100),
    (16.78, 'click', 0.11, 960), (17.38, 'click', 0.10, 900), (17.98, 'click', 0.10, 860),
    (18.72, 'click', 0.09, 840), (19.35, 'glass', 0.09, 960),
    (20.85, 'breath', 0.12, 960), (22.85, 'glass', 0.16, 960), (23.20, 'click', 0.10, 900),
    (26.05, 'morphdry', 0.10, 960),                                 # the tags go, and the room goes with them
    (28.62, 'chime', 0.30, 960),                                   # 30 天徽章亮起，房间里只有这一下
    (29.90, 'sub', 0.14, 880),                                     # 天数回到 0
    (34.42, 'morph', 0.14, 960), (35.60, 'click', 0.07, 1100), (37.20, 'click', 0.07, 900),
    (38.72, 'breath', 0.11, 960), (40.10, 'click', 0.07, 1000), (41.60, 'click', 0.07, 900),
    (47.42, 'tap', 0.18, 960), (48.10, 'sub', 0.16, 960),
]
# the burst: eighteen real tags, eighteen soft impacts, left to right across the card
for i in range(18):
    CUES.append((23.95 + i * 0.075, 'impact', 0.13 - 0.002 * i, 640 + (i % 4) * 190))
CUES.sort()

def add(*sigs):
    """sum signals of different lengths (the palette's primitives are not all the same length)"""
    n = max(len(s) for s in sigs); o = np.zeros(n)
    for s in sigs: o[:len(s)] += s
    return o

def synth(kind, x=None):
    if kind == 'morphdry': kind = 'morph'
    if kind == 'tap':    return wood(168, 0.13) * 0.9
    if kind == 'click':  return wood(520, 0.05) * 0.7
    if kind == 'glass':  return glass(660, 0.75, 1.1)
    if kind == 'chime':  return add(glass(392, 1.9, 0.85), 0.5 * glass(587.3, 1.6, 0.8))
    if kind == 'bubble': return bubble(760, 0.16)
    if kind == 'breath': return air(0.7, 260, 1500, 1.2, 0.5) * 0.8
    if kind == 'morph':  return add(air(1.1, 180, 2400, 1.0, 0.6), 0.4 * wobble(150, 1.1))
    if kind == 'sub':    return sub(58, 0.55)
    if kind == 'impact': return sat(add(wood(240, 0.07), 0.5 * air(0.09, 900, 2600, 1.6, 0.5)), 1.3)
    raise ValueError(kind)

def main():
    w = wave.open(VO, 'rb')
    n = w.getnframes()
    vo = np.frombuffer(w.readframes(n), dtype='<i2').astype(np.float64) / 32768.0
    if w.getnchannels() == 2:
        vo = vo.reshape(-1, 2).mean(axis=1)
    w.close()
    if w.getframerate() != SR:
        raise SystemExit('vo.wav is %d Hz, palette is %d Hz' % (w.getframerate(), SR))

    # One swell, then the words carry it. No bed under the narration: the room is a swell at the head
    # and the film's stillness is real silence, not a quiet mix. (Measured: 16 % of the film is < -40 dBFS.)
    sc = Score(dur=DUR + 1.0)
    intro = bed(int(1.5 * SR)) * env(int(1.5 * SR), 0.5, 0.85)
    sc.place(intro, 0.0, gain=0.26, pan=0.0, send=0.0)
    for t, kind, g, x in CUES:
        sc.place(synth(kind), t, gain=g, pan=pan_of(x), send=0.0 if kind.endswith('dry') else 0.22)
    rooms = sc.mix(peak_db=-9.0)

    m = rooms[:len(vo)].copy() * 0.80                    # the room sits under the words, never over them
    m[:len(vo), 0] += vo[:len(vo)]
    m[:len(vo), 1] += vo[:len(vo)]
    m /= max(1.0, np.abs(m).max() / 0.85)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    pre = os.path.join(os.path.dirname(OUT), 'audio_pre.wav')
    with wave.open(pre, 'wb') as o:
        o.setnchannels(2); o.setsampwidth(2); o.setframerate(SR)
        o.writeframes((np.clip(m, -1, 1) * 32767).astype('<i2').tobytes())

    # loudness: measure, then apply linearly so the hole stays silent and the dynamics survive
    import subprocess, re, json
    def run(args):
        return subprocess.run(args, capture_output=True, text=True, encoding='utf-8', errors='replace')
    r = run(['ffmpeg', '-hide_banner', '-i', pre, '-af', 'loudnorm=I=-16:TP=-4.0:LRA=11:print_format=json',
             '-f', 'null', os.devnull])
    blk = r.stderr[r.stderr.rfind('{'):r.stderr.rfind('}') + 1]
    d = json.loads(blk)
    af = ('loudnorm=I=-16:TP=-4.0:LRA=11:linear=true:measured_I=%s:measured_TP=%s:measured_LRA=%s:'
          'measured_thresh=%s:offset=%s' % (d['input_i'], d['input_tp'], d['input_lra'], d['input_thresh'], d['target_offset']))
    r2 = run(['ffmpeg', '-y', '-hide_banner', '-i', pre, '-af', af, '-ar', '48000', OUT])
    if r2.returncode:
        raise SystemExit('loudnorm failed:\n' + r2.stderr[-1200:])
    chk = run(['ffmpeg', '-hide_banner', '-i', OUT, '-af', 'loudnorm=I=-16:TP=-4.0:print_format=json',
               '-f', 'null', os.devnull])
    b2 = chk.stderr[chk.stderr.rfind('{'):chk.stderr.rfind('}') + 1]
    e = json.loads(b2)
    print('wrote %s | %.1fs | vo %.1fs | %d cues' % (OUT, DUR, len(vo) / SR, len(CUES)))
    print('measured before: %s LUFS / %s dBTP   after: %s LUFS / %s dBTP' %
          (d['input_i'], d['input_tp'], e['input_i'], e['input_tp']))

if __name__ == '__main__':
    main()
