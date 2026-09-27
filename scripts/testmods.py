#!/usr/bin/env python3
"""Test-only: put other mods (e.g. Sodium) into a dev run's mods folder to check compatibility and measure together.
Downloads the newest Fabric release for the Minecraft version from Modrinth, verifies its SHA-512 and caches it.
Never bundled with MW19. usage: scripts/testmods.py <mc> <dest-mods-dir> <slug> [<slug> ...]"""
import hashlib, json, os, sys, urllib.parse, urllib.request, zipfile

UA = {"User-Agent": "itzslcs/mw19-tests/0.1.0"}
CACHE = os.path.expanduser("~/.cache/mw19-test-mods")

def get(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60).read()

def main():
    args = sys.argv[1:]
    nest = "--no-unnest" not in args  # a production launch loads nested jars itself
    args = [a for a in args if a != "--no-unnest"]
    mc, dest, slugs = args[0], args[1], args[2:]
    os.makedirs(dest, exist_ok=True)
    os.makedirs(CACHE, exist_ok=True)
    for slug in slugs:
        q = urllib.parse.urlencode({"loaders": json.dumps(["fabric"]), "game_versions": json.dumps([mc])})
        versions = json.loads(get(f"https://api.modrinth.com/v2/project/{slug}/version?{q}"))
        releases = [v for v in versions if v["version_type"] == "release"] or versions
        if not releases:
            sys.exit(f"{slug}: no Fabric build for {mc}")
        f = next((x for x in releases[0]["files"] if x.get("primary")), releases[0]["files"][0])
        path = os.path.join(CACHE, f["filename"])
        if not os.path.exists(path):
            data = get(f["url"])
            if hashlib.sha512(data).hexdigest() != f["hashes"]["sha512"]:
                sys.exit(f"{slug}: SHA-512 mismatch")
            open(path, "wb").write(data)
        target = os.path.join(dest, f["filename"])
        if not os.path.exists(target):
            os.symlink(path, target)
        print(f"{slug} {releases[0]['version_number']} -> {target}")
        if nest:
            unnest(path, dest)


def unnest(jar, dest):
    """A dev run does not load jar-in-jar contents (a real install does), so nested jars are unpacked next to the mods:
    nested mods into <dest> (Loom remaps them), plain libraries into <dest>/../compat-libs (runtime classpath)."""
    libs = os.path.join(os.path.dirname(dest.rstrip("/")), "compat-libs")
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if not (name.startswith("META-INF/jars/") and name.endswith(".jar")):
                continue
            data = z.read(name)
            import io
            is_mod = "fabric.mod.json" in zipfile.ZipFile(io.BytesIO(data)).namelist()
            out_dir = dest if is_mod else libs
            os.makedirs(out_dir, exist_ok=True)
            out = os.path.join(out_dir, os.path.basename(name))
            if not os.path.exists(out):
                open(out, "wb").write(data)

if __name__ == "__main__":
    main()
