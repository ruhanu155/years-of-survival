# Blueprint deviations

Where a merged fix or a lane's choice differs from, or fills a gap in, a number the blueprint states. The blueprint is not edited. Item numbers are RECONCILE items; "lane choice" means the blueprint is silent. Items above 199 are not stable until the Director confirms them.

| Item / PR | Blueprint says | Code does | Reason |
| --- | --- | --- | --- |
| 333 / #107 | Warm room +25...+35 C (a duty-cycle figure) | Steady-state model with fitted chimney-draw loss G = k kW^2, k = 1.0, toml dial `chimney_draw_w_per_c_per_kw2` in `[pieces]`; 30 m3 / 8 kW cabin about +32 C (HeatTest 25-35) | Unbounded steady state gave +86 C. Design debt: replace with a duty-cycle model. |
| 333, 334 | Hot fire loss in the chimney | Hot 3x with 50 % chimney loss not modelled | Deferred follow-up. |
| 315 / #91 | Scent follow at normal trot | `predatorScentTrotMultiplier` toml dial: 1.6x trot for scent sources more than 40 m away | Needed for bears to cover distance in a 2-day window. |
| 315 / #91 | Flint 1 % | Back at 1 % (lane had doubled it) | Ruling; braided-flat flint doubling and frozen-ground clay ban (Oct-breakup) left as later data. |
| 315 | Sod (silent) | Sod cut from grass_turf / tundra_turf loot; sod_block recipe deleted; dike not buildable Oct-breakup | Lane choice. |
| 324, 325 / #101 | Awake 24 / 48 / 72 h row | Each asleep minute repays 3 awake minutes; 4 h unbroken still clears | Broken nights never reset the clock. |
| 331 / #105 | Same | 420 min slept inside a 10 h window clears awakeMin; mixed sleep windows split by sim SleepSplit | Fire-tending walks erased the night's credit. |
| 304 / #85 | Arrow: one per craft | Arrow x6 per job | Director: one per craft is tedium. |
| 305 | Feathers (arrows) | 14 bird butcher tables drop feathers (grouse 15, ptarmigan 12, duck/loon 20, jays 6) | Arrows unmakeable otherwise. |
| 305 | Snow prints | Erase at 2 cm or more fresh depth over the laid depth, week cap kept | Review. |
| 340 / #113 | Livestock source (silent) | Hens/goats by supply drop day 365, reindeer day 1095; goat_wool from butchering; one tameable wolf pup per player each summer 120-200 m out at a random spot | Lane choices to make the homestead obtainable. |
| 340 | Grain | grain_sheaf = barley + straw + cord | Lane choice; balance unchecked. |
| 292 | Fish count M7 | Fish swim through FishEntity + WaterLife with no per-species JSON for salmon x4, lamprey, late fish | Open question in M7 review (298); not resolved. |
| 316 / #92 | Recipe costs | 22 station recipes (14 pieces, chimney_pipe, tin_stove) with numbers from piece.json labour | Lane's numbers. |
| 278 / #70 | Storms | Windstorm, smoke, heat, cold-snap storm defs not built; storms clamp at New Year | Skipped. |
| 307 | Instant placement | Hold-to-work pieces behind `[pieces] instant_place` | Config flag. |
| 322 / #98 | One generic frost line | Per-zone keys frost_<zone>_<nip|superficial|deep|clear|ease> (25 lang lines) | Bus dedupe swallowed second zone. |
| 159 (earlier) | Broadhead shatter 25 % | 5 % | Crafting owns Practice. |
| 163 (earlier) | Practiced bow-drill hold 3 s | 6 s | Director ruling. |

Start-site check failures (blueprint 12 checks): see docs/qa/start-sites-50.md.
