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
