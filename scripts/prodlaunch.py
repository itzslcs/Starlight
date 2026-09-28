#!/usr/bin/env python3
"""Test-only production launch: starts real Fabric (Knot, intermediary-mapped game) with the distributed Starlight jar in a
mods folder, the way a launcher does, in offline mode. A dev run cannot show what only the shipped jar does: nested
jar-in-jar mods (the bundled VulkanMod), remapping, the early renderer switch. Used by `PROD=1 scripts/smoke.sh`.

Libraries come from Mojang's version JSON (Loom's copy) and Fabric Meta's launch profile, downloaded once into
~/.cache/starlight-prod with their SHA-1 checked. Assets are Loom's. No account is used or read: the name is a fixed
offline one. usage: prodlaunch.py <mc> <game-dir> <mods-jar>... [-- <extra jvm args>]
"""
import hashlib, json, os, subprocess, sys, urllib.request

UA = {"User-Agent": "itzslcs/starlight-tests/0.1.0"}
LOOM = os.path.expanduser("~/.gradle/caches/fabric-loom")
CACHE = os.path.expanduser("~/.cache/starlight-prod")
LOADER = "0.19.5"


def get(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=120).read()


def fetch(url, path, sha1=None):
    if not os.path.exists(path):
        data = get(url)
        if sha1 and hashlib.sha1(data).hexdigest() != sha1:
            sys.exit(f"SHA-1 mismatch: {url}")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path + ".part", "wb") as f:
            f.write(data)
        os.replace(path + ".part", path)
    return path


def allowed(lib):
    rules = lib.get("rules")
    if not rules:
        return True
    ok = False
    for r in rules:
        if "features" in r:
            continue
        os_name = r.get("os", {}).get("name")
        if os_name is None or os_name == "linux":
            ok = r["action"] == "allow"
    return ok


def maven_path(coord):
    parts = coord.split(":")
    group, art, ver = parts[0], parts[1], parts[2]
    cls = "-" + parts[3] if len(parts) > 3 else ""
    return f"{group.replace('.', '/')}/{art}/{ver}/{art}-{ver}{cls}.jar"


def main():
    args = sys.argv[1:]
    extra = []
    if "--" in args:
        extra = args[args.index("--") + 1:]
        args = args[:args.index("--")]
    mc, game_dir, mods = args[0], os.path.abspath(args[1]), args[2:]
    info = json.load(open(os.path.join(LOOM, mc, "mojang_minecraft_info.json")))
    def key(coord):  # group:artifact[:classifier]; Fabric's pins (e.g. a newer ASM) replace Mojang's
        parts = coord.split(":")
        return ":".join(parts[:2] + parts[3:])

    libs = {}
    for lib in info["libraries"]:
        art = lib.get("downloads", {}).get("artifact")
        if art and allowed(lib):
            libs[key(lib["name"])] = fetch(art["url"], os.path.join(CACHE, "libraries", art["path"]), art.get("sha1"))
    profile = json.loads(get(f"https://meta.fabricmc.net/v2/versions/loader/{mc}/{LOADER}/profile/json"))
    for lib in profile["libraries"]:
        path = maven_path(lib["name"])
        libs[key(lib["name"])] = fetch(lib.get("url", "https://maven.fabricmc.net/").rstrip("/") + "/" + path,
                                       os.path.join(CACHE, "libraries", path), lib.get("sha1"))
    client = info["downloads"]["client"]
    uniq = list(libs.values()) + [fetch(client["url"], os.path.join(CACHE, "versions", mc, f"{mc}.jar"), client.get("sha1"))]
    mods_dir = os.path.join(game_dir, "mods")
    os.makedirs(mods_dir, exist_ok=True)
    for f in os.listdir(mods_dir):
        os.remove(os.path.join(mods_dir, f))
    for m in mods:
        os.symlink(os.path.abspath(m), os.path.join(mods_dir, os.path.basename(m)))
    # Loom's assets, under the layout the game expects (Loom names its index files "<mc>-<id>.json")
    assets = os.path.join(CACHE, "assets")
    os.makedirs(os.path.join(assets, "indexes"), exist_ok=True)
    idx = info["assetIndex"]["id"]
    if not os.path.exists(os.path.join(assets, "objects")):
        os.symlink(os.path.join(LOOM, "assets", "objects"), os.path.join(assets, "objects"))
    fetch(info["assetIndex"]["url"], os.path.join(assets, "indexes", f"{idx}.json"), info["assetIndex"].get("sha1"))
    java = (os.path.expanduser("~/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2/bin/java")
            if info.get("javaVersion", {}).get("majorVersion", 21) >= 25 else "/usr/lib/jvm/java-21-openjdk/bin/java")
    jvm = [java, "-Xmx2G", *[a for a in profile.get("arguments", {}).get("jvm", [])], *extra, "-cp", ":".join(uniq),
           profile["mainClass"]]
    game = ["--username", "Player", "--uuid", "00000000000000000000000000000000", "--accessToken", "0",
            "--userType", "legacy", "--version", mc, "--versionType", "release", "--gameDir", game_dir,
            "--assetsDir", assets, "--assetIndex", idx]
    print("prodlaunch:", mc, "loader", LOADER, "mods", [os.path.basename(m) for m in mods], flush=True)
    os.chdir(game_dir)
    os.execv(java, jvm + game)


if __name__ == "__main__":
    main()
