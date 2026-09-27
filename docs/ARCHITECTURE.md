# Architecture

See also: [DECISIONS](DECISIONS.md) (why), [CODE_MAP](CODE_MAP.md) (every file), [RULES_MATRIX](RULES_MATRIX.md) (what the server-rules engine enforces).

```
api/     Java 8, zero MC imports: Module/HudModule base classes, settings, events, Renderer, Http (the types core's
         modules are written against; the plugin system that also used them was removed in 0.5.0, D-026).
core/    Java 8, zero MC imports: module manager, event bus, config/profiles/migrations,
         GUI widget tree + pages + HUD editor, animation, themes, toasts, server-rule engine, JSON codec,
         HTTP client (LRU+TTL cache, rate limiter), the built-in modules' logic.
fabric/  Stonecutter tree → versions/<mc>/ for 1.21 … 26.3. Platform adapters + mixins, the renderer switch, and the
         Fast Chests pack (resources/mw19packs). 1.21.9–1.21.11 nest VulkanMod (D-024).
legacy/  Separate Gradle build: Forge 1.8.9 platform adapters + mixins. Consumes api/ and core/ sources.
```

`buildAll` → `dist/MW19-<modver>+mc<mc>.jar` ×18, `dist/sources/` (the bundled VulkanMod's source), `dist/prism/*.zip`,
`dist/SHA256SUMS`.

## Layering rule
`platform → core → api`. Core never imports a Minecraft class, and the build fails if it does: `core` and `api` have no
Minecraft on their compile classpath. Everything version-specific sits behind the interfaces below, and Stonecutter
`//? if` blocks live **only** in `fabric/src` adapter classes and mixins.

## Platform contract (core/src/main/java/dev/mw19/core/platform)

It was designed against 1.8.9 (LWJGL2, immediate-mode GL, MCP names, Forge events) and 26.x (Blaze3D, retained GUI
extraction, no raw GL) at the same time.

| Interface | Responsibility | 1.8.9 | 1.21.x | 26.x |
|---|---|---|---|---|
| [`Renderer`](../api/src/main/java/dev/mw19/api/render/Renderer.java) (api) | rect, gradient, text, item icon, clip, push/pop/translate/scale, guiScale | `Gui.drawRect`, `FontRenderer`, `GlStateManager`, `RenderItem` | `GuiGraphics` (+ pose) | `GuiGraphicsExtractor` |
| [`ScreenHost`](../core/src/main/java/dev/mw19/core/platform/ScreenHost.java) | open/close our screen, screen kind, scaled size | `GuiScreen` subclass | `Screen` subclass | `Screen` subclass (`gui.setScreen` on 26.2+) |
| `InputBackend` | key/mouse state, key names, vanilla binds (conflicts), clipboard | LWJGL2 `Keyboard`/`Mouse` (codes mapped to GLFW) | GLFW via `InputConstants` | same |
| `PlayerAccess` | pos/rot, ping, armor/hands/inventory counts, effects, hurt state, reducedDebugInfo | `EntityPlayerSP` | `LocalPlayer` | `LocalPlayer` |
| `WorldAccess` | loaded?, server address, dimension, time, tab players, scoreboard lines | `WorldClient` | `ClientLevel` | `ClientLevel` |
| [`ChatAccess`](../core/src/main/java/dev/mw19/core/platform/ChatAccess.java) | local message, explicit send (user-initiated only) | `GuiNewChat` | `ChatComponent` | same |
| `GameOptions` | gamma, fov, hideGui, perspective, guiScale, sensitivity | `GameSettings` | `Options` | `Options` |
| [`ModList`](../core/src/main/java/dev/mw19/core/platform/ModList.java) | loaded mod ids and versions → `compat.*` flags | `Loader` | `FabricLoader` | `FabricLoader` |

Key codes are **GLFW constants** everywhere in core. The 1.8.9 adapter translates LWJGL2 codes through a table. Mouse
buttons are `1000 + button`.

**Hooks in (platform → core)** are plain static calls on `dev.mw19.core.Mw19`. They cost nothing when no module is
interested. Each wraps its work in the guard described below.
- events: `tick(start/end)`, `renderHud(Renderer, partial)`, `key(code, action, mods)`, `mouse(button, action)`,
  `scroll(delta) → consumed?`, `chat(ChatLine) → keep/modify`, `joinServer(addr)`, `leaveServer()`, `screenOpened(kind)`,
  `attack(entityId)`, `screenshot(path)`, `frame(nanos)`
- queries: `fovMultiplier()`, `gammaOverride()`, `hitColor()`, `renderOwnName()`, `vanillaButton(...)` (menu style),
  `crosshairOverride()`, `fireOverlayOffset()`, `damageTiltScale()`, `particleMultiplier()`, `freelook*()`

## Error isolation
Every module callback (`onEnable/onTick/onRender/...`) runs inside `Guard.run(module, phase, fn)`:
- It catches `Throwable` (except VirtualMachineError), logs `[MW19] module <id> failed in <phase>` with the stack
  trace, and increments the module's failure counter.
- At **5 failures** the module is disabled for the session with a toast "‹name› was disabled after repeated errors" and
  a log line. It is not persisted, so a restart retries.
- Each platform hook wraps its whole body too, so an error inside core can never propagate into Minecraft's frame.
- The crash-report section lists enabled modules (Fabric: mixin into `Minecraft.fillReport`; Forge: `ICrashCallable`).

## Event bus
Listeners are indexed by exact event class into pre-sized arrays, and dispatch is a plain indexed loop. Hot events
(`RenderHudEvent`, `TickEvent`) are **singleton mutable objects reused every frame**, so dispatch allocates nothing.

## Config
`<gameDir>/MW19/config.json` (global) + `profiles/<name>.json`, both carrying `"schema": N`.
- **Migrations**: an ordered list `N → N+1` of pure functions over the JSON tree, unit-tested with fixtures.
- **Atomic writes**: write `name.json.tmp`, fsync, `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`. If the platform lacks
  atomic move, fall back to a non-atomic replace.
- **Debounced autosave**: a dirty flag plus one background thread. It saves 1.5 s after the last change and flushes on shutdown.
- **Rolling backups**: `backups/<file>.<yyyyMMdd-HHmmss>.json`, newest 10 kept. A corrupt file on load is moved to
  `*.corrupt-<ts>` and the newest backup is loaded.
- **Profiles**: unlimited; switching is instant (in memory). Export = `MW19-P1:` + base64(deflate(json)) + `:` + crc32.
  Import validates the CRC and schema, then migrates. Auto-switch maps a server pattern to a profile name.

## Renderer switch (1.21.9 – 1.21.11)
[`RendererSwitch`](../fabric/src/main/java/dev/mw19/fabric/RendererSwitch.java) is declared as a Fabric language adapter, because Fabric creates adapters after picking the mods
and before registering their entrypoints and mixin configs. It decides from files in `<gameDir>/MW19/`
(`renderer.txt`, `vulkan-probe.txt`, `vulkan-starting`/`vulkan-failed`) whether the nested VulkanMod runs; if not, it
empties VulkanMod's entrypoints and mixin configs in Fabric's metadata. Nothing Minecraft-related may load there. The
platform clears `vulkan-starting` once a menu or world has been up for 2 s and starts the one-time
[`VulkanProbe`](../fabric/src/main/java/dev/mw19/fabric/VulkanProbe.java) in OpenGL sessions (D-024). In dev runs Loom's `mods` block groups the mod's classes and resources so
Fabric exposes them that early.

## Fast Chests
A built-in resource pack in the Fabric jar (`mw19packs/fast_chests`, generated by [`scripts/fast-chests.py`](../scripts/fast-chests.py)) holds
blockstates, block models and a block-atlas source for the chest textures. [`FastChests`](../fabric/src/main/java/dev/mw19/fabric/FastChests.java) offers it from the client
pack scan while the module is on; the block entity renderer then skips the chests it covers (D-025).

## Ported optimizers (1.21+)
`fabric/.../port/` holds the only third-party-derived code in the Fabric tree (D-028, MIT, credited in each file):
[`CrystalOptimizer`](../fabric/src/main/java/dev/mw19/fabric/port/CrystalOptimizer.java) (Marlow's Crystal Optimizer) and [`AnchorOptimizer`](../fabric/src/main/java/dev/mw19/fabric/port/AnchorOptimizer.java) (Hero's Anchor Optimizer). Their mixins stay
one-liners into those classes and do nothing while `Hooks.crystalOptimizer` / `Hooks.anchorOptimizer` are off. The
crystal optimizer's plugin messages use vanilla's custom-payload codec, not Fabric API:
- **Incoming:** [`DiscardedPayloadMixin`](../fabric/src/main/java/dev/mw19/fabric/mixin/DiscardedPayloadMixin.java) supplies a codec for the two upstream receive channels. Vanilla asks it
  only for ids nobody registered.
- **Outgoing:** [`PayloadCodecMixin`](../fabric/src/main/java/dev/mw19/fabric/mixin/PayloadCodecMixin.java) writes MW19's own messages before any id lookup. This matters beside
  Fabric API, which has its own codec for `minecraft:register`.
- **Handling:** [`CrystalPacketListenerMixin`](../fabric/src/main/java/dev/mw19/fabric/mixin/CrystalPacketListenerMixin.java) moves each message onto the game thread.

## GUI
A retained widget tree in core ([`Widget`](../core/src/main/java/dev/mw19/core/gui/Widget.java): bounds, children, `render(Renderer, mouse, dt)`, input handlers, focus).
Pages: Mods (tiles or list), HUD Editor, Profiles, Keybinds, Skins, Packs, Host World, Server Rules, Performance,
Themes, About. A closed menu is released (textures, pages) and rebuilt on the next open. Minecraft's own buttons are
redrawn in the menu style by [`ButtonStyleMixin`](../fabric/src/main/java/dev/mw19/fabric/mixin/ButtonStyleMixin.java) / [`GuiButtonMixin`](../legacy/src/main/java/dev/mw19/forge/mixin/GuiButtonMixin.java) (D-027).
Animations use [`Anim`](../core/src/main/java/dev/mw19/core/gui/Anim.java) (value, target, 150–250 ms, ease-out-cubic) and are ticked with frame dt, which allocates nothing.
Themes are token sets (bg, surface, surface2, border, text, textDim, accent, good, warn, bad) with presets and a
user accent colour. [`MenuStyle`](../core/src/main/java/dev/mw19/core/gui/MenuStyle.java) is MW19's own look (D-029): keycap buttons and sliders, and the ember backdrop
with its block skyline. Minecraft's screens get it through `Mw19.vanillaButton` / `vanillaSlider` / `menuBackdrop`
(ButtonStyleMixin, SliderStyleMixin and ScreenBackdropMixin; on 1.8.9 GuiButtonMixin and GuiScreenMixin). Blur comes from vanilla `Screen` background rendering (1.20.5+). The 1.8.9 fallback is a dim overlay.

## HUD
[`HudModule`](../api/src/main/java/dev/mw19/api/module/HudModule.java) has a [`HudElement`](../core/src/main/java/dev/mw19/core/hud/HudElement.java) (anchor ∈ 9 points, offset in GUI units or % of screen, scale, opacity, colours,
background, border, shadow, radius). Layout math ([`HudLayout`](../core/src/main/java/dev/mw19/core/hud/HudLayout.java)) is pure and unit-tested: anchor → absolute rect,
clamping, snapping (edges/centres of other elements and screen guides, 4-unit threshold), and grid.
The editor supports drag, resize (scale), anchor picking, undo/redo (snapshot stack, 64 deep) and reset.
Text is cached per element and rebuilt only when the underlying value changes, so there is no per-frame string building.

## Threads
- Main/render thread: all Minecraft access.
- `MW19-IO` (1 thread): config saves and backups.
- `MW19-Net` (2 threads): HTTP. Results return to the main thread through `Scheduler.runOnMain`, drained at tick start.
- Netty's network thread: decodes the crystal optimizer's server messages and hands them to the game thread with
  `Minecraft.execute` (only `Hooks.crystalOptimizer`, a volatile, is read there).

## Performance budget
Own overhead < 0.3 ms/frame with default modules. Each hook is timed (`System.nanoTime` pairs, ~20 ns). Per-module
cost appears on the Performance page and is exported by the benchmark harness (Phase 5). No steady-state allocation
in our code: reused event objects, cached strings, pooled `EffectInfo`/[`ItemRef`](../api/src/main/java/dev/mw19/api/render/ItemRef.java) handles.

## Fabric version groups (hypothesis; confirmed with `javap` against each mapped jar in Phases 1–3)
| Group | Versions | Breaking points for us |
|---|---|---|
| A | 1.21, 1.21.1 | immediate `GuiGraphics`, `RenderSystem.setShaderColor`, `Screen.render(GuiGraphics,int,int,float)` |
| B | 1.21.2 – 1.21.4 | entity render states, `blit(RenderType::guiTextured …)`, `CoreShaders` |
| C | 1.21.5 | `RenderPipelines`/`GpuDevice` |
| D | 1.21.6 – 1.21.8 | deferred GUI render state, `Matrix3x2fStack` pose, blur changes |
| E | 1.21.9 – 1.21.10 | `KeyEvent`/`MouseButtonEvent`/`CharacterEvent` input records, `KeyMapping.Category` |
| F | 1.21.11 | `ResourceLocation` → `Identifier` |
| G | 26.1 – 26.1.2 | unobfuscated; `GuiGraphicsExtractor`, `Screen.extractRenderState`, `text()` |
| H | 26.2 | `gui.setScreen`, Gui/Hud split, optional Vulkan backend (so we must never touch GL directly) |
| I | 26.3 | TBD |
