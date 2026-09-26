#!/usr/bin/env python3
"""Test-only: put other mods (e.g. Sodium) into a dev run's mods folder to check compatibility and measure together.
Downloads the newest Fabric release for the Minecraft version from Modrinth, verifies its SHA-512 and caches it.
Never bundled with MW19. usage: scripts/testmods.py <mc> <dest-mods-dir> <slug> [<slug> ...]"""
import hashlib, json, os, sys, urllib.parse, urllib.request

UA = {"User-Agent": "itzslcs/mw19-tests/0.1.0"}
CACHE = os.path.expanduser("~/.cache/mw19-test-mods")

def get(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60).read()

def main():
    mc, dest, slugs = sys.argv[1], sys.argv[2], sys.argv[3:]
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

if __name__ == "__main__":
    main()
