#!/usr/bin/env python3
"""Writes data/yearofice/weather/interior_alaska.json: the home valley's climate file for M2.

The month normals, Markov tables, state offsets and the rest are copied from the docs
(Weather and storms > Monthly climate at 64°N and One weather model; Blueprint > Calendar, daylight and
weather). The 365-row daylight table is computed here once, so runtime code never computes sun angles
(Minecraft engine > Calendar, daylight and moon):

* Place: the Fairbanks area, latitude 64.84°N, longitude 147.72°W. The climate table's mid-month daylight
  hours (18.6 h in May, 21.6 in June, 3.8 in December) are this latitude's, with the standard −0.833° horizon.
* Clock: Alaska daylight time (UTC−8) all year, so the game never jumps an hour; solar noon falls near 13:50.
* Day 0 is May 1 of a non-leap reference year (2025); day 364 is April 30.
* Each row holds sunrise and sunset in minutes after midnight (−0.833°), and the geometric sun elevation and
  azimuth (degrees clockwise from north) at each whole hour 00:00–23:00, from the NOAA solar position formulas.

The hourly temperature curve κ(h) is the Blueprint's "coldest just before sunrise, warmest at 15:00": −1 at
05:00, +1 at 15:00, half-cosine rise over 10 hours and fall over 14, so its daily mean is exactly 0.

Usage: python3 tools/climate/make_climate.py [out.json]
"""
import json
import math
import sys
from pathlib import Path

LAT = 64.84
LON = -147.72
TZ_H = -8
REF_YEAR_MAY1_DOY = 121  # May 1 in a non-leap year, 1-based day of the calendar year

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/data/yearofice/weather/interior_alaska.json"

# Weather and storms > Monthly climate at 64°N. Keys are calendar months "01".."12".
NORMALS = {
    "05": (9.5, 5, 14.5, 4.5, (-12, 29), 15, 1, 0, 3.3, 1013, 0.3, 18.6),
    "06": (15.5, 5, 20.5, 10.5, (-2, 33), 35, 0, 0, 3.1, 1011, 4, 21.6),
    "07": (17, 5, 22, 12, (1, 34), 55, 0, 0, 2.8, 1010, 5, 20.6),
    "08": (14, 5, 19, 9, (-5, 31), 50, 0, 0, 2.7, 1010, 2, 16.8),
    "09": (7.5, 4, 11.5, 3.5, (-15, 26), 30, 2, 0, 2.8, 1012, 0, 13.0),
    "10": (-3.5, 4, 0.5, -7.5, (-32, 18), 22, 20, 17, 2.6, 1014, 0, 9.6),
    "11": (-15.5, 3, -12.5, -18.5, (-45, 8), 16, 24, 33, 2.2, 1018, 0, 6.1),
    "12": (-21, 3, -18, -24, (-50, 6), 13, 20, 42, 2.0, 1021, 0, 3.8),
    "01": (-23, 3, -20, -26, (-52, 5), 13, 20, 50, 2.0, 1022, 0, 4.7),
    "02": (-20, 3, -17, -23, (-50, 7), 10, 15, 51, 2.3, 1020, 0, 8.0),
    "03": (-12, 6, -6, -18, (-45, 12), 9, 13, 50, 2.8, 1018, 0, 11.6),
    "04": (-1, 6, 5, -7, (-32, 20), 7, 6, 5, 3.2, 1015, 0, 15.1),
}


def normals():
    out = {}
    for m in sorted(NORMALS):
        mean, swing, hi, lo, bounds, pmm, fresh, depth, wind, hpa, thunder, dl = NORMALS[m]
        out[m] = {
            "mean_c": mean, "swing_c": swing, "high_c": hi, "low_c": lo, "bounds_c": list(bounds),
            "precip_mm": pmm, "fresh_snow_cm": fresh, "snow_depth_cm": depth, "wind_ms": wind,
            "pressure_hpa": hpa, "thunder_days": thunder, "daylight_h": dl,
        }
    return out


def kappa(h):
    """The hourly curve at whole hour h: −1 at 05:00, +1 at 15:00."""
    if 5 <= h <= 15:
        return -math.cos(math.pi * (h - 5) / 10)
    t = (h - 15) % 24
    return math.cos(math.pi * t / 14)


def solar(doy, minute):
    """NOAA: (elevation°, azimuth° from north) at local clock minute of calendar day doy."""
    gamma = 2 * math.pi / 365 * (doy - 1 + (minute / 60 - 12) / 24)
    eqtime = 229.18 * (0.000075 + 0.001868 * math.cos(gamma) - 0.032077 * math.sin(gamma)
                       - 0.014615 * math.cos(2 * gamma) - 0.040849 * math.sin(2 * gamma))
    decl = (0.006918 - 0.399912 * math.cos(gamma) + 0.070257 * math.sin(gamma)
            - 0.006758 * math.cos(2 * gamma) + 0.000907 * math.sin(2 * gamma)
            - 0.002697 * math.cos(3 * gamma) + 0.00148 * math.sin(3 * gamma))
    offset = eqtime + 4 * LON - 60 * TZ_H
    tst = minute + offset
    ha = math.radians(tst / 4 - 180)
    phi = math.radians(LAT)
    cz = math.sin(phi) * math.sin(decl) + math.cos(phi) * math.cos(decl) * math.cos(ha)
    cz = max(-1.0, min(1.0, cz))
    elev = 90 - math.degrees(math.acos(cz))
    az = math.degrees(math.atan2(math.sin(ha), math.cos(ha) * math.sin(phi) - math.tan(decl) * math.cos(phi))) + 180
    return elev, az % 360


def rise_set(doy):
    """Sunrise and the following sunset in local clock minutes (−0.833° horizon), by minute search; −1 if none.

    Sunset can fall after midnight in June and July, so it may exceed 1,440 (the next day's clock minute + 1,440).
    """
    prev = solar(doy, 0)[0]
    rise = sset = -1
    for m in range(1, 1441):
        e = solar(doy, m)[0]
        if prev < -0.833 <= e:
            rise = m
            break
        prev = e
    if rise < 0:
        return -1, -1
    prev = solar(doy, rise)[0]
    for m in range(rise + 1, rise + 1441):
        e = solar(doy, m)[0] if m < 1440 else solar(doy % 365 + 1, m - 1440)[0]
        if prev >= -0.833 > e:
            sset = m
            break
        prev = e
    return rise, sset


def daylight_rows():
    rows = []
    for d in range(365):
        doy = (REF_YEAR_MAY1_DOY - 1 + d) % 365 + 1
        rise, sset = rise_set(doy)
        el, az = [], []
        for h in range(24):
            e, a = solar(doy, h * 60)
            el.append(round(e, 2))
            az.append(round(a, 1))
        rows.append({"sunrise_min": rise, "sunset_min": sset, "elevation_deg": el, "azimuth_deg": az})
    return rows


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else OUT
    data = {
        "schema": 1,
        "note": "Home valley climate (Weather and storms > Monthly climate at 64°N). Written by tools/climate/make_climate.py; change the script, not this file.",
        "place": {"latitude_deg": LAT, "longitude_deg": LON, "clock_utc_offset_h": TZ_H, "calendar_zero": "05-01"},
        "normals": normals(),
        "hourly_curve": [round(kappa(h), 4) for h in range(24)],
        "anomaly": {"keep": 0.7, "noise_c": 4, "winter_severe_kick_c": -6, "start_c": 0},
        "sky_factor": {"clear": 1.4, "cloudy": 0.6, "wet": 0.5, "severe": 0.3},
        "markov": {
            "seasons": {"spring": ["04-01", "05-31"], "summer": ["06-01", "08-15"], "fall": ["08-16", "10-31"], "winter": ["11-01", "03-31"]},
            "order": ["clear", "cloudy", "wet", "severe"],
            "spring": [[0.50, 0.30, 0.17, 0.03], [0.30, 0.40, 0.25, 0.05], [0.25, 0.35, 0.35, 0.05], [0.30, 0.40, 0.25, 0.05]],
            "summer": [[0.50, 0.30, 0.15, 0.05], [0.30, 0.35, 0.28, 0.07], [0.20, 0.35, 0.37, 0.08], [0.30, 0.40, 0.25, 0.05]],
            "fall": [[0.40, 0.35, 0.20, 0.05], [0.20, 0.40, 0.33, 0.07], [0.15, 0.35, 0.42, 0.08], [0.20, 0.40, 0.35, 0.05]],
            "winter": [[0.60, 0.28, 0.10, 0.02], [0.30, 0.45, 0.20, 0.05], [0.20, 0.40, 0.32, 0.08], [0.20, 0.30, 0.15, 0.35]],
            "start_state": "clear",
        },
        "state_offset_c": {"summer": [2, -1, -3, -4], "winter": [-3, 2, 3, 0], "winter_below_c": 1},
        "wind_ms": {"clear": [0, 5], "cloudy": [1, 6], "wet": [2, 8], "severe": [10, 20]},
        "gust_factor": {"clear": 1.4, "cloudy": 1.5, "wet": 1.6, "severe": 1.8},
        "wind_curve": {
            "months": ["03", "04", "05", "06", "07", "08", "09", "10"],
            "values": [round(0.95 + 0.35 * kappa(h), 4) for h in range(24)],
        },
        "precip": {
            "severe_factor": 3.5, "snow_cm_per_mm": 1.5, "snow_below_c": 1, "shoulder_band_c": 3,
            "sleet_from_c": 0.5, "rain_from_c": 2, "window_h": {"wet": 8, "severe": 12},
            "draw_low": 0.5, "draw_high": 1.5, "heavy_from_mm_h": 4, "drizzle_below_mm_h": 1,
        },
        "cloud_pct": {"clear": 10, "cloudy": 75, "wet": 95, "severe": 100},
        "humidity_pct": {"clear": 55, "cloudy": 70, "wet": 95, "severe": 100},
        "visibility_m": {
            "full": 30000, "drizzle": 2000, "rain": 300, "heavy_rain": 150, "light_snow": 600, "snow": 150,
            "blizzard": 20, "light_snow_below_cm_h": 0.5, "blizzard_from_ms": 12,
        },
        "air": {
            "valley_floor_y": 150, "lapse_c_per_100m": -0.65, "inversion_c_per_100m": 3, "inversion_max_c": 8,
            "inversion_wind_below_ms": 2,
        },
        "wind_terrain": {"ridge_per_300m": 0.4, "ridge_max": 1.5, "canopy_factor": 0.3},
        "water": {"follow_days": 5, "floor_c": 0.5, "creek_offset_c": -2, "lake_summer_offset_c": 2},
        "ground": {
            "depth_m": 0.5,
            "monthly_c": {
                "open_soil": [2, 7, 11, 10, 6, 1, -2, -4, -5, -5, -4, -1],
                "forest_duff": [0, 4, 7, 7, 5, 1, -1, -2, -3, -3, -3, -1],
                "permafrost": [-1, 0, 2, 2, 1, 0, -2, -4, -6, -6, -5, -3],
                "bog": [0, 5, 8, 8, 5, 1, -1, -2, -3, -3, -3, -1],
                "south_slope": [5, 10, 13, 12, 7, 1, -2, -4, -5, -5, -3, 1],
            },
            "snow_insulation": {"above_cm": 40, "per_cm_c": 0.05, "max_c": 2},
        },
        "daylight": {"rows": daylight_rows()},
    }
    out.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    # One daylight row per line keeps diffs readable.
    text = text.replace(',{"sunrise_min"', ',\n{"sunrise_min"').replace('[{"sunrise_min"', '[\n{"sunrise_min"')
    out.write_text(text + "\n", encoding="utf-8")
    print("wrote", out, len(text), "bytes")


if __name__ == "__main__":
    main()
