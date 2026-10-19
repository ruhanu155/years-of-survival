# Borrowed-code round (Ruhan: "Go ahead and borrow the code")

Year of Ice is a personal-use NeoForge 1.21.1 mod. Repo: /home/claude/years-of-survival (GitHub ruhanu155/years-of-survival). Read its CLAUDE.md first.

## Goal
Port the useful algorithms from open-source mods into OUR pure-Java simulation cores (`com.yearofice.sim.*`, no Minecraft imports). Each core gets JUnit tests that prove the blueprint's numbers. Our blueprint's rules and numbers always win; the borrowed code supplies structure, edge-case handling and proven algorithms, not tuning. Do not wire anything into gameplay yet, beyond optional `/yoi` debug hooks; adapters come in each system's milestone.

## Licence hygiene (mandatory)
- Fetch sources only from GitHub (raw.githubusercontent.com / github.com). Pin a commit hash.
- Copied or closely adapted files keep the original copyright and licence header, plus a line "Adapted for Year of Ice from <repo>@<commit>, <path>".
- Add or append `THIRD_PARTY_NOTICES.md` at the repo root: project, URL, commit, licence, which of our files derive from it, and `release_blocker: true` for GPL/EUPL/MPL-derived files. Ruhan's use is personal, but this keeps a public release possible later.
- Put the full licence texts in `licenses/<project>.txt`.
- Prefer re-implementing from understanding over line-by-line copying when the logic is small. Say which you did per file.

## Spec sources
All in v2/; grep with offsets and don't read whole files.
- final/exp2/*.md: the final tab exports. ready.md is the build order.
- DV1-lead.md: the package map, layer APIs (BodyService, DecayService, EnvSample) and determinism rules.
- DV4-client.md: the body record and the 13-step tick.
- RECONCILE.md: the rulings, and items 153 and up for the reuse list.
- MODS.md: the research table.

## Rules
- Work in your own git worktree and branch (named in your task). Touch only the packages your task names.
- `./gradlew build` must pass. All new tests are JUnit, plus at most a few GameTests if useful. Run `./gradlew runGameTestServer` before your PR. Never run `runServer`, which needs the EULA.
- Commit trailer:
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01Xr2CVpzoZ636TDaUZHoGmj
- Rebase on origin/main, push, then open a PR into main with `gh pr create` or the GitHub MCP `create_pull_request` (load it via ToolSearch). End the PR body with:
  🤖 Generated with [Claude Code](https://claude.com/claude-code)

  https://claude.ai/code/session_01Xr2CVpzoZ636TDaUZHoGmj
- Don't merge. Another department (World generation, M0) and Art are working in parallel. If `THIRD_PARTY_NOTICES.md` conflicts on rebase, keep both sides.

## Reply
Keep it short:
- the PR link;
- per core: the source (repo, commit, licence), whether you copied or reimplemented, and the tests with their results;
- anything in the borrowed code that contradicts the blueprint, and which side you followed. The blueprint wins unless it is clearly broken; flag that case.
