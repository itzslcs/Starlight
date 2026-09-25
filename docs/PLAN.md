# Plan

Targets: 18 jars (see [`versions.json`](../versions.json)). Two extra user requests are folded in: **Prism Launcher support** and **addons like
TierTagger** (the `tiertags` plugin), plus **show own name** (the `own_nametag` module).

## Environment (verified 2026-09-25)
- Arch Linux, 10 cores, 13 GiB RAM, 71 GB free. JDK 11/21/26 installed. JDK 8 and 25 come from Gradle toolchains (foojay).
- Xvfb, xdotool and ImageMagick are present, with Mesa 26.2 (llvmpipe under Xvfb). Network access works.
- Prism Launcher 11.0.3 (Flatpak) and Dawn 0.2.0.41 (Flatpak) are installed. **We will not log in with the user's
  accounts.** Launcher-level verification is a manual checklist; automated verification uses a production-layout launch.

## Phases, deliverables, exit criteria

| Phase | Deliverables | Exit criteria (evidence) |
|---|---|---|
| 0 Research & plan | PLAN, [ARCHITECTURE](ARCHITECTURE.md), [RULES_MATRIX](RULES_MATRIX.md), [FEATHER](FEATHER.md), [DECISIONS](DECISIONS.md), versions.json | Committed. Every claim has a source |
| 1 Walking skeleton | Gradle monorepo; `api`, `core` (+ tests); Fabric **1.21.11** + Forge **1.8.9** adapters; GUI shell (all pages stubbed, Mods and HUD Editor working); config/profiles; modules FPS, Keystrokes, Armor; [`scripts/smoke.sh`](../scripts/smoke.sh) | `./gradlew buildAll` builds both jars; core tests pass; `smoke.sh 1.21.11` and `smoke.sh 1.8.9` pass (title → world → GUI → screenshot → 60 s → clean log) |
| 2 Fan-out 1.21.x | 1.21 … 1.21.10 via Stonecutter; per-version code only in adapters/mixins | 12 Fabric jars build; smoke passes on each (or the failure is recorded with its log) |
| 3 26.x | 26.1, 26.1.1, 26.1.2, 26.2, 26.3 (Java 25, no remap, Blaze3D only) | 5 jars build; smoke per target; `grep -r "org.lwjgl.opengl\|GL11\|GlStateManager" fabric/src` finds nothing for 26.x |
| 4 Features | full module set, plugin API + loader + consent, sample plugin, `tiertags` addon, own nametag, docs ([README](../README.md), [PLUGIN_API](PLUGIN_API.md), [THIRD_PARTY](THIRD_PARTY.md), [PRISM](PRISM.md)) | unit tests (config migration, profile import/export, plugin loader, layout math, server rules, rules-matrix consistency); smoke clean |
| 5 Perf | benchmark harness (fixed seed, scripted camera path, PvP scene), then each optimisation measured before/after | `docs/PERF.md` with numbers per optimisation; losers removed or default-off |
| 6 1.8.9 Hypixel | location (Mod API / opt-in `/locraw`), Auto GG/GL, stats overlay framework (in-game-data provider; API provider private-build only, D-010), nameplate stats, Bedwars trackers, Hypixel chat, scoreboard, lobby clutter, quick commands | unit tests for parsers (locraw JSON, scoreboard, chat patterns); smoke 1.8.9 clean |
| 7 Release | production-layout launch test per target, Prism instance zips, [`docs/COMPAT_MATRIX.md`](COMPAT_MATRIX.md), Feather/Dawn and Prism manual checklists, final report | every matrix cell is pass, fail or not-run with evidence |

## Verification tooling
- `./gradlew buildAll`: all jars, plugin jars, Prism zips, SHA256SUMS.
- `./gradlew :core:test`: unit tests.
- `scripts/smoke.sh <mc>`: dev client under `xvfb-run` (Mesa llvmpipe). With `-Dkestrel.smoke=1` the mod itself drives
  title → our GUI → world (quick-play or integrated server) → GUI → screenshots → 60 s → quit. The script then fails on
  `Exception`, `Mixin apply failed`, `InvalidInjectionException`, `[Kestrel] module ... failed`, or a missing smoke marker.
- `scripts/prodtest.sh <mc>` (Phase 7): launches the **built jar** in a production layout (real Fabric Loader +
  intermediary, or Forge universal + LaunchWrapper), downloaded from Mojang/Fabric/Forge metadata. This catches
  refmap/remap problems that dev runs cannot.

## Risks and mitigations
| Risk | Mitigation |
|---|---|
| 18 Loom projects exhaust 13 GiB RAM | `org.gradle.jvmargs=-Xmx4G`, `workers.max=4`; `buildAll` builds versions in chunks if needed |
| Essential's architectury-loom fork misbehaves on Gradle 9 | `legacy/` has its own wrapper, so it can pin an older Gradle independently (D-003) |
| API churn inside 1.21.x larger than expected | adapters are the only versioned code; verify signatures with `javap` on Loom's mapped jars before writing each port |
| Software GL too slow or unsupported for some version | smoke only needs title + world + GUI. Record FPS; do not claim perf numbers from llvmpipe as representative |
| Dawn-internal mixins conflict with ours | `compat.dawn` flag, optional mixins, manual Dawn checklist; never marked verified without a real run |
| Hypixel API policy (D-010) | in-game-data stats provider by default; API provider private-build only |
