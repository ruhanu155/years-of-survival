"""soundgen prototype: recipe JSON -> mono 44.1 kHz WAV -> chain (trim, fade, normalise) -> Ogg Vorbis q5.
Usage: python3 -I soundgen.py <recipe.json>... <out_dir>"""
import json, sys, os, subprocess, hashlib, wave
import numpy as np

SR = 44100

def seed(*p): return int(hashlib.sha256("|".join(map(str, p)).encode()).hexdigest()[:8], 16)

def biquad_bp(x, f0, q):
    w = 2 * np.pi * f0 / SR; al = np.sin(w) / (2 * q)
    b = np.array([al, 0, -al]); a = np.array([1 + al, -2 * np.cos(w), 1 - al])
    b /= a[0]; a /= a[0]; y = np.zeros_like(x); x1 = x2 = y1 = y2 = 0.0
    for i, xi in enumerate(x):
        yi = b[0] * xi + b[1] * x1 + b[2] * x2 - a[1] * y1 - a[2] * y2
        x2, x1, y2, y1 = x1, xi, y1, yi; y[i] = yi
    return y

def onepole_lp(x, fc):
    a = np.exp(-2 * np.pi * fc / SR); y = np.zeros_like(x); s = 0.0
    for i, xi in enumerate(x): s = (1 - a) * xi + a * s; y[i] = s
    return y

def pink(n, rng):
    w = rng.standard_normal(n); f = np.fft.rfft(w); k = np.arange(len(f)); k[0] = 1
    return np.fft.irfft(f / np.sqrt(k), n)

def env_ad(n, attack_s, decay_s):
    t = np.arange(n) / SR
    return np.where(t < attack_s, t / max(attack_s, 1e-4), np.exp(-(t - attack_s) / decay_s))

def footstep(r, var):
    rng = np.random.default_rng(seed(r["id"], var)); n = int(r["length_s"] * SR); out = np.zeros(n)
    # 1) heel and 2) toe: two filtered noise bursts 60-110 ms apart
    for i, (t0, g) in enumerate(((0.0, 1.0), (rng.uniform(0.06, 0.11), 0.7))):
        k = int(t0 * SR); m = n - k
        burst = biquad_bp(rng.standard_normal(m), r["body_hz"] * rng.uniform(0.85, 1.15), r["body_q"]) * env_ad(m, 0.004, r["decay_s"])
        out[k:] += g * burst
        # 3) grain: sparse clicks (crunch) whose density rises as the snow gets colder
        clicks = (rng.random(m) < r["grain_density"]).astype(float) * rng.standard_normal(m)
        out[k:] += r["grain_gain"] * g * biquad_bp(clicks, r["grain_hz"], 1.2) * env_ad(m, 0.01, r["decay_s"] * 1.5)
        # 4) squeak: a gliding tone, only in the cold bands
        if r.get("squeak_hz"):
            sl = int(r["squeak_s"] * SR); t = np.arange(sl) / SR
            f = r["squeak_hz"] * (1 + 0.25 * t / r["squeak_s"]) * rng.uniform(0.93, 1.07)
            tone = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * t / r["squeak_s"]) ** 2
            tone += 0.3 * np.sign(tone) * np.abs(tone) ** 3          # rubbing harmonics
            out[k:k + sl] += r["squeak_gain"] * g * tone[: max(0, min(sl, n - k))]
    return out

def wind(r, var):
    rng = np.random.default_rng(seed(r["id"], var)); n = int(r["length_s"] * SR)
    base = pink(n, rng)
    # gusts: a slow random walk mapped to the band-pass centre and the level
    g = np.cumsum(rng.standard_normal(n // 4410 + 2)); g = (g - g.min()) / max(1e-6, np.ptp(g))
    gust = np.interp(np.arange(n), np.linspace(0, n, len(g)), g)
    out = np.zeros(n); blk = 2205
    for s in range(0, n, blk):
        e = min(n, s + blk); f0 = r["low_hz"] + (r["high_hz"] - r["low_hz"]) * gust[s]
        out[s:e] = biquad_bp(base[max(0, s - 400):e], f0, r["q"])[-(e - s):]
    out = out * (r["floor"] + (1 - r["floor"]) * gust) + onepole_lp(base, 120) * r["rumble"]
    # seamless loop: crossfade the last 2 s into the start
    xf = int(2 * SR); fade = np.linspace(0, 1, xf)
    out[:xf] = out[:xf] * fade + out[-xf:] * (1 - fade); out = out[:-xf]
    return out

KINDS = {"footstep": footstep, "wind": wind}

def chain(x, r):
    x = x - x.mean()
    if r.get("highpass_hz"): x = x - onepole_lp(x, r["highpass_hz"])
    if not r.get("loop"):
        f = int(0.005 * SR); x[-f:] *= np.linspace(1, 0, f)
    x = x / np.max(np.abs(x)) * 10 ** (-1 / 20)            # peak -1 dBFS
    return x

def write_wav(path, x):
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype("<i2").tobytes())

def measure(path):
    # clips under 1 s are measured padded with silence to 1 s (EBU R128 gates in 400 ms blocks)
    p = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", path, "-af", "apad=whole_dur=1", "-f", "wav", "-"], capture_output=True).stdout
    p = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", "-", "-af", "ebur128=peak=true", "-f", "null", "-"],
                       input=p, capture_output=True).stderr.decode()
    lufs = float(p.split("I:")[-1].split("LUFS")[0]); peak = float(p.split("Peak:")[-1].split("dBFS")[0])
    return lufs, peak

def main(recipes, out):
    os.makedirs(out, exist_ok=True)
    for rp in recipes:
        r = json.load(open(rp))
        for v in range(r.get("variants", 1)):
            x = chain(KINDS[r["kind"]](r, v), r)
            name = f"{r['id'].split(':')[1].replace('.', '/')}{v + 1 if r.get('variants', 1) > 1 else ''}"
            wav = os.path.join(out, name + ".wav"); os.makedirs(os.path.dirname(wav), exist_ok=True)
            write_wav(wav, x)
            # loudness target: one-shots -18 LUFS short-term class, beds -23 LUFS; never above the -1 dBFS peak
            target = r["lufs"]; lufs, _ = measure(wav)
            gain = min(target - lufs, 0.0)
            x = x * 10 ** (gain / 20); write_wav(wav, x)
            ogg = wav[:-4] + ".ogg"
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ar", str(SR), "-ac", "1", "-c:a", "libvorbis", "-q:a", "5", ogg])
            lufs, peak = measure(wav)
            print(f"{name}: {len(x) / SR:.2f}s lufs={lufs:.1f} (target {target}) peak={peak:.1f} dBFS ogg={os.path.getsize(ogg)} B")

if __name__ == "__main__":
    main(sys.argv[1:-1], sys.argv[-1])
