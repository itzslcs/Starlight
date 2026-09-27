# Performance

See also: [PLAN](PLAN.md) (Phase 5), [ARCHITECTURE](ARCHITECTURE.md) (performance budget), [debug-log](debug-log.md)
(benchmark pitfalls), [COMPAT_MATRIX](COMPAT_MATRIX.md).

## What MW19 does for FPS
| Feature | Where | What it changes |
|---|---|---|
| **Entity Culling** (module, on by default) | all 18 targets | Mobs, items and other entities fully hidden behind solid blocks are not drawn. Rays from the camera to each entity's box (centre and 8 corners) walk the block grid. The entity is skipped only if every ray hits a full opaque block. Players, glowing entities and anything showing a name tag are always drawn, because vanilla shows those through walls. Results are cached per entity, re-checks are spread over frames, and block lookups are capped per frame, so the check cannot cost frames. It fails open: drawing is the default. |
| **Graphics presets** (Performance page) | all 18 targets | [`VideoPreset`](../core/src/main/java/dev/mw19/core/perf/VideoPreset.java): Potato, Low, Medium, High. All keep VSync off and FPS unlimited, and even High stays at the default view distance (12). Potato: view 5, simulation 5, entity distance 50 %, minimal particles, no clouds, fast graphics/leaves, no smooth lighting, shadows, biome blend, mipmaps, menu blur or vignette. *Auto* ([`HardwareTier`](../core/src/main/java/dev/mw19/core/perf/HardwareTier.java)) picks from the GPU name (and device type on 26.2+), CPU threads and heap; MW19 applies it once on a fresh install, never raising view or simulation distance. *Undo* restores your values, even after a restart. (0.2.0's FPS Boost is now Low; the benchmark's "boost" phase uses Low.) |
| **Block entity culling** (part of Entity Culling) | all 18 targets | Chests, signs, banners, heads and other block entities fully hidden behind solid blocks get no block entity render, with the same ray test on a box one block larger than the block. Renderers the game draws off screen (beacon beams, end gateways) are never skipped. |
| **Fast Chests** (module, on by default) | 1.21 – 26.3 | Chests, trapped, ender and copper chests become ordinary block models (a built-in resource pack) baked into the world mesh, and their block entity renderer is skipped; lids no longer animate ([DECISIONS](DECISIONS.md) D-025). |
| **Vulkan renderer** (Performance page) | 1.21.9 – 1.21.11 (bundled VulkanMod), 26.2, 26.3 (Minecraft's own) | 1.21.9 – 1.21.11: the jar carries VulkanMod, which runs from the second start when the GPU check finds a Vulkan 1.2 device; a failed Vulkan start puts the next one back on OpenGL ([DECISIONS](DECISIONS.md) D-024). 26.2+: sets Minecraft's own graphics API preference, which falls back to OpenGL by itself. |
| Allocation-free HUD | all | No per-frame allocation in HUD paths; per-module cost is on the Performance page (budget 300 µs/frame). |
| Batched HUD text | 1.8.9 | Plain ASCII HUD text is drawn as one textured quad batch from the font atlas instead of the font renderer's per-glyph immediate-mode calls (colour codes, other characters, the Unicode font and right-to-left languages still use the font renderer). Smoke with every module on (llvmpipe): MW19's HUD 1201 → 842 µs/frame, KeyCPS 715 → 153 µs, with identical-looking text (screenshots compared). |
| Off-thread sampling | all | Memory/CPU reads the process CPU load on a background thread; on the render thread it was the costliest module (about 85 µs/frame on average). |

Sodium, ImmediatelyFast and similar mods are complementary. Their compatibility is recorded in the
[COMPAT_MATRIX](COMPAT_MATRIX.md) once it has been run.

## How it is measured
`scripts/bench.sh <mc>` (core [`Bench`](../core/src/main/java/dev/mw19/core/Bench.java)) builds one fixed scene in a fixed-seed world: a stone floor and wall at y=200,
120 villagers (no AI) behind the wall and 14 in front, 299 chests behind the wall above the villagers (since 0.4.0),
with the camera facing the wall. It measures phases in one run: an unreported warm-up, **baseline** (culling and Fast
Chests off, the options as they were), **culling**, **fastchests**, **boost** (the Low preset), all three together, and
**baseline-end** (everything undone). baseline-end exposes drift within the run. Each phase gets a warm-up (25 s after a
change that rebuilds chunk sections: options, or Fast Chests' resource reload) and then 15 s of frames. It reports
average FPS, 1 % low (the mean of the worst 1 % of frames), p99 frame time, hitches over 50 ms and GC time.
`BENCH_SCENE=chests scripts/bench.sh <mc>` instead fills the view with 1025 single chests on a floor and measures
Fast Chests off / on / off.

Runs must have the machine to themselves: llvmpipe uses every core, and a Gradle build running alongside cut
one phase from 127 to 72 FPS (2026-09-26). The script does not guard against that; the operator has to.

**Caveat:** this machine renders on the CPU (Xvfb + Mesa llvmpipe), which makes GPU work very expensive. That
exaggerates savings from drawing less (culling) and says nothing about a real GPU. Compare phases within a run, never
across machines. Vanilla's inactivity limiter (30 FPS after 60 s without input) is switched off for the bench
([debug-log](debug-log.md)).

## Results
Minecraft 1.21.11, [`scripts/bench.sh`](../scripts/bench.sh), this machine (Xvfb + Mesa llvmpipe, CPU rendering), 2026-09-26, MW19 0.2.0 code
(Entity Culling and FPS Boost are unchanged in 0.3.0). Two clean runs; a third ran while a Gradle build competed for
the CPU and is left out (its boost phases fell to 54 and 72 FPS).

| Phase | Run 2: avg FPS | 1 % low | p99 | Run 3: avg FPS | 1 % low | p99 |
|---|---|---|---|---|---|---|
| baseline | 46.2 | 13.8 | 63.8 ms | 48.7 | 28.3 | 32.4 ms |
| culling | 73.9 | 31.0 | 29.6 ms | 78.1 | 44.1 | 18.8 ms |
| boost | 68.2 | 16.7 | 50.1 ms | 74.4 | 21.4 | 37.8 ms |
| culling + boost | **127.0** | 24.0 | 30.1 ms | **140.1** | 31.9 | 25.5 ms |
| baseline-end | 45.5 | 13.7 | 61.7 ms | 51.5 | 19.8 | 29.3 ms |

- **Entity Culling** skipped 91 % of entity draws in this scene (about 9 000 of 10 000 per second) and raised both the
  average (+60 %) and the 1 % lows.
- **FPS Boost** raised the average (+48 % and +53 %), but its 1 % low was worse than baseline in run 3 (21.4 vs 28.3).
  Not explained yet; the GC and hitch counters added since will show whether it is chunk rebuilds after the option change.
- **Both together** gave 2.7× and 2.9× the baseline average.
- baseline-end matches baseline within about 6 %, so the runs did not drift.

These numbers exaggerate what a real GPU gains from drawing less (see the caveat above); they compare phases within
one run, not clients or machines.

### 0.5.0 (2026-09-27, Minecraft 1.21.11, same machine and caveat)
Main scene (villagers and 299 chests behind the wall), one clean run:

| Phase | avg FPS | 1 % low | p99 |
|---|---|---|---|
| baseline | 47.5 | 32.2 | 28.5 ms |
| culling | 72.9 | 47.9 | 19.7 ms |
| fastchests | 47.1 | 31.2 | 27.4 ms |
| boost (Low) | 64.7 | 37.4 | 22.8 ms |
| culling + fastchests + boost | **102.4** | 54.4 | 14.9 ms |
| baseline-end | 45.6 | 31.5 | 28.3 ms |

- Culling skipped 91 % of entity draws and 99.6 % of block entity draws (the chests are all behind the wall).
- Fast Chests alone changes nothing here: the chests are hidden, and on a CPU renderer their block entity work is
  small next to the 134 villagers. The scene where it matters is chests in view:

Chest field (`BENCH_SCENE=chests`, 1025 chests in view, nothing else):

| Phase | avg FPS | 1 % low | p99 |
|---|---|---|---|
| baseline | 50.8 | 36.6 | 24.1 ms |
| fastchests | **61.9** | **47.3** | 20.0 ms |
| baseline-end | 52.8 | 40.2 | 23.8 ms |

Fast Chests: **+22 % average, +29 % 1 % low** with 1025 chests in view. The 0.2.0 table above used a scene without
chests and older code, so the two are not comparable.

### Vulkan (VulkanMod, 1.21.9 – 1.21.11)
This machine cannot measure Vulkan's FPS: the headless tests run VulkanMod on Mesa's software Vulkan driver
(lavapipe), which is also CPU rendering. What the runs show is that it works: `PROD=1 scripts/smoke.sh` with the real
jar passes on 1.21.10 (VulkanMod 0.6.6) and 1.21.11 (0.6.8) with Vulkan running, including Fast Chests under
VulkanMod, and the automatic OpenGL-first start, GPU check and fallback behave as designed (DECISIONS D-024). How much
it gains depends on the graphics card and driver; the Performance page's FPS line shows it on yours.
