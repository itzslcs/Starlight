# Publishing

See also: [MODRINTH](MODRINTH.md) (the listing text), [COMPAT_MATRIX](COMPAT_MATRIX.md) (what was tested),
[CHANGELOG](../CHANGELOG.md).

## Modrinth
The project is a **draft** (id `TVWRTUcc`, now "Starlight Client" at `starlight-client`, with 0.8.0) until the owner
submits it for review. It was MW19 (slug `mw19`) up to 0.7.0.
[`scripts/modrinth.py`](../scripts/modrinth.py) names it "Starlight Client" with the slug `starlight-client` (plain `starlight` is taken by
Spottedleaf's lighting mod, D-033), uploads the description ([MODRINTH](MODRINTH.md)) and icon, deletes versions that
are not in `dist/`, and uploads every `dist/` jar as an alpha version (a version already on Modrinth is skipped, so a
run that stopped halfway can simply be repeated). The 1.21.9 – 1.21.11 versions list VulkanMod as an
*embedded* dependency and carry its source zip (`dist/sources/`) as a second file, which the LGPL asks for ([DECISIONS](DECISIONS.md)
D-024). Modrinth's moderators may ask about the bundled mod; the answer is D-024 (licence, unmodified, source attached,
replaceable).

1. On modrinth.com, click your avatar → **Settings** → **Personal access tokens** → **Create a PAT**.
2. Name it `Starlight upload`, set it to expire tomorrow, and tick **Create versions**, **Write projects**, **Delete versions**
   and **Read projects**. Click **Create PAT** and copy the token (it starts with `mrp_`).
3. Build: `./gradlew buildAll`.
4. In a terminal in the project folder: `MR=mrp_yourtoken scripts/modrinth.py TVWRTUcc dist "What changed"`.
   (Or give the token to Claude for this one command.)
5. Revoke the token afterwards (same settings page). The script never writes it anywhere.
6. Check the page, then **Submit for review** on Modrinth when you are happy with it.

## GitHub
`origin` is [github.com/itzslcs/Starlight](https://github.com/itzslcs/Starlight) (private), first pushed on 2026-09-28. This machine keeps no GitHub
login, so each push needs a token from the owner:

1. github.com/settings/personal-access-tokens/new: a short expiry, **Only select repositories** → `Starlight`, and
   under **Repository permissions** set **Contents** to **Read and write** (Read-only is refused with a 403).
2. Claude pushes with it for that one command (`git -c http.https://github.com/.extraheader=... push`), so it is never
   written to `.git/config`, and the owner revokes it afterwards.

Or, to push without tokens: install the GitHub CLI (`sudo pacman -S github-cli`) and type `! gh auth login` in Claude
Code (GitHub.com → HTTPS → log in with a web browser).
