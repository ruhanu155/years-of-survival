# Review round: foundations, cohesion, survival, and the core-loop audits

You are a reviewer in the Year of Ice studio (Ruhan → Director → Studio lead → departments → sub-agents). Content writing is done. This round makes the blueprint consistent and buildable before development starts. **Fix what you find in the doc. Don't just note it.**

## Owner requests (verbatim)
- "Once you finish adding and adapting, glance over all concepts with the team and see what we are missing in terms of foundation or building blocks for concepts and details that we are missing or if anything is too vague detail it. And see if we are missing anything regarding cohesion of all concepts, and the survival elements see if something has been forgotten or needs to be changed and if so do it."
- "Then we will move into development so make sure all food mechanics are good aswell as plants and fishing mechanics and swimming mechanics. and tree cutting and all other mechanics included in the forge labs mod."
- "Remember all the concepts from the forge labs video I want them all"
- "Make sure all minecraft concepts that need to be interlaced and detailed are."

## Read first
- v2/TAB-BRIEF.md: style rules, fixed facts and ENGINE: MINECRAFT. The world is now three rings: an 8 km home valley, a 64 km Interior and the endless Beyond.
- v2/RECONCILE.md: 103 numbered cross-tab fixes. Lines marked SETTLED or DIRECTOR RULING are decisions. Apply every item that touches a tab you own.
- The Minecraft development tab is binding for how things are built: its layer names, packages, ids, data paths and snake_case keys, and the build developer's "Conflicting ids" and "Names settled" tables. Local copies are DV1-lead.md, DV2-world.md, DV3-entities.md, DV4-client.md and DV5-build.md in the same folder; grep them.
- Local drafts that match the doc are in v2/: T*.md, main.md (index INDEX-main.txt), openworld.md, crafting.md, G-engine.md and the exp/ exports. For a tab with no local draft, export it with mcp__Claude_Docs__export: each export lands in a file as JSON [{text},{text}], and the second text is JSON whose data.bytes_b64 holds the markdown, so decode it with python.

## The doc
- Container C = {"kind":"project","id":"0f31ea9e-882b-4bc8-8554-69123e777660"}.
- Load the tools with ToolSearch `select:mcp__Claude_Docs__read,mcp__Claude_Docs__update,mcp__Claude_Docs__export,mcp__Claude_Docs__guide`. Read guide(items=["topic.editing"]) once.

| Tab | Tab id | Body node |
| --- | --- | --- |
| Blueprint (main, FULL) | 35966a5d-285d | 15dc2917-2687 |
| Minecraft engine | 3f9323f1-f210 | fca9313c-81e7 |
| Minecraft development | edb78978-392f | 84ec26f4-3bbf |
| World map | 99ca178e-2317 | ca9cc2c1-e2a0 |
| Every concept from the video | 76b43409-384d | 3c1a8d1e-da78 |
| Ideas from viewers | 590a6937-7c42 | b0337923-6fa8 |
| Open world in depth | ca8d8008-77e9 | d8537077-0133 |
| Crafting and building in depth | 97d32969-b7ec | 63ba968b-954d |
| Bestiary | 47d19f7c-3470 | 36ba24ff-4ff8 |
| Land animals in depth | 011efa65-ff2b | 12bad1d3-5ecb |
| Birds and the air | 98702e59-eb32 | 4801dee7-1ee9 |
| Waters and fish | 4113f4e5-afe8 | bbe1d6ef-f2a1 |
| Water life in depth | 524f8a28-7650 | affecef5-1a7f |
| Water, calls and encounters | 5acaa092-dc93 | bb013a54-4074 |
| Land and seasons in play | 64be7351-d618 | 565d4c99-e0fe |
| Plants and fungi | 44dbbf5e-850e | cb62e291-5c37 |
| Decay and time | 457d2710-550e | 5c53a53f-d9bd |
| Items | 688f54e0-c686 | 454ae177-f4d3 |
| Structures and buildables | 31db53d7-e402 | 211e2781-fb46 |
| Creative concepts | e618d0e8-d080 | b66857ad-d3c7 |

A Body department is writing a new tab, "Body, injury and sickness" (order a67). Don't edit it. Read it if it has landed, and tell the Director about any conflicts with it.

## How to edit
- **Find a block:** read with payload {"kind":"search","text":"<exact text>"} on the tab's body node, engine "prose".
- **Fix inside a block:** use a text replacement (op `replace` with a `find` or `chars` target). If the replaced text comes out code-formatted, replace the whole block as markdown. Whole-block replace needs ifHash and ifRev from your read.
- **Add text:** insert after the right block (side "after") as markdown, about 12 KB per call. No new `#` headings. `###` subsections are allowed only in tabs that already use them under that `##`.
- **The main Blueprint tab is full** (20,000 blocks). It accepts only text replacements inside existing blocks, never inserts. Put any new main-tab material in the topical tab you own, under the matching section, and replace one sentence in the main tab to point there ("… (see Plants and fungi › Tapping)").
- Check every ack is "allow", and never resend what landed.
- Edit only the tabs and main-tab sections you own (your task lists them). Other reviewers work in parallel, and ownership is exclusive.
- Before each edit, add it to your change log at v2/review/<your letter>-log.md (tab, block, before → after, one line each). Use v2/review/<your letter>/ as your working folder.

## What to do in your area
1. **Apply RECONCILE.** Make every item that touches your tabs true in the doc. That covers renames, numbers, ids, data paths, and camelCase → snake_case JSON keys. Do the rulings exactly as written.
2. **Foundations.** For every concept in your area, check it has all of these: what it is, where and when the player meets it, rules with numbers, inputs and outputs, what the player sees and hears, failure cases, ties to at least two systems, and a build line (id, data path, class, milestone, test).
   - Fill anything missing.
   - Detail anything vague ("some", "may", "rarely" with no number; an action named with no rules).
   - Watch for missing building blocks: an item used in a recipe that nothing produces, a tool needed before it can be made, a station named with no rules, a resource with no source or no season.
3. **Cohesion.** Make the numbers agree across tabs: kcal, weights, times, dates, temperatures, prices, ids and names. Follow each loop through end to end (get → process → store → use), and make sure every step exists and the hand-offs between tabs match. Fix the side that disagrees with the more specific or more authoritative tab, where the Minecraft development tab wins on build matters.
4. **Survival.** Walk a year in your area: May 1 start, summer, fall prep, freeze-up, deep winter, thaw. Is there a dead season with nothing to do? Is there a season where survival is trivial or impossible? Is there a forgotten need, a tedious chore with no upgrade path, or an exploit (infinite food, a safe AFK spot, a free lunch)? Fix each one, with numbers.
5. **Core-loop audit.** Your task names one or more loops. For each, trace it from first contact to mastery as a player would do it in Minecraft, keys and screens included. List every step with its rule, number, id and test. Fix gaps as you go. Finish with an audit table: step | rule location (tab › section) | status (Ready / Fixed in this round / Open decision for Ruhan).
6. **Forge Labs check.** Go through every row of the concept table at the top of the "Every concept from the video" tab (v2/T9-video.md) that falls in your area. Confirm the place it points to really specifies it. If it doesn't, specify it there.

## Your deliverables
- Fixes made in the doc, and your change log.
- One new block, inserted at the end of the most relevant tab you own (root end is fine there): a bold lead-in "**Review audit (<loop name>).**" followed by your audit table(s). Keep it under 12 KB.
- **Reply** (short):
  - counts of fixes by kind;
  - which RECONCILE numbers you applied;
  - anything you could not fix because another reviewer owns it (tab, block, what to change);
  - open decisions for Ruhan, with your recommendation;
  - any new ids or numbers.

Aim for thorough but tight: about 100–250 edits each is normal. Don't rewrite whole sections that are fine.
