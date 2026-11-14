# -*- coding: utf-8 -*-
"""
convert_bgm.py · 把 Gemini 生成的 mp3 处理成游戏用的循环 BGM / sting。

做四件事：量 BPM -> 按 4 小节整数倍裁 -> 循环头尾 2s 等功率交叉淡化 ->
归一到 -18 LUFS（sting -16）、峰值 ≤ -3 dBTP -> 导 44.1k 立体声 OGG。

用法：
    set FFMPEG=C:\\path\\ffmpeg.exe
    python tools/convert_bgm.py --src D:\\Downloads\\bgm
    python tools/convert_bgm.py --src D:\\Downloads\\bgm --out build\\audio

依赖：ffmpeg（要带 libvorbis）。没有 ffmpeg 的话 `pip install imageio-ffmpeg`，
脚本会自动找到它。
"""
import os, sys, json, wave, shutil, argparse, subprocess, tempfile, re
import numpy as np

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_OUT = os.path.join(ROOT, "android-assets", "audio", "bgm")

# 目标：循环曲目给「想要多长」，sting 给「要几秒」
PLAN = {
    "bgm_day":       dict(mode="loop",  target=80.0, use="主界面 · D1–D18 / D22–D24"),
    "bgm_night":     dict(mode="loop",  target=80.0, use="阁楼 · D19–D21"),
    "bgm_title":     dict(mode="loop",  target=38.0, use="启动画面"),
    "sting_chapter": dict(mode="sting", seconds=3.0, fade_out=0.30, use="第 6/12/18 天章末"),
    "sting_ending":  dict(mode="sting", seconds=12.0, fade_out=1.20, from_end=1.0, use="结局"),
    "amb_rain":      dict(mode="amb", seconds=20.0, use="第 1 天门廊 · 雨"),
    "amb_birds":     dict(mode="amb", seconds=15.0, use="院子 D16–D18 · 鸟"),
}


def find_ffmpeg():
    p = os.environ.get("FFMPEG")
    if p and os.path.exists(p):
        return p
    for name in ("ffmpeg.exe", "ffmpeg"):
        if shutil.which(name):
            return shutil.which(name)
    try:
        import imageio_ffmpeg
        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:
        raise SystemExit("找不到 ffmpeg：设置环境变量 FFMPEG，或 pip install imageio-ffmpeg")


FF = find_ffmpeg()


# ---------------------------------------------------------------- 基础

def decode(src, dst):
    subprocess.run([FF, "-y", "-hide_banner", "-loglevel", "error", "-i", src,
                    "-ar", str(SR), "-ac", "2", "-c:a", "pcm_s16le", dst], check=True)
    w = wave.open(dst)
    n = w.getnframes()
    return np.frombuffer(w.readframes(n), dtype="<i2").astype(np.float32).reshape(-1, 2) / 32768.0


def save(path, x):
    x = np.clip(x, -1, 1)
    with wave.open(path, "wb") as w:
        w.setnchannels(x.shape[1])
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767.0).astype("<i2").tobytes())


def measure(path):
    """ebur128：integrated loudness + true peak"""
    p = subprocess.run([FF, "-nostats", "-hide_banner", "-i", path,
                        "-filter_complex", "ebur128=peak=true", "-f", "null", "-"],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    i = re.findall(r"I:\s+(-?\d+\.?\d*)\s+LUFS", p.stderr)
    tp = re.findall(r"Peak:\s+(-?\d+\.?\d*)\s+dBFS", p.stderr)
    return (float(i[-1]) if i else -23.0), (float(tp[-1]) if tp else -1.0)


def detect_bpm(mono, lo=50.0, hi=190.0):
    win, hop = 1024, 256
    n = 1 + (len(mono) - win) // hop
    idx = np.arange(win)[None, :] + hop * np.arange(n)[:, None]
    S = np.log1p(np.abs(np.fft.rfft(mono[idx] * np.hanning(win)[None, :], axis=1)) * 10)
    flux = np.maximum(0, np.diff(S, axis=0)).sum(1)
    flux = np.concatenate([[0], flux])
    flux = np.maximum(0, flux - np.convolve(flux, np.ones(64) / 64, mode="same"))
    flux /= flux.max() + 1e-9
    dt = hop / SR
    e = flux - flux.mean()
    ac = np.correlate(e, e, mode="full")[len(e) - 1:]
    lags = np.arange(len(ac)) * dt
    bpm_axis = 60.0 / np.maximum(lags, 1e-9)
    sel = (bpm_axis >= lo) & (bpm_axis <= hi) & (lags > 0.25)
    return float(bpm_axis[sel][np.argmax(ac[sel])])


def fold_loop(x, xf=2.0):
    n = int(xf * SR)
    body, head = x[n:], x[:n]
    L = len(body)
    w = np.linspace(0, np.pi / 2, n)[:, None]
    out = body.copy()
    out[L - n:] = body[L - n:] * np.cos(w) + head * np.sin(w)
    return out


def fade(x, fi=0.005, fo=0.0):
    y = x.copy()
    a, r = int(fi * SR), int(fo * SR)
    if a:
        y[:a] *= np.linspace(0, 1, a)[:, None] ** 0.5
    if r:
        y[-r:] *= np.linspace(1, 0, r)[:, None] ** 0.5
    return y


def loudest_start(x, seconds, pre=0.30):
    w = int(0.02 * SR)
    e = np.array([np.mean(x[i * w:(i + 1) * w] ** 2) for i in range(len(x) // w)])
    return max(0.0, int(np.argmax(e)) * 0.02 - pre)


def flattest_start(x, seconds, win=0.5):
    """环境声：选短时 RMS 波动最小的一段，循环时不会忽大忽小"""
    m = x.mean(1)
    w = int(win * SR)
    r = np.array([np.sqrt(np.mean(m[i * w:(i + 1) * w] ** 2)) for i in range(len(m) // w)])
    db = 20 * np.log10(r + 1e-12)
    need = int((seconds + 2.0) / win)
    best, bestv = 0, 1e9
    for i in range(0, max(1, len(db) - need)):
        v = float(np.std(db[i:i + need]))
        if v < bestv:
            bestv, best = v, i
    return best * win


# ---------------------------------------------------------------- 主流程

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", required=True, help="mp3 所在文件夹")
    ap.add_argument("--out", default=DEFAULT_OUT)
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)

    manifest = {"version": 1, "processing": "bars -> 2s crossfade -> LUFS -> ogg q6",
                "targets": {"bgmLufs": -18, "stingLufs": -16, "truePeakMax": -3}, "files": []}
    tmp = tempfile.mkdtemp()

    for name, cfg in PLAN.items():
        src = os.path.join(args.src, name + ".mp3")
        if not os.path.exists(src):
            print(f"跳过 {name}：没有 {src}")
            continue
        x = decode(src, os.path.join(tmp, name + ".wav"))
        dur = len(x) / SR
        bpm = detect_bpm(x.mean(1))
        bar = 4 * 60.0 / bpm
        bars_out = None

        if cfg["mode"] == "loop":
            phrases = max(1, round(cfg["target"] / (4 * bar)))
            bars = phrases * 4
            need = bars * bar
            if need + 2.0 > dur - 2.0:
                bars = max(4, int((dur - 4.0) / bar) // 4 * 4)
                need = bars * bar
                print(f"  ! {name} 源只有 {dur:.1f}s，循环长度退到 {bars} 小节")
            y = fold_loop(x[:int(round((need + 2.0) * SR))], 2.0)
            bars_out = bars
        elif cfg["mode"] == "amb":
            start = flattest_start(x, cfg["seconds"])
            y = fold_loop(x[int(start * SR):int((start + cfg["seconds"] + 2.0) * SR)], 2.0)
        else:
            if "from_end" in cfg:
                start = max(0.0, dur - cfg["seconds"] - cfg["from_end"])
            else:
                start = loudest_start(x, cfg["seconds"])
            y = fade(x[int(start * SR):int((start + cfg["seconds"]) * SR)], 0.005, cfg["fade_out"])

        target = {"loop": -18.0, "sting": -16.0, "amb": -24.0}[cfg["mode"]]
        quality = "3" if cfg["mode"] == "amb" else "6"
        outdir = os.path.join(os.path.dirname(args.out), "amb") if cfg["mode"] == "amb" else args.out
        os.makedirs(outdir, exist_ok=True)
        pre = os.path.join(tmp, name + "_pre.wav")
        save(pre, y)
        i0, _ = measure(pre)
        y = y * 10 ** ((target - i0) / 20.0)
        pk = 20 * np.log10(np.max(np.abs(y)) + 1e-9)
        if pk > -3.0:
            y *= 10 ** ((-3.0 - pk) / 20.0)
        fin = os.path.join(tmp, name + ".wav")
        save(fin, y)
        lufs, tp = measure(fin)

        ogg = os.path.join(outdir, name + ".ogg")
        meta = ["-metadata", "title=" + name, "-metadata", "LOOPSTART=0"]
        if cfg["mode"] in ("loop", "amb"):
            meta += ["-metadata", f"LOOPEND={len(y)}"]
        subprocess.run([FF, "-y", "-hide_banner", "-loglevel", "error", "-i", fin,
                        "-ar", str(SR), "-ac", "2", "-c:a", "libvorbis", "-q:a", quality] + meta + [ogg],
                       check=True)

        sub = "amb/" if cfg["mode"] == "amb" else ""
        manifest["files"].append(dict(id=name, file=sub + name + ".ogg", use=cfg["use"],
                                      seconds=round(len(y) / SR, 2), samples=len(y),
                                      bars=bars_out, bpm=round(bpm, 2),
                                      lufs=round(lufs, 1), truePeak=round(tp, 1),
                                      loop=cfg["mode"] in ("loop", "amb"),
                                      loopStart=0,
                                      loopEnd=len(y) if cfg["mode"] in ("loop", "amb") else None,
                                      bytes=os.path.getsize(ogg)))
        print(f"{name:14s} {len(y)/SR:6.2f}s  bpm≈{bpm:5.1f}  {i0:6.1f} -> {lufs:6.1f} LUFS  "
              f"TP {tp:5.1f}  {os.path.getsize(ogg)/1024:7.1f} KB")

    json.dump(manifest, open(os.path.join(args.out, os.pardir, "manifest.json"), "w",
                             encoding="utf-8"), ensure_ascii=False, indent=1)
    print("done ->", os.path.abspath(args.out))


if __name__ == "__main__":
    main()
