# Progress

## Phase 0: research & plan (done 2026-09-25)
- Verified the 18 targets against Mojang's manifest (versions.json), with Java 8/21/25 and mappings per version.
- Fabric: Loom 1.18.x with `fabric-loom-remap` (≤1.21.11) and `fabric-loom` (26.x); Loader 0.19.5; Gradle 9.8.0; Stonecutter 0.9.8 + loom-back-compat 0.4.2.
- Feather is now Dawn: Fabric for 1.17.1–26.3 (no 1.21.2/1.21.6/1.21.9), Forge 11.15.1.2318 + MixinTweaker for 1.8.9. Fabric API is injected by Dawn (docs/FEATHER.md).
- Hypixel mod policy mapped (docs/RULES_MATRIX.md). The Hypixel **API** policy forbids entering keys into public mods (DECISIONS D-010).
- Tier lists: MCTiers v2 + SubTiers v2 APIs documented and live. PvPTiers is down (D-015).

## Phase 1: walking skeleton (done 2026-09-25)
- Build: Gradle 9.8.0 (daemon pinned to JDK 25 via `gradle/gradle-daemon-jvm.properties`), Stonecutter 0.9.8 +
  loom-back-compat 0.4.2 (Loom 1.18.2) for `fabric/`, and a separate `legacy/` build (Essential architectury-loom 1.15.50) for Forge 1.8.9.
  `./gradlew buildAll` → `dist/Kestrel-0.1.0+mc{1.21.11,1.8.9}.jar` + `SHA256SUMS`.
- `api` (plugin surface) + `core` (module system, event bus, config/profiles/migrations/backups, HUD layout + manager,
  full GUI shell: Mods, HUD editor, Profiles, Keybinds, Plugins stub, Server Rules, Performance, Themes, About).
- Modules: FPS, Keystrokes, Armor Status. 31 core unit tests pass.
- Smoke: 1.21.11 and 1.8.9 both **PASS** headless (title → GUI → HUD editor → world → GUI → 60 s → quit), 0 suspicious log records.
- Perf (llvmpipe): HUD cost 1.21.11 76 µs/frame, 1.8.9 275 µs/frame (see debug-log.md). Real-GPU numbers are Phase 5.

## Next: Phase 2
Port to 1.21 … 1.21.10 (Stonecutter conditionals in fabric/src only).
