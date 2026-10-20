"""soundgen music: seeded modal phrase grammar -> plucked string + bowed drone + wooden flute -> Ogg Vorbis.
Usage: python3 -I music.py <out_dir>     (writes music/dawn_spring, music/day_spring, music/fire_spring .ogg)
Art and audio production > Music tied to Spirits. These are the first three of the twelve planned pieces: short
(35-60 s) placeholders at grade G0, deterministic from their seed."""
import hashlib, os, subprocess, sys, wave
import numpy as np

SR = 32000
DORIAN = [0, 2, 3, 5, 7, 9, 10]
AEOLIAN = [0, 2, 3, 5, 7, 8, 10]

def seed(*p): return int(hashlib.sha256("|".join(map(str, p)).encode()).hexdigest()[:8], 16)
def hz(root, deg, mode, octv=0):
    o, i = divmod(deg, 7)
    return 440.0 * 2 ** ((root + mode[i] + 12 * (o + octv) - 9) / 12)

def pluck(f, dur, rng):
    n = int(dur * SR); p = max(2, int(SR / f)); buf = rng.uniform(-1, 1, p); out = np.zeros(n)
    for i in range(n):
        out[i] = buf[i % p]; buf[i % p] = 0.4975 * (buf[i % p] + buf[(i + 1) % p])
    return out * np.minimum(1, np.arange(n) / 40.0) * 0.5

def flute(f, dur, rng):
    n = int(dur * SR); t = np.arange(n) / SR
    vib = 1 + 0.004 * np.sin(2 * np.pi * 5 * t) * np.minimum(1, t / 0.4)
    ph = 2 * np.pi * np.cumsum(f * vib) / SR
    x = np.sin(ph) + 0.18 * np.sin(2 * ph) + 0.05 * np.sin(3 * ph) + 0.03 * rng.standard_normal(n)
    return x * np.minimum(1, t / 0.08) * np.minimum(1, (dur - t) / 0.15).clip(0, 1) * 0.28

def drone(f, dur):
    n = int(dur * SR); t = np.arange(n) / SR
    x = sum(a * np.sin(2 * np.pi * f * k * t + k) for k, a in ((1, 1), (2, .5), (3, .3), (4, .15)))
    return x * np.minimum(1, t / 3) * np.minimum(1, (dur - t) / 3) * 0.10

def phrase(rng, length, steps):
    deg, out = int(rng.integers(2, 5)), []
    for _ in range(length):
        deg = int(np.clip(deg + rng.choice([-2, -1, -1, 0, 1, 1, 2]), 0, 9)); out.append(deg)
    out[-1] = 0 if rng.random() < .6 else 4
    return out

def piece(name, bpm, root, mode, bars, flute_on, pluck_on, drone_on):
    rng = np.random.default_rng(seed("music", name)); beat = 60 / bpm; total = bars * 4 * beat + 4
    mix = np.zeros(int(total * SR) + SR)
    def put(x, t):
        k = int(t * SR); mix[k:k + len(x)] += x[: len(mix) - k]
    if drone_on: put(drone(hz(root, 0, mode, -2), total - 1), 0.5)
    motif = phrase(rng, 8, 8); t = 1.0
    for b in range(bars // 2):
        ph = motif if b % 2 == 0 else phrase(rng, 8, 8)
        for i, d in enumerate(ph):
            dur = beat * (1.5 if i % 4 == 3 else 1.0)
            if pluck_on: put(pluck(hz(root, d, mode), dur * 1.8, rng), t)
            if flute_on and b % 2 == 1: put(flute(hz(root, d, mode, 1), dur * 0.95, rng), t)
            t += beat
    # soft room: two feedback taps
    for dly, g in ((0.23, .30), (0.41, .18)):
        k = int(dly * SR); mix[k:] += g * mix[:-k].copy()
    mix /= max(1e-6, np.abs(mix).max()) / 0.8
    n = int(len(mix)); mix *= np.minimum(1, np.arange(n) / (SR * 2.0)) * np.minimum(1, (n - np.arange(n)) / (SR * 4.0))
    return mix

PIECES = {
    "dawn_spring": dict(bpm=54, root=2, mode=DORIAN, bars=16, flute_on=True, pluck_on=True, drone_on=True),
    "day_spring": dict(bpm=62, root=7, mode=DORIAN, bars=18, flute_on=True, pluck_on=True, drone_on=False),
    "fire_spring": dict(bpm=50, root=9, mode=AEOLIAN, bars=14, flute_on=False, pluck_on=True, drone_on=True),
}

def main(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    for name, kw in PIECES.items():
        x = piece(name, **kw); wav = os.path.join(out_dir, name + ".wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype("<i2").tobytes())
        ogg = wav[:-4] + ".ogg"
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "3", ogg], check=True)
        os.remove(wav); print(name, f"{len(x) / SR:.0f}s", os.path.getsize(ogg), "B")

if __name__ == "__main__": main(sys.argv[1])
