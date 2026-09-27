<!-- The Modrinth project description, uploaded as-is by scripts/modrinth.py (everything below the next line). -->
<!-- body -->
**MW19 Client** is a client-side HUD, GUI and performance suite for Minecraft **1.8.9 (Forge)** and **every release from 1.21 to 26.3 (Fabric)**. Legit features only: no cheats, no automation. **Fabric API is not required.**

> **Alpha.** Every version is launched in an automated headless test that clicks the menu with a real mouse, opens the Skins, Packs and Host World pages, runs a live pack search, hosts the test world, compares Fast Chests with vanilla and checks Exploit Protection, and every Fabric jar is also started in a real Fabric launch. Please report anything that breaks.

## Home screen
MW19 replaces the title screen: the game's panorama, your skin in 3D (drag to turn it), and one click to **Skins**, **Host World**, **Packs** and the MW19 menu. *Vanilla menu* opens the original title screen, and you can turn the home screen off in the settings.

## Skins
Preview skins in 3D, copy any player's skin by name, drop PNG files on the window, and change your own skin without leaving the game (Microsoft accounts; it uses Mojang's official skin service, and your login token is only sent there, only when you press *Use this skin*).

## Packs
Search Modrinth's resource packs in game, install one in a click (the download is checked against Modrinth's hash) and enable it on top of your packs.

## Host World
Open your singleplayer world to friends on your network, or over the internet: MW19 asks your router to forward the port (UPnP) and only players you invite by name can join. The forward is removed when you stop hosting or leave.

## More FPS
- **Graphics presets with auto-detection**: Potato (integrated graphics and old laptops), Low, Medium and High, all tuned for frame rate. On first start MW19 detects your graphics card, CPU and memory and picks one by itself; *Undo* puts your own settings back.
- **Vulkan built in** (1.21.9 – 1.21.11): the jar contains VulkanMod (LGPL-3.0, unmodified; its source zip is attached
  to each of those versions). The first start runs on OpenGL while MW19 checks your graphics card; from the second start
  it renders with Vulkan. If a Vulkan start ever fails, the next one is back on OpenGL by itself, and with Sodium or
  Iris installed it stays off. On **26.2 and 26.3** the same switch uses Minecraft's own Vulkan renderer.
- **Fast Chests** (on by default, 1.21+): chests are drawn as ordinary blocks instead of every frame (+22 % FPS with
  1000 chests in view in our benchmark). Lids no longer open visibly; turn the module off for the animation.
- **Entity Culling** (on by default): mobs, items, chests, signs and heads fully hidden behind blocks are not drawn.
  Players, glowing mobs and name tags always stay visible.

## Features
- **Menu** (Right Shift): Mods, HUD Editor, Profiles, Keybinds, Skins, Packs, Host World, Server Rules, Performance, Themes.
  Minecraft's own pause menu and server list get MW19's look, and the pause menu opens the MW19 menu and the pack browser.
- **HUD**: FPS, **KeyCPS** (keystrokes + CPS that follow your key bindings), Speed, Ping, Coordinates, Direction, Armor Status, Potion Effects, Item Counter, Clock, Memory/CPU, FPS Graph, Server Address, Combo Counter, Saturation, Session Time, Pack Display, Stopwatch, Day Counter, Reach Display.
- **Visual**: Custom Crosshair, Brightness, Hit Color, Damage Tilt, Low Fire, Shield Overlay, Particle Multiplier, Show Own Nametag, 1.8 Combat Visuals, Clear Weather, Low Health Warning, Hitboxes, TNT Timer.
- **Utility**: Zoom, Toggle Sprint/Sneak, Freelook, Screenshot Tools, Durability Alert, Chat Tools, Quick Commands, **Exploit Protection** (servers cannot detect your mods through sign/anvil text tricks or probe your home network through resource pack addresses; off by default).
- **Themes**: nine colour presets including Black, White and Crystal, with smooth menu animations.

KeyCPS is built in, so you don't need the standalone KeyCPS mod alongside MW19.

## Fair play
Modules that some servers restrict are off by default, and server rules can switch modules off (for example, Freelook is disabled on Hypixel).

## Privacy
No telemetry, no accounts, no analytics. MW19 only goes online when you use a feature that needs it: the Packs page (Modrinth), changing or copying a skin (Mojang), inviting a player (Mojang) and internet hosting (your router). The bundled VulkanMod's own update check is switched off.

## Install
Put the jar for your Minecraft version in your `mods` folder:
- 1.21 – 1.21.11: Fabric Loader 0.16 or newer
- 26.1 – 26.3: Fabric Loader 0.18 or newer (Java 25)
- 1.8.9: Forge 11.15.1.2318
