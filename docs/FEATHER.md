# Feather / Dawn compatibility research

Checked 2026-09-25. Feather Client has become **Dawn**. The launcher installed on the dev machine is
`gg.dawn.Launcher` 0.2.0.41 (Flatpak, described as "Dawn (Feather) - The modern Minecraft launcher").
Its game mod still ships `mixins.feather.*.json` configs and `net/digitalingot/feather-*` artifacts
(digitalingot is Feather's Maven group). This doc treats "Feather" and "Dawn" as the same product line.

Everything marked **[local]** was read from the Dawn install on this machine. We read metadata, logs
and manifests only, never decompiled code. Everything marked **[web]** is a secondary source.

## 1. How Dawn loads Fabric (1.17.1 → 26.3)

| Fact | Evidence |
|---|---|
| Profiles are standard Fabric instances: `profile.json` → `"loader": {"kind": "Fabric", "version": "0.19.3"}`, MC `26.2` | [local] `~/.var/app/gg.dawn.Launcher/data/dawn/.dawn/profiles/dawn-performance/profile.json` |
| User mods go in `<profile>/private-game-content/mods/`; `runtimePolicy.modAuthority = "UserManaged"` | [local] same profile dir |
| Launch log: `Loading Minecraft 26.2 with Fabric Loader 0.19.3`, `- java 25`, `\-- mixinextras 0.5.4`, `SpongePowered MIXIN Subsystem Version=0.8.7 ... Service=Knot/Fabric` | [local] `logs/game-sessions/825c6983…/chunk-000000.log` |
| Dawn injects its own client mod (`- dawn stable/4e93ec08`, `artifact=dawn-client.jar … stage=mod-injected`) | [local] same log + `logs/launcher.log` |
| Dawn injects **Fabric API** itself (`artifact=fabric-api-0.160.0-26.2.jar … stage=fabric-api-injected`) | [local] `logs/launcher.log` |
| 1.21.4 deployment: main class `net.fabricmc.loader.launch.knot.KnotClient`, Fabric Loader 0.19.5, intermediary 1.21.4, `fabric-api-0.119.4+1.21.4` **on the classpath** (not in `mods/`), runtime `zulu-21` | [web, first-party] `https://api.dawn.gg/v2/deployments/game/stable/1.21.4-fabric/manifest.json` |
| Sodium is loaded in the 26.2 profile (`Reloading ResourceManager: … sodium, dawn/mcpvp-default`) | [local] 26.2 game log |
| Graphics backend line on 26.2: `Using graphics backend OpenGL` (LWJGL 3.4.1; `lwjgl-vulkan` is on the classpath) | [local] 26.2 game log |
| Dawn keeps a mod-conflict catalog; the only rule today blocks **VulkanMod** (`severity: blockLaunch`) | [local] `cache/mod-conflicts/catalog.json` |

**Consequences for us**
- A standard Fabric mod in the profile's mods folder is the supported path. No Dawn-specific hacks are needed.
- Fabric API is present on Dawn but **not** guaranteed on Prism or vanilla Fabric, so the mod stays
  Fabric-API-free (see [DECISIONS](DECISIONS.md) D-004).
- Dawn bundles Sodium plus its own `mixins.feather.*` mixins. Our optimisation modules must detect
  Sodium and Dawn (mod id `dawn`) and step aside where they overlap (`compat.*` flags).
- Mixin 0.8.7 and MixinExtras 0.5.4 are available on every modern target, since Fabric Loader bundles them.

## 2. How Dawn loads 1.8.9

Dawn's stable index (`cache/dawn-client/version-index/stable.json` [local]) lists exactly two Forge
deployments: `1.8.9-forge` and `1.12.2-forge`. The public 1.8.9 manifest
(`https://api.dawn.gg/v2/deployments/game/stable/1.8.9-forge/manifest.json`) shows:

- `main_class`: `net.minecraft.launchwrapper.Launch` (LaunchWrapper 1.12)
- Forge `1.8.9-11.15.1.2318-1.8.9` (universal)
- Tweakers: `--tweakClass net.minecraftforge.fml.common.launcher.FMLTweaker --tweakClass org.spongepowered.asm.launch.MixinTweaker`
- Mixin: `net/digitalingot/mixin0/0.8.7/mixin-0.8.7-legacy.jar` (Feather's legacy fork), `mixinextras-common-0.5.0-beta.4`, `asm-all 5.2`
- Injected mods: `FeatherOpt-1.0.0-SNAPSHOT.jar` (an optimisation mod) and `feather-1.8.9-forge-1.0.0-SNAPSHOT.jar`
- Runtime: `zulu-8`

**Consequences**
- **1.8.9 target = Forge 11.15.1.2318.** Legacy Fabric is not offered by Dawn, so Forge is the only option.
- Mixin is already bootstrapped by the launcher (0.8.7-legacy). A normal Forge mod that declares
  `TweakClass: org.spongepowered.asm.launch.MixinTweaker` + `MixinConfigs` in its manifest and shades
  Mixin 0.7.11 will run on Dawn's Mixin (it is first on the classpath) and on plain Forge (our shaded copy).
  Mixins must therefore stay within the 0.7.x feature set: `@Inject`, `@Redirect`, `@ModifyVariable`,
  `@ModifyArg`, `@Accessor`, `@Invoker`. No MixinExtras on 1.8.9.
- FeatherOpt is always present on Dawn 1.8.9. Our 1.8.9 build has no optimisation modules
  (optimisations are a 1.21+/26.x feature), so there is nothing to collide with.

## 3. Supported versions (Dawn stable index, 2026-09-25)

`1.8.9-forge, 1.12.2-forge, 1.17.1, 1.18.2, 1.19.2, 1.19.3, 1.19.4, 1.20, 1.20.1, 1.20.2, 1.20.4,
1.20.6, 1.21, 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.7, 1.21.8, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2,
26.2, 26.3` (all Fabric unless noted).

Of our 18 targets, **1.21.2, 1.21.6 and 1.21.9 have no Dawn deployment**. Those jars can only be
verified on Prism or vanilla Fabric.

## 4. Web sources (secondary)

- Dawn news, "Feather is now Dawn!": https://dawn.gg/community/news/feather-is-now-dawn (page is JS-rendered; the title was confirmed via search results)
- Search-result summaries stating that Feather's 1.8.9 runs on Forge and accepts user Forge/Fabric mods: https://en.namu.wiki/w/Feather%20Client , https://featherclientdownload.wiki/minecraft-versions/ (unofficial)
- The first-party manifests above (`api.dawn.gg`) are stronger evidence than any of these.

## 5. What we cannot verify here

- We cannot sign in to Dawn from an automated session. It needs the user's Microsoft account. So
  "Feather/Dawn verified" stays **not-run** in [`docs/COMPAT_MATRIX.md`](COMPAT_MATRIX.md) until someone runs the manual
  checklist in `docs/FEATHER_CHECKLIST.md` (written in Phase 7).
- Whether Dawn's own mixins conflict with ours is unknown until a real run. The runtime `compat.dawn` flag
  (mod id `dawn`) is the mitigation hook.
