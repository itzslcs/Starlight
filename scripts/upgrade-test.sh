#!/usr/bin/env bash
# Upgrade from MW19 to Starlight in production launches, in one game folder (DECISIONS D-033):
#  a. the old MW19 jar alone: its smoke run writes MW19/
#  b. then the state of a player who had Fast Chests on (module on, mw19/fast_chests selected), theme "Violet" (a
#     pre-0.8.0 name) and a marker file in MW19/
#  c. the Starlight dist jar alone: must PASS the whole smoke run, with MW19/ moved to Starlight/ and the theme "Nebula"
#  d. both jars: Fabric must refuse to start and name the conflict
# usage: scripts/upgrade-test.sh <old MW19 jar> [mc=1.21.11]   (the Starlight jar comes from dist/; run buildAll first)
# Evidence: smoke-out/upgrade-<mc>/ (a.log, c.log, d.log, the game folder, result.txt).
set -uo pipefail
OLD="${1:?usage: upgrade-test.sh <old MW19 jar> [mc]}"
MC="${2:-1.21.11}"
M="$(cd "$(dirname "$0")/.." && pwd)"
U="$M/smoke-out/upgrade-$MC"; G="$U/game"
NEW=$(ls "$M"/dist/Starlight-*+mc"$MC".jar | head -1)
rm -rf "$U"; mkdir -p "$G"
# Options as smoke.sh writes them (no onboarding, small view distance)
# (with each ${VAR:-default} replaced by its default: an unexpanded value makes Minecraft drop the whole file)
sed -n "/^cat > \"\$RUN\/options.txt\" <<OPT$/,/^OPT$/p" "$M/scripts/smoke.sh" | sed '1d;$d' | sed 's/\${[A-Z_]*:-\([^}]*\)}/\1/g' > "$G/options.txt"
# OpenGL throughout: otherwise MW19's first start probes this PC's GPU (RADV answers "ok"), and the Starlight start
# after it switches to Vulkan, which cannot present on Xvfb (no DRI3) and takes the X server down
mkdir -p "$G/MW19" && printf opengl > "$G/MW19/renderer.txt"
# Each launch gets its own X display: xvfb-run does not wait for its Xvfb to exit, so back-to-back "-a" launches can
# land on a display whose old server is still going away
N=140
launch() { N=$((N + 1)); xvfb-run -n $N -s "-screen 0 1280x720x24" env LIBGL_ALWAYS_SOFTWARE=1 timeout 900 python3 "$M/scripts/prodlaunch.py" "$MC" "$G" "$@"; }
say() { echo "$*" | tee -a "$U/result.txt"; }

launch "$OLD" -- -Dmw19.smoke=1 -Dmw19.smoke.seconds=10 > "$U/a.log" 2>&1
say "a. $(basename "$OLD"): exit $?, $(grep -ao 'MW19 SMOKE PASS\|MW19 SMOKE FAIL.\{0,60\}' "$G/logs/latest.log" | head -1); MW19/: $(ls "$G/MW19" | tr '\n' ' ')"
[ -f "$G/MW19/config.json" ] || { say "FAIL: MW19 wrote no config"; exit 1; }

python3 - "$G" <<'PY' | tee -a "$U/result.txt"
import json, re, sys
g = sys.argv[1]
c = json.load(open(f"{g}/MW19/config.json"))
c.setdefault("client", {})["theme"] = "Violet"
json.dump(c, open(f"{g}/MW19/config.json", "w"), indent=2)
p = f"{g}/MW19/profiles/Default.json"
d = json.load(open(p))
d.setdefault("modules", {}).setdefault("fast_chests", {})["enabled"] = True
json.dump(d, open(p, "w"), indent=2)
o = re.sub(r"^resourcePacks:.*$", 'resourcePacks:["vanilla","mw19/fast_chests"]', open(f"{g}/options.txt").read(), flags=re.M)
if "resourcePacks:" not in o: o += 'resourcePacks:["vanilla","mw19/fast_chests"]\n'
open(f"{g}/options.txt", "w").write(o)
print(f"b. MW19 config schema {c.get('schema')}, theme Violet, Fast Chests on with mw19/fast_chests selected")
PY
echo upgrade > "$G/MW19/upgrade-marker.txt"

launch "$NEW" -- -Dstarlight.smoke=1 -Dstarlight.smoke.seconds=10 > "$U/c.log" 2>&1
code=$?
cp "$G/logs/latest.log" "$U/c-latest.log"
theme=$(python3 -c "import json;c=json.load(open('$G/Starlight/config.json'));print(c.get('schema'), c['client']['theme'])" 2>/dev/null)
say "c. $(basename "$NEW"): exit $code, $(grep -ao 'Starlight SMOKE PASS\|Starlight SMOKE FAIL.\{0,80\}' "$G/logs/latest.log" | head -1)"
say "   MW19/ left: $([ -e "$G/MW19" ] && echo yes || echo no); marker: $(cat "$G/Starlight/upgrade-marker.txt" 2>/dev/null || echo missing); schema and theme: $theme"
say "   $(grep -ao 'Removed resource pack mw19/fast_chests[^$]*' "$G/logs/latest.log" | head -1)"
say "   $(grep -ao 'SMOKE: fast chests.*' "$G/logs/latest.log" | head -1)"

launch "$NEW" "$OLD" -- -Dfabric.noGui=true -Dstarlight.smoke=1 > "$U/d.log" 2>&1
say "d. both jars: exit $?, $(grep -ao "Mod 'Starlight'.*present: [^!]*!" "$U/d.log" | head -1)"

ok=1
grep -q "Starlight SMOKE PASS" "$U/c-latest.log" || ok=0
[ -e "$G/MW19" ] && ok=0
[ "$theme" = "2 Nebula" ] || ok=0
grep -q "is incompatible with any version of mod 'MW19'" "$U/d.log" || ok=0
say "$([ $ok = 1 ] && echo PASS || echo FAIL)"
[ $ok = 1 ]
