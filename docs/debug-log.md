# Debug log

> The client was called **Kestrel** until 2026-09-26 and is now **MW19** ([DECISIONS](DECISIONS.md) D-019). Entries before that date keep
> the old names (`Kestrel`, `dev.kestrel`, `Kestrel/`) as they appeared at the time.

See also: [COMPAT_MATRIX](COMPAT_MATRIX.md), [PROGRESS](PROGRESS.md), and the debugging protocol in [CLAUDE.md](../CLAUDE.md).

Every entry follows the protocol: reproduce, state a hypothesis (and what would refute it), isolate, then fix with evidence.

## 2026-09-25 · Smoke log "suspicious lines" on the first 1.21.11 run
- **Repro:** `scripts/smoke.sh 1.21.11`. The checker flagged 11 lines while the run printed `KESTREL SMOKE PASS`.
- **Hypothesis:** the lines come from the Loom dev environment (fake offline account, Realms), not from Kestrel. Refuted if any
  flagged record's logger is `(Kestrel)` or its stack contains `dev.kestrel`.
- **Evidence:** the records are `(Minecraft) Failed to fetch user properties` → `InvalidCredentialsException: Status: 401`
  (dev user "Player685" has no token), `Failed to fetch Realms feature flags` → `Failed to parse into SignedJWT: FabricMC`,
  and a JNA `libflite.so` lookup (text-to-speech natives). None contains `dev.kestrel`.
- **Fix:** the checker now groups log lines into records (timestamp head + stack) and allowlists these heads only. 1.8.9 adds
  FML's dev-only `binary patch set is missing` / `missing any signature data` and `Couldn't initialize twitch stream`.
  Re-ran both targets: `PASS` with 0 suspicious records.

## 2026-09-25 · HUD cost 220 µs/frame on 1.21.11 (budget 300)
- **Repro:** smoke run 1 printed `ownUsPerFrame=219.55` (1.21.11) and `354.3` (1.8.9) with FPS, Keystrokes and Armor.
- **Hypothesis:** the cost is dominated by the number of `fill` calls from scanline rounded rects (≈25 per key box:
  one per corner row plus four anti-aliasing pixels per row). On 1.21.11 each fill is also a render-state object plus a
  pose push/scale/pop. Refuted if cutting fills does not reduce `ownUsPerFrame`.
- **Change:** `Gfx.roundRect` anti-aliases only when the radius is ≥ 7 physical px (invisible below that) and merges
  consecutive rows with the same inset into one fill (a key box goes from ~25 fills to ~5).
- **Result (same smoke scenario):** 1.21.11 **219.6 → 75.6 µs/frame**; 1.8.9 354.3 → 275.4 µs/frame. What remains on
  1.8.9 is mostly vanilla `FontRenderer` immediate-mode glyph draws, which llvmpipe executes on the CPU. These numbers come
  from software GL; real-GPU numbers are Phase 5 ([PERF.md](PERF.md)).

## 2026-09-25 · smoke-all: every 1.21.x run failed in 3–8 s
- **Repro:** `scripts/smoke-all.sh 1.21 … 1.21.10`. All exit=1, no game log.
- **Evidence:** `smoke-out/1.21.4/gradle.log`: `stonecutter.gradle.kts line 3: Version '1.21.11' is not registered.`
- **Cause:** smoke.sh narrows configuration with `-Pkestrel.fabricTargets=<mc>`, which dropped Stonecutter's *active*
  version (1.21.11) from the tree.
- **Fix:** settings.gradle.kts always keeps the active version (parsed from [`fabric/stonecutter.gradle.kts`](../fabric/stonecutter.gradle.kts)) registered.
  Verified by re-running the loop (results below in [COMPAT_MATRIX](COMPAT_MATRIX.md)).

## 2026-09-25 · Zoom (and any default-on module) failed in onEnable at startup
- **Repro:** [`PluginManagerTest`](../core/src/test/java/dev/kestrel/core/PluginManagerTest.java) (boots a real Kestrel on a test platform); stderr: `module zoom failed in enable (1/5)`,
  `NullPointerException: Cannot read field "events" because "k" is null` at `ZoomModule.onEnable`.
- **Hypothesis:** `Kestrel.init` published `instance` only after `start()`, but `start()` → `config.load()` enables
  default-on modules, whose `onEnable` calls `Kestrel.get()`. Refuted if the NPE persists with the instance published first.
- **Fix:** publish the instance before `start()`; late registrations are applied based on a `started` flag, not on
  `instance != null`. Re-ran the test: no `failed in enable` lines. This would have broken Zoom on every real startup;
  the Phase 1 smoke runs did not cover it because Zoom did not exist yet.
- **Also in that run (test bug, not product):** the test's jar builder packaged only `Main.class`, so a plugin using an
  anonymous class failed with `NoClassDefFoundError: paddon/Main$1`. The builder now packages every compiled class.

## 2026-09-25 · 26.1: GameRendererMixin (damage tilt) not applied
- **Repro:** `scripts/smoke.sh 26.1` (snapshot 634166b). Log: `Mixin apply for mod kestrel failed … GameRendererMixin …
  kestrel$tilt … expected 1 invocation(s) but 0 succeeded` (surfaced by `-Dmixin.debug.countInjections`; in production
  the optional mixin would have been skipped silently and Damage Tilt would do nothing).
- **Hypothesis:** 26.1 moved option reads out of rendering into render-state extraction, so `bobHurt` no longer calls
  `Options.damageTiltStrength().get()`. Refuted if javap shows that call in 26.1 `GameRenderer.bobHurt`.
- **Evidence (javap -c, 26.1/26.2/26.3 client jars):** `bobHurt(CameraRenderState, PoseStack)` reads
  `getfield OptionsRenderState.damageTiltStrength:D` (offset 141/141/131); the `Options.damageTiltStrength()` call now
  sits in the extraction method that fills that field (`putfield … damageTiltStrength` at offset 221, 26.1).
- **Fix:** for ≥26.1 the mixin modifies that GETFIELD in `bobHurt` (still visual only; the option is untouched).
  Regression check: the smoke's mixin audit covers `GameRenderer.bobHurt -> kestrel$tilt` on every target.

## 2026-09-25 · 26.1.1: client hung on exit after a passing smoke run (OPEN)
- **Repro:** `smoke-all.sh 26.1 … 26.3` (snapshot 848598d). 26.1.1 printed `KESTREL SMOKE PASS` and `Stopping!`, then the JVM
  stayed alive (last log line `All dimensions are saved`, 19:29:35). 26.1 on the same snapshot exited cleanly (exit=0).
- **Evidence:** Kestrel's shutdown finished. It runs at `Minecraft.close()` HEAD, and `Kestrel/config.json` was written at
  19:29:35.198. `/proc/<pid>/task/*` showed all 32 threads in `futex_do_wait`, including `Render thread`,
  `Sound engine`, and PipeWire client threads `PWEventThread`/`module-rt`. There were no Kestrel threads and no
  `Kestrel-Shutdown` hook thread. jstack could not attach, SIGQUIT printed no dump, SIGTERM was ignored (consistent
  with a JVM already inside its exit path), and ptrace is blocked (yama), so there are no native stacks. SIGKILL ended it.
- **Hypothesis (unconfirmed):** vanilla teardown blocks in `SoundManager.destroy()` on the OpenAL/PipeWire backend
  under Xvfb. Refuted if a stack dump shows the render thread elsewhere, or if the hang reproduces with sound
  disabled.
- **Next:** the smoke now starts a shutdown watchdog after PASS (30 s → dump every Java thread to log/stderr, halt with
  exit 3), and [`smoke.sh`](../scripts/smoke.sh) fails any run whose exit code is non-zero. Re-run 26.1.1 to reproduce and capture stacks.
- **Reproduced (f82c0d6, 20:23):** 26.1.1 hung again after `KESTREL SMOKE PASS` and `Stopping!`, and `timeout 1500` ended it
  (exit 124). That makes 2 of 2 runs on 26.1.1 and 0 on the other 17 targets. The watchdog printed **nothing**: its
  `Thread.getAllStackTraces()` needs a safepoint, like jstack and SIGQUIT, so the VM itself appears to be stuck in its exit
  sequence. The LWJGL build is not the difference (26.1, 26.1.1, 26.1.2 and 26.2 all report 3.4.1-snapshot).
- **Next:** isolate Kestrel's share. (1) Leave the world first, then quit from the title screen. (2) Run the same flow with
  every Kestrel module disabled. (3) Capture native stacks with a ptrace-capable run (e.g. launch under `gdb -batch`) if
  both still hang.
- **Not seen since (2026-09-26):** 26.1.1 exited cleanly in both later full runs (9473849: exit 0 after 101 s; 420a474:
  exit 0 after 80 s) and in every targeted run. Still OPEN, because nothing explains the two hangs; the watchdog and
  smoke.sh's exit-code check stay in place to catch a recurrence.

## 2026-09-25 · 26.2 smoke flagged 4 records after a PASS
- **Repro:** [`smoke-all.sh`](../scripts/smoke-all.sh) on snapshot 848598d; 26.2 printed `KESTREL SMOKE PASS` (audit 20/20) but the checker found 4 records.
- **Evidence:** (1-3) one GLFW error framed as `GL ERROR` / `@ Render` / `65547: X11: Standard cursor shape unavailable`
  at stage 3, which is Xvfb lacking a cursor theme. (4) `Failed to retrieve profile key pair`, an authlib 401 on
  `/player/certificates` for the token-less dev account, stack entirely in authlib/`AccountProfileKeyPairManager`.
  Neither involves `dev.kestrel`.
- **Fix:** the checker allowlists exactly that GLFW message together with its two frame records (any other GL error still
  fails) and the key-pair 401. Replayed against the 26.2 log (now PASS) and 26.1 (still PASS).

## 2026-09-26 · Clicks in the Right Shift menu did nothing (owner report)
- **Repro:** 1.21.11 dev client on Xvfb, real input with xdotool: open the menu, click the Ping toggle. Nothing changes;
  log: `GUI click failed` → `NullPointerException: ... RenderBackend.textWidth(String) because "this.b" is null` at
  `Gfx.textWidth` ← `ModsPage.mouseClicked:208` ← `KestrelScreen.mouseClicked` ← `MouseHandler.onButton`.
- **Cause:** `Gfx.end()` drops its backend after each render pass, but widgets also measure text while handling input
  (between passes). The Mods page measures its category chips before anything else, so **every** click on the default
  page threw, and the guard swallowed it. This has been present since Phase 1. The smoke only rendered the menu and
  never clicked it, so it never saw the failure.
- **Fix:** [`Gfx`](../core/src/main/java/dev/starlight/core/render/Gfx.java) keeps the last backend for measuring (`textWidth`/`lineHeight`); drawing still needs an active pass.
  Verified with xdotool on 1.21.11: toggle, category chip, every sidebar page and a settings page all respond, and
  there are 0 `GUI click failed`.
- **Regression tests:** [`GfxMeasureTest`](../core/src/test/java/dev/starlight/core/GfxMeasureTest.java) (measuring after `end()`), and every smoke run now clicks the FPS toggle through
  real X11 input (`Smoke.requestClick` + the xdotool helper in `smoke.sh`) and fails unless the module flips. It passes
  through GLFW (1.21.11), SDL3 (26.3) and LWJGL 2 (1.8.9). xdotool's `--name` cannot read SDL3's UTF-8 window title,
  so the helper finds the window by `_NET_WM_PID` first.

## 2026-09-26 · Toggle Sprint/Sneak failed at startup when saved as enabled
- **Repro:** the same session. With the modules enabled in the saved profile: `module toggle_sprint failed in enable (1/5)`,
  `NullPointerException: Cannot invoke "Options.toggleSprint()" because "this.mc.options" is null`.
- **Cause:** Fabric runs client entrypoints inside `Minecraft`'s constructor, before `options` exists, and loading the
  profile ran `onEnable` immediately. Smoke runs start from a fresh profile, so they never enabled these at startup.
- **Fix:** `ModuleManager.SUSPEND_STARTING` holds every module registered during startup until the first client tick
  (`gameReady()`), so any module that touches game state in `onEnable` is covered, not only these two.
- **Regression test:** `ModuleManagerTest.profileEnablesWaitForTheGame`.

## 2026-09-26 · Chroma colours (and the new home screen's embers) did not animate
- **Repro:** the home-screen smoke screenshot showed no embers. Code review found the same pattern in `ColorSetting.argb`.
- **Cause:** time was computed in `float` from epoch milliseconds (`now / 1000f`, `millis * chromaSpeed % 4000L`). At
  ~1.79e12 ms a float's ulp is ~131 072 ms, so the fractional part is constant for about two minutes: the embers sat
  off-screen, and chroma hues jumped roughly every two minutes instead of cycling every 4 s. Chroma had been broken
  since Phase 4.
- **Fix:** relative time for the home screen (`now - start`), `double` arithmetic for chroma.
- **Regression test:** [`ColorChromaTest`](../core/src/test/java/dev/starlight/core/ColorChromaTest.java) (the hue changes within 250 ms at a 2026 timestamp, and a cycle is 4 s).

## 2026-09-26 · Benchmark phases capped at 30 FPS
- **Repro:** `scripts/bench.sh 1.21.11`. Every phase after the first minute measured about 29.5 FPS, and the culling counters were
  identical across runs (3540 of 3870 per second = 118 and 129 entities × 30 frames).
- **Cause:** vanilla's inactivity limiter (1.21.2+, `FramerateLimitTracker`: `AFK_LIMIT = 30` after
  `AFK_THRESHOLD_MS = 60000`), and a benchmark gives no input. A methodology bug in the harness, not an MW19 cost.
- **Fix:** the bench writes `inactivityFpsLimit:"minimized"` into its options. FPS Boost does not touch that option,
  because it is the player's power-saving choice.

## 2026-09-26 · Config save failed at exit ("could not save config.json")
- **Repro:** `scripts/smoke.sh 1.8.9 30` (0.3.0 work in progress): the run passed, but the checker flagged
  `could not save config.json` and `could not save Default.json`, both `NoSuchFileException: ./MW19/config.json.tmp -> ./MW19/config.json`,
  logged in the same second as the exit.
- **Hypothesis:** two writers used the same `.tmp` file at once: the smoke's `flush()` on the game thread and a
  debounced background save (or the shutdown hook's `flush()`). The first `move` took the tmp file; the second found
  none. A queued background save could also overwrite a newer `flush()` with an older snapshot.
- **Isolate:** [`ConfigRaceTest`](../core/src/test/java/dev/starlight/core/ConfigRaceTest.java) runs `flush()` from 4 threads
  40 times each. Against the 0.2.0 [`ConfigManager`](../core/src/main/java/dev/starlight/core/config/ConfigManager.java) it fails (errors logged); with the fix it passes.
- **Fix:** `ConfigManager.writeBoth` writes under one lock and skips snapshots older than the last one written;
  profile writes take the same lock.
- **Regression test:** `ConfigRaceTest`.

## 2026-09-26 · 1.21.9 smoke flagged vanilla's key fetch timing out
- **Repro:** 0.4.0 `smoke-all.sh` (snapshot b2fc30b): 1.21.9 printed `MW19 SMOKE PASS` and audit 27/27, but the checker
  flagged `[Yggdrasil Key Fetcher/ERROR] Failed to request yggdrasil public key`, caused by
  `MinecraftClientException: Failed to read from https://api.minecraftservices.com/publickeys due to Connect timed out`.
- **Cause:** authlib (vanilla) fetches Mojang's service public keys at start; the connection timed out on this
  network. No MW19 frame is in the trace, and the other 17 targets did not hit it in the same run.
- **Fix:** smoke.sh allowlists that record head, like the existing dev-account `Failed to retrieve profile key pair`.
  A rerun of 1.21.9 is the regression check (below).

## 2026-09-26 · Compatibility run with Sodium: three setup problems, then PASS
- **Run 1:** `WITH_MODS="sodium immediatelyfast ferrite-core lithium" smoke.sh 1.21.11` crashed at start:
  `Mixin apply for mod sodium failed sodium-frapi.mixins.json:BlockRenderDispatcherMixin ... @Redirect ... could not find
  any targets`. MW19 has no mixin on that class. The jars had been dropped into `run/<mc>/mods`, which leaves remapping
  to Fabric Loader's runtime remapper in a Mojang-mapped dev run. **Fix:** `-Pmw19.withMods=<dir>` adds them with Loom's
  `modLocalRuntime`, remapped at build time ([fabric/build.gradle.kts](../fabric/build.gradle.kts)).
- **Run 2:** Fabric Loader refused to start: Sodium 0.8.14 requires `fabric-block-view-api-v2`,
  `fabric-rendering-fluids-v1` and `fabric-resource-loader-v0`, i.e. **Fabric API**. So the MW19 Performance packs
  need Fabric API too ([`scripts/mrpack.py`](../scripts/mrpack.py) now lists it).
- **Run 3:** Fabric API as a file dependency still left its modules missing: they are nested jars inside the Fabric
  API jar, which a plain file dependency does not unpack. **Fix:** `-Pmw19.fabricApi=<version>` pulls it from Fabric's
  Maven (smoke.sh looks the version up on Modrinth).
- **Run 4: PASS** on 1.21.11 with fabric-api 0.141.6, sodium 0.8.14, immediatelyfast 1.14.3, lithium 0.21.4 and
  ferritecore 8.2.0 (55 mods): every smoke check, and the mixin audit 27/27 (Entity Culling still wired under Sodium).
- **26.3:** the first try failed while configuring `:fabric:1.21.11`: Stonecutter always configures its active version,
  which then got 26.3's mods to remap. The extra mods now apply only to the requested target. Then **PASS** with
  fabric-api 0.161.0, sodium 0.9.2, immediatelyfast 1.17.1, lithium 0.26.1 and ferritecore 9.0.0 (audit 28/28).

## 2026-09-26 · 1.8.9 entity culling cast its rays from the player's feet
- **Found by reading** (while adding block entity culling), not by a report. `RenderGlobal.renderEntities` (javap)
  passes `RenderManager.shouldRender` the render-view entity's interpolated `prevPos/pos` (feet), and MW19's
  [`RenderManagerMixin`](../legacy/src/main/java/dev/starlight/forge/mixin/RenderManagerMixin.java) used that as the ray origin. The camera sits `eyeHeight` above it (and further back in third
  person), so a mob visible over a one-block wall could be judged hidden and skipped. The Fabric targets were right:
  there `LevelRenderer` passes `Camera.getPosition()`.
- **Fix:** 1.8.9 adds `ActiveRenderInfo.getPosition()` (the camera's offset from that interpolated position, updated
  every frame) to the origin, for entities and the new tile entity culling alike.
- **Test:** covered by the smoke run's culling path only; the ray logic itself is unit-tested in [`OcclusionTest`](../core/src/test/java/dev/starlight/core/OcclusionTest.java).

## 2026-09-26 · VulkanMod could not be tested headless (hang, then crash)
- **Repro:** `WITH_MODS=vulkanmod smoke.sh 1.21.11` under Xvfb. Xvfb has no DRI3, so RADV's X11 presentation died
  (`XIO: fatal IO error`); with `MESA_VK_WSI_DEBUG=sw` the render thread hung in `vkWaitForFences` after
  `Failed to submit draw command buffer: VK_ERROR_UNKNOWN` (jstack, debug.log).
- **Hypothesis:** the hardware driver cannot present into Xvfb; a software Vulkan device can.
- **Fix (test setup):** Mesa's lavapipe, extracted from the Arch `vulkan-swrast` package of the installed Mesa
  version (26.2.2) and selected with `VK_DRIVER_FILES`. **PASS** on 1.21.11 with VulkanMod 0.6.8, mixin audit 28/28.
  The game's own screenshots are blank under VulkanMod (it does not render into the target they read), so smoke.sh
  now also grabs the X11 screen for every screenshot (`x11-*.png`); those show the world, HUD and menus drawn.
- **Found on the way:** without any Vulkan driver VulkanMod ends the game (`Failed to create instance:
  VK_ERROR_INCOMPATIBLE_DRIVER`), so bundling it needed a launch-time switch (DECISIONS D-024).

## 2026-09-26 · Fast Chests: a dark line on double chest lids
- **Repro:** the smoke's chest scene, front view: the double chest's lid showed a faint dark line at the seam that
  vanilla's renderer does not draw (zoomed side-by-side of `6b-chests-vanilla` vs `6a-chests-fast`).
- **Hypothesis:** mipmaps. Entity textures are drawn without them; block atlas sprites have them. The left half's lid
  top region starts on column 29 (odd), so its first mip-1 texel averages columns 28 and 29, and 28 is the dark lid
  underside region.
- **Fix:** [`scripts/fast-chests.py`](../scripts/fast-chests.py) drops the seam-side column of those two faces (left half's lid top, right half's
  bottom). The rerun's zoom shows no line; the top view (`6c`/`6d`) matches vanilla's plank pattern and rims.

## 2026-09-27 · The renderer switch did not load in dev runs
- **Repro:** after adding [`RendererSwitch`](../fabric/src/main/java/dev/starlight/fabric/RendererSwitch.java) as a language adapter, the dev smoke died at start: `Failed to instantiate
  language adapter ... can't load class dev.mw19.fabric.RendererSwitch at .../build/classes/java/main as it hasn't been
  exposed to the game (yet? The system property fabric.classPathGroups may not be set correctly in-dev)`. Production
  launches had passed.
- **Hypothesis:** in dev the mod's classes directory is not grouped with its resources (where fabric.mod.json is), so
  Fabric only exposes it with the game's classpath, after language adapters are created.
- **Fix:** Loom's `mods { register("mw19") { sourceSet(main) } }` in [fabric/build.gradle.kts](../fabric/build.gradle.kts). The dev smoke passes again.

## 2026-09-27 · The Vulkan check failed with "Vulkan has already been created"
- **Repro:** the first production start with VulkanMod kept off wrote `vulkan-probe.txt` = `no (IllegalStateException:
  Vulkan has already been created.)`.
- **Hypothesis:** something in the OpenGL session had already loaded LWJGL's Vulkan binding (GLFW's Vulkan support), so
  `VK.create()` refused a second time.
- **Fix:** [`VulkanProbe`](../fabric/src/main/java/dev/starlight/fabric/VulkanProbe.java) shares an already created binding and leaves it loaded. Rerun: `no (llvmpipe ... is not
  enough)` on the CPU driver (correct: not a real GPU), and `ok llvmpipe ...` with `VK_SOFT=1`.

## 2026-09-27 · A crashed first start skipped the automatic preset
- **Repro:** forced Vulkan on a machine without Vulkan (`PROD=1 RENDERER=vulkan VK_DRIVER_FILES=none smoke.sh`), then a
  second start with the state kept: the second start ran on OpenGL as designed, but failed the smoke's check that the
  first-run preset was applied.
- **Hypothesis:** "fresh install" meant "no saved profile", and the shutdown hook saves the profile even when the game
  crashes, so the start after a crashed first start was no longer fresh.
- **Fix:** a fresh install records `video.fresh` until the first-run step has actually run
  ([`VideoPresets`](../core/src/main/java/dev/starlight/core/VideoPresets.java)). Regression test `aFirstStartThatCrashedStillGetsThePresetNextTime`; the two-start run
  then passes. (The same run also showed smoke.sh overwrote `config.json` even with `KEEP_STATE`; it now merges.)

## 2026-09-27 · The bundled VulkanMod checks Modrinth for updates at every start
- **Found by** searching the bundled jars for network code before writing [THIRD_PARTY](THIRD_PARTY.md): `UpdateChecker` sends a request
  to `api.modrinth.com` from VulkanMod's client initializer, unconditionally (its source at the pinned commits).
- **Fix:** [`VulkanUpdateMixin`](../fabric/src/main/java/dev/starlight/fabric/mixin/VulkanUpdateMixin.java) (`@Pseudo`, only when VulkanMod runs) cancels it: MW19 uses the network only
  when the player asks. Checked in the exported class of a Vulkan run (`checkForUpdates` calls `mw19$offline` first);
  [`scripts/mixin-audit.py`](../scripts/mixin-audit.py) checks it whenever VulkanMod was loaded.

## 2026-09-27 · Hitboxes stayed on with the module off (1.21.9+)
- **Repro:** the smoke's chest scene showed entity hitboxes (and look vectors) although a fresh MW19 config has the
  Hitboxes module off. The run directory had been used by earlier runs that switched every module on.
- **Hypothesis:** on 1.21.9+ the module set vanilla's `ENTITY_HITBOXES` debug entry, and `DebugScreenEntryList.setStatus`
  saves the debug profile at once (javap: `rebuildCurrentList` then `save`). A game closed with the module on keeps
  hitboxes on for good, whatever MW19's config says later.
- **Fix:** the module no longer changes the entry. [`HitboxesMixin`](../fabric/src/main/java/dev/starlight/fabric/mixin/HitboxesMixin.java) reports it as enabled while the module is on,
  and the platform refreshes the debug renderer's list on each toggle (`LevelRenderer.debugRenderer` up to 26.1,
  `levelExtractor.debugRenderer` on 26.2+). The player's own F3+B choice stays untouched. The mixin audit checks it.

## 2026-09-27 · 26.x: the new Mods page threw on the title screen
- **Repro:** smoke-all: every 26.x target failed with 130-160 `GUI render failed` records: `NullPointerException:
  Components not bound yet` from `new ItemStack(item)` in `FabricPlatform.itemIcon`, called by the Mods page's tiles.
- **Hypothesis:** 26.x binds item data components when the first world loads; before that (title screen) an
  `ItemStack` cannot be made. The HUD only asks for icons in a world, so nothing hit it before the tiles.
- **Fix:** `itemIcon` returns an empty, uncached icon when that happens and tries again next time; tiles show the
  module's initial until then. Rerun: 26.1, 26.1.1, 26.1.2, 26.2 and 26.3 pass.

## 2026-09-27 · 26.x warns about chest sprites in two atlases
- **Seen in** the 26.x smoke logs: `Duplicate sprite minecraft:entity/chest/... from atlas ...chest.png, already defined
  in atlas ...blocks.png. This will be rejected in a future version` (one per chest texture per resource reload, only
  with Fast Chests on).
- **Checked:** 26.3's `AtlasManager` still keys sprites by atlas and name (javap: the duplicate check is a separate
  map filled with `putIfAbsent`, then the warning), and the chest scene renders identically with Fast Chests on and
  off. So it works on 26.1–26.3; a future Minecraft that enforces this will need Fast Chests to draw from its own
  sprite names (DECISIONS D-025).

## 2026-09-27 · Fabric API's renderer would stand down for a VulkanMod that is kept off
- **Found by reading** Fabric API's Indigo (`IndigoMixinConfigPlugin`, 1.21.11 branch): Indigo disables itself when any
  mod's metadata has `fabric-renderer-api-v1:contains_renderer`, which VulkanMod's has. With VulkanMod kept off (OpenGL
  session) and Fabric API installed, mods that use the Fabric Rendering API would have no renderer.
- **Fix:** RendererSwitch removes that custom value from VulkanMod's metadata along with its entrypoints and mixin
  configs (Indigo reads it later, from its mixin plugin).

## 2026-09-27 · Optimizer smoke stage: the anchor check failed beside Fabric API
- **Repro:** `WITH_MODS="fabric-api" scripts/smoke.sh 1.21.11`: `optimizers exploded=FAIL (still Block{minecraft:fire}...
  where the anchor exploded)`. The same stage without Fabric API had passed with air there.
- **Hypothesis:** the ghost was replaced as intended. A respawn anchor's explosion lights fires (`createFire`), and a
  fire can land on the anchor's own spot above the obsidian. The server's state there is then fire, not air, and the
  check only accepted air. It depends on the explosion's randomness, not on Fabric API.
- **Evidence:** the block reported is `minecraft:fire` (neither the purple glass ghost nor the anchor). The steps before
  it passed in that run (codec, hit, removed, anchor).
- **Fix:** the "gone" step checks that the ghost was replaced by the server's answer: neither the ghost nor the anchor.
  It names the block it found. The rerun beside Fabric API is below in COMPAT_MATRIX.

## 2026-09-27 · Production 1.21.5 failed on a Mojang profile lookup timeout
- **Repro:** the 0.6.0 production round: `prod 1.21.5 FAIL: 1 suspicious log record(s)`, with every smoke check
  (optimizers included) passing. The record: `[Download-1/WARN]: Couldn't look up profile properties for
  00000000-0000-0000-0000-000000000000`, caused by `SocketTimeoutException: Read timed out` from
  `sessionserver.mojang.com`.
- **Hypothesis:** this is vanilla's own profile fetch for the offline test player at start (`Minecraft` constructor,
  `fetchProfile` on the non-critical IO pool, frame `class_310.method_53464`), and the network timed out. MW19 takes no
  part in it.
- **Fix:** smoke.sh allowlists that record head, as it already does for the similar "Failed to fetch user properties"
  and yggdrasil key timeouts. It was swapped in by rename, because a running smoke.sh keeps reading its old copy.
  1.21.5 was rerun after the round (COMPAT_MATRIX).

## 2026-09-27 · 1.21–1.21.8: the pause menu's MW19 row sat under the title, screen-wide
- **Found** while checking the new menu look: on 1.21 the "MW19 Menu | Packs" row was right under "Game Menu", each
  button half the screen wide, above "Back to Game". The 0.6.0 (and 0.5.0) screenshots show it on 1.21–1.21.8; from
  1.21.9 it sits correctly. The smoke only counted the two buttons, so it passed.
- **Hypothesis:** `pauseRow` anchors the row to the topmost visible widget at least 150 wide, meaning "Back to Game".
  Up to 1.21.8 the "Game Menu" title is a text widget as wide as the screen among the screen's children, so it won.
- **Evidence:** the row's x and width match the title widget's (0, screen width), and the misplacement ends exactly
  where 1.21.9 stopped adding the title as a child.
- **Fix:** the anchor is the topmost wide *button*, never the MW19 row itself. The smoke's `pauserow` self-test now
  fails unless both MW19 buttons are right under "Back to Game" and inside its width.

## 2026-09-27 · A skin change did not show in game (owner report, 1.21.11)
- **Report:** "skin changing doesnt actually change it".
- **Reproduction:** not possible here. An upload needs a Microsoft account, and the test setup uses none (CLAUDE.md
  forbids reading the launcher's accounts). The upload request itself is covered: a unit test against a local fake
  endpoint checks the multipart body and the bearer token.
- **Hypothesis:** Mojang takes the new skin, but the game never learns about it. The game fetches the player's profile
  once, at start, and keeps it. MW19's own-skin preview and the player list entry (the player in the world) both read
  that profile, and the integrated server of each new world is started with it.
- **Evidence** (javap, 1.21–26.3): `Minecraft.getGameProfile()` returns `profileFuture.join().profile()`, filled once
  by the startup lambda's `sessionService.fetchProfile(uuid, true)`. `PlayerInfo.skinLookup` is made once from the
  profile by `createSkinLookup`. Nothing refreshes either of them. authlib's `fetchProfile(uuid, true)` bypasses its
  own profile cache, so fetching again returns fresh data.
- **Fix:** after the upload is accepted, `Skins.refreshOwnSkin`:
  - Fetches the signed profile again (1 s, then every 5 s, for up to a minute) until Mojang serves the new skin. The
    new skin's texture id comes from the upload's answer.
  - Puts that profile into the game's `profileFuture` (`obtrudeValue`), and the player list entry gets a new skin
    lookup made from it.

  The Skins page then says so; if a minute passes, it says the new skin shows after a restart.
- **Regression tests:**
  - The upload test checks that the texture id is read from Mojang's answer.
  - A unit test covers reading the texture from a profile's textures property.
  - The smoke's `skinrefresh` self-test swaps a fresh profile and the player's entry lookup in, then restores the start
    state, on every Fabric target.

  Not tested: a real upload to Mojang (no account here).

## 2026-09-27 · Modrinth: the listing text stopped updating ("Slug collides with other project's id!")
- **Repro:** the 0.7.0 upload replaced all 18 versions (hashes match dist), but the project's summary and description
  stayed 0.5.0's. A re-run printed `project: 400 {"error":"request_error","description":"Slug collides with other
  project's id!"}`.
- **Hypothesis:** [`scripts/modrinth.py`](../scripts/modrinth.py) sends `slug: "mw19"` in every project PATCH. Modrinth now validates the slug
  even when it is the project's current one, and refuses it. The 400 drops the whole PATCH, so the title, summary and
  description go with it. The icon (a separate call) and the versions were unaffected.
- **Fix:** the script reads the project first and sends the slug only when it differs. The re-run answered `project:
  204`, and the description on Modrinth now matches docs/MODRINTH.md byte for byte.

## 2026-09-27 · 1.8.9 after the rename to Starlight: no mixin applied
- **Repro:** the first 1.8.9 smoke run after the rename failed with `mixin audit 1.8.9: 0/15 wired` and `Error
  encountered reading mixin config mixins.starlight_client.json: ... The specified resource
  'mixins.starlight_client.json' was invalid`. The run then failed on the missing pause-menu backdrop.
- **Hypothesis:** the legacy build names the mixin config after the mod id (`mixins.$modId.json`, in the jar manifest's
  `MixinConfigs`). The mod id became `starlight_client` (not `starlight`, D-033), but the file was renamed to
  `mixins.starlight.json`.
- **Fix:** the file is [`mixins.starlight_client.json`](../legacy/src/main/resources/mixins.starlight_client.json). The next run: PASS, `mixin audit 1.8.9: 15/15 wired`.
- **Regression test:** the smoke run's mixin audit already fails any run where a hook is not wired, which is how this
  was caught.

## 2026-09-27 · Bind profiles would have been lost under a Flatpak launcher (found before release)
- **Found by:** checking where the owner's launchers let the game write. The owner runs Prism and Dawn as Flatpaks
  (CLAUDE.md), and bind profiles were kept in `~/.starlight/`.
- **Evidence:** `flatpak info --show-permissions org.prismlauncher.PrismLauncher` grants no home-folder access. In Prism's
  sandbox (`flatpak run --command=sh`), `$HOME` is `/home/luna` but holds only `Downloads`. `mkdir ~/.starlight-sandbox-test`
  succeeds there, yet the folder does not exist on the host afterwards: the sandbox's home is temporary. `Files.isWritable`
  says yes, so the old fallback to the instance folder never triggered, and every profile would have been gone when the
  game closed. `XDG_DATA_HOME` in the sandbox is `~/.var/app/org.prismlauncher.PrismLauncher/data`, which lasts.
- **Fix:** profiles live in `$XDG_DATA_HOME/starlight/` when that is set, else `~/.starlight/`
  ([`BindProfiles`](../core/src/main/java/dev/starlight/core/binds/BindProfiles.java)). Under a Flatpak launcher they are shared by that launcher's instances. The Keybinds page says
  "Shared by your instances" or, when they had to stay in the instance, "This instance only".
- **Checked in the sandbox:** a probe using the built core jar, run with Prism's own Java inside Prism's sandbox
  (`flatpak run --filesystem=<probe>:ro --command=sh`), saved a profile to
  `~/.var/app/org.prismlauncher.PrismLauncher/data/starlight/bind-profiles.json`, and the file was still there on the
  host after the sandbox exited. The probe's file was then removed.
- **Regression test:** `BindProfilesTest.aFlatpakLauncherKeepsThemInItsOwnDataFolder`. Not tested: the game itself
  started from Prism (the tests never use the owner's launchers or accounts).

## 2026-09-27 · 26.2+: no Starlight backdrop on the pause menu (since 0.7.0)
- **Repro:** the 0.8.0 production screenshots of the pause menu (`x11-starlight-smoke-3b-pause.png`) show the night-sky
  tint and stars over the world on 1.21–26.1, but on 26.2 and 26.3 only the world, with the HUD at full brightness
  on top. The 0.7.0 run's 26.3 screenshot is the same, so this shipped in 0.7.0. The smoke check passed anyway.
- **Hypothesis:** the Starlight backdrop hooks `Screen.extractBackground` (26.x) at HEAD. If 26.2's pause menu no longer
  calls that method, the hook never runs for it. The smoke check read a flag that any earlier screen (the world-loading
  screens) had already set.
- **Evidence** (javap): 26.1's `PauseScreen.extractBackground` is `if (showPauseMenu) super.extractBackground(...)`.
  26.2 and 26.3 replace the `super` call with their own `extractBlurredBackground` (when topmost) and
  `extractMenuBackground`, then `hud.extractDeferredSubtitles()`. 26.2's `Screen.extractBackground` also ends with
  `hud.extractDeferredSubtitles()`, and gives the new `isInGameUi()` screens (containers, signs, books, command, structure,
  jigsaw and test blocks) a see-through background.
- **Fix:**
  - 26.2+: a `PauseScreen.extractBackground` hook ([`PauseScreenMixin`](../fabric/src/main/java/dev/starlight/fabric/mixin/PauseScreenMixin.java)) draws the backdrop when the menu is shown.
  - A replaced background still draws the deferred subtitles. Before this, subtitles went missing under every screen
    with the Starlight backdrop on 26.2+.
  - `isInGameUi()` screens keep vanilla's see-through background ([`StarlightFabric`](../fabric/src/main/java/dev/starlight/fabric/StarlightFabric.java).backdrop).
- **Regression test:** the smoke clears the flag when it opens the pause menu, so only the pause menu can set it. On
  26.3 with the new hook removed, the run fails with "the pause menu did not draw the Starlight backdrop" (and the
  mixin audit, which now expects the hook on 26.2+, reports 44/47). With the hook, it passes (47/47) and the screenshot
  shows the backdrop.

## 2026-09-28 · Production 1.21.3: "Player lost connection: Internal Exception: ClosedChannelException" at shutdown
- **Repro:** one production run in the 0.8.0 round (1.21.3) failed only on this log record, after `Starlight SMOKE PASS`
  with every check ok. The machine was under load (memory nearly full, the round at the lowest CPU priority).
- **Hypothesis:** a vanilla shutdown race, not ours. The smoke quits with `Minecraft.stop()` (what closing the window
  does) while the player is in the singleplayer world. Normally the integrated server reads the client's disconnect
  first and logs `Player lost connection: Disconnected`. If it is still sending when the channel closes, it logs the
  closed channel as the reason instead.
- **Evidence:** in the log the record comes right after `[Render thread/INFO]: Stopping!` and is followed by `Player left
  the game` and `Stopping singleplayer server as player logged out` (the world is saved as usual). Every other run
  kept (26 dev, 14 production) logged `lost connection: Disconnected` at the same point.
- **Fix** (test harness only): smoke.sh accepts exactly this record, and only after the client's `Stopping!`.
- **Regression test:** the log check on this run's log now passes. On a copy with the same record moved before
  `Stopping!` it still fails (`1 suspicious log record(s)`).

## 2026-09-28 · After an upgrade from MW19, switching Fast Chests off did not take effect
- **Repro:** the upgrade test (production 1.21.11). MW19 0.7.0 with Fast Chests on (`mw19/fast_chests` in options.txt),
  then Starlight 0.8.0 in the same folder. At start vanilla logs `Removed resource pack mw19/fast_chests from options`,
  and Starlight's pack is active at once (`SMOKE: fast chests ok`). But after the smoke switches Fast Chests off, the
  resources never reload: `Fast Chests: resources did not reload within 90 s after turning it off`. Fresh installs pass
  the same step.
- **Hypothesis:** `FastChests.set` leaves the reload to vanilla's `Options.updateResourcePacks`, which reloads only
  when the saved pack list changed. The Fast Chests pack is *required*, so at startup the pack repository selects it
  without it ever being in that list. With the stale MW19 id dropped, the list is `["vanilla"]` before and after
  switching the pack off, so nothing reloads. The chest models stay baked while the block entity renderer draws the
  chests again, so every chest is drawn twice until the next reload.
- **Evidence** (javap, 1.21.11 `Options`): `updateResourcePacks` copies `resourcePacks`, rebuilds it from the selected
  packs, saves, and calls `Minecraft.reloadResourcePacks()` only `if (!new.equals(old))`. `loadSelectedResourcePacks`
  removes unknown ids from `resourcePacks` and never adds the required packs the repository selects.
- **Fix:** [`FastChests`](../fabric/src/main/java/dev/starlight/fabric/FastChests.java).set reloads resources itself when `updateResourcePacks` left the saved list unchanged.
- **Regression test:** the upgrade test (MW19 0.7.0 → Starlight, Fast Chests on) runs the whole smoke, including Fast
  Chests off and on again, in the upgraded folder.
