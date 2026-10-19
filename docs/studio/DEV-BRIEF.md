# Main developers' Minecraft pass

You are one of five main developers in the Year of Ice studio (Ruhan → Director → Studio lead → departments → sub-agents; you work for the studio lead). Owner requests (verbatim):
- "After this mob additions are done I want you to review them, detail them and flesh them out and see if we are missing anything for the minecraft mechanics that we need to add, to work through development and using minecraft as a evoling base for our engine as this will be a evolving modpack."
- "Make sure all minecraft concepts that need to be interlaced and detailed are."
- "Add a detailed and fleshed pass through the minecraft portion for me please with the main devs"

Read v2/TAB-BRIEF.md (style rules, fixed facts, ENGINE: MINECRAFT) and v2/RECONCILE.md (45 cross-tab fixes the Director has logged; resolve the ones in your area inside your section, as decisions, and list which numbers you settled).

The doc: container C = {"kind":"project","id":"0f31ea9e-882b-4bc8-8554-69123e777660"}. Export the tabs you need with mcp__Claude_Docs__export (each export lands in a file as JSON [{text},{text}]; the second text is JSON with data.bytes_b64 holding the markdown; decode with python). Tab ids:
- Blueprint (main) 35966a5d-285d (huge; use the local copy v2/main.md and INDEX-main.txt instead)
- Minecraft engine 3f9323f1-f210 (the conventions; your section builds on it and must not contradict it unless you say why)
- World map 99ca178e-2317 · Open world in depth ca8d8008-77e9 · Crafting and building in depth 97d32969-b7ec
- Bestiary 47d19f7c-3470 (now with Carcasses and bodies, No vanilla mob mechanics) · Waters and fish 4113f4e5-afe8 · Water, calls and encounters 5acaa092-dc93 · Land and seasons 64be7351-d618 · Items 688f54e0-c686 · Structures and buildables 31db53d7-e402 · Creative concepts e618d0e8-d080
- Every concept from the video 76b43409-384d · Ideas from viewers 590a6937-7c42
- Still being written (export them at the end too and cover whatever is there): Land animals in depth, Birds and the air, Water life in depth, Plants and fungi, Decay and time (find their ids with mcp__Claude_Docs__read ref {"object":"project","id":"0f31ea9e-882b-4bc8-8554-69123e777660"}, which lists the tabs).

Your section goes in the "Minecraft development" tab (tab edb78978-392f, body node 84ec26f4-3bbf). You own exactly one pending block there (id in your task). Fill it with ONE first call: a `replace` of your pending block with "## <your heading>" plus your first part:
{"op":"replace","target":{"kind":"blocks","ids":["<your pending id>"]},"with":{"as":"markdown","from":{"kind":"inline","content":"## ...\n\n..."}}}
Then continue with inserts placed "after" the last block id from the previous ack (side "after"), about 12 KB per call. Never use root end (other developers' sections follow yours). Never touch other blocks. Save your full text to your draft file first.

What every section must deliver, at implementation depth:
- For each system in your area: the exact NeoForge mechanisms (registries, DeferredRegister, data attachments, data components, capabilities, events, block entities, Brain/sensors/memories, GUI layers and screens, payloads, reload listeners, data maps, tags, loot tables, recipe types, worldgen hooks), client vs server split, what is saved and synced, tick cost, and its GameTests.
- The INTERLACE: which shared layers each system reads and writes. The lead engineer defines the shared layers; the others must use them by name (read the lead's section in the doc when it appears; until then follow the list in your task).
- Vanilla gaps in your area: everything vanilla still does that we haven't replaced, decided.
- A table of every registry entry your area needs (id, type, class, data file, milestone), drawn from all tabs, so nothing in the design is left without a build entry.
- Risks and the first prototype to build to retire each one.
Aim for 40–70 KB. Reply with: what you covered, the RECONCILE items you settled, and anything you need from another developer.
