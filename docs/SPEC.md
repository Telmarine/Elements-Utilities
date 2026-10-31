# Elements: Utilities — spec

Built as 1.0.0 (16 Oct 2026).

Server-side NeoForge 1.21.1 mod for server performance and the small tweaks server management mods normally don't cover. Runs on its own; no other mod needed.

## Planned features

### Spawner stacking
- Spawners mined with Silk Touch keep their mob (Apothic Spawners or vanilla).
- Spawner items with the same mob type stack, whatever their spawner settings (modifiers, delay, spawn range).
- No merging of settings: stacked spawners just stack. Whatever their settings and modifiers (Apothic Spawners included) do together is fine, as long as the hard caps hold.
- Using a spawner item on a placed spawner with the same mob adds it to that spawner's stack instead of placing a new block. Goes through the normal interaction event, so claim protection applies.
- Hard cap: 12 spawners per stack.
- Output grows with the stack: 1 spawner releases 1 mob per cycle, a full stack of 12 reaches the cap.
- A stack of N spawners runs its spawn cycle N times as often (never faster than once a second), so a full stack can actually reach the cap.
- Hard cap on output: at most 6 mobs per 5 seconds from one spawner stack.
- Spawner mobs stack: mobs from the same spawner merge into stacked mobs of up to 100 each (spawner blocks themselves never merge in the world; only their mobs do).
- Live-mob limit: one spawner has at most 6 stacked mobs alive at once (up to 600 mobs in total). Once those 6 are full, the spawner stops and only starts again when all of them are killed. Mobs are tagged with the spawner that made them.
- Mobs from spawners have no AI: they don't walk, attack, target or wander on their own.
- No loot bags from spawner mobs: loot bag items from loot bag mods are removed from their drops (detected loot bag mods, plus a config list of item ids).
- They still have physics: water streams, Create fans, knockback and gravity move them, so they can be carried to a kill spot. (Done by clearing their goals, not the vanilla NoAI flag, which also stops falling and water push.)
- Breaking a stacked spawner: normal break drops the whole stack, sneak + break drops one spawner (config can pick one behaviour for both).
- Without Silk Touch, a break destroys one spawner of the stack and drops nothing, so a wrong tool never wipes a whole stack.
- Explosions and Create drills/saws can't break a spawner stack (blocked).
- Stack counts and spawner tags are saved on the spawner and the mobs, so they survive restarts and chunk unloads.

### Create integration (auto farming)
- Spawner mobs can be farmed with Create machines.
- Every Create machine that can damage an entity or item counts as a kill source (deployers, saws, crushing wheels, drills and the rest).
- Mobs killed by Create machines drop their items where Create can collect them.
- XP from those kills is turned into Create Experience Nuggets (items Create can move, store and crush).

### XP orb merging
- Nearby XP orbs merge into one orb that holds the full amount.
- Overlap guard: turns itself off when Clumps (`clumps`) is installed. `overlap_guard.force_on = ["xp_orbs"]` keeps it on.
- Tenko: replaces Clumps once this mod is built.

### Hostile mob cap
- Natural hostile spawns are capped per loaded chunks, the same way the vanilla mob cap counts (vanilla: 70 per 289 chunks). Default 40, set in config.
- Spawner mobs don't count and aren't blocked. Passive mobs are left alone.

### Ground item merging
- Dropped items of the same kind near each other merge into one stack (like XP orb merging), up to the item's stack size.

### Dropped item cleanup
- Clears dropped items when staff run `/utilities cleanup` (default), or every X minutes in timer mode (set in config).
- Chat countdown before each sweep (e.g. 60, 30, 10 seconds). A manual sweep counts down from the largest value.
- Items in claimed land, named items and items players just dropped can be left alone (config).

### Villager slowdown
- Villagers boxed into small spaces (trading halls) run their pathfinding and job checks less often, e.g. every 4 ticks instead of every tick.
- Trading, restocking and breeding work the same; they just cost less to run.

### Dropped item cap per chunk (optional, off by default)
- Sets a maximum number of dropped item entities per chunk; the oldest are merged first, then cleared.
- Never touches loot bags (detected loot bag mods plus a config list), named items or items a player just dropped.

### Natural mob stacking (on by default)
- Identical idle mobs standing together merge into one mob with a count in its name, up to 12 per stack.
- Spawner mobs follow the spawner rules instead (up to 100 per stack).
- Only the front mob of a stack takes damage. Its death is a normal kill, so drops, XP and Butchery (knife -> carcass) work as usual, and a new mob with one less in the count takes its place.
- A lead or name tag on the stack pulls out one mob.
- Carry On: sneak + right-click a stack with empty hands picks up one mob, not the stack.
- Feeding a stack with breeding food pulls out two mobs to breed.
- Shearing a stacked sheep shears the whole stack (wool for every sheep).
- Milking a stacked cow gives 1 bucket; a stack of chickens lays 1 egg at a time.
- Despawning: a hostile stack despawns as a whole, like a single mob; passive stacks don't despawn.
- Merge checks run every few seconds in a small radius, not every tick.
- `/kill` removes a whole stack at once, so staff can clear stacks.
- Bred, tamed, named or leashed mobs never stack.
- A stack counts as 1 entity for per-chunk entity limits (Elements: Nexus, Elements: Claims and this mod's own caps).

### Smarter hoppers
- Hoppers with nothing to pull from and nothing to push into stop checking every tick and wake up when a neighbour changes.

### Lag report (staff)
- `/utilities lag [page]` lists the busiest chunks: entities, block entities and hoppers, with coordinates and the top entity types.
- Kept short: one line per chunk, 3 lines per page, with clickable previous/next page buttons.
- Only runs when staff type it; results go only to that person, never to chat or Discord.
- Optional alert (off by default): one staff-only message when a chunk passes a set threshold, with a long cooldown per chunk so it can't flood chat.

### Redstone clock limiter (optional, off by default)
- Detects very fast redstone clocks and slows or breaks them when a chunk goes over a set number of updates per second.
- Off by default; owners who run clocks responsibly can leave it off.

### Faster leaf decay
- Leaves with no log nearby decay within a few seconds after a tree is cut.

### Sapling self-planting (optional, off by default)
- Saplings left lying on grass or dirt plant themselves after about a minute.

### More bone meal targets
- Bone meal works on sugar cane, cactus, nether wart and vines.

### Double doors
- Opening one door of a matching pair opens both. Works on vanilla and modded doors.

### Longer despawn for valuable items (optional, off by default)
- Items on a config list (e.g. diamonds, netherite, enchanted gear) stay on the ground longer than the default 5 minutes.

### Mob griefing per mob
- Per-mob switches for block griefing (creeper explosions, endermen picking up blocks, ghast fireballs, and so on) instead of the all-or-nothing vanilla game rule.
- Overlap guard: turns itself off when Elements: Claims is installed (Claims handles mob griefing). `overlap_guard.force_on = ["mob_griefing"]` keeps it on.

### Keep XP on death
- Players keep a set percentage of their XP when they die (config, 0-100).

### Right-click harvest
- Right-click a fully grown crop to harvest and replant it.
- Checked against every jar on Tenko (15 Oct 2026): nothing else on the server does right-click harvesting, Aquaculture 2 included.
- Overlap guard: turns itself off when a right-click harvest mod is installed (e.g. Right Click Harvest, Quark). `overlap_guard.force_on = ["right_click_harvest"]` keeps it on.
- Goes through the normal block-interaction event, so claim protection (Elements: Claims or any other claim mod) applies.

### More
- Further small tweaks go here as they come up.

## 1.0.1 workup

Status: built.

### Three kinds of stacking
Every mob falls into one of three groups, checked in this order:

1. **Spawner mobs**: mobs a spawner block made (spawn type `SPAWNER`, tagged by Utilities). They follow the spawner rules only: no AI, up to 100 per stack (heavy mobs 4, or their `[spawners] mob_caps` limit), 6 live stacks per spawner. This already covers every mob, including Brain-driven ones like the Warden (1.0.0 clears goals, targets and Brain behaviors), so Warden spawners from crates are farmed like any other spawner. Natural and farm stacking never touch spawner mobs.
2. **Farm mobs**: heavy mobs (below) standing in player-claimed land. Stack by the farm rules.
3. **Natural mobs**: everything else. Stack by the normal `[mob_stacking]` rules (up to 12), except heavy mobs, which never stack outside a player farm.

Fixes natural stacking merging boss mobs (seen at an Ocean Monument). Regular Guardians are left as they are (normal mobs).

### Heavy mobs
- A mob type is heavy when any of these is true (checked once per mob type and remembered until `/utilities reload`):
  - base max health at or above `heavy_health` (default 100). Catches Samurai Dynasty's Jorogumo, Kitsune and Oni (100) and Fujin and Raijin (400) without naming the mod
  - it has a boss bar (Wither, Ender Dragon, Fujin, Raijin, most modded bosses)
  - it's in the `#c:bosses` tag (`skip_boss_tag = true`)
  - it's on `heavy_mobs` (default `["minecraft:elder_guardian"]`, 80 health, so it only stacks in player farms)
- `light_mobs` takes mobs out of the heavy group so they stack like normal mobs everywhere. Empty by default. Its comment in `settings.toml`:
  ```
  # Mobs listed here never count as heavy, so they stack everywhere like normal mobs.
  # Add a mob by its id in quotes: "modid:mob_name", for example "minecraft:ravager".
  # A whole mod: "modid:*". Ids for every mob from other mods are listed in
  # detected_mobs.toml next to this file. Save, then run /utilities reload.
  ```
- Iron Golems and Ravagers (100 health) are heavy, so they stack in player farms only, never in the open world.
- Heavy mobs never stack outside player-claimed land.
- Heavy mobs with no spawn type never stack anywhere. Vanilla creates player-built Iron Golems (pumpkin on iron blocks) and Withers without one, so built golems and built Withers never stack.

### Spawner mob effects
- Some mobs give players effects from code that runs outside their AI, so taking the AI away doesn't stop it:
  - Warden: Darkness on every player within 20 blocks, every 6 seconds
  - Elder Guardian: Mining Fatigue III on every player within 50 blocks, every minute
- With `spawner_mob_no_effects` on, effects from spawner mobs are blocked, so a spawner farm next to a base doesn't blind players or slow their mining. Covers any mob (modded ones included), not only these two, by blocking any effect whose source is a spawner mob.
- Natural and farm mobs keep their effects.

### Mob detection
- At server start and on `/utilities reload`, Utilities reads every mob type that isn't vanilla (any id outside `minecraft:`) and groups them by mod.
- Writes `config/elements_utilities/detected_mobs.toml`, rewritten each time (not a setting file, a list to copy from). One section per mod, one line per mob: id, base health, and the group it falls in now (`normal`, `heavy`, `boss`, `never`) with the reason (health, boss bar, boss tag, listed):
  ```
  [samurai_dynasty]
  "samurai_dynasty:oni" = "heavy (100 health)"
  "samurai_dynasty:tanuki" = "normal (20 health)"
  "samurai_dynasty:raijin" = "boss (boss bar)"
  ```
- The server log gets one summary line: how many mods with mobs were found and how many of their mobs are heavy.
- Nothing is changed by the detection itself; owners copy ids into `light_mobs`, `heavy_mobs`, `never_stack`, `mob_caps` or `mob_spawn_types`.
- Switch: `mob_detection` in `features.toml` `[mobs]` (default true).

### Never stack
- `never_stack` (already in 1.0.0) keeps working: those mobs don't stack anywhere, claims included. Spawner mobs still follow the spawner rules.
- 1.0.1 default: the 1.0.0 list minus `minecraft:warden`, `minecraft:iron_golem` and `minecraft:elder_guardian` (all three move to farm stacking): villager, wandering trader, snow golem, allay, wither, ender dragon, shulker.
- `bosses_never_stack = true`: every mob with a boss bar never stacks, claims included. Withers stay single either way (never_stack, boss bar, and built Withers have no spawn type).

### Farm stacking (player farms)
- Farm = player-claimed land, read through Utilities' claim check (Elements: Claims or any supported claim mod). Admin/server claims don't count unless `admin_claims_count = true`.
- A heavy mob only stacks in a farm when its spawn type is on `spawn_types`, or on its own line in `mob_spawn_types`, which replaces `spawn_types` for that mob:
  - Iron Golem: `MOB_SUMMONED` only, so only golems villagers summon (golem farms) stack. Built golems never do.
  - Elder Guardian: `STRUCTURE`, since Elder Guardians only come with the monument; a player who claims the monument can stack them.
- Wardens: 4 per stack at most, in farms and from spawners.
- Heavy mobs from spawners: 4 per stack at most (`[spawners] heavy_mob_cap`). Covers Warden, Elder Guardian, Ravager, Iron Golem and Samurai Dynasty's Jorogumo, Kitsune and Oni spawners, and any heavy mob from other mods, without naming them.

### Splitting stacks that aren't allowed
- Checked when a chunk loads (so every stack made under 1.0.0 is checked the first time its chunk loads after the update) and at every merge check (`merge_seconds`):
  - a heavy or never-stack stack standing where it isn't allowed (outside a player farm, a claim removed, pushed out of the farm by water or a lead) splits into single mobs
  - any stack over its limit (`mob_caps`, `[spawners] mob_caps`, `max_stack`) splits down to the limit; the extra mobs come out as their own stacks, also at most the limit
  - normal mobs are never split by this
- `/utilities stacks check` runs the same check on every loaded chunk right away and reports how many stacks were split, by mob (node `utilities.stacks`, staff only by default).
- Every split is counted in one log line per check, not one line per mob.

### Master Control File (`features.toml`, needs a restart)
Four new switches, three under `[mobs]` and one under `[spawners]`:

| Key | Default | What it does |
| --- | --- | --- |
| `heavy_mob_rules` | true | Heavy mobs (health, boss bar, boss tag, `heavy_mobs`) never stack outside player farms, and stacks that aren't allowed are split. Off: 1.0.0 behaviour |
| `farm_stacking` | true | Heavy mobs stack inside player-claimed land, by `[farm_stacking]` |
| `mob_detection` | true | Lists every non-vanilla mob, grouped by mod, in `detected_mobs.toml` at start and on reload |
| `spawner_mob_no_effects` | true | Under `[spawners]`. Spawner mobs can't give players effects (Warden Darkness, Elder Guardian Mining Fatigue) |

`heavy_mob_rules` and `farm_stacking` sit with `natural_mob_stacking` and are turned off with it by the same overlap guard (mob stacking mods). `mob_detection` keeps running either way.

### settings.toml (reloads with `/utilities reload`)

`[mob_stacking]` (existing section, new keys):

| Key | Default | What it does |
| --- | --- | --- |
| `never_stack` | see above | Mobs that never stack anywhere |
| `bosses_never_stack` | true | Mobs with a boss bar never stack |
| `heavy_health` | 100 | Base max health from which a mob counts as heavy (1-10000) |
| `skip_boss_tag` | true | Mobs in `#c:bosses` count as heavy |
| `heavy_mobs` | ["minecraft:elder_guardian"] | Extra mobs that count as heavy |
| `light_mobs` | [] | Mobs that never count as heavy (stack everywhere) |

`[farm_stacking]` (new section):

| Key | Default | What it does |
| --- | --- | --- |
| `max_stack` | 8 | Most heavy mobs per stack inside a player farm (2-1000) |
| `mob_caps` | ["minecraft:warden=4"] | Per-mob limits, `"id=number"`, override `max_stack` |
| `spawn_types` | ["NATURAL", "MOB_SUMMONED", "EVENT", "PATROL", "TRIGGERED", "REINFORCEMENT"] | Spawn reasons that may stack in a farm (NeoForge saves each mob's spawn type on the mob). `STRUCTURE` and `CHUNK_GENERATION` are left out, so mobs that came with the world never stack. Mobs with no spawn type never stack |
| `mob_spawn_types` | ["minecraft:iron_golem=MOB_SUMMONED", "minecraft:elder_guardian=STRUCTURE"] | Per-mob spawn reasons, `"id=TYPE,TYPE"`, replacing `spawn_types` for that mob |
| `admin_claims_count` | false | Admin/server claims count as farms |
| `no_claim_mod` | "never" | No claim mod installed: `"never"` = heavy mobs never stack, `"spawn_type"` = they stack anywhere by the spawn type lists and the farm limits |

`[spawners]` (existing section, new keys):

| Key | Default | What it does |
| --- | --- | --- |
| `heavy_mob_cap` | 4 | Most heavy mobs (by the heavy rules above) per spawner mob stack (1-10000) |
| `mob_caps` | ["minecraft:warden=4"] | Per-mob limits for spawner mob stacks, override `heavy_mob_cap` and `mob_stack_max` (100) |

Ids take full ids or a whole mod (`somemod:*`).

### Commands
- New: `/utilities stacks check`, node `utilities.stacks`, staff only by default.

### Reload
- 1.0.0's `/utilities reload` only re-reads `messages.toml` and the installed mods. 1.0.1 makes it re-read `settings.toml` too (every list and number, including the remembered heavy-mob results), so no config change except `features.toml` switches needs a restart.

### Upgrading from 1.0.0
- An existing `settings.toml` keeps its old `never_stack`, which still lists `minecraft:warden`, `minecraft:iron_golem` and `minecraft:elder_guardian`. On first start with 1.0.1, if `never_stack` is exactly the 1.0.0 default, those three are taken out and a backup of the old file is kept. An edited list is left alone and a log line says which of the three are still on it.

## Commands and messages
- `/utilities lag [page]`, `/utilities cleanup` and `/utilities reload`, staff-only by default. Permission nodes through the NeoForge permission API (`utilities.<command>`), so they work with Elements: Nexus ranks, any other permission mod, or OP levels.
- `config/elements_utilities/messages.toml`: every player-facing line (stack names, cleanup countdown, lag report, etc.), `&` colors.

## Standing rules
- Overlap guard on every feature: any installed mod that already does the same thing turns that feature off (`overlap_guard.force_on` keeps it on). Known so far: Clumps (XP orbs), In Control! (hostile cap), Right Click Harvest / Quark (harvest), mob stacking mods (natural stacking), Elements: Claims (mob griefing), Elements: Nexus / Elements: Claims (entity caps).
- Mod detection happens only at server start and on reload: Utilities recognises the mods that are actually loaded then (by mod id) and sets its overlap guards and integrations from that list. Nothing is looked up while the server runs.
- `config/elements_utilities/features.toml` is the Master Control File: every feature has its own switch, and a feature that's off doesn't load.
- Stand-alone: works without Elements: Nexus or any other TAC5 mod.
- When another Elements mod handles the same thing (entity caps in Elements: Nexus / Elements: Claims, mob griefing in Elements: Claims), Utilities uses that mod's settings.
- License: same text as the other mods, header "Elements: Utilities".
