#!/usr/bin/env bash
# Benchmark one target headless (core Bench): fixed scene, phases baseline / culling / culling+boost.
# usage: scripts/bench.sh <mc-version>   -> bench-out/<mc>/{bench.txt,latest.log,gradle.log}
# Software GL (llvmpipe): compare phases within a run; the numbers are not GPU performance.
set -uo pipefail
MC="${1:?usage: bench.sh <mc-version>}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/run/$MC"
OUT="$ROOT/bench-out/$MC"
rm -rf "$OUT" && mkdir -p "$OUT" "$RUN"
rm -rf "$RUN/saves/mw19-bench" "$RUN/logs/latest.log" "$RUN/MW19"
# WITH_MODS="sodium ..." adds those mods from Modrinth to the dev run (compatibility/perf checks); cleared otherwise.
rm -rf "$RUN/mods" && mkdir -p "$RUN/mods"
if [ -n "${WITH_MODS:-}" ] && [ "$MC" != "1.8.9" ]; then python3 "$ROOT/scripts/testmods.py" "$MC" "$RUN/mods" $WITH_MODS || exit 1; fi
cat > "$RUN/options.txt" <<OPT
onboardAccessibility:false
skipMultiplayerWarning:true
joinedFirstServer:true
tutorialStep:none
narrator:0
renderDistance:6
simulationDistance:5
maxFps:260
enableVsync:false
pauseOnLostFocus:false
# vanilla caps FPS at 30 after 60 s without input (FramerateLimitTracker.AFK_LIMIT); a benchmark never touches input
inactivityFpsLimit:"minimized"
fullscreen:false
guiScale:2
soundCategory_master:0.0
OPT
if [ "$MC" = "1.8.9" ]; then
  (cd "$ROOT" && ./gradlew :api:jar :core:jar -q) || { echo "FAIL build core"; exit 1; }
  CMD=(bash -c "cd '$ROOT/legacy' && ./gradlew runClient --console=plain -Pmw19.bench=1")
else
  CMD=("$ROOT/gradlew" -p "$ROOT" ":fabric:$MC:runClient" --console=plain "-Pmw19.bench=1" "-Pmw19.fabricTargets=$MC")
fi
echo "bench $MC: running (log: $OUT/gradle.log)"
xvfb-run -a -s "-screen 0 1280x720x24" env LIBGL_ALWAYS_SOFTWARE=1 timeout 900 "${CMD[@]}" > "$OUT/gradle.log" 2>&1
echo "exit=$?" > "$OUT/bench.txt"
cp "$RUN/logs/latest.log" "$OUT/latest.log" 2>/dev/null || true
# in run order (latest.log only; gradle.log duplicates it)
grep -h -o 'MW19 BENCH .*\|BENCH: scene has .*' "$OUT/latest.log" 2>/dev/null >> "$OUT/bench.txt"
cat "$OUT/bench.txt"
