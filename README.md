# Elements: Utilities

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge) ![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-E68A2E?style=for-the-badge) ![Side](https://img.shields.io/badge/Side-Server-5865F2?style=for-the-badge) ![Type](https://img.shields.io/badge/Type-Performance-8A2BE2?style=for-the-badge) [![Source](https://img.shields.io/badge/Source-GitHub-181717?style=for-the-badge&logo=github)](https://github.com/Telmarine/Elements-Utilities)

A server-side mod for NeoForge 1.21.1 that covers the things server management mods usually leave out: spawner and mob stacking, item cleanup, mob caps, a lag report and a set of small tweaks. Every feature has its own on/off switch. It runs on its own; no other mod is needed.

## Spawners

- **Silk Touch keeps the mob.** Spawner items only carry their mob, so spawners of the same mob stack in your inventory.
- **Stacking:** right-click a placed spawner with a spawner item of the same mob to add it to the stack (up to 12). Sneak + right-click places it as a new block. Claim mods can stop this like any other block click.
- **Settings don't merge.** The placed spawner keeps its own settings (including other spawner mods' upgrades); the stack only multiplies its output.
- **Output:** a stack of N spawners cycles N times as often, and each cycle releases 1 to N mobs. One spawner stack never releases more than 6 mobs per 5 seconds.
- **Stacked mobs:** spawner mobs merge into stacked mobs of up to 100. A spawner keeps at most 6 stacked mobs alive. When all 6 are full the spawner stops, and starts again once every one of them is dead.
- **No AI:** spawner mobs don't walk, attack or target anyone. Gravity, water, fans and knockback still move them, so they can be carried to a kill spot.
- **No loot bags** from spawner mobs. Utilities doesn't add loot bags; it hooks into loot bag mods (see Items).
- **Breaking:** with Silk Touch, a normal break drops the whole stack and sneak + break drops one (`break_mode`). Without Silk Touch one spawner of the stack is destroyed and nothing drops.
- Explosions and Create drills and saws can't break a stack.

## Create farming

Only active when Create is installed.

- Every Create machine that damages mobs (saws, drills, crushing wheels, fans, rollers, deployers) counts as a player kill, so mobs drop XP and player-only loot.
- Drops are normal items, so funnels and belts pick them up.
- The XP drops as Experience Nuggets (3 XP each).

## Mob stacking

- Identical idle mobs standing together merge into one mob with the count in its name, up to 12.
- Only the front mob takes damage. Its death is a normal kill (drops, XP, Butchery carcasses), and the next mob takes its place. Fall damage, lava and magma blocks work the same way; a magma block floor makes a good kill spot.
- A lead or name tag pulls one mob out. With Carry On, sneak + right-click with an empty hand picks up one mob.
- Breeding food pulls two mobs out to breed. Shearing a stack shears every sheep in it. Milking gives one bucket and a chicken stack lays one egg at a time.
- Bred, tamed, named and leashed mobs never stack. A stack counts as 1 entity for chunk entity limits.
- A hostile stack despawns as a whole; passive stacks don't despawn.
- `/kill` removes a whole stack at once.

## Items

- **XP orbs** merge into one orb when they land near each other.
- **Dropped items** of the same kind merge into one stack.
- **Cleanup:** staff clear dropped items with `/utilities cleanup`, after a chat countdown. Set `mode = "timer"` in `settings.toml` to also clear them every 10 minutes (or any interval). Items on claimed land, named items, loot bags and items a player just dropped are left alone.
- **Loot bags:** LootBoxesPlus, Mob Loot Bags, Dungeon Loot Bags, Treasure Bags, Loot Bags Renewal, LootBags+ and Mark's LootBags are detected at start, and their items count as loot bags. Other loot bag items or mods go in `[loot_bags]` in `settings.toml`.
- **Chunk cap** (off by default): most dropped items per chunk. Items are merged first, then the oldest are cleared.
- **Valuable items** (off by default) stay on the ground longer.

## Performance

- **Hostile mob cap:** natural hostile spawns per loaded chunks, counted the vanilla way (vanilla 70, default 40). Spawner mobs and passive mobs aren't affected.
- **Smart hoppers:** idle hoppers stop checking every tick and wake up when a block next to them changes or an item lands in them.
- **Villager slowdown:** villagers boxed into small spaces (trading halls) think every 4 ticks. Trading, restocking and breeding work the same.
- **Redstone clock limiter** (off by default): very fast redstone clocks are slowed or broken.

## Small tweaks

- **Faster leaf decay** after a tree is cut, however it's cut.
- **Sapling self-planting** (off by default) on grass or dirt.
- **More bone meal targets:** sugar cane, cactus, nether wart and vines.
- **Double doors** open together (vanilla and modded doors).
- **Keep XP on death:** players keep a percentage of their XP (50% by default).
- **Right-click harvest:** right-click a fully grown crop to harvest and replant it. Claim mods can stop it.
- **Mob griefing per mob:** pick which mobs can't change blocks, instead of the all-or-nothing game rule.

## Commands

- `/utilities lag [page]`: the busiest chunks, 3 per page, with their entity, block entity and hopper counts. Only the person who runs it sees the result. Click a line to put a teleport command in chat.
- `/utilities cleanup`: clear dropped items after the chat countdown.
- `/utilities reload`: reload `messages.toml` and check installed mods again.

An optional lag alert (off by default) tells online staff when a chunk passes a threshold, with a long cooldown per chunk.

## Permissions

Nodes are `utilities.<name>` and work with Elements: Nexus ranks, other permission mods, or OP levels.

| Node | Default |
| --- | --- |
| `lag`, `lag.alert`, `cleanup` | OP 2 |
| `reload` | OP 3 |

## Config

`config/elements_utilities/`
- `features.toml`: an on/off switch for every feature, plus the overlap guard
- `settings.toml`: numbers and lists for each feature (stack sizes, timers, item lists, mob lists)
- `messages.toml`: every message, `&` colors

Item and mob lists take full ids (`minecraft:diamond`) or a whole mod (`lootboxesplus:*`).

## Overlap guard

When another installed mod already does the same job, that feature turns itself off. Installed mods are checked at server start and on `/utilities reload`.

| Feature | Turned off by |
| --- | --- |
| XP orb merging | Clumps |
| Hostile mob cap | In Control! |
| Mob stacking | Mob Stacker |
| Mob griefing | Elements: Claims |
| Leaf decay | Leaves Be Gone |
| Double doors | Double Doors, Quark |
| Right-click harvest | Right Click Harvest, Quark |

The list is `overlap_guard.mods` in `features.toml` (`"feature=modid"`), so you can add mods without a rebuild. Add a feature to `overlap_guard.force_on` to keep it on anyway.

## Building

`gradlew build`. The jar is `build/libs/elements_utilities-<version>.jar`.
