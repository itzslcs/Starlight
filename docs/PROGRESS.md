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
- Docs: [README](../README.md) (hub + privacy/network list), PLUGIN_API (removed with plugins in 0.5.0), [THIRD_PARTY](THIRD_PARTY.md) (licences
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

## 2026-09-26: MW19 (0.2.0)
- **Renamed** Kestrel → MW19 (packages `dev.mw19`, mod id `mw19`, plugin API 2.0). Settings and profile codes migrate ([DECISIONS](DECISIONS.md) D-019).
- **Fixed (owner report):** clicks in the Right Shift menu did nothing (every Mods-page click threw). Toggle Sprint/Sneak
  failed after a restart (onEnable ran before Minecraft had options). Chroma colours froze (float time). All three have
  regression tests, and every smoke run now clicks the menu through real X11 input ([debug-log](debug-log.md)).
- **Home screen** replaces the title screen on every target (Surface + TitleSwapMixin / GuiOpenEvent).
- **Phase 5 started:** the benchmark harness ([`scripts/bench.sh`](../scripts/bench.sh)), Entity Culling (on by default), FPS Boost with Undo, and the
  Vulkan switch on 26.2+. Numbers are in [PERF](PERF.md).
- New modules: Speed, FPS Graph, Server Address, Clear Weather, Durability Alert.

## 2026-09-26: 0.3.0 (owner requests: skin changer, pack browser, world hosting, ExploitPreventer, home screen, modules)
- **Home screen** redone: vanilla panorama, pixel-block logo, the player's skin in 3D with a Skins button, Host World /
  Packs / MW19 Menu on the right ([DECISIONS](DECISIONS.md) D-022).
- **Skins** page (3D preview, skin folder, copy by player name, change skin via Mojang's API), **Packs** page (Modrinth
  search, verified download, enable), **Host World** page (Open to LAN, UPnP forwarding, invite whitelist).
- **Exploit Protection** (GRAY, D-021) and six small modules. Core tests: 63 pass, including a fake UPnP router, the
  skin upload request format, Modrinth parsing and file-name safety.
- Every smoke run now opens the three new pages (screenshots), runs a live Modrinth search, opens the world to LAN and
  runs an exploit self-test (a sign editor must send back a mod-only key untranslated); the mixin audit covers the
  five new injections.
- **Fixed:** a config save race at exit (debug-log, [`ConfigRaceTest`](../core/src/test/java/dev/mw19/core/ConfigRaceTest.java)); the 1.8.9 dev version label.
- **Not built:** in-game account switching (reads other programs' stored logins; D-022).

## 2026-09-26: 0.4.0 (owner: max-FPS presets with auto-detection, smooth animations, the community suggestion list)
- **Graphics presets** Potato/Low/Medium/High replace FPS Boost; **auto-detection** picks one on a fresh install from the
  GPU name (device type on 26.2+), CPU threads and heap ([DECISIONS](DECISIONS.md) D-023). Every smoke run checks it
  (llvmpipe → Potato) and applies/undoes Medium.
- Menu animations, themes Black/White/Crystal, finer crosshair, modules Hitboxes, TNT Timer, Reach Display, Quick Commands.
- Memory/CPU sampled off the render thread (was the costliest HUD module); smoke logs the five costliest HUD modules.
- 70 core tests (hardware tiers from real renderer strings, preset ranges vs vanilla's, apply/undo/first-run rules).

## Release status (2026-09-25)
- **Modrinth:** draft project `mw19` with all 18 jars as `0.1.0+mc<mc>` alpha versions (uploaded hashes match `dist/`).
  It is not submitted for review yet: the owner checks the page (the name is still the D-001 placeholder) and submits.
- **GitHub:** not pushed. The repo has no remote yet.

## Next
- Isolate the 26.1.1 exit hang (steps in [debug-log](debug-log.md)).
- Phase 5: benchmark harness first, then measured optimisations ([PLAN](PLAN.md)).
- Phase 6: the 1.8.9 Hypixel suite. Phase 7: production-layout launch tests, Prism/Dawn checks, final COMPAT_MATRIX.

## 2026-09-27: 0.5.0 (owner requests: fast chests, Sodium or VulkanMod inside the jar, MW19-style menus, remove plugins)
- **Fast Chests** (D-025): chests as block models from a built-in pack; verified against vanilla's renderer by
  screenshot on every Fabric target, in dev and production, on OpenGL and under VulkanMod. Chest field: +22 % average
  FPS, +29 % 1 % low ([PERF](PERF.md)).
- **VulkanMod bundled** on 1.21.9–1.21.11 (D-024), LGPL-compliant (source zips in `dist/sources/`), behind a launch-time
  switch (OpenGL first, GPU check, automatic fallback); its update check is off. Verified in production launches with
  Mesa's software Vulkan, including the no-Vulkan crash-then-recover path; the check found this PC's RX 6500 XT.
- **Production launch test** ([`scripts/prodlaunch.py`](../scripts/prodlaunch.py), `PROD=1 scripts/smoke.sh`): the dist jars pass on all 17 Fabric
  targets ([COMPAT_MATRIX](COMPAT_MATRIX.md)).
- **Menus:** vanilla buttons in the MW19 style and an MW19 Menu / Packs row on the pause menu (all 18 targets, D-027);
  a new Mods page (tiles with icons and ENABLED/DISABLED bars, list view). **Plugins removed** (D-026).
- **Block entity culling** and a lighter, unloaded-when-closed menu (from the 0.4.0 working tree).
- Dev smoke 18/18, production 17/17 (1.8.9 has no production launcher here), 67 core tests pass (the plugin tests went with the plugins).

