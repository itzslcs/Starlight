# Debug log

Every entry follows the protocol: reproduce, state a hypothesis (and what would refute it), isolate, then fix with evidence.

## 2026-09-25 · Smoke log "suspicious lines" on the first 1.21.11 run
- **Repro:** `scripts/smoke.sh 1.21.11`. The checker flagged 11 lines while the run printed `KESTREL SMOKE PASS`.
- **Hypothesis:** the lines come from the Loom dev environment (fake offline account, Realms), not from Kestrel. Refuted if any
  flagged record's logger is `(Kestrel)` or its stack contains `dev.kestrel`.
- **Evidence:** the records are `(Minecraft) Failed to fetch user properties` → `InvalidCredentialsException: Status: 401`
  (dev user "Player685" has no token), `Failed to fetch Realms feature flags` → `Failed to parse into SignedJWT: FabricMC`,
  and a JNA `libflite.so` lookup (text-to-speech natives). None contains `dev.kestrel`.
- **Fix:** the checker now groups log lines into records (timestamp head + stack) and allowlists these heads only. 1.8.9 adds
  FML's dev-only `binary patch set is missing` / `missing any signature data` and `Couldn't initialize twitch stream`.
  Re-ran both targets: `PASS` with 0 suspicious records.

## 2026-09-25 · HUD cost 220 µs/frame on 1.21.11 (budget 300)
- **Repro:** smoke run 1 printed `ownUsPerFrame=219.55` (1.21.11) and `354.3` (1.8.9) with FPS, Keystrokes and Armor.
- **Hypothesis:** the cost is dominated by the number of `fill` calls from scanline rounded rects (≈25 per key box:
  one per corner row plus four anti-aliasing pixels per row). On 1.21.11 each fill is also a render-state object plus a
  pose push/scale/pop. Refuted if cutting fills does not reduce `ownUsPerFrame`.
- **Change:** `Gfx.roundRect` anti-aliases only when the radius is ≥ 7 physical px (invisible below that) and merges
  consecutive rows with the same inset into one fill (a key box goes from ~25 fills to ~5).
- **Result (same smoke scenario):** 1.21.11 **219.6 → 75.6 µs/frame**; 1.8.9 354.3 → 275.4 µs/frame. What remains on
  1.8.9 is mostly vanilla `FontRenderer` immediate-mode glyph draws, which llvmpipe executes on the CPU. These numbers come
  from software GL; real-GPU numbers are Phase 5 (docs/PERF.md).
