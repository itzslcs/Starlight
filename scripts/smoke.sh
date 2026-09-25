#!/usr/bin/env bash
# Smoke test one target headless: title -> our GUI -> HUD editor -> world -> GUI -> N seconds -> quit.
# usage: scripts/smoke.sh <mc-version> [seconds=60]
# Needs: xvfb-run, Mesa (llvmpipe). Writes smoke-out/<mc>/{latest.log,gradle.log,*.png,result.txt}.
set -uo pipefail
MC="${1:?usage: smoke.sh <mc-version> [seconds]}"
SECS="${2:-60}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/run/$MC"
OUT="$ROOT/smoke-out/$MC"
rm -rf "$OUT" && mkdir -p "$OUT" "$RUN"
rm -rf "$RUN/saves/kestrel-smoke" "$RUN"/screenshots/kestrel-smoke-* "$RUN/logs/latest.log" "$RUN/Kestrel"

# Fresh Kestrel config with the addon plugins installed and pre-approved (consent is keyed by the jar's SHA-256).
mkdir -p "$RUN/Kestrel/plugins"
(cd "$ROOT" && ./gradlew -q :addons:sample:jar :addons:tiertags:jar) || { echo "FAIL build addons" | tee "$OUT/result.txt"; exit 1; }
cp "$ROOT"/addons/*/build/libs/*.jar "$RUN/Kestrel/plugins/"
python3 - "$RUN/Kestrel" <<'PY'
import hashlib, json, os, sys, zipfile
root = sys.argv[1]; consent = {}
for f in os.listdir(os.path.join(root, "plugins")):
    p = os.path.join(root, "plugins", f)
    pid = json.loads(zipfile.ZipFile(p).read("plugin.json"))["id"]
    consent[pid] = hashlib.sha256(open(p, "rb").read()).hexdigest()
json.dump({"schema": 1, "activeProfile": "Default", "pluginConsent": consent}, open(os.path.join(root, "config.json"), "w"))
PY

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

if [ "$MC" = "1.8.9" ]; then
  (cd "$ROOT" && ./gradlew :api:jar :core:jar -q) || { echo "FAIL build core" | tee "$OUT/result.txt"; exit 1; }
  CMD=(bash -c "cd '$ROOT/legacy' && ./gradlew runClient --console=plain -Pkestrel.smoke=$SECS")
else
  CMD=("$ROOT/gradlew" -p "$ROOT" ":fabric:$MC:runClient" --console=plain "-Pkestrel.smoke=$SECS" "-Pkestrel.fabricTargets=$MC")
fi

echo "smoke $MC: launching (log: $OUT/gradle.log)"
START=$(date +%s)
xvfb-run -a -s "-screen 0 1280x720x24" env LIBGL_ALWAYS_SOFTWARE=1 timeout 1500 "${CMD[@]}" > "$OUT/gradle.log" 2>&1
CODE=$?
echo "exit=$CODE after $(( $(date +%s) - START ))s" > "$OUT/result.txt"

cp "$RUN/logs/latest.log" "$OUT/latest.log" 2>/dev/null || true
cp "$RUN"/screenshots/kestrel-smoke-*.png "$OUT/" 2>/dev/null || true
LOG="$OUT/latest.log"
[ -s "$LOG" ] || LOG="$OUT/gradle.log"

python3 "$ROOT/scripts/mixin-audit.py" "$RUN" "$MC" > "$OUT/mixin-audit.txt" 2>&1
AUDIT=$?

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
bad = [r"KESTREL SMOKE FAIL", r"Mixin apply .*failed", r"InvalidInjectionException", r"MixinApplyError",
       r"InjectionError", r"\(Kestrel\).*(failed|ERROR)", r"/ERROR\]", r"Exception", r"^\s+at "]
# Dev-environment noise that is not ours: the fake dev account (authlib 401, Realms JWT) and missing
# text-to-speech natives under Xvfb. Matched against the record HEAD only.
allow = [r"Failed to fetch user properties", r"Realms", r"realms", r"Narrator", r"narrator", r"text2speech",
         r"libflite", r"OpenAL", r"Failed to fetch Realms",
         # Forge 1.8.9 dev runtime (no binary patches/signatures in dev) and its dead Twitch integration
         r"binary patch set is missing", r"missing any signature data", r"twitch stream"]
hits = []
for ln, head, cont in records:
    body = "\n".join([head] + cont)
    if any(re.search(p, body, re.M) for p in bad) and not any(re.search(a, head) for a in allow):
        hits.append(f"{ln}: {head[:200]}" + (f"  [+{len(cont)} lines]" if cont else ""))
fail = []
if "KESTREL SMOKE PASS" not in text: fail.append("no PASS marker")
if hits: fail.append(f"{len(hits)} suspicious log record(s)")
audit_lines = open(audit_file).read().strip().splitlines()
if audit != "0": fail.append("mixin audit: " + (audit_lines[-1] if audit_lines else "failed"))
with open(res, "a") as f:
    f.write(("PASS" if not fail else "FAIL: " + "; ".join(fail)) + "\n")
    for h in hits[:40]: f.write("  " + h + "\n")
    m = re.search(r"KESTREL SMOKE PASS.*", text)
    if m: f.write(m.group(0)[:900] + "\n")
    if audit_lines: f.write(audit_lines[-1] + "\n")
print(open(res).read())
sys.exit(0 if not fail else 1)
PY
