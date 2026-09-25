#!/usr/bin/env python3
"""Proves Kestrel's injections were applied: disassembles the classes Mixin exported during a dev run
(-Dmixin.debug.export) and checks each target method calls our handler.
usage: scripts/mixin-audit.py <run-dir> <mc-version>   (exit 1 if anything is not wired)"""
import glob, os, re, subprocess, sys

run, mc = sys.argv[1], sys.argv[2]
def ver(v): return tuple(int(x) for x in re.findall(r'\d+', v))
V = ver(mc)
if mc == "1.8.9":
    checks = [("net.minecraft.client.renderer.entity.RendererLivingEntity", "setBrightness", "kestrel$r"),
              ("net.minecraft.client.renderer.entity.RendererLivingEntity", "canRenderName", "kestrel$own"),
              ("net.minecraft.client.renderer.EntityRenderer", "hurtCameraEffect", "kestrel$tilt"),
              ("net.minecraft.client.renderer.EntityRenderer", "orientCamera", "kestrel$yaw"),
              ("net.minecraft.client.renderer.ItemRenderer", "renderFireInFirstPerson", "kestrel$fireAlpha"),
              ("net.minecraft.client.particle.EffectRenderer", "emitParticleAtEntity", "kestrel$more"),
              ("net.minecraft.entity.Entity", "setAngles", "kestrel$turn"),
              ("net.minecraft.client.renderer.entity.Render", "renderLivingLabel", "kestrel$label"),
              ("net.minecraft.client.gui.GuiPlayerTabOverlay", "getPlayerName", "kestrel$decorate"),
              ("net.minecraft.client.Minecraft", "shutdownMinecraftApplet", "kestrel$shutdown")]
    jars = [j for j in glob.glob(os.path.expanduser("~/.gradle/caches/essential-loom/minecraftMaven/**/*.jar"), recursive=True)
            if "sources" not in j and "-srg-" not in j and "-intermediary-" not in j]
else:
    nametag = "getNameTag" if V >= (1, 21, 2) else "renderNameTag"
    checks = [("net.minecraft.client.gui.Gui", "render", "kestrel$hud"),
              ("net.minecraft.client.gui.Gui", "renderCrosshair", "kestrel$crosshair"),
              ("net.minecraft.client.Minecraft", "tick", "kestrel$tickStart"),
              ("net.minecraft.client.KeyboardHandler", "keyPress", "kestrel$key"),
              ("net.minecraft.client.MouseHandler", "onScroll", "kestrel$scroll"),
              ("net.minecraft.client.renderer.LightTexture", "updateLightTexture", "kestrel$gamma"),
              ("net.minecraft.client.renderer.GameRenderer", "getFov", "kestrel$zoom"),
              ("net.minecraft.client.renderer.GameRenderer", "bobHurt", "kestrel$tilt"),
              ("net.minecraft.client.renderer.ScreenEffectRenderer", "renderFire", "kestrel$fireAlpha"),
              ("net.minecraft.client.renderer.ItemInHandRenderer", "tick", "kestrel$noDip"),
              ("net.minecraft.client.renderer.ItemInHandRenderer", "renderArmWithItem", "kestrel$shieldDown"),
              ("net.minecraft.client.Camera", "setup", "kestrel$pitch"),
              ("net.minecraft.world.entity.Entity", "turn", "kestrel$turn"),
              ("net.minecraft.client.gui.components.ChatComponent", "addMessage", "kestrel$chat"),
              ("net.minecraft.client.renderer.entity.EntityRenderer", nametag, "kestrel$decorate"),
              ("net.minecraft.client.gui.components.PlayerTabOverlay", "getNameForDisplay", "kestrel$decorate"),
              ("net.minecraft.client.particle.ParticleEngine", "createTrackingEmitter", "kestrel$more"),
              ("net.minecraft.client.renderer.entity.LivingEntityRenderer", "shouldShowName", "kestrel$own"),
              ("net.minecraft.client.multiplayer.MultiPlayerGameMode", "attack", "kestrel$attack")]
    M = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft")
    jars = [j for p in (f"{M}/minecraft-clientonly/{mc}-loom.mappings.*/*.jar", f"{M}/minecraft-common/{mc}-loom.mappings.*/*.jar",
                        f"{M}/minecraft-merged/{mc}/*.jar", f"{M}/minecraft-clientonly/{mc}/*.jar", f"{M}/minecraft-common/{mc}/*.jar")
            for j in glob.glob(p) if "sources" not in j]
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
