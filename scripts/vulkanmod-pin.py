#!/usr/bin/env python3
"""Pins the VulkanMod build bundled into each Starlight Fabric jar (fabric/bundled.json, read by fabric/build.gradle.kts):
the newest release with a known source commit for that Minecraft version (see SOURCES), with its Modrinth version id,
SHA-512 and source links. VulkanMod is LGPL-3.0-only (docs/THIRD_PARTY.md); versions without such a build, or with
Minecraft's own Vulkan renderer (26.2+), get none. Run it to move to newer VulkanMod builds, then smoke-test those targets:
`PROD=1 VK_DRIVER_FILES=<lavapipe icd> scripts/smoke.sh <mc>` (docs/PERF.md "Vulkan")."""
import json, os, urllib.request

UA = {"User-Agent": "itzslcs/starlight-tests/0.1.0"}
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
VANILLA_VULKAN = ("26.2", "26.3")  # Minecraft's own Vulkan backend (Starlight's Performance page switch)
REPO = "https://github.com/xCollateral/VulkanMod"
# LGPL-3.0 means shipping a build obliges us to offer its exact source. Only builds whose release commit is public
# (checked by hand: gradle.properties at the commit carries that mod_version) are bundled; the source zip of that
# commit goes to dist/sources/. Upstream has not published the sources of its other ports (1.21-1.21.5, 26.1.x).
# Keyed by Modrinth version id: one version number can name builds for several Minecraft versions from different
# branches (0.6.6 for 1.21.10 is the tag, 0.6.6 for 26.1.2 is not public).
SOURCES = {
    "hWKRTXbQ": "d3db07925f60257433233409125a08d7c08bf023",  # 0.6.8+1.21.11: dev, "Bump version" 2026-06-14
    "jkDMvvWU": "67e20f1f359a3a0611e2312a8a72e47fc9a09737",  # 0.6.6 for 1.21.9/1.21.10: tag 0.6.6
}


def main():
    versions = json.loads(urllib.request.urlopen(urllib.request.Request(
        "https://api.modrinth.com/v2/project/vulkanmod/version?loaders=%5B%22fabric%22%5D", headers=UA), timeout=60).read())
    targets = [t["mc"] for t in json.load(open(os.path.join(ROOT, "versions.json")))["targets"] if t["loader"] == "fabric"]
    out = {}
    for mc in targets:
        if mc in VANILLA_VULKAN:
            continue
        builds = [v for v in versions if mc in v["game_versions"] and v["id"] in SOURCES]
        pick = next((v for v in builds if v["version_type"] == "release"), None) or next(
            (v for v in builds if v["version_type"] == "beta"), None)
        if not pick:
            continue
        f = next(x for x in pick["files"] if x.get("primary")) if any(x.get("primary") for x in pick["files"]) else pick["files"][0]
        commit = SOURCES[pick["id"]]
        out[mc] = {"version": pick["version_number"], "type": pick["version_type"], "modrinth": pick["id"],
                   "file": f["filename"], "sha512": f["hashes"]["sha512"], "source": f"{REPO}/tree/{commit}",
                   "sourceZip": f"{REPO}/archive/{commit}.zip"}
        print(f"{mc:8} VulkanMod {pick['version_number']} ({pick['version_type']}) {pick['id']}")
    with open(os.path.join(ROOT, "fabric", "bundled.json"), "w") as fh:
        json.dump({"_": "VulkanMod per Minecraft version, written by scripts/vulkanmod-pin.py", "vulkanmod": out}, fh, indent=2)
        fh.write("\n")


if __name__ == "__main__":
    main()
