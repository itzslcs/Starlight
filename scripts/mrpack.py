#!/usr/bin/env python3
"""Builds "Starlight Performance" Modrinth packs (.mrpack) for Prism Launcher, the Modrinth app and ATLauncher.

Each pack is Starlight plus performance mods that are *referenced*, not copied. The index lists Modrinth CDN URLs and
hashes, and the launcher downloads them from Modrinth, so no one else's jar is redistributed. Starlight's own jar is
included under overrides/. Only mods that were tested with Starlight are listed (docs/COMPAT_MATRIX.md).

usage: scripts/mrpack.py <dist-dir> [mc ...]      (default: every Fabric jar in <dist-dir>)
       -> <dist-dir>/modrinth/Starlight-Performance-<mc>.mrpack
"""
import glob, hashlib, json, os, re, sys, urllib.parse, urllib.request, zipfile

# Sodium 0.8 needs Fabric API (block view, fluid rendering and resource loader modules), so the pack carries it too.
MODS = ["fabric-api", "sodium", "immediatelyfast", "ferrite-core", "lithium"]
NAMES = {"fabric-api": "Fabric API", "sodium": "Sodium", "immediatelyfast": "ImmediatelyFast", "ferrite-core": "FerriteCore", "lithium": "Lithium"}
UA = {"User-Agent": "itzslcs/starlight-mrpack/0.2.0"}
LOADER = "0.19.5"


def api(url):
    return json.loads(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60).read())


def newest(slug, mc):
    q = urllib.parse.urlencode({"loaders": json.dumps(["fabric"]), "game_versions": json.dumps([mc])})
    versions = api(f"https://api.modrinth.com/v2/project/{slug}/version?{q}")
    releases = [v for v in versions if v["version_type"] == "release"]
    return releases[0] if releases else None


def pack(dist, jar, mc):
    version = re.search(r"Starlight-(.+)\+mc", os.path.basename(jar)).group(1)
    files, skipped = [], []
    for slug in MODS:
        v = newest(slug, mc)
        if v is None:
            skipped.append(slug)
            continue
        f = next((x for x in v["files"] if x.get("primary")), v["files"][0])
        files.append({"path": "mods/" + f["filename"], "hashes": {"sha1": f["hashes"]["sha1"], "sha512": f["hashes"]["sha512"]},
                      "env": {"client": "required", "server": "unsupported"}, "downloads": [f["url"]], "fileSize": f["size"]})
    index = {"formatVersion": 1, "game": "minecraft", "versionId": f"{version}+mc{mc}", "name": f"Starlight Performance {mc}",
             "summary": "Starlight Client with " + ", ".join(NAMES[slug] for slug in MODS if slug not in skipped),
             "files": files, "dependencies": {"minecraft": mc, "fabric-loader": LOADER}}
    out_dir = os.path.join(dist, "modrinth")
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.join(out_dir, f"Starlight-Performance-{mc}.mrpack")
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr(zipfile.ZipInfo("modrinth.index.json", (1980, 1, 1, 0, 0, 0)), json.dumps(index, indent=2))
        z.write(jar, "overrides/mods/" + os.path.basename(jar))
    print(f"{mc:8} {len(files)} mods" + (f" (no {mc} build: {', '.join(skipped)})" if skipped else "") + f" -> {out}")


def main():
    dist = sys.argv[1]
    wanted = set(sys.argv[2:])
    for jar in sorted(glob.glob(os.path.join(dist, "Starlight-*+mc*.jar"))):
        mc = re.search(r"\+mc(.+)\.jar$", jar).group(1)
        if mc == "1.8.9" or (wanted and mc not in wanted):
            continue
        pack(dist, jar, mc)


if __name__ == "__main__":
    main()
