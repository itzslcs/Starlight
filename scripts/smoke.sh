#!/usr/bin/env bash
# Smoke test one target headless: title -> our GUI -> HUD editor -> world -> GUI -> N seconds -> quit.
# usage: scripts/smoke.sh <mc-version> [seconds=60]
# Needs: xvfb-run, Mesa (llvmpipe). Writes smoke-out/<mc>/{latest.log,gradle.log,*.png,result.txt}.
set -uo pipefail
MC="${1:?usage: smoke.sh <mc-version> [seconds]}"
SECS="${2:-60}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# PROD=1: the built jar (dist/) in a real Fabric production launch (scripts/prodlaunch.py) instead of a Loom dev run.
PROD="${PROD:-}"
RUN="$ROOT/run/${PROD:+prod-}$MC"
OUT="$ROOT/smoke-out/${PROD:+prod-}$MC"
rm -rf "$OUT" && mkdir -p "$OUT" "$RUN"
rm -rf "$RUN/saves/mw19-smoke" "$RUN"/screenshots/mw19-smoke-* "$RUN/logs/latest.log"
rm -f "$RUN/debug-profile.json"  # vanilla's saved F3 entries (hitboxes) must not carry over between runs
# KEEP_STATE=1 keeps MW19/ from the last run (renderer markers: vulkan-starting, vulkan-probe.txt...);
# RENDERER=auto|vulkan|opengl writes MW19/renderer.txt; VK_SOFT=1 lets the Vulkan check accept a CPU driver (lavapipe).
[ -z "${KEEP_STATE:-}" ] && rm -rf "$RUN/MW19"
# WITH_MODS="sodium ..." adds those mods from Modrinth to the dev run (compatibility/perf checks); cleared otherwise.
rm -rf "$RUN/mods" && mkdir -p "$RUN/mods"
rm -rf "$RUN/compat-mods" "$RUN/compat-libs"
if [ -n "${WITH_MODS:-}" ] && [ "$MC" != "1.8.9" ]; then
  MODS_LIST=""
  for m in $WITH_MODS; do
    if [ "$m" = "fabric-api" ]; then  # from Fabric's Maven: its modules are nested jars (fabric/build.gradle.kts)
      FAPI=$(python3 -c "import json,sys,urllib.request,urllib.parse;q=urllib.parse.urlencode({'loaders':'["fabric"]','game_versions':json.dumps([sys.argv[1]])});v=json.load(urllib.request.urlopen(urllib.request.Request('https://api.modrinth.com/v2/project/fabric-api/version?'+q,headers={'User-Agent':'itzslcs/mw19-smoke'})));print([x for x in v if x['version_type']=='release'][0]['version_number'])" "$MC") || exit 1
      WITH_MODS_ARG="${WITH_MODS_ARG:-} -Pmw19.fabricApi=$FAPI"
    else
      MODS_LIST="$MODS_LIST $m"
    fi
  done
  mkdir -p "$RUN/compat-mods"
  if [ -n "$MODS_LIST" ]; then python3 "$ROOT/scripts/testmods.py" ${PROD:+--no-unnest} "$MC" "$RUN/compat-mods" $MODS_LIST || exit 1; fi
  WITH_MODS_ARG="${WITH_MODS_ARG:-} -Pmw19.withMods=$RUN/compat-mods"  # Loom remaps them at build time
fi

mkdir -p "$RUN/MW19"
[ -n "${RENDERER:-}" ] && printf '%s' "$RENDERER" > "$RUN/MW19/renderer.txt"

# Skip first-run screens and keep software rendering responsive. Written fresh every run.
cat > "$RUN/options.txt" <<OPT
onboardAccessibility:false
skipMultiplayerWarning:true
joinedFirstServer:true
tutorialStep:none
narrator:0
renderDistance:4
simulationDistance:5
maxFps:60
enableVsync:false
pauseOnLostFocus:false
fullscreen:false
guiScale:2
soundCategory_master:0.0
OPT
cp "$RUN/options.txt" "$RUN/optionsof.txt" 2>/dev/null || true

# With xdotool, the run also clicks the menu through real X11 input (Smoke.requestClick logs where).
CLICKS=""
command -v xdotool >/dev/null && CLICKS="-Pmw19.smokeClicks=1"
if [ -n "$PROD" ]; then
  (cd "$ROOT" && ./gradlew -q ":fabric:$MC:collectJar" "-Pmw19.fabricTargets=$MC") || { echo "FAIL build jar" | tee "$OUT/result.txt"; exit 1; }
  JAR=$(ls -t "$ROOT"/dist/MW19-*+mc"$MC".jar | head -1)
  PROD_MODS=("$JAR")
  [ -d "$RUN/compat-mods" ] && PROD_MODS+=("$RUN"/compat-mods/*.jar)
  CMD=(python3 "$ROOT/scripts/prodlaunch.py" "$MC" "$RUN" "${PROD_MODS[@]}" -- -Dmw19.smoke=1 "-Dmw19.smoke.seconds=$SECS"
       ${CLICKS:+-Dmw19.smoke.clicks=true} ${VK_SOFT:+-Dmw19.vulkan.allowSoftware=true})
elif [ "$MC" = "1.8.9" ]; then
  (cd "$ROOT" && ./gradlew :api:jar :core:jar -q) || { echo "FAIL build core" | tee "$OUT/result.txt"; exit 1; }
  CMD=(bash -c "cd '$ROOT/legacy' && ./gradlew runClient --console=plain -Pmw19.smoke=$SECS $CLICKS")
else
  CMD=("$ROOT/gradlew" -p "$ROOT" ":fabric:$MC:runClient" --console=plain "-Pmw19.smoke=$SECS" "-Pmw19.fabricTargets=$MC" $CLICKS ${WITH_MODS_ARG:-})
fi

# Click helper: waits for "SMOKE CLICK <display> <pid> <x> <y>" (window pixels) in the run output and clicks there.
# Window lookup by _NET_WM_PID first: xdotool's --name cannot read SDL3's UTF-8 title (26.3); LWJGL 2 sets no PID.
if [ -n "$CLICKS" ]; then
  : > "$OUT/gradle.log"
  (
    # One click per run: poll (tail -F re-read the line after the redirect truncated the file, clicking twice).
    while :; do
      line=$(grep -m1 'SMOKE CLICK ' "$OUT/gradle.log" 2>/dev/null)
      if [ -n "$line" ]; then
        set -- ${line##*SMOKE CLICK }
        win=$(DISPLAY="$1" xdotool search --onlyvisible --pid "$2" 2>/dev/null | head -1)
        [ -z "$win" ] && win=$(DISPLAY="$1" xdotool search --onlyvisible --name 'Minecraft' 2>/dev/null | head -1)
        [ -z "$win" ] && win=$(DISPLAY="$1" xdotool search --onlyvisible --class 'minecraft' 2>/dev/null | head -1)
        [ -n "$win" ] && DISPLAY="$1" xdotool mousemove --window "$win" "$3" "$4" sleep 0.2 click 1
        echo "clicked $* (window $win)" >> "$OUT/clicks.txt"
        break
      fi
      sleep 0.5
    done
  ) &
  CLICKER=$!
fi

# X11 grab next to each game screenshot ("SMOKE SHOT <display> <name>"): renderers that bypass the game's own
# capture (VulkanMod) leave its screenshots blank, so x11-<name>.png is the evidence of what was on screen.
if command -v import >/dev/null; then
  (
    while :; do
      grep -o 'SMOKE SHOT .*' "$OUT/gradle.log" 2>/dev/null | while read -r _ _ d name; do
        [ -e "$OUT/x11-$name.png" ] || DISPLAY="$d" import -window root "$OUT/x11-$name.png" 2>/dev/null
      done
      sleep 0.3
    done
  ) &
  SHOOTER=$!
fi

echo "smoke $MC: launching (log: $OUT/gradle.log)"
START=$(date +%s)
xvfb-run -a -s "-screen 0 1280x720x24" env LIBGL_ALWAYS_SOFTWARE=1 timeout 1500 "${CMD[@]}" > "$OUT/gradle.log" 2>&1
CODE=$?
[ -n "${CLICKER:-}" ] && { kill "$CLICKER" 2>/dev/null; pkill -P "$CLICKER" 2>/dev/null; }
[ -n "${SHOOTER:-}" ] && { kill "$SHOOTER" 2>/dev/null; pkill -P "$SHOOTER" 2>/dev/null; }
echo "exit=$CODE after $(( $(date +%s) - START ))s" > "$OUT/result.txt"

cp "$RUN/logs/latest.log" "$OUT/latest.log" 2>/dev/null || true
cp "$RUN"/screenshots/mw19-smoke-*.png "$OUT/" 2>/dev/null || true
LOG="$OUT/latest.log"
[ -s "$LOG" ] || LOG="$OUT/gradle.log"

if [ -n "$PROD" ]; then  # production classes carry intermediary names; the dev runs audit the wiring
  echo "mixin audit skipped (production run)" > "$OUT/mixin-audit.txt"; AUDIT=0
else
  python3 "$ROOT/scripts/mixin-audit.py" "$RUN" "$MC" > "$OUT/mixin-audit.txt" 2>&1
  AUDIT=$?
fi

python3 - "$LOG" "$OUT/result.txt" "$AUDIT" "$OUT/mixin-audit.txt" <<'PY'
import re, sys
log, res, audit, audit_file = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
text = open(log, errors="replace").read()
# Group into records: a record starts with "[HH:MM:SS]"; stack traces and continuation lines belong to it.
records, cur = [], None
for i, l in enumerate(text.splitlines()):
    if re.match(r"^\[\d\d:\d\d:\d\d\]", l) or cur is None:
        cur = [i + 1, l, []]
        records.append(cur)
    else:
        cur[2].append(l)
bad = [r"MW19 SMOKE FAIL", r"Mixin apply .*failed", r"InvalidInjectionException", r"MixinApplyError",
       r"InjectionError", r"\(MW19\).*(failed|ERROR)", r"/ERROR\]", r"Exception", r"^\s+at "]
# Dev-environment noise that is not ours: the fake dev account (authlib 401, Realms JWT) and missing
# text-to-speech natives under Xvfb. Matched against the record HEAD only.
allow = [r"Failed to fetch user properties", r"Realms", r"realms", r"Narrator", r"narrator", r"text2speech",
         r"libflite", r"OpenAL", r"Failed to fetch Realms",
         # Forge 1.8.9 dev runtime (no binary patches/signatures in dev) and its dead Twitch integration
         r"binary patch set is missing", r"missing any signature data", r"twitch stream",
         # the dev account has no token, so authlib's key-pair fetch gets a 401 (26.2+)
         r"Failed to retrieve profile key pair",
         # vanilla fetching Mojang's service keys at start; a network timeout there is not ours (debug-log 2026-09-26)
         r"Failed to request yggdrasil public key",
         # vanilla looking up the offline test player's profile at Mojang's session server; a timeout there is not
         # ours either (debug-log 2026-09-27, production 1.21.5)
         r"Couldn't look up profile properties",
         # VulkanMod ships shader files with an upper-case letter in their path (terrain_earlyZ), which Fabric's
         # resource loader rejects and logs on every reload; harmless, not ours (debug-log 2026-09-26 "VulkanMod")
         r"Invalid path in mod resource-pack vulkanmod: vulkanmod:shaders/basic/terrain_earlyZ/"]
# Minecraft logs a GLFW error as three records ("#### GL ERROR ####", "@ <where>", "<code>: <message>"). Xvfb has no
# cursor theme, so only that one message (with its frame) is environment noise; any other GL error still fails.
env = set()
for i, (ln, head, cont) in enumerate(records):
    if "X11: Standard cursor shape unavailable" in head and i >= 2 \
            and "GL ERROR" in records[i - 2][1] and "@ " in records[i - 1][1]:
        env.update((i - 2, i - 1, i))
hits = []
for i, (ln, head, cont) in enumerate(records):
    body = "\n".join([head] + cont)
    if i in env: continue
    if any(re.search(p, body, re.M) for p in bad) and not any(re.search(a, head) for a in allow):
        hits.append(f"{ln}: {head[:200]}" + (f"  [+{len(cont)} lines]" if cont else ""))
fail = []
if "MW19 SMOKE PASS" not in text: fail.append("no PASS marker")
# A clean PASS must also exit cleanly: a crash or hang after the marker (see the shutdown watchdog) is a failure.
code = re.search(r"exit=(\d+)", open(res).read())
if code and code.group(1) != "0": fail.append("process exit code " + code.group(1))
if hits: fail.append(f"{len(hits)} suspicious log record(s)")
audit_lines = open(audit_file).read().strip().splitlines()
if audit != "0": fail.append("mixin audit: " + (audit_lines[-1] if audit_lines else "failed"))
with open(res, "a") as f:
    f.write(("PASS" if not fail else "FAIL: " + "; ".join(fail)) + "\n")
    for h in hits[:40]: f.write("  " + h + "\n")
    m = re.search(r"MW19 SMOKE PASS.*", text)
    if m: f.write(m.group(0)[:900] + "\n")
    if audit_lines: f.write(audit_lines[-1] + "\n")
print(open(res).read())
sys.exit(0 if not fail else 1)
PY
