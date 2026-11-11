# -*- coding: utf-8 -*-
"""
build_sfx.py · 把 Kenney / Freesound 素材裁成 docs/15 规定的 42 条音效。

规则：从第一个起振点开始截，长度按文档；两端 3ms / 30ms 淡入淡出；
归一化到 -14 LUFS（峰值 ≤ -3 dBTP）；单声道 44.1k，Vorbis q3（≈96kbps）。
"""
import os, json, wave, subprocess, re, sys, argparse, tempfile
import numpy as np

FF = os.environ.get("FFMPEG") or "ffmpeg"
KEN = os.environ.get("KENNEY_DIR", r"D:\Downloads\sfx\kenny")
FS = os.environ.get("FREESOUND_DIR", r"D:\Downloads\sfx\freesound")
HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TMP = os.path.join(tempfile.gettempdir(), "cathouse_sfx")
OUT = os.path.join(HERE, "android-assets", "audio", "sfx")
SR = 44100

# 索引：Kenney 文件名 -> 路径（main 里按实际目录重建）
KEN_IDX = {}
FS_IDX = {}


def build_index():
    global KEN_IDX, FS_IDX
    KEN_IDX, FS_IDX = {}, {}
    if not os.path.isdir(KEN):
        raise SystemExit(f"找不到 Kenney 目录：{KEN}")
    for pack in os.listdir(KEN):
        d = os.path.join(KEN, pack, "Audio")
        if not os.path.isdir(d):
            continue
        for f in os.listdir(d):
            if f.endswith(".ogg"):
                KEN_IDX[f[:-4]] = os.path.join(d, f)
    if os.path.isdir(FS):
        FS_IDX = {f: os.path.join(FS, f) for f in os.listdir(FS) if f.lower().endswith((".wav", ".mp3", ".ogg", ".flac"))}

FS_IDX = {f: os.path.join(FS, f) for f in os.listdir(FS)}


def ken(name):
    if name not in KEN_IDX:
        raise KeyError(f"Kenney 里没有 {name}")
    return KEN_IDX[name]


def fs(name):
    return FS_IDX[name]


# id: (时长, 源, 选项)   src: ("k", 名) / ("f", 文件名)
SPECS = {
    # UI
    "ui_tap":          (0.05, ("k", "impactWood_light_000"), {}),
    "ui_back":         (0.08, ("k", "impactWood_light_002"), {"rate": 0.88}),
    "ui_panel_open":   (0.15, ("k", "bookOpen"), {}),
    "ui_panel_close":  (0.12, ("k", "bookClose"), {}),
    "ui_toggle":       (0.06, ("k", "toggle_001"), {}),
    "ui_error":        (0.20, ("k", "lowDown"), {}),
    # 合成
    "item_pick":       (0.08, ("f", "856380__adinatrx__bed-sheets-and-blanket-rustling-body-turning-over-fabric-friction.wav"), {"dense": True}),
    "item_drop":       (0.10, ("k", "impactWood_medium_001"), {}),
    "merge_low":       (0.20, ("k", "pepSound2"), {}),
    "merge_mid":       (0.22, ("k", "pepSound5"), {}),
    "merge_high":      (0.25, ("k", "powerUp4"), {}),
    "merge_invalid":   (0.18, ("k", "impactSoft_heavy_002"), {}),
    # 生成器
    "gen_tap_wood":    (0.12, ("k", "impactWood_heavy_000"), {}),
    "gen_tap_metal":   (0.12, ("k", "impactMetal_light_000"), {}),
    "gen_tap_fabric":  (0.12, ("k", "cloth1"), {}),
    "gen_tap_glass":   (0.12, ("k", "impactGlass_light_000"), {}),
    "gen_tap_ceramic": (0.12, ("k", "impactPlate_light_000"), {}),
    "gen_tap_plant":   (0.12, ("f", "257483__sycopation__flax2.wav"), {"dense": True}),
    "gen_tap_relic":   (0.12, ("f", "588509__tampajoey__wooddoorsqueak.wav"), {}),
    "gen_upgrade":     (0.45, ("k", "powerUp11"), {}),
    "gen_blocked":     (0.15, ("k", "impactSoft_heavy_004"), {}),
    # 订单
    "order_ready":     (0.50, ("f", "398496__anthousai__wind-chimes-single-04.wav"), {"chime": True}),
    "order_complete":  (0.80, ("k", "handleCoins"), {"coins": True}),
    "coin_fly":        (0.30, ("k", "handleCoins2"), {}),
    # 建造
    "build_step":      (0.40, ("k", "impactWood_heavy_002"), {"double": True}),
    "build_reveal":    (1.20, ("k", "powerUp12"), {}),
    # 仓库与气泡
    "store_item":      (0.15, ("k", "dropLeather"), {}),
    "retrieve_item":   (0.12, ("k", "handleSmallLeather"), {}),
    "warehouse_full":  (0.25, ("k", "impactSoft_heavy_003"), {}),
    "bubble_appear":   (0.40, ("k", "phaserUp2"), {}),
    "bubble_place":    (0.15, ("k", "impactGeneric_light_001"), {}),
    # 猫（后两只是变调，见 README）
    "cat_orange":      (0.50, ("f", "66518__freemaster2__meow9.wav"), {}),
    "cat_calico":      (0.40, ("f", "110011__tuberatanka__cat-meow.wav"), {}),
    "cat_ragdoll":     (0.40, ("f", "763906__countrygirls13__cat_meow.wav"), {}),
    "cat_black":       (0.40, ("f", "61259__timtube__cat-meowing.wav"), {}),
    "cat_tabby":       (0.50, ("f", "110011__tuberatanka__cat-meow.wav"), {"rate": 1.16, "speed": 1.12}),
    "cat_huzi":        (0.50, ("f", "66518__freemaster2__meow9.wav"), {"rate": 0.90, "speed": 0.92, "lowpass": 3200, "gain": -2}),
    # 对话与体力
    "text_blip":       (0.03, ("k", "tick_001"), {"gain": -6}),
    "dialog_open":     (0.20, ("k", "open_001"), {}),
    "energy_gain":     (0.30, ("k", "powerUp1"), {}),
    "energy_empty":    (0.30, ("k", "phaserDown1"), {}),
}


# 归一化之后再压的音量（text_blip 要极轻，文档要求 -20dB）
POST_GAIN_DB = {"text_blip": -10.0}


def decode(src, rate=1.0, speed=1.0, lowpass=None):
    """解码成单声道 44.1k；rate 变速变调，speed 只变速（atempo）"""
    key = os.path.basename(src)[:40] + f"_{rate}_{speed}_{lowpass}"
    wav = os.path.join(TMP, key.replace("/", "_") + ".wav")
    if not os.path.exists(wav):
        af = []
        if rate != 1.0:
            af.append(f"asetrate={int(SR * rate)}")
        af.append(f"aresample={SR}")
        if speed != 1.0:
            af.append(f"atempo={speed}")
        if lowpass:
            af.append(f"lowpass=f={lowpass}")
        cmd = [FF, "-y", "-hide_banner", "-loglevel", "error", "-i", src,
               "-af", ",".join(af) if af else "anull", "-ar", str(SR), "-ac", "1",
               "-c:a", "pcm_s16le", wav]
        subprocess.run(cmd, check=True)
    w = wave.open(wav)
    return np.frombuffer(w.readframes(w.getnframes()), dtype="<i2").astype(np.float32) / 32768.0


def onset(x, thresh=0.15):
    w = int(0.005 * SR)
    r = np.array([np.sqrt(np.mean(x[i:i + w] ** 2)) for i in range(0, len(x) - w, w)])
    if not len(r):
        return 0
    i = int(np.argmax(r > r.max() * thresh))
    return max(0, i * w - int(0.003 * SR))


def densest(x, dur):
    """找能量最密的 dur 秒窗口（用于长素材取一段摩擦声）"""
    n = int(dur * SR)
    if len(x) <= n:
        return 0, x
    w = int(0.02 * SR)
    e = np.array([np.mean(x[i:i + w] ** 2) for i in range(0, len(x) - w, w)])
    i = int(np.argmax(np.convolve(e, np.ones(max(1, n // w)) / max(1, n // w), mode="same")))
    s = max(0, min(len(x) - n, i * w - n // 2))
    return s, x[s:s + n]


def cut(x, dur, dense=False, thresh=0.15):
    n = int(dur * SR)
    if dense:
        s, seg = densest(x, dur)
        return seg[:n]
    s = onset(x, thresh)
    seg = x[s:s + n]
    if len(seg) < n:
        seg = np.concatenate([seg, np.zeros(n - len(seg), dtype=np.float32)])
    return seg


def envelope(y, fi=0.003, fo=0.030):
    n = len(y)
    a, r = int(fi * SR), min(int(fo * SR), n // 2)
    if a:
        y[:a] *= np.linspace(0, 1, a) ** 0.5
    if r:
        y[-r:] *= np.linspace(1, 0, r) ** 0.5
    return y


def measure(path):
    r = subprocess.run([FF, "-nostats", "-hide_banner", "-i", path,
                        "-filter_complex", "ebur128=peak=true", "-f", "null", "-"],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    i = re.findall(r"I:\s+(-?\d+\.?\d*)\s+LUFS", r.stderr)
    tp = re.findall(r"Peak:\s+(-?\d+\.?\d*)\s+dBFS", r.stderr)
    return (float(i[-1]) if i else -23.0), (float(tp[-1]) if tp else -1.0)


def lufs(y):
    p = os.path.join(TMP, "_m.wav")
    save(p, y)
    return measure(p)


def loudness_of(y):
    """短音效单独量 LUFS 会被门限吃掉（几十毫秒判成静音），先补 0.8s 静音再量；
    仍然量不出来就退回 RMS 估算（-20 dBFS ≈ -14 LUFS）"""
    pad = np.concatenate([y, np.zeros(int(0.8 * SR), dtype=np.float32)])
    p = os.path.join(TMP, "_p.wav")
    save(p, pad)
    i, _ = measure(p)
    if i <= -55:
        rms = 20 * np.log10(np.sqrt(np.mean(y.astype(np.float64) ** 2)) + 1e-12)
        return rms + 6.0
    return i


def save(p, y):
    y = np.clip(y, -1, 1)
    with wave.open(p, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((y * 32767).astype("<i2").tobytes())


def chime_run(x, dur):
    """单次钟声拼成三连音（风铃）"""
    base = cut(x, 0.34)
    out = np.zeros(int(dur * SR), dtype=np.float32)
    for i, (at, r) in enumerate([(0.00, 1.00), (0.085, 1.12), (0.170, 1.26)]):
        seg = base if r == 1.0 else base[::1] * 1.0
        n = int(len(base) / r)
        idx = np.arange(n) * r
        seg = np.interp(idx, np.arange(len(base)), base).astype(np.float32)
        s = int(at * SR)
        e = min(len(out), s + len(seg))
        out[s:e] += seg[:e - s] * (1.0 - 0.15 * i)
    return envelope(out, 0.002, 0.03)[:int(dur * SR)]


def main(only=None):
    build_index()
    os.makedirs(TMP, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for sid, (dur, (kind, name), opt) in SPECS.items():
        if only and sid not in only:
            continue
        src = ken(name) if kind == "k" else fs(name)
        x = decode(src, opt.get("rate", 1.0), opt.get("speed", 1.0), opt.get("lowpass"))
        if opt.get("chime"):
            y = chime_run(x, dur)
        elif opt.get("double"):
            y = np.zeros(int(dur * SR), dtype=np.float32)
            one = cut(x, min(0.16, dur / 2))
            for s in (0.0, 0.17):
                i = int(s * SR)
                e = min(len(y), i + len(one))
                y[i:e] += one[:e - i]
            y = envelope(y, 0.002, 0.04)
        elif opt.get("coins"):
            y = np.zeros(int(dur * SR), dtype=np.float32)
            one = cut(x, 0.30)
            catfile = fs("763906__countrygirls13__cat_meow.wav")
            cat = cut(decode(catfile), 0.42)
            for s, seg in ((0.0, one), (0.07, one * 0.8), (0.20, cat)):
                i = int(s * SR)
                e = min(len(y), i + len(seg))
                y[i:e] += seg[:e - i]
            y = envelope(y, 0.002, 0.05)
        else:
            y = cut(x, dur, dense=opt.get("dense", False))
            y = envelope(y)
        if opt.get("gain"):
            y = y * 10 ** (opt["gain"] / 20.0)
        i0 = loudness_of(y)
        y = y * 10 ** ((-14.0 - i0) / 20.0)
        pk = 20 * np.log10(np.max(np.abs(y)) + 1e-9)
        if pk > -3.0:
            y *= 10 ** ((-3.0 - pk) / 20.0)
        if sid in POST_GAIN_DB:
            y = y * 10 ** (POST_GAIN_DB[sid] / 20.0)
        fin = os.path.join(TMP, sid + ".wav")
        save(fin, y)
        lufs_out = loudness_of(y)
        tp = measure(fin)[1]
        ogg = os.path.join(OUT, f"sfx_{sid}.ogg")
        subprocess.run([FF, "-y", "-hide_banner", "-loglevel", "error", "-i", fin,
                        "-ar", str(SR), "-ac", "1", "-c:a", "libvorbis", "-q:a", "3", ogg], check=True)
        rows.append(dict(id=sid, file=f"sfx/sfx_{sid}.ogg", source=os.path.basename(src),
                         seconds=round(len(y) / SR, 3), lufs=round(lufs_out, 1),
                         truePeak=round(tp, 1), bytes=os.path.getsize(ogg)))
        print(f"{sid:16s} {len(y)/SR:5.2f}s  {lufs_out:6.1f} LUFS  TP {tp:5.1f}  "
              f"{os.path.getsize(ogg)/1024:6.1f} KB  <- {os.path.basename(src)[:40]}")
    json.dump(rows, open(os.path.join(TMP, "sfx_report.json"), "w", encoding="utf-8"),
              ensure_ascii=False, indent=1)
    print("count:", len(rows), " total KB:", round(sum(r["bytes"] for r in rows) / 1024))


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description="从 Kenney / Freesound 素材裁出 42 条 SFX")
    ap.add_argument("--kenney", default=KEN, help="解压后的 Kenney 音频目录（里面要能找到 */Audio/*.ogg）")
    ap.add_argument("--freesound", default=FS, help="Freesound 下载的原始文件目录")
    ap.add_argument("--out", default=OUT, help="输出目录")
    ap.add_argument("only", nargs="*", help="只处理某几条 id")
    a = ap.parse_args()
    KEN, FS, OUT = a.kenney, a.freesound, a.out
    os.makedirs(OUT, exist_ok=True)
    main(set(a.only) or None)

