#!/usr/bin/env python3
"""Updates MW19's Modrinth project from dist/: title, summary, description (docs/MODRINTH.md), icon, and one alpha
version per jar. Versions whose MW19 version is not in dist/ are deleted (the project is a draft until the owner submits it).

usage: MR=<personal access token> scripts/modrinth.py <project id or slug> [dist] ["release notes"]
The token needs: Create versions, Write projects, Delete versions. It is read from the environment only and never
written anywhere; revoke it on modrinth.com (Settings -> PATs) after use. See docs/PUBLISHING.md.
"""
import glob, json, os, re, sys, time, urllib.error, urllib.request, uuid

API = "https://api.modrinth.com/v2"
UA = "itzslcs/mw19-publish (modrinth.com/user/itzslcs)"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SUMMARY = ("Max-FPS client for 1.8.9 and 1.21-26.3: auto-detected graphics presets, entity culling, skin changer, "
           "in-game pack browser, world hosting, KeyCPS. Legit only; no Fabric API needed.")


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
    st, resp = call("PATCH", f"/project/{project}", json.dumps({"slug": "mw19", "title": "MW19 Client", "description": SUMMARY, "body": body}).encode(),
                    "application/json")
    print("project:", st, "" if st == 204 else resp)
    if st == 401:
        sys.exit("the token was refused (revoked or missing scopes)")
    icon = os.path.join(ROOT, "core", "src", "main", "resources", "assets", "mw19", "icon.png")
    st, resp = call("PATCH", f"/project/{project}/icon?ext=png", open(icon, "rb").read(), "image/png")
    print("icon:", st, "" if st == 204 else resp)
    jars = sorted(glob.glob(os.path.join(dist, "MW19-*+mc*.jar")), key=mc_key)
    current = {re.search(r"MW19-(.+)\+mc", os.path.basename(j)).group(1) for j in jars}
    st, versions = call("GET", f"/project/{project}/version")
    for v in versions if st == 200 else []:
        if v["version_number"].split("+mc")[0] not in current:
            print("delete", v["version_number"], call("DELETE", f"/version/{v['id']}")[0])
            time.sleep(0.3)
    ok = 0
    for jar in jars:
        mc = re.search(r"\+mc(.+)\.jar$", jar).group(1)
        ver = re.search(r"MW19-(.+)\+mc", os.path.basename(jar)).group(1)
        forge = mc == "1.8.9"
        req = "Requires " + ("Forge 11.15.1.2318." if forge else
                             ("Fabric Loader 0.18+ and Java 25." if mc.startswith("26") else "Fabric Loader 0.16+.") + " Fabric API is not needed.")
        data = {"name": f"MW19 {ver} ({mc})", "version_number": f"{ver}+mc{mc}", "changelog": (notes + "\n\n" + req).strip(),
                "dependencies": [], "game_versions": [mc], "version_type": "alpha", "loaders": ["forge" if forge else "fabric"],
                "featured": False, "project_id": project, "file_parts": ["file"], "primary_file": "file", "environment": "client_only"}
        payload, ctype = multipart({"data": json.dumps(data)}, {"file": (os.path.basename(jar), "application/java-archive", open(jar, "rb").read())})
        st, resp = call("POST", "/version", payload, ctype)
        print(f"{mc:8}", "OK " + resp["id"] if st == 200 else f"FAILED {st}: {str(resp)[:300]}")
        if st != 200:
            break
        ok += 1
        time.sleep(0.5)
    print("uploaded", ok, "of", len(jars))


if __name__ == "__main__":
    main()
