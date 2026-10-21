"""Dawn-chorus clips (Birds and the air > The dawn chorus), original synthesis: a layer of birds, each a series of
short frequency-swept whistles and trills, mixed into three twenty-second beds (sparse, full, white-night).
Usage (from the repository root): python3 -I tools/soundgen/chorus.py
Writes src/main/resources/assets/yearofice/sounds/ambient/dawn_chorus_{sparse,full,white_night}.ogg (needs ffmpeg with libvorbis)."""
import hashlib, os, subprocess, wave
import numpy as np

SR = 44100
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice", "sounds", "ambient")


def rng(*p): return np.random.default_rng(int(hashlib.sha256("|".join(map(str, p)).encode()).hexdigest()[:8], 16))


def whistle(r, f0, f1, dur, trill=0.0):
    n = int(dur * SR); t = np.arange(n) / SR
    f = f0 + (f1 - f0) * (t / dur) + trill * np.sin(2 * np.pi * r.uniform(18, 34) * t)
    ph = 2 * np.pi * np.cumsum(f) / SR
    env = np.sin(np.pi * t / dur) ** 2
    return (np.sin(ph) + 0.25 * np.sin(2 * ph)) * env


def bird(r, length_s, rate_hz, centre_hz, loud):
    out = np.zeros(int(length_s * SR))
    t = r.uniform(0, 2.0)
    while t < length_s - 0.5:
        phrase = r.integers(2, 6)
        base = centre_hz * r.uniform(0.8, 1.25)
        for _ in range(phrase):
            d = r.uniform(0.05, 0.22)
            w = whistle(r, base * r.uniform(0.8, 1.2), base * r.uniform(0.9, 1.5), d, trill=r.choice([0, 0, 150, 400]))
            k = int(t * SR)
            if k + len(w) < len(out): out[k:k + len(w)] += loud * w
            t += d + r.uniform(0.02, 0.1)
        t += r.exponential(1.0 / rate_hz)
    return out


def bed(name, birds, rate, loud):
    r = rng("chorus", name)
    mix = np.zeros(20 * SR)
    for i in range(birds):
        pan_gain = r.uniform(0.5, 1.0)
        mix += pan_gain * bird(r, 20, rate, r.uniform(2200, 5600), loud * r.uniform(0.5, 1.0))
    fade = np.minimum(1, np.minimum(np.arange(len(mix)) / (0.5 * SR), (len(mix) - np.arange(len(mix))) / (0.5 * SR)))
    mix *= fade
    mix = 0.8 * mix / max(1e-6, np.max(np.abs(mix)))
    return mix


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    wav = os.path.join(OUT, name + ".wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "5", os.path.join(OUT, name + ".ogg")], check=True)
    os.remove(wav)


if __name__ == "__main__":
    save("dawn_chorus_sparse", bed("sparse", 2, 0.35, 0.5))
    save("dawn_chorus_full", bed("full", 9, 0.8, 0.5))
    save("dawn_chorus_white_night", bed("white_night", 12, 1.0, 0.45))
    print("wrote 3 clips to", OUT)
