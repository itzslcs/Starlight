# Progress

## Phase 0: research & plan (done 2026-09-25)
- Verified the 18 targets against Mojang's manifest (versions.json), with Java 8/21/25 and mappings per version.
- Fabric: Loom 1.18.x with `fabric-loom-remap` (≤1.21.11) and `fabric-loom` (26.x); Loader 0.19.5; Gradle 9.8.0; Stonecutter 0.9.8 + loom-back-compat 0.4.2.
- Feather is now Dawn: Fabric for 1.17.1–26.3 (no 1.21.2/1.21.6/1.21.9), Forge 11.15.1.2318 + MixinTweaker for 1.8.9. Fabric API is injected by Dawn ([FEATHER.md](FEATHER.md)).
- Hypixel mod policy mapped ([RULES_MATRIX.md](RULES_MATRIX.md)). The Hypixel **API** policy forbids entering keys into public mods ([DECISIONS](DECISIONS.md) D-010).
- Tier lists: MCTiers v2 + SubTiers v2 APIs documented and live. PvPTiers is down (D-015).

## Phase 1: walking skeleton (done 2026-09-25)
- Build: Gradle 9.8.0 (daemon pinned to JDK 25 via [`gradle/gradle-daemon-jvm.properties`](../gradle/gradle-daemon-jvm.properties)), Stonecutter 0.9.8 +
  loom-back-compat 0.4.2 (Loom 1.18.2) for `fabric/`, and a separate `legacy/` build (Essential architectury-loom 1.15.50) for Forge 1.8.9.
  `./gradlew buildAll` → `dist/MW19-0.1.0+mc{1.21.11,1.8.9}.jar` + `SHA256SUMS`.
- `api` (plugin surface) + `core` (module system, event bus, config/profiles/migrations/backups, HUD layout + manager,
  full GUI shell: Mods, HUD editor, Profiles, Keybinds, Plugins stub, Server Rules, Performance, Themes, About).
- Modules: FPS, Keystrokes, Armor Status. 31 core unit tests pass.
- Smoke: 1.21.11 and 1.8.9 both **PASS** headless (title → GUI → HUD editor → world → GUI → 60 s → quit), 0 suspicious log records.
- Perf (llvmpipe): HUD cost 1.21.11 76 µs/frame, 1.8.9 275 µs/frame (see [debug-log.md](debug-log.md)). Real-GPU numbers are Phase 5.

## Phase 2: 1.21 … 1.21.10 (done 2026-09-25)
- API breaks located with javap diffs over Loom's mapped jars (not memory): 1.21.2 world creation/GameRules; 1.21.6
  Matrix3x2fStack pose, Screenshot.grab signature, blur/background flow; 1.21.9 input event records, Window.handle();
  1.21.11 Util/GameRules packages, ResourceLocation→Identifier.
- All 12 1.21.x jars build; smoke PASS on every one ([COMPAT_MATRIX](COMPAT_MATRIX.md)). Smoke runs now use a detached git worktree snapshot
  (`.worktrees/smoke`) so ongoing edits cannot break a running test (debug-log 2026-09-25).

## Phase 4: features (done 2026-09-25, commit dbeb9f1 + this commit)
- Full module set, plugin API + loader with per-jar consent, the Tier Tags addon and the Session Stats sample, own nametag.
- **KeyCPS** (the owner's own mod) replaced the CPS and Keystrokes modules ([DECISIONS](DECISIONS.md) D-018). It counts per
  binding from input events ([`InputRates`](../core/src/main/java/dev/mw19/core/modules/InputRates.java)), and every smoke run feeds it 7 attack + 4 use presses and asserts the counts.
- Docs: [README](../README.md) (hub + privacy/network list), [PLUGIN_API](PLUGIN_API.md), [THIRD_PARTY](THIRD_PARTY.md) (licences
  checked from artifacts/repos; Mixin's MIT notice now ships in the 1.8.9 jar), [PRISM](PRISM.md), and a generated
  [CODE_MAP](CODE_MAP.md). The docs are a linked graph (repo root = Obsidian vault; [`scripts/docs-graph.py`](../scripts/docs-graph.py)).
- `buildAll` also writes importable Prism instance zips (`dist/prism/`). 40 core unit tests pass.

## Phase 3: 26.1 … 26.3 (done except one open issue, 2026-09-25)
- 26.x API seams from javap on the unobfuscated jars: GuiGraphics→GuiGraphicsExtractor (26.1), Gui→Hud and screen/chat
  moved under `mc.gui` (26.2), render-state extraction (lightmap, camera FOV/angles, damage tilt), private chat
  `addMessage` with GuiMessageSource, world clocks, and **26.3's switch from GLFW to SDL3** (scancodes, SDL_Keymod,
  1-based mouse buttons). MW19 keeps GLFW codes as its canonical key space via [`SdlKeys`](../core/src/main/java/dev/mw19/core/SdlKeys.java), so profiles stay portable.
- All 18 jars build. The dev smoke passes on 17 of 18 ([COMPAT_MATRIX](COMPAT_MATRIX.md)); mixin audits are fully wired.
- **Open:** 26.1.1 hangs in JVM exit after a passing run (2/2 runs; [debug-log](debug-log.md)).

## Release status (2026-09-25)
- **Modrinth:** draft project `mw19` with all 18 jars as `0.1.0+mc<mc>` alpha versions (uploaded hashes match `dist/`).
  It is not submitted for review yet: the owner checks the page (the name is still the D-001 placeholder) and submits.
- **GitHub:** not pushed. The repo has no remote yet.

## Next
- Isolate the 26.1.1 exit hang (steps in [debug-log](debug-log.md)).
- Phase 5: benchmark harness first, then measured optimisations ([PLAN](PLAN.md)).
- Phase 6: the 1.8.9 Hypixel suite. Phase 7: production-layout launch tests, Prism/Dawn checks, final COMPAT_MATRIX.
