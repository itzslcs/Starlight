# Compatibility matrix

See also: [PROGRESS](PROGRESS.md), [debug-log](debug-log.md) (the failures behind the cells), [FEATHER](FEATHER.md) (Dawn/Feather checks), [PLAN](PLAN.md) (exit criteria).

Legend: **pass** / **fail** / **not-run**. Each cell links to or names its evidence. Nothing is marked pass without a run.

| Target | Build (`buildAll`) | Dev smoke ([`scripts/smoke.sh`](../scripts/smoke.sh)) | Production-layout launch | Prism | Dawn/Feather |
|---|---|---|---|---|---|
| 1.8.9 (Forge 11.15.1.2318) | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 15/15) | not-run (no production Forge launcher in the test setup) | not-run | not-run |
| 1.21 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 46/46) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 1.21.1 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 46/46) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 1.21.2 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 46/46) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 1.21.3 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 46/46) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 1.21.4 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 45/45) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 1.21.5 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 45/45) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 1.21.6 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 45/45) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 1.21.7 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 45/45) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 1.21.8 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 45/45) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 1.21.9 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 46/46) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks; with RENDERER=vulkan on lavapipe also pass, VulkanMod 0.6.6 running) | not-run | not-run |
| 1.21.10 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 46/46) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks; with RENDERER=vulkan on lavapipe also pass, VulkanMod 0.6.6 running) | not-run | not-run |
| 1.21.11 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 46/46) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks; with RENDERER=vulkan on lavapipe also pass, VulkanMod 0.6.8+1.21.11 running) | not-run | not-run |
| 26.1 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 45/45) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 26.1.1 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 45/45) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 26.1.2 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 45/45) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 26.2 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 47/47) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |
| 26.3 | pass (0.8.0, 2026-09-28) | pass on 45ad254 (0.8.0; Starlight look (backdrop drawn by the pause menu itself, keycaps, options screen with a slider), theme tour (10 scenes), bind profiles, Tier Tagger (live MCTiers lookup, the tier on the nametag and in the tab list), skin refresh, optimizer stage, chest scene fast/vanilla/fast, pause-menu row placed, Themes and About pages, presets, packs, LAN, exploit self-test; audit 47/47) | pass (0.8.0 dist jar, `PROD=1 smoke.sh`, OpenGL, all smoke checks) | not-run | not-run |

Snapshots: 45ad254 = 0.8.0 working-tree snapshot (dev smoke, 2026-09-28); dfb66f0 = 0.7.0 working-tree snapshot (dev smoke, 2026-09-27); 08e091d = 0.6.0 working-tree snapshot (dev smoke, 2026-09-27); 4cbfe11 = 0.5.0 working-tree snapshot (dev smoke; 26.x rerun on the tree after the icon fix, debug-log
2026-09-27); b2fc30b = 0.4.0; 420a474 = 0.3.0 (18/18 pass); 9473849 = 0.2.0 (18/18 pass, 2026-09-26); dbeb9f1 = Phase 4
commit; 848598d = Phase 3 (26.x) work tree; f82c0d6 = KeyCPS build (the code in the 0.1.0 jars). Every run enables all
modules, checks the log with the allowlist in `smoke.sh`, and runs the mixin audit. Since 0.3.0 every run also opens
the Skins, Packs and Host World pages, searches Modrinth, opens the world to LAN and runs the exploit self-test; since
0.5.0 it also builds the chest scene (Fast Chests on, off, on again, compared by screenshot and self-test) and checks the
MW19 row on the pause menu; since 0.6.0 it runs the optimizer stage (Marlow's Crystal Optimizer and Hero's Anchor
Optimizer against the integrated server, D-028) and shows the About page; since 0.7.0 it checks the skin refresh
and where the pause-menu row sits, requires the MW19 backdrop on the pause menu, and opens the Options screen
(keycaps and a slider on the backdrop, D-029); since 0.8.0 (Starlight) it shows every theme's scene on the home
screen and the Themes page, applies a bind profile to Minecraft's key bindings and back (D-032), and looks up a
ranked player on MCTiers, then shows that tier on the player's own nametag (front view) and reads it back from the
tab list (D-030). The pause-menu backdrop check now clears its flag when the pause menu opens, so only
that menu can pass it (26.2 and 26.3 had passed without drawing it, debug-log 2026-09-27). **Production-layout launch** = [`scripts/prodlaunch.py`](../scripts/prodlaunch.py): real Fabric (Knot, intermediary
game jar), the dist jar in a mods folder, offline name. Evidence lives in `smoke-out/<mc>/` and `smoke-out/prod-<mc>/` (gitignored,
regenerated by the script). Smoke runs use Xvfb and Mesa llvmpipe, so FPS figures from them are not performance claims.

## Alongside other mods
Dev smoke with `WITH_MODS="fabric-api sodium immediatelyfast ferrite-core lithium"` (the mods of the MW19 Performance
packs, from Modrinth and Fabric's Maven, never bundled; [debug-log](debug-log.md) has how the run was made to work).

| Target | Mods | Result |
|---|---|---|
| 1.21.11 | Fabric API 0.141.6, Sodium 0.8.14, ImmediatelyFast 1.14.3, Lithium 0.21.4, FerriteCore 8.2.0 | **pass** (0.8.0 working tree, 2026-09-28): all smoke checks, Tier Tagger included, audit 46/46 |
| 1.21.11 | Fabric API 0.141.6 alone | **pass** (0.6.0 working tree, 2026-09-27): optimizer stage ok. MW19's `minecraft:register` is written beside Fabric API's own codec for that channel. Audit 39/39 |
| 26.3 | Fabric API 0.161.0, Sodium 0.9.2, ImmediatelyFast 1.17.1, Lithium 0.26.1, FerriteCore 9.0.0 | **pass** (0.4.0 working tree, 2026-09-26): all smoke checks, audit 28/28 |

The other 16 targets have not been run with these mods yet, so MW19 Performance packs exist only for 1.21.11 and 26.3.
The 26.3 row is still the 0.4.0 run.

### Upgrading from MW19 (0.8.0)
[`scripts/upgrade-test.sh`](../scripts/upgrade-test.sh) with the MW19 0.7.0 jar, production launches of 1.21.11 in one game folder, OpenGL (2026-09-28;
evidence in `smoke-out/upgrade-1.21.11/`):

| Step | Result |
|---|---|
| MW19 0.7.0 alone, then Fast Chests on with `mw19/fast_chests` selected, theme "Violet", a marker file in `MW19/` | MW19's smoke run passes and writes `MW19/` (config schema 1) |
| Starlight 0.8.0 alone | **pass**, the whole smoke run: `MW19/` moved to `Starlight/` with the marker, config schema 2, theme Nebula. Vanilla drops the old `mw19/fast_chests` id and Starlight's pack is active from the first frame; switching it off and on reloads (debug-log 2026-09-28) |
| Starlight 0.8.0 and MW19 0.7.0 together | Fabric refuses to start: "Mod 'Starlight' (starlight_client) 0.8.0+mc1.21.11 is incompatible with any version of mod 'MW19' (mw19)" |

1.8.9 was not upgrade-tested (no production Forge launcher here); it moves the folder with the same core code
([`ConfigFolder`](../core/src/main/java/dev/starlight/core/ConfigFolder.java), covered by [`RenameCompatTest`](../core/src/test/java/dev/starlight/core/RenameCompatTest.java)). Its in-game warning when the MW19 jar is still installed has
not been run.

### The bundled VulkanMod (0.5.0)
| Target | Setup | Result |
|---|---|---|
| 1.21.9, 1.21.10, 1.21.11 | production launch, `RENDERER=vulkan`, Mesa lavapipe (software Vulkan) | **pass** (2026-09-27): Vulkan running (VulkanMod 0.6.6 / 0.6.6 / 0.6.8), chest scene ok |
| 1.21.11 | production launch, fresh install, lavapipe with `VK_SOFT=1`, then a second start | **pass**: OpenGL first with the GPU check `ok`, then Vulkan |
| 1.21.11 | production launch, Vulkan forced on a machine without a Vulkan driver, then a second start | first start ends in VulkanMod's own crash report (expected); the second **passes** on OpenGL with the preset applied |
| 1.21.9 – 1.21.11 | production launch, fresh install, this PC's own Vulkan driver (RADV) | **pass** on OpenGL; the GPU check found `AMD Radeon RX 6500 XT (RADV NAVI24) (Vulkan 1.4)` |
| 1.21.11 | dev run, Fabric API 0.141.6 + VulkanMod kept off | **pass**, audit 30/30; Fabric API's Indigo renderer registered |
| 1.21.11 | dev run, VulkanMod running (lavapipe) | **pass**, audit 31/31 (its update check off) |
