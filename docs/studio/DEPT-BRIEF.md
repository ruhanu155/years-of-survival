# Department round brief: studio detail for the Added and Deepened entries

You are a department in the Year of Ice studio (chain: Ruhan → Director → Studio lead → departments → sub-agents). The Director (the main session) has sent you a set of entries that two helpers just wrote. Your job is to make each one buildable at production depth by adding a "Studio detail" block under it, and nothing else.

Read first: v2/TAB-BRIEF.md (style rules, fixed facts, ENGINE: MINECRAFT). Owner request (verbatim): "Run these deepened entries through the team and have them create more details for the blueprint". Earlier: "make them detailed and flesh out their gameplay, rules, and mechanics".

Sources (local drafts that match the doc exactly):
- Video tab "Every concept from the video": v2/T9-video.md (doc tab body node 3c1a8d1e-da78)
- Viewers tab "Ideas from viewers": v2/T10-comments.md (doc tab body node b0337923-6fa8)
- Decoded exports of the other tabs are in v2/exp/ and t10x/ (grep them to stay consistent); the engine conventions are in the Minecraft engine tab export.

## What a Studio detail block contains
For each entry you own, write a block that starts with a bold lead-in: **Studio detail (<your department>).** Then cover whatever the entry still lacks, typically:
1. Edge cases and exact rules (what happens at the boundaries, in each season, at night, in each realism preset, in Endless vs Hardcore).
2. Numbers and a short formula or table where useful.
3. Interactions with at least two other systems, named by tab › section.
4. Content lists where the entry implies them (variants, species, item lists, messenger lines, journal lines, sound list).
5. Feel: what the player sees, hears and does, step by step; the first-time experience and how it teaches itself.
6. Minecraft build: registry ids, data-pack paths, Java classes (per the engine conventions), client vs server, performance cost.
7. Risks: how it could become tedious or exploitable, and the guard against it.
Keep it tight: about 1,500–3,500 characters per entry. Do not restate the entry; add to it.

## How to insert
Load tools: ToolSearch `select:mcp__Claude_Docs__read,mcp__Claude_Docs__update`. Container C = {"kind":"project","id":"0f31ea9e-882b-4bc8-8554-69123e777660"}.
For each entry: find the NEXT heading after the entry in that tab (read with payload {"kind":"search","text":"<next heading text>"} on the tab's body node, engine "prose") to get its block id, then insert your block before it:
{"op":"insert","target":{"kind":"blocks","ids":["<next heading block id>"]},"side":"before","source":{"as":"markdown","from":{"kind":"inline","content":"<your Studio detail markdown>"}}}
Use bold lead-ins, tables, lists and ```latex/```json blocks only; no new headings. At most 2–3 entries per update call, about 12 KB per call. Check every ack is "allow"; never resend what landed. Touch only your own blocks. Other departments are inserting into the same two tabs at the same time, under other entries.

Also save your full additions to your draft file (path in your task) before inserting.

When done, reply: which entries got detail, any new names, numbers or ids other departments must know, and any conflict you found with an existing rule (with your proposed resolution). Keep the reply short.
