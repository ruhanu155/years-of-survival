# Brief: a new gameplay tab for the Year of Ice blueprint

## Context
Year of Ice is a first-person, open-ended (Minecraft-like, no ending) wilderness survival browser game set in an Alaskan valley (about 64°N, interior Alaska), based on Forge Labs' video "1 Year in Alaska in ULTRA REALISTIC Minecraft". Its design blueprint is a Claude Docs doc with three tabs. Current markdown exports:

- Main tab ("Blueprint", FULL, it takes no more blocks): v2/main.md (487 KB; heading index with line numbers in INDEX-main.txt)
- "Open world in depth": .../v2/openworld.md (index INDEX-openworld.txt)
- "Crafting and building in depth": .../v2/crafting.md (index INDEX-crafting.txt)

Do NOT read main.md whole. Read the index, then read the sections relevant to your topic with offset/limit (Read tool), plus "Scope and pillars", "Gameplay ideas from the video" and the parts that already cover your topic, so you never duplicate or contradict them. Grep is your friend (e.g. grep -n -i "lynx" main.md).

## The owner's request (verbatim)
"Flesh out more gameplay mechanics and gameplay such as more animals, fish, environment gameplay and mechanics, items, structures, buildable things create more creative concepts to establish a basis for gameplay"

## ENGINE: MINECRAFT (owner decision, overrides the doc)
The game is now built as a Minecraft Java Edition mod on NeoForge (mod id `yearofice`, Java package `com.yearofice`), plus a few library mods: GeckoLib for animal models and animations, and Curios API for extra equipment slots (belt, back, neck). It is pinned to the newest Minecraft version NeoForge supports at project start (the 1.21.x line). A separate helper is writing a "Minecraft engine" tab that maps the whole blueprint onto Minecraft; wherever the existing tabs say JavaScript, browser, WebGL, IndexedDB, Playwright or `js/` modules, Minecraft wins. Write YOUR content for Minecraft:
- 1 block = 1 m; 20 ticks per second. Things are blocks, block entities, items, entities (mobs), particles, sounds, data-pack JSON (loot tables, recipes, tags, worldgen features, biome modifiers) and Java classes.
- Vanilla mobs that don't belong (zombies, creepers and so on) are off; vanilla hunger, health hearts and the vanilla crafting table are replaced by the blueprint's body, Condition and station systems. The vanilla 9-slot hotbar and 27-slot inventory map onto the blueprint's inventory.
- Animals are custom entities with GeckoLib models; fish are entities in water plus items when caught.
- Players can't dig freely: only soil 2 blocks down, rock only at generated rock faces, no deep mining (pillar 3).
- In "Data and build", name the registry ids (`yearofice:moose`, `yearofice:smokehouse`), the data-pack path (`data/yearofice/...`), the Java class, the milestone, and tests written as NeoForge GameTests.
- Keep the design itself as written. Change only HOW it is built.

## Facts you must keep consistent
- Six ranked pillars: Scarcity is the clock; Respect the player's time; Explore, don't dig (surface iron only, no digging down); The year changes the map; Everything readable; Grow without an end.
- 365-day years starting May 1; new seed each run; 8 km world of 16 regions on a 4 × 4 grid (see openworld.md); look undecided.
- Time: world clock 1 game minute per real second; travel rate 1 game minute per 5 real seconds (1 km on foot about 1 game hour, about 250 kcal); hold-to-work is a time-lapse; a close clock (6 game seconds per real second) runs in danger, minigames and water.
- Body: Strength = Condition × clamp((BMI − 17)/6, 0, 1); longbow needs 70. BMI 17 = SOS. Endless (default) vs Hardcore; presets Gentle/Standard/Brutal.
- Inventory: Minecraft-style 9-slot hotbar (keys 1–9), off-hand slot (X), pack rows of 9 (crate backpack 18, hide pack 27, pack frame 27 + 3 lash points, chest 27, barrel 18, food cache 27, sled 36, canoe 36), weight limits (30 kg backpack, 45 kg frame, 120 kg sled on snow, 150 kg canoe).
- Clothing: 5 zones × inner/outer, warmth in °C; bare skin safe to −20 °C feels-like.
- Wolves replace dogs (tamable, also prey). Messenger = HUD/information screen; it never reports trap catches.
- Story names: Station Nine (the outpost), Dr. Elin Marsh (missing observer), her wolf Tok, trapper Arvid Lund (1938–1971), miners Kessler and Doane (1906–1912), pilot "J.O." (1979 wreck), radio contacts incl. Pete Lund (Arvid's grandson).
- Crafting tiers: Start, Bushcraft, Hide and bone, Iron, Homestead. Milestones M0–M9.
- Studio chain: Ruhan → Director → Studio lead → departments → sub-agents.

## Your job
Create ONE new tab in the doc for your topic and fill it with NEW content that establishes a basis for gameplay: more things to find, meet, make and do, each specified well enough to build. For every new element give: what it is, where and when the player meets or gets it, the mechanics with numbers, how it ties to at least two existing systems, what it gives or costs, and how it shows on screen (the messenger, the world, sound). End the tab with a "Data and build" section: the JSON shape, the code module, the milestone it ships in, and 3–6 numeric tests. Invent freely but plausibly for interior Alaska. Be creative: emergent interactions, surprises, small stories, things players will tell friends about.

Do not repeat what the other tabs already specify; reference them by section name instead. Size: about 30–50 KB of markdown.

## Style rules (strict)
- Lead each block with its point. Short plain sentences (under 25 words), paragraphs of at most 3 sentences.
- Specifics, not adjectives: numbers with units, names, dates. No marketing words, no emoji.
- Pipe tables for items compared on several attributes; numbered lists for ordered steps; bullets for parallel items; bold lead-ins ("**Spoilage.** …"); ```latex for formulas (bare TeX); ```json for data.
- Headings: the tab starts with "# <Tab title>", then a 2–3 sentence lead, then "## " sections and "### " subsections. No other heading levels.
- Metric first (°C, kg, m, km), kcal, game hours/days. No links except tab mentions, no HTML, no emoji.

## How to create and fill your tab
Load tools with ToolSearch: `select:mcp__Claude_Docs__batch,mcp__Claude_Docs__update,mcp__Claude_Docs__guide`. Read `guide(items=["topic.tabs"])` once.

Doc container C = {"kind":"project","id":"0f31ea9e-882b-4bc8-8554-69123e777660"}.

1. Write your full content first to your draft file (path in your task) so it is not lost.
2. Create the tab in ONE batch (container C), with members exactly like this (replace NAME, FILE and ORDER from your task):
   - {"$lid":"f","verb":"create","object":"file","payload":{"value":{"birthName":"FILE.ldoc"}}}
   - {"$lid":"n","verb":"create","object":"node","engine":"prose","payload":{"parent":{"object":"file","id":"$lid:f"},"source":{"as":"markdown","from":{"kind":"inline","content":"# NAME\n\n<your 2–3 sentence lead>"}}}}
   - {"verb":"update","ref":{"object":"file","id":"$lid:f"},"payload":{"kind":"value","value":{"birthName":"FILE.ldoc","content":{"kind":"node","id":"$lid:n"}}}}
   - {"verb":"update","ref":{"object":"project","id":"0f31ea9e-882b-4bc8-8554-69123e777660"},"payload":{"kind":"patch","patch":[{"op":"set","path":["tabs","$lid:f"],"value":{"name":"NAME","order":"ORDER"}}]}}
   The ack gives lids.f (tab id) and lids.n (body node id).
3. Fill it: `mcp__Claude_Docs__update` with ref {"object":"node","id":"<lids.n>"}, engine "prose", container C, payload {"ops":[{"op":"insert","target":{"kind":"root"},"side":"end","source":{"as":"markdown","from":{"kind":"inline","content":"<one or two ## sections>"}}}]}. At most about 12 KB per call; chain at root end in order. Check each ack is "allow". Never resend something that landed.
4. Touch nothing outside your own tab. Other helpers are creating their own tabs in parallel.

When done, reply with: the tab id, a one-line list of what the tab contains (counts: species, items, structures, etc.), and any new names or numbers other helpers must know.
