#!/usr/bin/env python3
"""Proves MW19's injections were applied: disassembles the classes Mixin exported during a dev run
(-Dmixin.debug.export) and checks each target method calls our handler.
usage: scripts/mixin-audit.py <run-dir> <mc-version>   (exit 1 if anything is not wired)"""
import glob, os, re, subprocess, sys

run, mc = sys.argv[1], sys.argv[2]
def ver(v): return tuple(int(x) for x in re.findall(r'\d+', v))
V = ver(mc)
if mc == "1.8.9":
    checks = [("net.minecraft.client.renderer.entity.RendererLivingEntity", "setBrightness", "mw19$r"),
              ("net.minecraft.client.renderer.entity.RendererLivingEntity", "canRenderName", "mw19$own"),
              ("net.minecraft.client.renderer.EntityRenderer", "hurtCameraEffect", "mw19$tilt"),
              ("net.minecraft.client.renderer.EntityRenderer", "orientCamera", "mw19$yaw"),
              ("net.minecraft.client.renderer.ItemRenderer", "renderFireInFirstPerson", "mw19$fireAlpha"),
              ("net.minecraft.client.particle.EffectRenderer", "emitParticleAtEntity", "mw19$more"),
              ("net.minecraft.entity.Entity", "setAngles", "mw19$turn"),
              ("net.minecraft.client.renderer.entity.Render", "renderLivingLabel", "mw19$label"),
              ("net.minecraft.client.gui.GuiPlayerTabOverlay", "getPlayerName", "mw19$decorate"),
              ("net.minecraft.client.Minecraft", "shutdownMinecraftApplet", "mw19$shutdown"),
              ("net.minecraft.client.renderer.entity.RenderManager", "shouldRender", "mw19$cull"),
              ("net.minecraft.world.World", "getRainStrength", "mw19$rain")]
    jars = [j for j in glob.glob(os.path.expanduser("~/.gradle/caches/essential-loom/minecraftMaven/**/*.jar"), recursive=True)
            if "sources" not in j and "-srg-" not in j and "-intermediary-" not in j]
else:
    nametag = "getNameTag" if V >= (1, 21, 2) else "renderNameTag"
    unobf = V >= (26, 1)  # 26.1: render -> extract, new homes for FOV, gamma and camera angles
    hud = "net.minecraft.client.gui.Hud" if V >= (26, 2) else "net.minecraft.client.gui.Gui"
    hands = "net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer" if V >= (26, 3) else "net.minecraft.client.renderer.ItemInHandRenderer"
    swap = "net.minecraft.client.player.FirstPersonHandsAndItems" if V >= (26, 3) else "net.minecraft.client.renderer.ItemInHandRenderer"
    fire = ([("net.minecraft.client.renderer.ScreenEffectRenderer", "lambda$submitFire$0", "mw19$fireY"),
             ("net.minecraft.client.renderer.ScreenEffectRenderer", "buildFireQuad", "mw19$fireAlpha")] if V >= (26, 2) else
            [("net.minecraft.client.renderer.ScreenEffectRenderer", "renderFire", "mw19$fireAlpha")])
    checks = [(hud, "extractRenderState" if unobf else "render", "mw19$hud"),
              (hud, "extractCrosshair" if unobf else "renderCrosshair", "mw19$crosshair"),
              ("net.minecraft.client.Minecraft", "tick", "mw19$tickStart"),
              ("net.minecraft.client.KeyboardHandler", "keyPress", "mw19$key"),
              ("net.minecraft.client.MouseHandler", "onScroll", "mw19$scroll"),
              ("net.minecraft.client.renderer.LightmapRenderStateExtractor", "extract", "mw19$gamma") if unobf else
              ("net.minecraft.client.renderer.LightTexture", "updateLightTexture", "mw19$gamma"),
              ("net.minecraft.client.Camera", "calculateFov", "mw19$zoom") if unobf else
              ("net.minecraft.client.renderer.GameRenderer", "getFov", "mw19$zoom"),
              ("net.minecraft.client.renderer.GameRenderer", "bobHurt", "mw19$tilt"),
              *fire,
              (swap, "tick", "mw19$noDip"),
              (hands, "submitArmWithItem" if V >= (26, 2) else "renderArmWithItem", "mw19$shieldDown"),
              ("net.minecraft.client.Camera", "alignWithEntity" if unobf else "setup", "mw19$pitch"),
              ("net.minecraft.world.entity.Entity", "turn", "mw19$turn"),
              ("net.minecraft.client.gui.components.ChatComponent", "addMessage", "mw19$chat"),
              ("net.minecraft.client.renderer.entity.EntityRenderer", nametag, "mw19$decorate"),
              ("net.minecraft.client.gui.components.PlayerTabOverlay", "getNameForDisplay", "mw19$decorate"),
              ("net.minecraft.client.particle.ParticleEngine", "createTrackingEmitter", "mw19$more"),
              ("net.minecraft.client.renderer.entity.LivingEntityRenderer", "shouldShowName", "mw19$own"),
              ("net.minecraft.client.multiplayer.MultiPlayerGameMode", "attack", "mw19$attack"),
              ("net.minecraft.client.gui.Gui" if V >= (26, 2) else "net.minecraft.client.Minecraft", "setScreen", "mw19$home"),
              ("net.minecraft.client.renderer.entity.EntityRenderDispatcher", "shouldRender", "mw19$cull"),
              ("net.minecraft.world.level.Level", "getRainLevel", "mw19$rain")]
    M = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft")
    jars = [j for p in (f"{M}/minecraft-clientonly/{mc}-loom.mappings.*/*.jar", f"{M}/minecraft-common/{mc}-loom.mappings.*/*.jar",
                        f"{M}/minecraft-merged/{mc}/*.jar", f"{M}/minecraft-clientonly/{mc}/*.jar", f"{M}/minecraft-common/{mc}/*.jar")
            for j in glob.glob(p) if "sources" not in j]
    if not jars:  # 26.x ships unobfuscated: Loom keeps the raw, already-named jar
        jars = glob.glob(os.path.expanduser(f"~/.gradle/caches/fabric-loom/{mc}/minecraft-client.jar"))
out_dir = os.path.join(run, ".mixin.out", "class")
cp = out_dir + ":" + ":".join(jars)
bad = 0
for cls, meth, handler in checks:
    res = subprocess.run(["javap", "-c", "-p", "-cp", cp, cls], capture_output=True, text=True).stdout
    wired = any(re.search(r"\b" + re.escape(meth) + r"\(", part.split("\n", 1)[0]) and handler in part.split("\n", 1)[1]
                for part in re.split(r"\n  (?=\S)", res) if "\n" in part)
    exported = os.path.exists(os.path.join(out_dir, cls.replace(".", "/") + ".class"))
    if not wired: bad += 1
    print(f"{'WIRED' if wired else 'NOT WIRED' + ('' if exported else ' (class not exported)')}  {cls.split('.')[-1]}.{meth} -> {handler}")
print(f"mixin audit {mc}: {len(checks) - bad}/{len(checks)} wired")
sys.exit(1 if bad else 0)
