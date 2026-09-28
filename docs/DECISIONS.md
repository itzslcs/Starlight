# Decisions log

See also: [PLAN](PLAN.md), [ARCHITECTURE](ARCHITECTURE.md), [debug-log](debug-log.md) (evidence behind fixes), [THIRD_PARTY](THIRD_PARTY.md).

Newest last. Each entry states the decision, why, and what would change it.

## D-001 Name: Starlight (was MW19, and before that the placeholder "Kestrel")
- CLIENT_NAME = **Starlight** ("Starlight Client" in full), MOD_ID = **starlight_client**, BASE_PACKAGE = **dev.starlight**,
  LICENSE = **MIT**. The owner renamed the placeholder "Kestrel" to MW19 on 2026-09-26 (D-019), and MW19 to Starlight on
  2026-09-27 (D-033). The mod id is not plain `starlight`: that is Spottedleaf's lighting mod.
- Config/plugin dir: `<gameDir>/Starlight/` (spec: `<gameDir>/[CLIENT_NAME]/plugins/`).
- The display name, mod id and version live in [`gradle.properties`](../gradle.properties).

## D-002 1.8.9 loader = Forge 1.8.9-11.15.1.2318
Dawn/Feather's own 1.8.9 deployment is Forge 11.15.1.2318 with FMLTweaker and MixinTweaker ([FEATHER.md](FEATHER.md) §2).
The user's Prism 1.8.9 instance uses the same Forge build. Dawn does not offer Legacy Fabric.

## D-003 Build layout
- `api/`, `core/`: plain Java libraries, `--release 8`, compiled once and merged into every jar.
- `fabric/`: a Stonecutter 0.9.8 tree for all 17 Fabric targets, using `dev.kikugie.loom-back-compat` 0.4.2. That plugin
  applies `net.fabricmc.fabric-loom-remap` (≤1.21.11) or `net.fabricmc.fabric-loom` (26.x) per version, following the
  official Stonecutter Fabric template, which targets 1.21.1, 1.21.11, 26.2 and 26.3 in one tree.
- `legacy/`: a **separate Gradle build** for 1.8.9 Forge, using Essential's architectury-loom fork. It is invoked from
  `buildAll` through its own wrapper, not as a subproject, because both Loom forks ship classes under `net.fabricmc.loom.*`.
  Loading them into one Gradle build's plugin classloaders risks class clashes. A separate build costs one extra daemon.
- Gradle 9.8.0 (current release). Toolchains are provisioned by the foojay resolver (JDK 8 and 25 are not installed locally).

## D-004 No Fabric API dependency
Dawn injects Fabric API (docs/FEATHER.md §1), but Prism and vanilla Fabric instances need not have it. The hooks we
need (tick, HUD, key/mouse input, screen buttons, crash-report section) are a handful of small mixins per version, and
Fabric API's own replacements for them also changed across 1.21.x (e.g. HudRenderCallback → HudElementRegistry in 1.21.6).
Side effect: mod assets are not loaded without Fabric API's resource loader, so the mod ships **no assets**. See D-009.

## D-005 Mojang mappings for 1.21.x
This follows the spec: names then line up with unobfuscated 26.x, which minimises Stonecutter conditionals.

## D-006 Mixin failure policy (to be verified empirically in Phase 1)
- [`starlight.mixins.json`](../fabric/src/main/resources/starlight.mixins.json) holds core hooks with `"required": false` and `defaultRequire: 1`. A failed hook is logged loudly,
  which the smoke test catches, but does not stop the game.
- [`starlight.optional.mixins.json`](../fabric/src/main/resources/starlight.optional.mixins.json) holds feature mixins with `"required": false` and `defaultRequire: 0`, gated by an
  `IMixinConfigPlugin` that checks loaded mods (e.g. skips our culling when Sodium/EntityCulling is present).
- A runtime **hook watchdog** records which hooks have fired. If a core hook has not fired after the first world load,
  a toast and the About/Compat page report it.
- No `@Overwrite`, anywhere.

## D-007 Own tiny JSON codec in `core`
Minecraft bundles Gson 2.2.4 on 1.8.9 and Gson 2.14.0 on 26.3. Coding against their common API means deprecated
constructors that may disappear. A ~200-line parser/writer in core gives identical behaviour in all 18 jars, and core
unit tests need no dependencies.

## D-008 Rounded rectangles = scanline fills at physical-pixel resolution
Built only from `fill`, which every target has (1.8.9 `Gui.drawRect` → 26.x `GuiGraphicsExtractor.fill`). There are no
texture or shader APIs to port, and corners are anti-aliased with one alpha-weighted pixel per row. If the perf
dashboard shows HUD cost from the 1.21.6+ per-fill render-state objects, switch to a generated corner texture.

## D-009 Icons = Unicode glyphs from the vanilla font
They are original, need no bundled assets (see D-004), and work on every target.

## D-010 Hypixel API keys: spec conflicts with the Hypixel API Policy
The spec says "The user supplies their own key". The Hypixel API Policy (developer.hypixel.net/policies, last updated
July 16, 2026) says: *"Do not share your API key with 3rd parties, such as entering them into a website or a mod. This
applies to any publicly available mod, regardless of it being allowed on the Hypixel Server."* It also says
*"Projects should not de-anonymize players such as those who are using the Hypixel nick feature."*
**Closest compliant alternative:**
1. The default stats provider uses **only data the client already receives**: tab-list/nametag prefixes (e.g. Bedwars
   stars), scoreboard, and chat (session final kills/beds). It needs no key and no network.
2. An **API-key provider** exists for a developer's *own* registered Hypixel application in a *private* build. It is compiled in
   only with `-Pstarlight.privateHypixelApi=true`, so release jars from `buildAll` do not contain it. When present it shows
   the policy text before accepting a key, and it caches for hours (policy: "caching that expires either hours, or even
   days later") and honours `RateLimit-*` headers and 429s.
If the owner registers a Hypixel production application for this client, flip the flag and document the approval.

## D-011 Hypixel game detection
The official **Hypixel Mod API** location event is used when the `hypixel_mod_api` mod is installed (the user's
Prism 1.8.9 instance has `HypixelModAPI-1.0.1`). It sends no chat. The throttled `/locraw` fallback (once per
server switch) is opt-in and GRAY.

## D-012 Phase-1 modern baseline = **1.21.11** (not the suggested 1.21.4)
- It uses the same GUI model as every newer target: deferred GUI render state (1.21.6+) and input-event records (1.21.9+),
  shared with 26.x. 1.8.9 covers the immediate-mode extreme. Validating the adapter interfaces against **both
  extremes** in Phase 1 is a stronger test than a middle version.
- It is the version the user actually plays (Prism instances "pvp", "Fabulously Optimized", "kwipton kwient"), so Phase 1
  output is usable right away. Dawn supports it.
- Cost: Phase 2 ports in one direction only (1.21.11 → 1.21). Phase 3 (26.x) starts one step away.

## D-013 Units
Core lays out in **GUI units** (floats). Backends convert to physical pixels (× guiScale) for pixel-exact edges. The
client has its own UI scale on top of vanilla GUI scale.

## D-014 Plugin trust model
*Superseded by D-026: the plugin system was removed in 0.5.0.*
There is no signing authority, so every plugin is "unsigned". The first load of each plugin (id + SHA-256) is blocked
until the user accepts a warning that plugins run with full mod privileges. Java 17+ removed the SecurityManager, so
no sandbox is possible. The API exposes name/UUID only and never the session or access token. That limits the API
surface, not what reflective code could do.

## D-015 Tier tags addon (the user's "addons like TierTagger")
*Superseded by D-026: removed with the plugin system in 0.5.0.*
This is an addon **plugin** built on our plugin API. It reads MCTiers API v2 (`https://mctiers.com/api/v2/profile/{uuid}`, documented
at mctiers.com/docs/v2, v1 removed 2026-06-01) and SubTiers (same v2 schema, `https://subtiers.net/api/v2`). PvPTiers
returned HTTP 503 on 2026-09-25, so it is left out by default. The provider list is user-editable. Code is original: TierTagger
(MPL-2.0) and Tiers (GPL-3.0) were only used to identify the tier lists, never read or copied.

## D-016 "Show own name" (user request)
Implemented as module `own_nametag`: renders your own nametag in third-person views (vanilla hides it). Tier tags and
nameplate decorations apply to it too.

## D-017 Prism Launcher (user request)
Standard jars work in Prism (Fabric Loader component, or the Forge 11.15.1.2318 component for 1.8.9). `buildAll` also
emits importable Prism instance zips (`dist/prism/Starlight-<mc>.zip`) and [`docs/PRISM.md`](PRISM.md). Verification uses a
production-layout launch (remapped jar, real Fabric Loader/Forge, no Gradle dev runtime) because driving the user's
Prism install would use their Microsoft accounts.

## D-018 KeyCPS replaces the CPS counter (user request, 2026-09-25)
The owner asked for Starlight's CPS counter to be replaced by **KeyCPS** (modrinth.com/mod/keycps), their own
keystrokes + CPS mod, and for it to appear in Starlight's Mods GUI in place of KeyCPS's own settings screen. Authorship
was checked: the local KeyCPS repository (`~/Desktop/KeyCPS`, 1.6.1) is committed by the owner's address. Its
fabric.mod.json declares MIT while the Modrinth page lists All-Rights-Reserved. Either way, the copyright holder asked
for this integration. If the standalone mod (id `keycps`) is also installed, Starlight shows a startup notice to remove it,
because two overlays would draw.
- **Ported, not bundled.** [`KeyCpsModule`](../core/src/main/java/dev/starlight/core/modules/KeyCpsModule.java) (core, Java 8) re-implements KeyCPS 1.6.1's HUD (layout, fade, space-bar line,
  CPS inside the mouse keys, CPS warning, rainbow, always-LMB/RMB, per-key rates) and its counting ([`InputRates`](../core/src/main/java/dev/starlight/core/modules/InputRates.java): per
  binding, from input events, key repeat included) on Starlight's platform API. Nesting the KeyCPS jar was rejected: it
  needs Fabric API (Starlight is Fabric-API-free), it has its own Right Shift settings screen and move screen (a second GUI
  and a key clash), it would sit outside profiles, server rules and the HUD editor, and it does not exist for 1.8.9.
- **Mapping of KeyCPS's UI:** module settings (Mods page) replace the settings screen; the HUD editor replaces "Move HUD"
  and the scale slider and supplies text colour and shadow; the module toggle and Starlight's keybinds replace "Toggle HUD".
  Not ported: the first-join chat tip and the 14 translations (Starlight's UI is English-only for now).
- The old `cps` and `keystrokes` modules and `ClickTracker` were removed. KeyCPS draws keystrokes and CPS in one element,
  and the first all-modules smoke run showed the old keystrokes element drawn on top of it.
- The ported code is part of Starlight and so falls under Starlight's MIT licence. The standalone KeyCPS mod keeps its own
  licence.
- 1.8.9 limitation: LWJGL 2 sends no key-repeat events in game, so held keyboard keys count once there.

## D-019 Rename to MW19 (owner request, 2026-09-26)
*Superseded by D-033: the client is now Starlight.*
Everything was renamed from the placeholder: packages `dev.kestrel` → `dev.mw19` (including the plugin API; no third-party
plugins existed yet), mod id `mw19`, mixin configs `mw19.*.json`, jars `MW19-<version>+mc<mc>.jar`, config folder
`MW19/`, smoke markers `MW19 SMOKE PASS`. Compatibility for the owner's existing setup: a `Kestrel/` folder is moved to
`MW19/` on first launch, and old `KESTREL-P1:` profile codes still import ([`RenameCompatTest`](../core/src/test/java/dev/starlight/core/RenameCompatTest.java)). "MW19" is also the
common nickname for Call of Duty: Modern Warfare (2019). That is fine as a name, but the Modrinth page should not
use Call of Duty branding.

## D-020 "Max FPS client": performance packs by reference, native Vulkan, no bundling (owner request, 2026-09-26)
*The "no bundling" part is revised by D-024 (VulkanMod ships inside Starlight where its source is public).*
The owner pointed at Frost Client (frostclient.eu) as the bar. From its public site: Minecraft 1.21+, **Sodium and
VulkanMod bundled**, "Vulkan by default", 50+ bundled mods, capes and badges, a paid tier, and a claim of 85 → 810 FPS
(RTX 3060 Ti, 4K, 20 chunks). That gain comes from Sodium and VulkanMod, not from client code. Frost's launcher and files
were not unpacked, run or copied. Its config folder holds account logins and was not opened.
- **Starlight's own work:** Entity Culling, FPS Boost, the Vulkan switch on 26.2+ (Minecraft's own RenderPearl Vulkan backend,
  which falls back to OpenGL), and an allocation-free HUD. Measured in [PERF](PERF.md).
- **Other people's performance mods ship by reference:** [`scripts/mrpack.py`](../scripts/mrpack.py) writes `Starlight Performance` Modrinth packs
  (`.mrpack`) per version. Their index lists Modrinth CDN URLs and hashes for Sodium, ImmediatelyFast, FerriteCore and
  Lithium, and the launcher downloads them from Modrinth, so no third-party jar is redistributed. Only mods tested with Starlight
  ([COMPAT_MATRIX](COMPAT_MATRIX.md)) go in.
- **VulkanMod** (LGPL-3.0) exists for 1.21–1.21.5, 1.21.9–1.21.11 and 26.1.x. It replaces the renderer and conflicts with
  Sodium, so it is not in the default pack. On 26.2+ Minecraft's own Vulkan backend makes it unnecessary.

## D-021 Exploit Protection: ExploitPreventer's list, Starlight's own code, GRAY (owner request, 2026-09-26)
The owner asked to build [ExploitPreventer](https://modrinth.com/mod/exploitpreventer) (NikOverflow, MIT, Fabric
1.21.9+) into Starlight. Only its public Modrinth description was read, for the list of exploits; its source was not read
or copied (hard rule: original work only). Starlight's version is written from the behaviour of vanilla's own code (javap):
- **Sign and anvil text.** The sign editor's constructor turns each line into a string with `Component.getString()`,
  which resolves translation keys and keybinds with the client's language and bindings, and sends those strings back.
  A server can therefore open an editor with a mod's key and read whether it translated. Starlight re-resolves the lines
  after the constructor ([`SignEditMixin`](../fabric/src/main/java/dev/starlight/fabric/mixin/SignEditMixin.java)) and the anvil's name field ([`AnvilNameMixin`](../fabric/src/main/java/dev/starlight/fabric/mixin/AnvilNameMixin.java)) as an unmodded client
  with default bindings would: keys from vanilla's own `en_us.json` translate, other keys stay as written, vanilla
  keybinds show their default key. 1.8.9 needs nothing here: it sends unedited sign lines back as components, and anvil
  names are plain strings.
- **Resource pack addresses.** A pack URL may not name a local host or a loopback, private, link-local or unique-local
  address ([`LocalAddress`](../core/src/main/java/dev/starlight/core/net/LocalAddress.java)): checked without DNS when the request arrives and again with DNS
  on the download thread (Fabric), or before the download on a helper thread (1.8.9). 1.8.9 also refuses `level://`
  paths that leave the world folder, and `level://` outside singleplayer: vanilla 1.8.9 answers whether any file exists.
- **Pack cache per account (1.21+).** Server packs are cached under `downloads/account-<hash>`, so the cache cannot
  link one computer's accounts. This must be decided when the game starts, so it is always on.
- **Verdict GRAY, default off.** It has no gameplay effect, but it changes what the client sends to a server that
  probes, and Hypixel's policy lists changes to how the client communicates as disallowed. The owner can switch it on
  under Mods; a smoke self-test proves each part on every target.

## D-022 Skins, Packs and Host World (owner request, 2026-09-26)
Asked for together with a home screen like Essential's (skin shown on the title screen), a less "generated" look and
more modules.
- **Skins** use Mojang's official endpoints only: `POST api.minecraftservices.com/minecraft/profile/skins` with the
  session token when the player presses *Use this skin*, and the public profile API to copy a player's skin. The token
  is read at that moment, only if it is a Microsoft-account token (a JWT), and sent only to that endpoint over HTTPS. It
  is not logged, stored or reachable from plugins. The 3D preview is vanilla's `PlayerSkinWidget` on 1.21+ (the one
  the skin-report screen uses) and ModelPlayer's parts on 1.8.9. Only 64×64 skins are accepted.
- **Packs** search Modrinth's public API (no key) and download from its CDN with a size cap and SHA-512 check, to a
  sanitised file name inside `resourcepacks/`. Icons load only when they are PNG (the game cannot decode WebP), else a
  letter is shown.
- **Host World** is vanilla's Open to LAN plus two things: UPnP port forwarding on the player's router
  ([`Upnp`](../core/src/main/java/dev/starlight/core/net/Upnp.java), SSDP + SOAP; XML with DTDs off; answers only from the device that responded; a 1-hour lease
  renewed while hosting, removed on stop, world close or exit) and an invite-only whitelist whenever the world is open
  to the internet. There is no relay server: behind carrier-grade NAT or with UPnP off, the page says so and names the
  port to forward by hand. Opening a port is a real exposure, so the internet switch is separate and the whitelist
  cannot be skipped.
- **Home screen:** vanilla's panorama (1.21+: `Screen.renderPanorama`; 1.8.9: Starlight's own cube renderer, because
  vanilla's is private), a logo drawn from pixel blocks, the player model with a Skins button, and flat buttons.
  The particles and tip card were removed.
- **Not built: account switching.** An in-game switcher that reads the launcher's saved accounts would handle other
  programs' stored login tokens, which Starlight's rules forbid (never read launcher account files). Launchers switch
  accounts themselves.

## D-023 Graphics presets with auto-detection, and the community suggestion list (owner request, 2026-09-26)
The owner passed on a list of player suggestions for Frost Client ("frost = mw19") and asked for low/mid/high video
configs with automatic detection on first start, since Starlight's focus is maximum FPS, plus smooth animations.
- **Presets** ([`VideoPreset`](../core/src/main/java/dev/starlight/core/perf/VideoPreset.java)): values are platform-neutral ids; every version applies the ones it has. Values stay
  inside vanilla's option ranges (read with javap), because vanilla replaces an out-of-range value with its default.
- **Detection** ([`HardwareTier`](../core/src/main/java/dev/starlight/core/perf/HardwareTier.java)) is a heuristic from the renderer string (26.2+ also reports integrated/discrete/CPU):
  software renderers and older Intel/mobile GPUs get Potato, Iris Xe and AMD APUs Low, GTX 10/16, RX 400-5000 and
  strong APUs Medium, RTX, RX 6000+ and Arc B-series High; 4 or fewer CPU threads, or under 2 GB of heap, each step down.
  It runs once, on the first start of a fresh install (no saved profile), never raises view distance, and says what it
  picked; existing installs only get a hint. The benchmark skips it so its baseline stays the player's settings.
- **From the suggestion list, built:** presets/"Lite"/iGPU (above), animations, UI vs vanilla (the home screen switch,
  since 0.2.0), vanity themes (Black, White, Crystal), finer crosshair, Hitboxes, TNT Timer, Reach Display (DISALLOWED
  on Hypixel), Quick Commands (GRAY), saturation and day counter (0.3.0), world hosting with a shareable address (0.3.0).
- **Not built, and why:** a launcher, installers, update systems, RAM settings and instance/mod management are launcher
  features (Prism and the Modrinth app do them; Starlight is a mod). Cracked/offline or alternative authentication and an
  account switcher conflict with Starlight's rules (legit only; never touch stored logins). Capes, emotes, friends, badges
  and cloud-synced cosmetics need servers Starlight does not have (and no telemetry). Free cam gives an unfair view on
  servers. Bedrock and 1.8(.0) are other games/targets. Discord Rich Presence needs a Discord application id that only
  the owner can create. Motion blur, colour saturation and connected glass need shader or resource-pack work per
  version and are open for later. Monetisation was set aside by the owner.

## D-024 VulkanMod inside the Starlight jar, not Sodium (owner request, 2026-09-26)
The owner asked for Sodium or VulkanMod inside Starlight so the mods folder holds one jar.
- **Not Sodium.** Its licence (PolyForm Shield 1.0.0) forbids using it to provide a product that competes with it, and
  says a product marketed as a practical substitute "definitely competes". A max-FPS client that ships Sodium inside is
  that substitute, so it is not bundled (the `.mrpack` packs still reference it, D-020).
- **VulkanMod** (LGPL-3.0-only) may be redistributed with the licence texts and the exact source. It is nested unmodified
  as Fabric jar-in-jar ([`fabric/build.gradle.kts`](../fabric/build.gradle.kts), checked against the SHA-512 pinned in [`bundled.json`](../fabric/bundled.json)). The jar carries
  `THIRD_PARTY_NOTICES.txt` and `META-INF/licenses/` (LGPL + GPL), the About page names it (LGPL section 4(c)), and
  `dist/sources/` holds the source zip of the release commit. A player can use another VulkanMod by dropping it in the
  mods folder (Fabric loads the newer one), which is the "suitable shared library mechanism" of LGPL section 4(d).
- **Only builds with a public release commit** ([`scripts/vulkanmod-pin.py`](../scripts/vulkanmod-pin.py)): 0.6.8 for 1.21.11 (dev branch,
  d3db079) and 0.6.6 for 1.21.9/1.21.10 (tag 0.6.6). Upstream has not published the sources of its 1.21–1.21.5 and
  26.1.x ports, and shipping a build without its source would break the licence, so those jars bundle nothing. 26.2+
  have Minecraft's own Vulkan backend (D-020); 1.21.6–1.21.8 have no VulkanMod.
- **Never a crash on PCs without Vulkan.** VulkanMod has no fallback: without a Vulkan 1.2 driver it stops the game
  ("Failed to create instance"). [`RendererSwitch`](../fabric/src/main/java/dev/starlight/fabric/RendererSwitch.java) is a Fabric language adapter, which Fabric creates after choosing the
  mods and before reading their entrypoints and mixin configs; when this session should be OpenGL it removes VulkanMod's
  entrypoints and mixin configs from its metadata, so the jar stays loaded but none of it runs. It also removes VulkanMod's
  "contains a Fabric renderer" flag, which would otherwise make Fabric API's own renderer (Indigo) stand down. A fresh install starts on
  OpenGL, [`VulkanProbe`](../fabric/src/main/java/dev/starlight/fabric/VulkanProbe.java) asks the driver in the background for a real (non-CPU) Vulkan 1.2 device, and "ok" switches the next
  start to Vulkan. A start that never reaches the menu leaves `Starlight/vulkan-starting` behind and the following start stays
  on OpenGL (and says why) until the player picks Vulkan again on the Performance page. Sodium, Iris or another renderer
  mod also keeps it off. A VulkanMod the player installed separately is left alone.
- Verified in a real Fabric production launch ([`scripts/prodlaunch.py`](../scripts/prodlaunch.py), `PROD=1 scripts/smoke.sh`) on Mesa's software Vulkan
  driver: OpenGL with VulkanMod kept off, Vulkan chosen, the automatic first-start check then Vulkan, and a forced Vulkan
  start on a machine without Vulkan followed by the automatic OpenGL recovery (debug-log 2026-09-27).

## D-025 Fast Chests: chests as ordinary blocks from a built-in resource pack (owner request, 2026-09-26)
"Make chests/block entities normal blocks on the client." A chest is drawn by a block entity renderer every frame;
drawn as a block it is baked into the world mesh once.
- The models are generated by [`scripts/fast-chests.py`](../scripts/fast-chests.py) from the vanilla texture layout (a standard box unwrap), not copied:
  base, lid and latch per chest type, single and both halves, with each face's region rotated as the texture stores it.
  The chest textures join the block atlas through an atlas source file (atlas definitions from every pack are merged).
- The pack is offered by Starlight's own entry in the client pack scan ([`PackSourceMixin`](../fabric/src/main/java/dev/starlight/fabric/mixin/PackSourceMixin.java)) only while the module is on, and
  is "required" then, so the module is the only switch; toggling reloads resources once. `Pack`, `PackLocationInfo`,
  `Pack.Metadata` and `PathPackResources` have the same shape from 1.21 to 26.3, so one code path serves every version.
- The block entity renderer skips chests whose model the pack provides. Before 1.21.4 chests report the
  ENTITYBLOCK_ANIMATED shape, so a mixin makes them MODEL while the pack is on; on 1.21.4–1.21.11 the special block
  renderer that also draws a chest minecart's or block display's chest is skipped for them (26.x draws those from the
  special model only). Result: lids no longer animate, which the module description says.
- Block textures are mipmapped and entity textures are not, so a texture region that starts on an odd column bleeds into
  its neighbour at a distance. On a double chest's seam that showed as a dark line; the generator drops that one column.
- 26.x logs a warning for each chest texture that is now in two atlases and says a future version will reject that;
  26.1–26.3 still render both correctly (debug-log 2026-09-27). A future version will need its own sprite names.
- Verified: the smoke's chest scene (single, double both ways, trapped, ender, copper, a chest minecart and a block
  display, front and top views) against vanilla's renderer, in dev and in production, on OpenGL and under VulkanMod.

## D-026 Plugins removed (owner request, 2026-09-26)
"Remove plugins." The plugin loader, its consent screen and Plugins page, the plugin API types, the Tier Tags and
Session Stats addons, the name-decoration hooks that only plugins used (four mixins) and MCTiers/SubTiers network access
are gone. The `api/` module keeps the module, setting, event and render types core itself is built from. Old configs
keep working: their `pluginConsent`/`pluginDisabled` keys are simply unused.

## D-027 Minecraft's own menus in the Starlight style (owner request, 2026-09-26)
"Make [the pause menu and server list] MW19-like, for all versions." Vanilla screens are kept (their per-version
behaviour, e.g. disconnecting, and buttons other mods add) and only their buttons are redrawn like the home screen's: dark
glass, a thin edge that lights up on hover ([`ButtonStyleMixin`](../fabric/src/main/java/dev/starlight/fabric/mixin/ButtonStyleMixin.java); 1.8.9 [`GuiButtonMixin`](../legacy/src/main/java/dev/starlight/forge/mixin/GuiButtonMixin.java)). Up to 1.21.10
the button draws its background and label in one method, so Starlight draws both; from 1.21.11 only the background sprite is
replaced. The pause menu gets a row under Back to Game with **Starlight Menu** and **Packs** (the pack browser in game);
everything below moves down one row. Themes → "Starlight game menus" turns the restyle off.


## D-028 Marlow's Crystal Optimizer and Hero's Anchor Optimizer ported in (owner request, 2026-09-27)
The owner asked for the two mods built in, one jar in the mods folder, and "you have to implement these optimisers from
the code, thats the only way itll be allowed. give credits ofcourse". That is an explicit exception to "original work
only", like KeyCPS (D-018). Both are MIT, so the port keeps their copyright and licence texts (THIRD_PARTY "Ported
optimizers").
- **Behaviour is upstream's**, from Marlow's Crystal Optimizer 2.0.0-SNAPSHOT (commit 62831e6) and HerosAnchorOptimizer
  1.1.3 (commit 8e70b8a). The crystal optimizer's server protocol is kept byte for byte (upstream PROTOCOL.md), because it
  is what lets servers allow or refuse it. Starlight registers `marlowcrystal:opt_out` and `marlowcrystal:challenge`, sends
  `marlowcrystal:version` on join (not in singleplayer), and answers challenges. An opt-out switches it off for that
  connection and shows upstream's chat notice. The version packet names the ported build (2.0.0, snapshot, Fabric, commit
  62831e6, dirty) with that commit's time.
- **Changes, all in the file headers:**
  - No Fabric API. Messages go through vanilla's payload codec (fallback codec for the two incoming channels, the
    codec's encode for outgoing ones). This works beside Fabric API, whose own codec for `minecraft:register` would reject
    a foreign payload.
  - Kept crystals live in a list, not in fields on the entity.
  - The opt-out belongs to the connection object (weakly held), not a flag reset on disconnect.
  - The anchor ghost is vanilla purple stained glass, replaceable only where Starlight put it, instead of a registered block.
    It is set inside the use's block prediction, so the server's answer always replaces it.
  - Spectators get no ghost (upstream's hook ran before vanilla's spectator check).
- **Rules:** both are GRAY and default off (client-side prediction of game actions, and the crystal one talks to the
  server). Each steps aside when the original mod is installed (`marlowcrystal`, `herosanchoroptimizer`).
- **Declined in the same request:** No Chat Restrictions. It bypasses the chat restrictions set on a Microsoft account
  (by a parent, or through age or account settings). That is not a client-side optimisation, so it is not built in.
- **Verified** by the smoke run's optimizer stage in a singleplayer world, on every Fabric target in dev and in production
  (COMPAT_MATRIX 0.6.0), and beside Fabric API and the Performance pack mods on 1.21.11. Each step is checked right after
  the client acts, and again after the integrated server answered:
  - The crystal is hidden at once, then removed by the server.
  - The anchor ghost is placed and replaceable at once, then replaced by the server's air or fire.
  - The codec round trip covers both directions.
  - An opt-out through the packet listener is honoured afterwards, with upstream's chat notice.

  Singleplayer counts as a server only for the anchor step (upstream turns the anchor optimizer off there). No real
  server with an opt-out plugin was tested.

## D-029 MW19's own look: keycaps, embers, a blocky skyline (owner feedback, 2026-09-27)
*The keycaps stay; the ember backdrop became the Ember theme's scene in 0.8.0 (D-031).*
"instead of just making the cubes black in the menus make it like actual buttons that look nice, and instead of the
normal backdrop make it something like mw19, the buttons are just bland and everything looks AI, make it unique."
The look came from what was already MW19's own: the logo's letters made of blocks, with depth, and the orange "19".
- **Keycaps** ([`MenuStyle`](../core/src/main/java/dev/starlight/core/gui/MenuStyle.java)):
  - Each button has a dark outline with notched corners, in whole GUI pixels.
  - The face has a lit top edge, and below it a 2 px side shows the key's depth.
  - On hover the side glows in the accent, like a backlit key (and KeyCPS, the owner's mod, draws keys too).
  - The same keycap is used for Minecraft's buttons (all versions), the home screen, and Starlight's own menu buttons (in
    theme colours: primary is an accent key).
  - Sliders (1.21+) get a keycap track whose side is lit up to the value, with a small keycap handle.
  - The label stays vanilla's, so its position and scrolling are unchanged.
- **Backdrop**, for every vanilla screen that has a background (pause menu, server list, options, loading screens,
  other mods' screens) and for the home screen:
  - Deep charcoal, with a warm glow in the accent from below.
  - Pixel embers rise through it: a third are sparks in the accent, the rest ash. They are still when animations are
    off.
  - Without a world, a skyline of stepped block columns in two rows stands in front of the glow, catching it on their
    top edges.
  - Over a world the backdrop is see-through and has no skyline.
  - Inventories, chat and the death screen keep vanilla's background, because there the world is the point. So do the
    screens 26.2 made see-through over the world (`isInGameUi`: signs, books, command and structure blocks).
  - From 26.2 the pause menu draws its own background, so it has its own hook (debug-log 2026-09-27).
  - It is about 100 quads with no allocation per frame. Vanilla's blur is not used.
- All of it follows Themes → "Starlight game menus" (on by default). Off gives vanilla's buttons and backdrop, and the
  panorama on the home screen.
- Starlight's menu over the home screen now draws the backdrop too. It used to be flat black when menu blur was off.

## D-030 Tier Tagger back, as a built-in module (owner request, 2026-09-27)
"wheres my tiertagger module using mctiers api." It went with the plugins in 0.5.0 (the Tier Tags addon, D-026), and
is now a module ([`TierTaggerModule`](../core/src/main/java/dev/starlight/core/modules/TierTaggerModule.java), formatting in [`TierFormat`](../core/src/main/java/dev/starlight/core/modules/TierFormat.java)).
- It shows a player's tier (e.g. `[HT1]`, `[RHT1]` for a retired player's peak) after their name on the nametag and in
  the tab list, from MCTiers, SubTiers or any list serving the MCTiers v2 API. The gamemode is the best tier by default,
  or one chosen mode.
- **Gamemode icon (owner request, 2026-09-28):** *Show gamemode* draws the mode as a glyph rather than its name
  (`[HT1 ⚔]`, not `[HT1 sword]`). The glyphs come from the font Minecraft itself ships (unifont, checked: all 20 are
  in `unifont_all_no_pua-15.1.05.hex`), so nothing of ours has to be drawn into a vanilla nametag or the tab list, and
  every version has them. A mode we have no glyph for (a custom list's own) keeps its name.
- **Network:** only while the module is on (it is off by default): one HTTPS GET of `<list>/profile/<uuid>` per visible
  player with a real account (UUID v4; offline-mode UUIDs are never looked up), each answer (ranked or not) cached 4 hours. Nothing is sent but the UUID.
- **Rules:** extra information about other players, so DISALLOWED on Hypixel ([`serverrules.json`](../core/src/main/resources/starlight/serverrules.json)) and off by default.
- The name hooks D-026 removed are back (nametag and tab list, all versions), and are one-line calls to
  `Starlight.nameSuffix`, which returns nothing while the module is off.

## D-031 A scene per theme (owner request, 2026-09-27)
"make it like star themed ... add multible themes, like the current one." Each theme now has an animated scene behind
Starlight's menus and Minecraft's own ([`Scenes`](../core/src/main/java/dev/starlight/core/gui/Scenes.java)), with its own sky and keycap colours ([`Theme`](../core/src/main/java/dev/starlight/core/gui/Theme.java)):

| Theme | Scene |
|---|---|
| Starlight (default) | Night sky: twinkling stars, a Milky Way band, a crescent moon, a shooting star, dark hills |
| Ember | The 0.7.0 look: rising embers and the blocky skyline |
| Aurora | Aurora curtains over pines |
| Nebula | Violet gas clouds and stars |
| Glacier, Crystal | Snowfall over snow-capped mountains |
| Sakura | Falling petals at dusk |
| Daylight, White | Blocky clouds, a square sun, green hills |
| Black | The Starlight night sky on pure black |

- Everything is rects and gradients in whole GUI pixels, and each particle is a function of its index and the time, so
  nothing is allocated per frame. Particle counts follow the area. Scenes are still when animations are off.
- Over a world the sky is see-through and the landscape is left out.
- The Themes page previews each theme as a live card.
- Old theme names are migrated (config schema 2): MW19 → Starlight, Violet → Nebula, Forest → Aurora, Rose → Sakura.

## D-032 Bind profiles (owner request, 2026-09-27)
"if i make a new instance or make a bind profile it changes my minecraft keybinds." A bind profile is a named set of
Minecraft's own key bindings ([`BindProfiles`](../core/src/main/java/dev/starlight/core/binds/BindProfiles.java)), saved and applied on the Keybinds page.
- **Shared by every instance:** stored outside the game folder, in `$XDG_DATA_HOME/starlight/bind-profiles.json` when
  that variable is set, else in `~/.starlight/bind-profiles.json`. Flatpak launchers (the owner's Prism and Dawn) run
  the game with a temporary home folder that is emptied when the game closes, and set XDG_DATA_HOME to the launcher's
  own lasting data folder: there, profiles are shared by that launcher's instances (debug-log 2026-09-27). If neither
  folder can be written, the file stays in the instance's `Starlight/` folder. Smoke and benchmark runs always use the
  instance folder, so tests never touch the player's real profiles.
- **Portable:** bindings are stored by id (`key.hotbar.1`) with Starlight's canonical key codes, so a profile saved on
  1.21 applies on 26.x (SDL key codes) and 1.8.9 (LWJGL 2 codes) too. Bindings a version does not have are skipped.
- **New instances:** one profile can be the default (★). A fresh install applies it once on first start, with a toast.
  Existing instances are never changed without the player pressing Apply.
- Applying goes through Minecraft's own `KeyMapping.setKey` and saves `options.txt`, like the Controls screen.
- **The page shows what a profile holds** (0.8.1; the owner: "i meant actual minecraft binds, not just the starlight
  binds"). The page listed only Starlight's binds, so profiles looked like they were about those. It now lists
  Minecraft's bindings by category, each editable in place (applied and saved at once), with *Reset all* for Minecraft's
  defaults, above Starlight's own binds. Clashes follow Minecraft's rule: two Minecraft bindings on their default keys
  never clash (1.21.11's F3 debug keys share A, S, B... with movement by design).

## D-033 Rename to Starlight (owner request, 2026-09-27)
"change name to Starlight and make it like star themed." Everything was renamed from MW19: packages `dev.mw19` →
`dev.starlight`, the core class `Mw19` → [`Starlight`](../core/src/main/java/dev/starlight/core/Starlight.java), jars `Starlight-<version>+mc<mc>.jar`, config folder `Starlight/`,
mixin configs `starlight.*.json`, smoke markers `Starlight SMOKE PASS`, Gradle properties `starlight.*`, the logo, the
icon ([`scripts/icon.py`](../scripts/icon.py)) and the default theme (D-031).
- **Mod id `starlight_client`, not `starlight`:** `starlight` is Spottedleaf's lighting engine mod (Fabric and
  NeoForge), and two mods with one id refuse to load together. For the same reason the Modrinth slug is
  `starlight-client` and the title "Starlight Client".
- **Upgrading from MW19:** the `MW19/` folder (or an older `Kestrel/`) is moved to `Starlight/` once ([`ConfigFolder`](../core/src/main/java/dev/starlight/core/ConfigFolder.java)),
  theme names migrate (D-031), and `MW19-P1:`/`KESTREL-P1:` profile codes still import ([`RenameCompatTest`](../core/src/test/java/dev/starlight/core/RenameCompatTest.java)). The
  old jar must go: the Fabric build declares `breaks: mw19`, so Fabric names the conflict instead of loading both, and the
  1.8.9 build shows a warning.

## D-034 In-game account switcher: only with Starlight's own approved sign-in (owner request, 2026-09-28)
The owner asked for an in-game account switcher with Microsoft sign-in, accounts kept in Starlight's own file in the
layout of Prism's `accounts.json`, like In-Game Account Switcher. That is fine in principle and revises the "account
switcher" part of D-023. But Minecraft's login only accepts Microsoft app registrations that Mojang has approved for
the Minecraft API, so it needs Starlight's own Entra (Azure) app with that approval.
- **Aproved** Use prisms accounts.json to ingame account switch (Only if owner puts it in manually)
- **Shipped (2026-09-28)** The Accounts page reads `Starlight/accounts.json`, a copy of Prism's file the owner puts
  there himself (or drops on the page), and switches the session to a chosen account: `dev.starlight.core.account.Accounts`
  parses it, `Platform.setSession` puts the session and the game's cached profile in (Fabric: a `@Mutable` accessor on
  Minecraft's `user` field; 1.8.9 keeps the default "cannot switch"). Starlight never writes that file, never refreshes
  a token and never signs anyone in, so no Entra app is needed for this part. A server already joined keeps the old
  identity: the switch lands on the next join, and an expired token only fails at that join.
- **0.8.3 fix** A switched account was kicked with "Invalid signature for profile public key": Minecraft holds the chat
  signing key (`Minecraft.profileKeyPairManager`) and the `UserApiService` of the account that started the game, both
  derived from the session, so `setSession` replaces them too. The key is emptied on the switch (no key is accepted by
  servers that do not enforce secure profiles; a key from the wrong profile is refused by all of them) and the new
  account's key is fetched off the game thread, then installed only if that account is still signed in.
