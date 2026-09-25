# Kestrel plugin API (1.0)

See also: [ARCHITECTURE](ARCHITECTURE.md) (plugin loader and error isolation), [RULES_MATRIX](RULES_MATRIX.md), [THIRD_PARTY](THIRD_PARTY.md) (Tier Tags services), [DECISIONS](DECISIONS.md) (D-015 Tier Tags).

A plugin is a jar in `<game dir>/Kestrel/plugins/`. The same jar works on every Kestrel target, from 1.8.9 Forge to
26.3 Fabric, because it only sees the `dev.kestrel.api` package (Java 8, no Minecraft classes). Two working examples
ship in this repo: `addons/sample` (Session Stats, which uses every API area) and `addons/tiertags` (Tier Tags).

## Trust model (read this first)
- **Plugins are not sandboxed.** A plugin runs in the game process with the same privileges as Kestrel: it can read and
  write files and use the network. The Plugins page says this before the user approves a plugin.
- **Consent is per jar.** A new plugin starts in *Needs approval* and nothing from it is loaded until the user clicks
  *Allow and enable*. The approval stores the plugin id together with the jar's SHA-256. If the jar changes, the user is
  asked again.
- **The API has no access to the session/access token**, account data or chat-sending methods. A plugin could still
  reach Minecraft internals by reflection. The API does not offer that, and the consent warning is the protection.
- Modules a plugin registers go through the same **server rules** as built-in ones ([`serverrules.json`](../core/src/main/resources/kestrel/serverrules.json), Server Rules
  page). A module that a server disallows is suspended while you are connected to that server.

## plugin.json
```json
{
  "id": "session_stats",                    // required, [a-z0-9_-]{1,32}, unique
  "name": "Session Stats (sample)",         // shown in the UI (default: id)
  "version": "0.1.0",                       // required
  "api": "1.0",                             // required: the API version you compiled against
  "main": "com.example.MyPlugin",           // required: class implementing dev.kestrel.api.Plugin
  "authors": ["You"],
  "description": "One line for the Plugins page",
  "depends": { "other_plugin": ">=1.2" }    // optional: "*", exact "1.2.0" or ">=1.2"
}
```
(The comments are for this document. Real plugin.json files must be plain JSON.)

**API compatibility:** a plugin loads when its `api` major equals the host's and its minor is ≤ the host's (1.0 plugins
run on 1.x). Minor versions only add things. **Load order:** dependencies first (topological). A missing dependency, a
version mismatch or a cycle marks the plugin *Incompatible*, and the reason is shown on its card.

## Lifecycle
```java
public final class MyPlugin implements Plugin {
    @Override public void onEnable(PluginContext ctx) { /* register things here */ }
    @Override public void onDisable() { /* optional */ }
}
```
- `onEnable` runs on the game thread when the game starts (for approved plugins), when the user approves it, or when
  it is toggled on. If it throws, the plugin is marked *Failed* with the message and the game continues.
- On disable, everything registered through the context (event handlers, scheduler tasks, modules, name decorators,
  panels) is removed automatically. `onDisable` is only needed for your own resources.
- Each plugin has its own class loader, created from its jar with Kestrel's loader as the parent.

## PluginContext
| Method | What it gives you |
|---|---|
| `pluginId()`, `apiVersion()` | Your id and the host API version (`"1.0"`). |
| `logger()` | `info/warn/error` into the game log, prefixed with your id. |
| `events()` | `on(Class<E>, Consumer<E>)` → [`Subscription`](../api/src/main/java/dev/kestrel/api/Subscription.java). Handlers run on the game thread; exceptions are caught and logged. |
| `game()` | Read-only game view: version, loader, in world, player name/UUID, position, rotation, FPS, ping, server address, armor/hands ([`ItemRef`](../api/src/main/java/dev/kestrel/api/render/ItemRef.java)), item counts, key state, vanilla binding state and names, screen size. |
| `scheduler()` | `runOnMain`, `runLater(ticks)`, `every(ticks)`, `runAsync`. Game state only on the main thread. |
| `config()` | Small key/value store private to the plugin (string/boolean/number). Stored in `Kestrel/plugin-data/<id>.json` and written atomically about 2 s after the last change and on disable. |
| `nameTags()` | `register(NameDecorator)`: a suffix next to player names in nametags and/or the tab list. |
| `http()` | `getJson(url, ttlMillis, callback)`: background GET with an LRU+TTL cache, request coalescing, per-host spacing (120 ms) and `Retry-After` handling for 429/503. The callback runs on the game thread. |
| `gui()` | `registerPanel(title, Panel)` (a page under Plugins), `toast(title, message)`. |
| `registerModule(Module)` | Adds a module (or [`HudModule`](../api/src/main/java/dev/kestrel/api/module/HudModule.java)) to the Mods page with its settings, profiles and keybinds. |

### Events (package `dev.kestrel.api.event`)
| Event | Fields | Notes |
|---|---|---|
| [`ClientTickEvent`](../api/src/main/java/dev/kestrel/api/event/ClientTickEvent.java) | `end` | Start and end of every client tick. |
| [`ChatReceivedEvent`](../api/src/main/java/dev/kestrel/api/event/ChatReceivedEvent.java) | `plain`, `formatted` (`§` codes), `cancel()` | Incoming chat line, before it is displayed. Cancelling hides it (display only; nothing is sent). |
| [`ServerEvent`](../api/src/main/java/dev/kestrel/api/event/ServerEvent.java) | `joined`, `address` | World/server join and leave. `address` is null in singleplayer. |
| [`KeyPressEvent`](../api/src/main/java/dev/kestrel/api/event/KeyPressEvent.java) | `key` | Key or mouse button pressed while no screen is open. Codes are GLFW codes on every version (mouse = 1000 + button). |

Event objects are reused between calls. Copy the fields if you need to keep them.

### Modules and settings
```java
final class Timer extends HudModule {
    private final BoolSetting seconds = add(new BoolSetting("seconds", "Seconds", "Show seconds", true));
    Timer() { super("session_stats", "Session Timer", "Time since launch", Rule.ALLOWED, false, Anchor.BOTTOM_RIGHT, 4, 52); }
    @Override public float width(Renderer r) { return r.textWidth(text()); }
    @Override public float height(Renderer r) { return r.lineHeight() - 1; }
    @Override public void render(Renderer r, HudStyle style, boolean preview) {
        r.text(text(), 0, 0, style.textColor(), style.textShadow());   // text() is cached, see the sample
    }
}
```
- Module ids are `[a-z0-9_]{1,48}`. An id that is already taken (built-in or another plugin) throws from
  `registerModule`, and the plugin is marked *Failed*.
- [`Rule`](../api/src/main/java/dev/kestrel/api/module/Rule.java): `ALLOWED`, `GRAY` (not a cheat, but some servers restrict it) or `DISALLOWED_ON_SOME_SERVERS`.
  Anything other than `ALLOWED` is **always default-off**: the [`Module`](../api/src/main/java/dev/kestrel/api/module/Module.java) constructor ignores `defaultEnabled` for it.
- [`HudStyle`](../api/src/main/java/dev/kestrel/api/hud/HudStyle.java) carries the user's per-element `textColor()`, `accentColor()` and `textShadow()`.
- Settings: [`BoolSetting`](../api/src/main/java/dev/kestrel/api/setting/BoolSetting.java), [`NumberSetting`](../api/src/main/java/dev/kestrel/api/setting/NumberSetting.java), [`ChoiceSetting`](../api/src/main/java/dev/kestrel/api/setting/ChoiceSetting.java), [`MultiChoiceSetting`](../api/src/main/java/dev/kestrel/api/setting/MultiChoiceSetting.java), [`TextSetting`](../api/src/main/java/dev/kestrel/api/setting/TextSetting.java), [`KeySetting`](../api/src/main/java/dev/kestrel/api/setting/KeySetting.java),
  [`ColorSetting`](../api/src/main/java/dev/kestrel/api/setting/ColorSetting.java) (with chroma), [`ListSetting`](../api/src/main/java/dev/kestrel/api/setting/ListSetting.java). `visibleWhen(...)` hides a setting based on another one.
- HUD modules are positioned by the user in the HUD editor. `render` draws in local coordinates starting at (0,0).
  Return `false` from `visible(preview)` to hide the element in game while keeping it in the editor preview.
- Performance: `render` runs every frame. Do not allocate or format strings there. Cache text in `onTick`, as the
  built-in modules do.

### Rendering (`Renderer`)
`rect`, `gradient`, `roundRect`, `roundOutline`, `text(…, shadow)`, `textWidth`, `lineHeight`, `item(ItemRef, x, y)`,
`pushClip/popClip`, `push/pop/translate/scale`, `guiScale`, `millis`. Colours are ARGB ints. The same calls work on
1.8.9 (immediate mode) and on 1.21+/26.x (GUI render state).

## Building a plugin
Compile against `api.jar` with `release 8` (Java 8 bytecode runs on every target) and put `plugin.json` at the jar root:
```kotlin
plugins { java }
dependencies { compileOnly(files("libs/api.jar")) }   // in this repo: compileOnly(project(":api"))
tasks.withType<JavaCompile>().configureEach { options.release = 8 }
```
`./gradlew buildAll` builds the two addons into `dist/plugins/`. To install a plugin, copy the jar to
`Kestrel/plugins/`, press *Rescan* on the Plugins page (or restart), then approve it.

## Stability promises
- 1.x only adds things. Nothing is removed or changed incompatibly before 2.0.
- Anything outside `dev.kestrel.api` (core, platform and mixin classes) is internal and changes without notice.
