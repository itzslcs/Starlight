#!/usr/bin/env bash
# Runs scripts/smoke.sh for each given version (default: every target in versions.json) and prints a summary.
# usage: scripts/smoke-all.sh [mc ...]
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [ $# -eq 0 ]; then set -- $(python3 -c "import json;print(' '.join(t['mc'] for t in json.load(open('$ROOT/versions.json'))['targets']))"); fi
SUMMARY="$ROOT/smoke-out/SUMMARY.txt"
mkdir -p "$ROOT/smoke-out"; : > "$SUMMARY"
for v in "$@"; do
  "$ROOT/scripts/smoke.sh" "$v" "${SMOKE_SECONDS:-60}" > /dev/null 2>&1
  r=$(sed -n '2p' "$ROOT/smoke-out/$v/result.txt" 2>/dev/null)
  t=$(sed -n '1p' "$ROOT/smoke-out/$v/result.txt" 2>/dev/null)
  printf "%-8s %-60s %s\n" "$v" "${r:-no result}" "$t" | tee -a "$SUMMARY"
done
