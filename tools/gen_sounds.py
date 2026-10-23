#!/usr/bin/env python3
"""
Синтез звуков «Парового гаража» (раздел «Звуки и голос» SPEC.md).

Все звуки собираются из синусов и шума — без чужих записей, поэтому лицензионных вопросов нет.
Результат: OGG Vorbis, моно, 44,1 кГц, каждый файл до 50 КБ, пики не выше −3 dBFS,
одинаковая громкость (по RMS звучащей части).

Запуск (нужны numpy и soundfile):
    pip install numpy soundfile
    python tools/gen_sounds.py

Файлы пишутся в app/src/main/res/raw/. Генерация детерминирована: одинаковый результат при каждом запуске.
"""

from pathlib import Path

import numpy as np
import soundfile as sf

SR = 44_100
OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"
PEAK_DBFS = -3.5        # запас до требуемых −3 dBFS: Vorbis слегка меняет пики
TARGET_RMS_DBFS = -17.0  # общая громкость звучащей части
MAX_BYTES = 50 * 1024

rng = np.random.default_rng(1871)


# ── Строительные блоки ─────────────────────────────────────────────────────

def t_axis(duration: float) -> np.ndarray:
    return np.arange(int(duration * SR)) / SR


def silence(duration: float) -> np.ndarray:
    return np.zeros(int(duration * SR))


def decay(duration: float, tau: float) -> np.ndarray:
    return np.exp(-t_axis(duration) / tau)


def adsr(n: int, attack: float, release: float) -> np.ndarray:
    env = np.ones(n)
    a = max(1, int(attack * SR))
    r = max(1, int(release * SR))
    env[:a] = np.linspace(0, 1, a)
    env[-r:] *= np.linspace(1, 0, r)
    return env


def band(x: np.ndarray, lo: float, hi: float) -> np.ndarray:
    """Полосовой фильтр через БПФ с плавными краями."""
    spectrum = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(len(x), 1 / SR)
    gain = np.clip((freqs - lo * 0.8) / (lo * 0.2 + 1e-9), 0, 1) * np.clip((hi * 1.25 - freqs) / (hi * 0.25), 0, 1)
    return np.fft.irfft(spectrum * gain, len(x))


def noise(duration: float) -> np.ndarray:
    return rng.standard_normal(int(duration * SR))


def partials(duration: float, base: float, ratios, taus, amps) -> np.ndarray:
    """Металлический удар: сумма затухающих негармоничных обертонов."""
    t = t_axis(duration)
    out = np.zeros_like(t)
    for ratio, tau, amp in zip(ratios, taus, amps):
        out += amp * np.sin(2 * np.pi * base * ratio * t + rng.uniform(0, 2 * np.pi)) * np.exp(-t / tau)
    return out


def place(canvas: np.ndarray, sound: np.ndarray, at: float) -> None:
    start = int(at * SR)
    end = min(len(canvas), start + len(sound))
    canvas[start:end] += sound[: end - start]


def click_tick(freq: float, duration: float = 0.03, tau: float = 0.006) -> np.ndarray:
    hit = partials(duration, freq, [1.0, 1.59, 2.37, 3.11], [tau, tau * 0.8, tau * 0.6, tau * 0.5], [1.0, 0.6, 0.35, 0.2])
    burst = band(noise(duration), 2_000, 9_000) * decay(duration, 0.0015) * 0.6
    return hit + burst


def bell(duration: float, base: float) -> np.ndarray:
    # Обертоны колокола: негармоничный ряд, верхние гаснут быстрее.
    return partials(
        duration,
        base,
        ratios=[1.0, 2.0, 2.76, 4.07, 5.40, 6.80],
        taus=[0.70, 0.45, 0.30, 0.18, 0.12, 0.08],
        amps=[1.0, 0.5, 0.45, 0.25, 0.18, 0.10],
    )


# ── Звуки ──────────────────────────────────────────────────────────────────

def snd_click() -> np.ndarray:
    """Короткий металлический щелчок: кнопки, вентили, тумблеры."""
    x = silence(0.07)
    place(x, click_tick(3_150, 0.06, 0.008), 0.002)
    return x


def snd_drum_tick() -> np.ndarray:
    """Тихий щелчок барабана одометра на каждый шаг цифры."""
    x = silence(0.04)
    place(x, click_tick(1_900, 0.035, 0.005) * 0.8, 0.001)
    return x


def snd_lever() -> np.ndarray:
    """Ход рычага, треск храповика, звонок колокольчика."""
    x = silence(1.25)
    # Ход: шорох трения металла о металл.
    travel = band(noise(0.22), 300, 1_800) * adsr(int(0.22 * SR), 0.03, 0.08) * 0.35
    place(x, travel, 0.0)
    # Храповик: щелчки ускоряются к концу хода.
    times = [0.05, 0.10, 0.145, 0.185, 0.22, 0.25, 0.275]
    for i, at in enumerate(times):
        place(x, click_tick(2_400 + 120 * i, 0.03, 0.005) * 0.75, at)
    # Колокольчик в конце хода.
    place(x, bell(0.9, 1_320) * 0.55, 0.32)
    return x


def snd_steam() -> np.ndarray:
    """Шипение пара при сохранении, не дольше 0,8 с."""
    duration = 0.75
    n = int(duration * SR)
    hiss = band(noise(duration), 2_200, 10_000)
    flutter = 1 + 0.15 * np.sin(2 * np.pi * 17 * t_axis(duration))
    env = adsr(n, 0.04, 0.35) * np.linspace(1.0, 0.7, n)
    return hiss * flutter * env


def snd_clang() -> np.ndarray:
    """Глухой лязг при удалении."""
    x = silence(0.5)
    place(x, partials(0.5, 182, [1.0, 1.6, 2.5, 3.8], [0.22, 0.15, 0.09, 0.06], [1.0, 0.7, 0.4, 0.25]), 0.0)
    thump = np.sin(2 * np.pi * 62 * t_axis(0.18)) * decay(0.18, 0.05) * 0.9
    place(x, thump, 0.0)
    place(x, band(noise(0.05), 200, 2_500) * decay(0.05, 0.01) * 0.5, 0.0)
    return x


def snd_sync_start() -> np.ndarray:
    """Начало синхронизации: раскрутка шестерён."""
    duration = 0.85
    x = silence(duration)
    at, rate = 0.0, 7.0
    while at < duration - 0.04:
        place(x, click_tick(2_100, 0.025, 0.004) * 0.55, at)
        at += 1.0 / rate
        rate = min(rate * 1.18, 45.0)
    t = t_axis(duration)
    freq = 70 + 160 * (t / duration) ** 1.5
    phase = 2 * np.pi * np.cumsum(freq) / SR
    whirr = (np.sin(phase) + 0.5 * np.sin(2 * phase) + 0.25 * np.sin(3 * phase)) * adsr(len(t), 0.2, 0.15) * 0.18
    return x + whirr


def snd_sync_ok() -> np.ndarray:
    """Синхронизация прошла: двойной звонок."""
    x = silence(1.15)
    place(x, bell(0.95, 1_568) * 0.6, 0.0)
    place(x, bell(0.95, 2_093) * 0.6, 0.18)
    return x


def snd_sync_error() -> np.ndarray:
    """Ошибка синхронизации: низкий гудок."""
    duration = 0.55
    t = t_axis(duration)
    tone = sum(np.sin(2 * np.pi * 110 * k * t) / k for k in (1, 3, 5, 7))
    tremolo = 1 - 0.3 * (0.5 + 0.5 * np.sin(2 * np.pi * 11 * t))
    return tone * tremolo * adsr(len(t), 0.02, 0.18)


def snd_whistle() -> np.ndarray:
    """Паровой свисток — звук канала уведомлений «Пора менять»."""
    duration = 1.3
    t = t_axis(duration)
    n = len(t)
    glide = 1 - 0.06 * np.exp(-t / 0.06)          # подъём высоты в начале, как у настоящего свистка
    vibrato = 1 + 0.004 * np.sin(2 * np.pi * 5.5 * t)
    out = np.zeros(n)
    for freq, amp in ((440.0, 1.0), (554.4, 0.8), (659.3, 0.7)):  # ля-мажорное трезвучие
        phase = 2 * np.pi * np.cumsum(freq * glide * vibrato) / SR
        out += amp * (np.sin(phase) + 0.18 * np.sin(2 * phase) + 0.06 * np.sin(3 * phase))
    breath = band(noise(duration), 400, 3_500) * 0.25
    return (out * 0.4 + breath) * adsr(n, 0.06, 0.3)


SOUNDS = {
    "sfx_click": snd_click,
    "sfx_drum_tick": snd_drum_tick,
    "sfx_lever": snd_lever,
    "sfx_steam": snd_steam,
    "sfx_clang": snd_clang,
    "sfx_sync_start": snd_sync_start,
    "sfx_sync_ok": snd_sync_ok,
    "sfx_sync_error": snd_sync_error,
    "notify_whistle": snd_whistle,
}


# ── Выравнивание громкости и запись ────────────────────────────────────────

def db(x: float) -> float:
    return 20 * np.log10(max(x, 1e-12))


def normalize(x: np.ndarray) -> np.ndarray:
    x = x - np.mean(x)
    peak = np.max(np.abs(x))
    loud = x[np.abs(x) > peak * 0.05]  # звучащая часть без тихих хвостов
    rms = np.sqrt(np.mean(loud ** 2))
    x = x * (10 ** (TARGET_RMS_DBFS / 20) / rms)
    peak = np.max(np.abs(x))
    limit = 10 ** (PEAK_DBFS / 20)
    if peak > limit:
        x = x * (limit / peak)
    # Короткое затухание в самом конце, чтобы не было щелчка обрыва.
    fade = min(len(x), int(0.005 * SR))
    x[-fade:] *= np.linspace(1, 0, fade)
    return x


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, build in SOUNDS.items():
        data = normalize(build()).astype(np.float32)
        path = OUT / f"{name}.ogg"
        sf.write(path, data, SR, format="OGG", subtype="VORBIS", compression_level=0.5)
        decoded, _ = sf.read(path)
        peak = db(np.max(np.abs(decoded)))
        size = path.stat().st_size
        status = "ok" if size <= MAX_BYTES and peak <= -3.0 else "ПРОВЕРИТЬ"
        print(f"{name:16s} {len(data) / SR:4.2f} с  {size / 1024:5.1f} КБ  пик {peak:5.1f} dBFS  {status}")


if __name__ == "__main__":
    main()
