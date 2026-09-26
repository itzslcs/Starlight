# Prism Launcher

See also: [README](../README.md) (install table), [DECISIONS](DECISIONS.md) (D-017), [COMPAT_MATRIX](COMPAT_MATRIX.md) (Prism column).

MW19 is an ordinary mod jar, so Prism needs nothing special. There are two ways to set it up.

## A. Import a ready-made instance (easiest)
`./gradlew buildAll` writes one instance zip per Minecraft version to `dist/prism/MW19-<mc>.zip`.

1. In Prism: **Add Instance → Import**, choose the zip, then **OK**.
2. Select the new instance **MW19 &lt;mc&gt;** and press **Launch** (log in with your own account when Prism asks).
3. In game, press **Right Shift** to open MW19.

Each zip contains:
| File | Contents |
|---|---|
| `mmc-pack.json` | Minecraft + Fabric Loader 0.19.5 (+ intermediary), or Minecraft 1.8.9 + Forge 11.15.1.2318. The component ids and versions were checked against Prism's own metadata |
| `instance.cfg` | name `MW19 <mc>`, everything else left to Prism's defaults |
| `minecraft/mods/` | the MW19 jar for that version |
| `minecraft/MW19/plugins/` | the **Tier Tags** addon, which stays off until you approve it on the Plugins page ([PLUGIN_API](PLUGIN_API.md) explains the approval) |

MW19 needs the same Java as its Minecraft version: 8 for 1.8.9, 21 for 1.21.x, 25 for 26.x. With Prism's automatic
Java setting on, Prism downloads it; otherwise pick one under **Edit → Settings → Java**.

## B. Add the jar to an instance you already have
1. **Edit** the instance → **Version**. It needs *Fabric Loader* (0.16 or newer, 0.18 or newer for 26.x), or *Forge*
   11.15.1.2318 for 1.8.9. **Fabric API is not needed.**
2. **Mods → Add file**, then pick `MW19-<version>+mc<mc>.jar` for exactly that Minecraft version.

MW19 is meant to run next to performance mods such as Sodium, but that is not tested yet. If a mod does the same job
as a MW19 feature (freelook, perspective), MW19's matching mixin steps aside ([ARCHITECTURE](ARCHITECTURE.md)). The
standalone KeyCPS mod is not needed: it is built in, and MW19 shows a notice if both are installed.

## Where things are
- MW19's settings, profiles and plugins: `<instance>/minecraft/MW19/`.
- Log: Prism's **Minecraft Log** tab. MW19's lines are tagged `(MW19)`.

## Verification status
The instance zips and a real Prism launch are **not verified yet**: the Prism column of
[COMPAT_MATRIX](COMPAT_MATRIX.md) is `not-run`. Phase 7 checks them with a production-layout launch (the real loader and
the built jar, outside Gradle). That test does not drive your Prism install, because that would use your Microsoft
account.
