# Decisions log

See also: [PLAN](PLAN.md), [ARCHITECTURE](ARCHITECTURE.md), [debug-log](debug-log.md) (evidence behind fixes), [THIRD_PARTY](THIRD_PARTY.md).

Newest last. Each entry states the decision, why, and what would change it.

## D-001 Name: MW19 (was the placeholder "Kestrel")
- CLIENT_NAME = **MW19** ("MW19 Client" in full), MOD_ID = **mw19**, BASE_PACKAGE = **dev.mw19**, LICENSE = **MIT**.
  The owner picked the name on 2026-09-26 (D-019). "Kestrel" was a placeholder until then.
- Config/plugin dir: `<gameDir>/MW19/` (spec: `<gameDir>/[CLIENT_NAME]/plugins/`).
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
- [`mw19.mixins.json`](../fabric/src/main/resources/mw19.mixins.json) holds core hooks with `"required": false` and `defaultRequire: 1`. A failed hook is logged loudly,
  which the smoke test catches, but does not stop the game.
- [`mw19.optional.mixins.json`](../fabric/src/main/resources/mw19.optional.mixins.json) holds feature mixins with `"required": false` and `defaultRequire: 0`, gated by an
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
   only with `-Pmw19.privateHypixelApi=true`, so release jars from `buildAll` do not contain it. When present it shows
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
There is no signing authority, so every plugin is "unsigned". The first load of each plugin (id + SHA-256) is blocked
until the user accepts a warning that plugins run with full mod privileges. Java 17+ removed the SecurityManager, so
no sandbox is possible. The API exposes name/UUID only and never the session or access token. That limits the API
surface, not what reflective code could do.

## D-015 Tier tags addon (the user's "addons like TierTagger")
This is an addon **plugin** built on our plugin API. It reads MCTiers API v2 (`https://mctiers.com/api/v2/profile/{uuid}`, documented
at mctiers.com/docs/v2, v1 removed 2026-06-01) and SubTiers (same v2 schema, `https://subtiers.net/api/v2`). PvPTiers
returned HTTP 503 on 2026-09-25, so it is left out by default. The provider list is user-editable. Code is original: TierTagger
(MPL-2.0) and Tiers (GPL-3.0) were only used to identify the tier lists, never read or copied.

## D-016 "Show own name" (user request)
Implemented as module `own_nametag`: renders your own nametag in third-person views (vanilla hides it). Tier tags and
nameplate decorations apply to it too.

## D-017 Prism Launcher (user request)
Standard jars work in Prism (Fabric Loader component, or the Forge 11.15.1.2318 component for 1.8.9). `buildAll` also
emits importable Prism instance zips (`dist/prism/MW19-<mc>.zip`) and [`docs/PRISM.md`](PRISM.md). Verification uses a
production-layout launch (remapped jar, real Fabric Loader/Forge, no Gradle dev runtime) because driving the user's
Prism install would use their Microsoft accounts.

## D-018 KeyCPS replaces the CPS counter (user request, 2026-09-25)
The owner asked for MW19's CPS counter to be replaced by **KeyCPS** (modrinth.com/mod/keycps), their own
keystrokes + CPS mod, and for it to appear in MW19's Mods GUI in place of KeyCPS's own settings screen. Authorship
was checked: the local KeyCPS repository (`~/Desktop/KeyCPS`, 1.6.1) is committed by the owner's address. Its
fabric.mod.json declares MIT while the Modrinth page lists All-Rights-Reserved. Either way, the copyright holder asked
for this integration. If the standalone mod (id `keycps`) is also installed, MW19 shows a startup notice to remove it,
because two overlays would draw.
- **Ported, not bundled.** [`KeyCpsModule`](../core/src/main/java/dev/mw19/core/modules/KeyCpsModule.java) (core, Java 8) re-implements KeyCPS 1.6.1's HUD (layout, fade, space-bar line,
  CPS inside the mouse keys, CPS warning, rainbow, always-LMB/RMB, per-key rates) and its counting ([`InputRates`](../core/src/main/java/dev/mw19/core/modules/InputRates.java): per
  binding, from input events, key repeat included) on MW19's platform API. Nesting the KeyCPS jar was rejected: it
  needs Fabric API (MW19 is Fabric-API-free), it has its own Right Shift settings screen and move screen (a second GUI
  and a key clash), it would sit outside profiles, server rules and the HUD editor, and it does not exist for 1.8.9.
- **Mapping of KeyCPS's UI:** module settings (Mods page) replace the settings screen; the HUD editor replaces "Move HUD"
  and the scale slider and supplies text colour and shadow; the module toggle and MW19's keybinds replace "Toggle HUD".
  Not ported: the first-join chat tip and the 14 translations (MW19's UI is English-only for now).
- The old `cps` and `keystrokes` modules and `ClickTracker` were removed. KeyCPS draws keystrokes and CPS in one element,
  and the first all-modules smoke run showed the old keystrokes element drawn on top of it.
- The ported code is part of MW19 and so falls under MW19's MIT licence. The standalone KeyCPS mod keeps its own
  licence.
- 1.8.9 limitation: LWJGL 2 sends no key-repeat events in game, so held keyboard keys count once there.

## D-019 Rename to MW19 (owner request, 2026-09-26)
Everything was renamed from the placeholder: packages `dev.kestrel` → `dev.mw19` (including the plugin API; no third-party
plugins existed yet), mod id `mw19`, mixin configs `mw19.*.json`, jars `MW19-<version>+mc<mc>.jar`, config folder
`MW19/`, smoke markers `MW19 SMOKE PASS`. Compatibility for the owner's existing setup: a `Kestrel/` folder is moved to
`MW19/` on first launch, and old `KESTREL-P1:` profile codes still import ([`RenameCompatTest`](../core/src/test/java/dev/mw19/core/RenameCompatTest.java)). "MW19" is also the
common nickname for Call of Duty: Modern Warfare (2019). That is fine as a name, but the Modrinth page should not
use Call of Duty branding.

## D-020 "Max FPS client": performance packs by reference, native Vulkan, no bundling (owner request, 2026-09-26)
The owner pointed at Frost Client (frostclient.eu) as the bar. From its public site: Minecraft 1.21+, **Sodium and
VulkanMod bundled**, "Vulkan by default", 50+ bundled mods, capes and badges, a paid tier, and a claim of 85 → 810 FPS
(RTX 3060 Ti, 4K, 20 chunks). That gain comes from Sodium and VulkanMod, not from client code. Frost's launcher and files
were not unpacked, run or copied. Its config folder holds account logins and was not opened.
- **MW19's own work:** Entity Culling, FPS Boost, the Vulkan switch on 26.2+ (Minecraft's own RenderPearl Vulkan backend,
  which falls back to OpenGL), and an allocation-free HUD. Measured in [PERF](PERF.md).
- **Other people's performance mods ship by reference:** [`scripts/mrpack.py`](../scripts/mrpack.py) writes `MW19 Performance` Modrinth packs
  (`.mrpack`) per version. Their index lists Modrinth CDN URLs and hashes for Sodium, ImmediatelyFast, FerriteCore and
  Lithium, and the launcher downloads them from Modrinth, so no third-party jar is redistributed. Only mods tested with MW19
  ([COMPAT_MATRIX](COMPAT_MATRIX.md)) go in.
- **VulkanMod** (LGPL-3.0) exists for 1.21–1.21.5, 1.21.9–1.21.11 and 26.1.x. It replaces the renderer and conflicts with
  Sodium, so it is not in the default pack. On 26.2+ Minecraft's own Vulkan backend makes it unnecessary.

## D-021 Exploit Protection: ExploitPreventer's list, MW19's own code, GRAY (owner request, 2026-09-26)
The owner asked to build [ExploitPreventer](https://modrinth.com/mod/exploitpreventer) (NikOverflow, MIT, Fabric
1.21.9+) into MW19. Only its public Modrinth description was read, for the list of exploits; its source was not read
or copied (hard rule: original work only). MW19's version is written from the behaviour of vanilla's own code (javap):
- **Sign and anvil text.** The sign editor's constructor turns each line into a string with `Component.getString()`,
  which resolves translation keys and keybinds with the client's language and bindings, and sends those strings back.
  A server can therefore open an editor with a mod's key and read whether it translated. MW19 re-resolves the lines
  after the constructor ([`SignEditMixin`](../fabric/src/main/java/dev/mw19/fabric/mixin/SignEditMixin.java)) and the anvil's name field ([`AnvilNameMixin`](../fabric/src/main/java/dev/mw19/fabric/mixin/AnvilNameMixin.java)) as an unmodded client
  with default bindings would: keys from vanilla's own `en_us.json` translate, other keys stay as written, vanilla
  keybinds show their default key. 1.8.9 needs nothing here: it sends unedited sign lines back as components, and anvil
  names are plain strings.
- **Resource pack addresses.** A pack URL may not name a local host or a loopback, private, link-local or unique-local
  address ([`LocalAddress`](../core/src/main/java/dev/mw19/core/net/LocalAddress.java)): checked without DNS when the request arrives and again with DNS
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
  ([`Upnp`](../core/src/main/java/dev/mw19/core/net/Upnp.java), SSDP + SOAP; XML with DTDs off; answers only from the device that responded; a 1-hour lease
  renewed while hosting, removed on stop, world close or exit) and an invite-only whitelist whenever the world is open
  to the internet. There is no relay server: behind carrier-grade NAT or with UPnP off, the page says so and names the
  port to forward by hand. Opening a port is a real exposure, so the internet switch is separate and the whitelist
  cannot be skipped.
- **Home screen:** vanilla's panorama (1.21+: `Screen.renderPanorama`; 1.8.9: MW19's own cube renderer, because
  vanilla's is private), a logo drawn from pixel blocks, the player model with a Skins button, and flat buttons.
  The particles and tip card were removed.
- **Not built: account switching.** An in-game switcher that reads the launcher's saved accounts would handle other
  programs' stored login tokens, which MW19's rules forbid (never read launcher account files). Launchers switch
  accounts themselves.
