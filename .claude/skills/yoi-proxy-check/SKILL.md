---
name: "yoi-proxy-check"
description: "Run a Year of Ice milestone proxy check: a scripted Xvfb client run standing in for Ruhan's human gate, with screenshots the Director reads and a pass/fail ruling. Use to close any milestone M3–M9 or to re-run one after a fix."
---

# Year of Ice proxy check

Ruhan plays once at the end, so every milestone's human gate is replaced by a scripted run that a Sonnet lane builds and the Director judges. One lane, briefed with yoi-lane-brief, with the scope below. One client run costs ~30–60 min of box time and a lane's worth of usage: run each version ONCE, rule, fix in a builder lane, re-run once. Never loop a proxy to green.

## What the lane builds
1. A smoke mode `YEAROFICE_SMOKE=<name>` in `com.yearofice.client.smoke` next to the existing ones (world, body, m5month, m5look, m6bear, m6hunt, m7week, m8cabin, m9hour). The driver plays survival with NO debug commands after setup, doing what a good player would — and like a player: light a LAID ring (tinder + sticks) with a match — that opens bushcraft; build/use a forge for iron; never sneak at a block you use; wear the kit through each item's use action; birch, loaded full, for the night fire; eat fat (pemmican) with every meal; leave an uncleared cleaning spot and move 60–120 m off; read `/yoi craft why` on any refusal. Every grant is logged `GRANT <id>: <why>` and is itself a finding (registered ≠ obtainable).
2. Assertions from the milestone's gate line in docs/ready-for-development.md. Print `RESULT PASS|FAIL` and a per-day ledger (kcal in/out by item with macros, BMI, warmth min, hands/head frost, morning energy cap + its named cause, chores vs free minutes) to `run/smoke/<name>.log`. Count Messenger lines through an append-only tap on HudBusImpl.post (the `sent` list is capped at 32); lines raised asleep flush at wake.
3. Screenshots at the gate moments, JPG under `docs/proxy/m<N>/`, HUD forced visible (HudDebug FULL), tutorial toast and chat/sign prompts cleared, chunks meshed, camera AIMED at the subject — never the sky.
4. Run: `JAVA_TOOL_OPTIONS="-Xmx2200m" DISPLAY=:8N YEAROFICE_SMOKE=<name> setsid timeout 1800 ./gradlew --offline runClient` under its own Xvfb. Box rules: ≥3500 MB free and no other client while builder lanes test; with no builder testing, two clients at ≥2400 MB free. Exit 137 = OOM, retry the same run; never pkill broadly (one lane's stray pkill killed another's client). Poll from a background script, not a foreground sleep loop.
5. On FAIL, diagnose against the blueprint tables and report the numbers; never tune. The Director rules; a builder lane fixes the dial or bug.

## What the Director does with the report
- Read the screenshots with the Read tool (one or two is enough when usage is tight). Look for: sky/prompt shots, magenta textures, placeholder HUD lines, an overloaded start kit, a missing strip.
- Rule in RECONCILE: `M<N> PROXY CHECK PASSED/FAILED`, the cause, the ruling (intended tension vs bug vs driver), the lane that fixes it. Driver faults do not fail a milestone: fix the driver and re-run once.
- A bot that cannot do the thing is a design signal first and a driver bug second: check whether a real player could, in the game as built, before blaming the script.
- A gate stated in blueprint terms is read literally ("an UNCLEARED cleaning spot" means the player is not standing on it).
- Catches so far: the body wore no clothes (M4); pilot bread had no weight_g (M5); the body never read worn items (M7); frost lines deduped per generic key (M7); the awake clock never reset on a broken night, then a fire-tending wake erased the night (M7); a lit stove posted 0 W to a stale room cache, then the body ignored rooms, then the room read +86 °C (M8); every crafting gate shut because the ring was never laid (M8); stovehouse greens read outdoor air (M9); live bodies only exist within 180 m of a player, so a far pile is unreachable (M6); bears below half hunger went to bed mid-walk (M6).