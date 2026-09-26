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
  from software GL; real-GPU numbers are Phase 5 (docs/PERF.md).

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
- **Fix:** [`Gfx`](../core/src/main/java/dev/mw19/core/render/Gfx.java) keeps the last backend for measuring (`textWidth`/`lineHeight`); drawing still needs an active pass.
  Verified with xdotool on 1.21.11: toggle, category chip, every sidebar page and a settings page all respond, and
  there are 0 `GUI click failed`.
- **Regression tests:** [`GfxMeasureTest`](../core/src/test/java/dev/mw19/core/GfxMeasureTest.java) (measuring after `end()`), and every smoke run now clicks the FPS toggle through
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
