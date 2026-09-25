# Rules matrix: every module vs. the Hypixel Allowed Modifications policy

Policy source: https://support.hypixel.net/hc/en-us/articles/6472550754962 (fetched 2026-09-25 through the
Zendesk API, article `updated_at 2025-02-24`). Its permitted categories:

- **PERF**: Client Performance Improvement ("improve the performance … without making changes to the game itself")
- **AESTHETIC**: Aesthetic ("change only the look and feel … must not … change the player's perspective")
- **HUD**: Cosmetic HUD ("without adding extra information which would normally be unavailable to the player … mini-maps, other player health/armor indicators, player distance/range … are not")
- **BRIGHT**: Brightness & Gamma Adjustment
- Listed examples: Armor & Effect Status, Animations (cosmetic only), OptiFine, Replay Mod, Shaders, all-in-one clients.

The policy also explicitly disallows: significant advantage, "anything which automates any player gameplay action … macros,
auto-sprint, and aim assists", and changes to "the way … your Minecraft client interacts with and communicates with our server".
It closes with: "If a modification does not fit clearly into any of the allowed modification categories, it should be assumed to be disallowed."

**Verdicts**
- `ALLOWED`: clearly inside a category or a listed example. May ship default-on.
- `GRAY`: not clearly inside a category. **Ships default-off**, and is switched off by *competitive-safe*.
- `DISALLOWED@hypixel`: outside the policy on Hypixel. Default-off, and `serverrules.json` force-disables it on Hypixel.

The engine reads each module's verdict from code (`Module.rule()`). This table is the human-readable source of
truth, and a unit test (`RulesMatrixTest`) checks that every registered module appears here with the same verdict.

| id | Module | Targets | Category | Verdict | Default | Notes |
|---|---|---|---|---|---|---|
| fps | FPS | all | HUD | ALLOWED | on | Own client performance, not game info |
| cps | CPS (display only) | all | HUD | ALLOWED | on | Counts the player's own clicks. Never generates clicks |
| ping | Ping | all | HUD | ALLOWED | off | The player's own latency (the tab list already shows bars) |
| coords | Coordinates | all | HUD | ALLOWED | off | Same as F3. **Hidden while the server sets reducedDebugInfo** |
| direction | Direction | all | HUD | ALLOWED | off | Same as F3 facing |
| keystrokes | Keystrokes | all | HUD | ALLOWED | on | The player's own inputs |
| armor | Armor Status | all | HUD | ALLOWED | on | Listed example "Armor Status" |
| effects | Potion Effects | all | HUD | ALLOWED | on | Listed example "Effect Status" |
| item_counter | Item Counter | all | HUD | ALLOWED | off | Counts items already visible in your inventory |
| clock | Clock | all | HUD | ALLOWED | off | Not game info |
| system | Memory / CPU | all | HUD | ALLOWED | off | Not game info |
| combo | Combo Counter | all | HUD | GRAY | off | A number derived from your own hits that vanilla never shows |
| crosshair | Custom Crosshair | all | AESTHETIC | ALLOWED | off | Look only. Same position and function |
| zoom | Zoom | all | listed example (OptiFine) | ALLOWED | on | OptiFine is an explicitly allowed mod and ships a zoom key |
| toggle_sprint | Toggle Sprint | all | none | GRAY | off | Policy names "auto-sprint" as disallowed. Toggle is not auto, but that distinction is not in the text |
| toggle_sneak | Toggle Sneak | all | none | GRAY | off | Same reasoning as toggle_sprint |
| fullbright | Brightness | all | BRIGHT | ALLOWED | off | Its own explicit category |
| hit_color | Hit Color | all | AESTHETIC | ALLOWED | off | Tint only |
| damage_tilt | Damage Tilt | all | AESTHETIC | ALLOWED | off | Camera shake amount on damage |
| fire_overlay | Low Fire | all | AESTHETIC | ALLOWED | off | Lowers and fades the fire overlay |
| shield_overlay | Shield Overlay | 1.21+ | AESTHETIC | ALLOWED | off | Lowers or fades a raised shield in first person |
| particles | Particle Multiplier | all | AESTHETIC | ALLOWED | off | Client-side extra crit/sharpness particles |
| chat | Chat Tools | all | AESTHETIC | ALLOWED | off | Timestamps, compacting, search, filters, highlights. Display only; never sends chat |
| screenshot | Screenshot Tools | all | none (not gameplay) | ALLOWED | off | Copy to clipboard, open folder. No uploads |
| freelook | Freelook | all | fails AESTHETIC ("perspective") | DISALLOWED@hypixel | off | Server-gated by serverrules.json |
| own_nametag | Show Own Nametag | all | AESTHETIC | ALLOWED | off | Your own name in third person |
| old_animations | 1.8 Combat Visuals | 1.21+ | listed example (Animations) | ALLOWED | off | Animations only. Gameplay unchanged |
| hypixel_location | Game Detection | 1.8.9 | none | GRAY | off | Prefers the official Hypixel Mod API (sends no chat). Falls back to throttled `/locraw` (an automated command) |
| auto_gg | Auto GG / GL | 1.8.9 | none | GRAY | off | Automatically sends a chat message |
| stats_overlay | Stat Overlay | 1.8.9 | none | GRAY | off | Also limited by the Hypixel API policy (DECISIONS D-010) |
| nameplate_stats | Nameplate Stats | 1.8.9 | none | GRAY | off | Extra info about other players |
| bedwars_tracker | Bedwars Trackers | 1.8.9 | HUD? | GRAY | off | Built only from scoreboard and chat, but timers are derived info |
| hypixel_chat | Hypixel Chat | 1.8.9 | AESTHETIC | ALLOWED | off | Filters, tabs, mention highlights. Display only |
| scoreboard | Scoreboard Tweaks | 1.8.9 | HUD | ALLOWED | off | Hide numbers, move, retitle |
| lobby_clutter | Lobby Clutter | 1.8.9 | AESTHETIC | ALLOWED | off | Hides lobby spam and holograms client-side |
| quick_commands | Quick Commands | 1.8.9 | none | GRAY | off | **One command per keypress**. No sequences, no timers |
| tiertags | Tier Tags (addon plugin) | all | none | GRAY | off | Third-party PvP tier-list data next to names. Force-disabled on Hypixel |
| opt_* | Optimizations (Phase 5) | 1.21+ | PERF | ALLOWED | per benchmark | Only kept if measured; see docs/PERF.md |

## serverrules.json (shipped default)

```json
{ "schema": 1, "servers": [ { "name": "Hypixel", "match": ["hypixel.net", "*.hypixel.net"],
  "disallow": ["freelook", "tiertags"], "note": "Allowed Modifications policy, perspective + extra player info" } ] }
```

The user can update `<gameDir>/Kestrel/serverrules.json` without a new jar (it is merged over the bundled default).
The **competitive-safe** toggle disables every GRAY and DISALLOWED module on every server.
