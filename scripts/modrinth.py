#!/usr/bin/env python3
"""Updates Starlight's Modrinth project from dist/: title, summary, description (docs/MODRINTH.md), icon, and one alpha
version per jar. Versions whose Starlight version is not in dist/ are deleted (the project is a draft until the owner submits it).

usage: MR=<personal access token> scripts/modrinth.py <project id or slug> [dist] ["release notes"]
The token needs: Create versions, Write projects, Delete versions. It is read from the environment only and never
written anywhere; revoke it on modrinth.com (Settings -> PATs) after use. See docs/PUBLISHING.md.
"""
import glob, json, os, re, sys, time, urllib.error, urllib.request, uuid

API = "https://api.modrinth.com/v2"
UA = "itzslcs/starlight-publish (modrinth.com/user/itzslcs)"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SUMMARY = ("Max-FPS client for 1.8.9 and 1.21-26.3 with animated themes: Vulkan built in, fast chests, culling, auto graphics "
           "presets, skin changer, pack browser, world hosting, KeyCPS, tier tags, bind profiles, crystal/anchor optimizers. Legit; one jar.")


def multipart(fields, files):
    b = uuid.uuid4().hex
    out = []
    for name, value in fields.items():
        out += [f"--{b}\r\n".encode(), f'Content-Disposition: form-data; name="{name}"\r\nContent-Type: application/json\r\n\r\n'.encode(),
                value.encode(), b"\r\n"]
    for name, (filename, ctype, data) in files.items():
        out += [f"--{b}\r\n".encode(),
                f'Content-Disposition: form-data; name="{name}"; filename="{filename}"\r\nContent-Type: {ctype}\r\n\r\n'.encode(),
                data, b"\r\n"]
    out.append(f"--{b}--\r\n".encode())
    return b"".join(out), f"multipart/form-data; boundary={b}"


def call(method, path, body=None, ctype=None):
    headers = {"Authorization": os.environ["MR"], "User-Agent": UA}
    if ctype:
        headers["Content-Type"] = ctype
    req = urllib.request.Request(API + path, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            raw = r.read()
            return r.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode(errors="replace")


def mc_key(jar):
    return tuple(int(x) for x in re.search(r"\+mc(.+)\.jar$", jar).group(1).split("."))


def main():
    project = sys.argv[1]
    dist = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "dist")
    notes = sys.argv[3] if len(sys.argv) > 3 else ""
    body = open(os.path.join(ROOT, "docs", "MODRINTH.md")).read().split("<!-- body -->\n", 1)[1]
    fields = {"title": "Starlight Client", "description": SUMMARY, "body": body}
    # The slug only when it changes: Modrinth refuses to re-set a project's own slug ("Slug collides with other
    # project's id", 2026-09-27), and that 400 would drop the title, summary and description with it. Not plain
    # "starlight": that is Spottedleaf's lighting mod (DECISIONS D-033).
    st, info = call("GET", f"/project/{project}")
    if st != 200 or info.get("slug") != "starlight-client":
        fields["slug"] = "starlight-client"
    st, resp = call("PATCH", f"/project/{project}", json.dumps(fields).encode(), "application/json")
    print("project:", st, "" if st == 204 else resp)
    if st == 401:
        sys.exit("the token was refused (revoked or missing scopes)")
    icon = os.path.join(ROOT, "core", "src", "main", "resources", "assets", "starlight", "icon.png")
    st, resp = call("PATCH", f"/project/{project}/icon?ext=png", open(icon, "rb").read(), "image/png")
    print("icon:", st, "" if st == 204 else resp)
    jars = sorted(glob.glob(os.path.join(dist, "Starlight-*+mc*.jar")), key=mc_key)
    # VulkanMod inside the jar (DECISIONS D-024): an "embedded" dependency, and its source zip attached (LGPL-3.0)
    bundled = json.load(open(os.path.join(ROOT, "fabric", "bundled.json")))["vulkanmod"]
    current = {re.search(r"Starlight-(.+)\+mc", os.path.basename(j)).group(1) for j in jars}
    st, versions = call("GET", f"/project/{project}/version")
    kept = set()
    for v in versions if st == 200 else []:
        if v["version_number"].split("+mc")[0] not in current:
            print("delete", v["version_number"], call("DELETE", f"/version/{v['id']}")[0])
            time.sleep(0.3)
        else:
            kept.add(v["version_number"])  # already uploaded (a re-run after a partial upload): skipped below
    ok = 0
    for jar in jars:
        mc = re.search(r"\+mc(.+)\.jar$", jar).group(1)
        ver = re.search(r"Starlight-(.+)\+mc", os.path.basename(jar)).group(1)
        forge = mc == "1.8.9"
        if f"{ver}+mc{mc}" in kept:
            print(f"{mc:8}", "already there")
            ok += 1
            continue
        req = "Requires " + ("Forge 11.15.1.2318." if forge else
                             ("Fabric Loader 0.18+ and Java 25." if mc.startswith("26") else "Fabric Loader 0.16+.") + " Fabric API is not needed.")
        vk = bundled.get(mc)
        files = {"file": (os.path.basename(jar), "application/java-archive", open(jar, "rb").read())}
        deps = []
        if vk:
            src = os.path.join(dist, "sources", f"VulkanMod-{vk['version']}-src.zip")
            files["sources"] = (os.path.basename(src), "application/zip", open(src, "rb").read())
            deps.append({"version_id": vk["modrinth"], "dependency_type": "embedded"})
            req += (f" Includes VulkanMod {vk['version']} (LGPL-3.0, unmodified; its source zip is attached and linked from"
                    f" {vk['source']}); Starlight turns it on from the second start when the graphics card supports Vulkan.")
        data = {"name": f"Starlight {ver} ({mc})", "version_number": f"{ver}+mc{mc}", "changelog": (notes + "\n\n" + req).strip(),
                "dependencies": deps, "game_versions": [mc], "version_type": "alpha", "loaders": ["forge" if forge else "fabric"],
                "featured": False, "project_id": project, "file_parts": list(files), "primary_file": "file", "environment": "client_only"}
        payload, ctype = multipart({"data": json.dumps(data)}, files)
        st, resp = call("POST", "/version", payload, ctype)
        print(f"{mc:8}", "OK " + resp["id"] if st == 200 else f"FAILED {st}: {str(resp)[:300]}")
        if st != 200:
            break
        ok += 1
        time.sleep(0.5)
    print("uploaded", ok, "of", len(jars))


if __name__ == "__main__":
    main()
