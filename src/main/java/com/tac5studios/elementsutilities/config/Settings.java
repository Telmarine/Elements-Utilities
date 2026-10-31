package com.tac5studios.elementsutilities.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.tac5studios.elementsutilities.ElementsUtilities;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Numbers and lists for each feature: config/elements_utilities/settings.toml
 * The on/off switches are in features.toml. Changes are picked up while the server runs.
 * Item and mob lists take full ids ("minecraft:diamond") or a whole mod ("lootboxesplus:*").
 */
public final class Settings {

    public static final ModConfigSpec SPEC;

    // [loot_bags]
    public static final ConfigValue<List<? extends String>> LOOT_BAGS;

    // [xp_orbs]
    public static final DoubleValue XP_RADIUS;

    // [item_merging]
    public static final DoubleValue ITEM_MERGE_RADIUS;

    // [item_cleanup]
    public static final ConfigValue<String> CLEANUP_MODE;
    public static final IntValue CLEANUP_MINUTES;
    public static final ConfigValue<List<? extends Integer>> CLEANUP_WARNINGS;
    public static final BooleanValue CLEANUP_SKIP_CLAIMED, CLEANUP_SKIP_NAMED, CLEANUP_SKIP_LOOT_BAGS;
    public static final IntValue CLEANUP_JUST_DROPPED;
    public static final ConfigValue<List<? extends String>> CLEANUP_KEEP;

    // [item_chunk_cap]
    public static final IntValue CAP_MAX, CAP_CHECK_SECONDS, CAP_JUST_DROPPED;

    // [valuable_despawn]
    public static final IntValue VALUABLE_MINUTES;
    public static final BooleanValue VALUABLE_ENCHANTED;
    public static final ConfigValue<List<? extends String>> VALUABLE_ITEMS;

    // [leaf_decay]
    public static final IntValue LEAF_MIN_TICKS, LEAF_MAX_TICKS;

    // [sapling_planting]
    public static final IntValue SAPLING_SECONDS;

    // [keep_xp]
    public static final IntValue KEEP_XP_PERCENT;

    // [mob_griefing]
    public static final ConfigValue<List<? extends String>> NO_GRIEFING;

    // [spawners]
    public static final IntValue SPAWNER_MAX_STACK, SPAWNER_MOB_MAX, SPAWNER_LIVE_STACKS, SPAWNER_CAP_MOBS, SPAWNER_CAP_SECONDS,
            SPAWNER_SEARCH_RADIUS, SPAWNER_MIN_DELAY;
    public static final ConfigValue<String> SPAWNER_BREAK_MODE;
    public static final IntValue SPAWNER_HEAVY_CAP;
    public static final ConfigValue<List<? extends String>> SPAWNER_CAPS;

    // [create]
    public static final BooleanValue CREATE_XP_NUGGETS, CREATE_PLAYER_KILLS;

    // [mob_stacking]
    public static final IntValue STACK_MAX, STACK_MERGE_SECONDS;
    public static final DoubleValue STACK_RADIUS;
    public static final BooleanValue STACK_NAME_VISIBLE;
    public static final ConfigValue<List<? extends String>> STACK_NEVER, HEAVY_MOBS, LIGHT_MOBS;
    public static final BooleanValue BOSSES_NEVER_STACK, SKIP_BOSS_TAG;
    public static final IntValue HEAVY_HEALTH;

    // [farm_stacking]
    public static final IntValue FARM_MAX;
    public static final ConfigValue<List<? extends String>> FARM_CAPS, FARM_SPAWN_TYPES, FARM_MOB_SPAWN_TYPES;
    public static final BooleanValue ADMIN_CLAIMS_COUNT;
    public static final ConfigValue<String> NO_CLAIM_MOD;

    /** never_stack as 1.0.0 wrote it. A file still holding exactly this list is updated once on start. */
    public static final List<String> NEVER_STACK_1_0_0 = List.of(
            "minecraft:villager", "minecraft:wandering_trader", "minecraft:iron_golem", "minecraft:snow_golem",
            "minecraft:allay", "minecraft:warden", "minecraft:elder_guardian", "minecraft:wither",
            "minecraft:ender_dragon", "minecraft:shulker");
    /** Taken out of never_stack in 1.0.1: they stack in player farms now. */
    public static final List<String> MOVED_TO_FARMS = List.of("minecraft:warden", "minecraft:iron_golem", "minecraft:elder_guardian");

    // [hostile_cap]
    public static final IntValue HOSTILE_MAX;

    // [smart_hoppers]
    public static final IntValue HOPPER_IDLE_TICKS;

    // [villager_slowdown]
    public static final IntValue VILLAGER_EVERY;

    // [redstone_limiter]
    public static final IntValue REDSTONE_MAX;
    public static final ConfigValue<String> REDSTONE_ACTION;

    // [lag]
    public static final IntValue LAG_TOP, ALERT_ENTITIES, ALERT_BLOCK_ENTITIES, ALERT_CHECK_SECONDS, ALERT_COOLDOWN_MINUTES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Elements: Utilities settings. The on/off switches are in features.toml.",
                "Item and mob lists take full ids (\"minecraft:diamond\") or a whole mod (\"lootboxesplus:*\").");

        b.comment("Loot bag items. Spawner mobs never drop these, and the item cap and cleanup leave them alone.").push("loot_bags");
        LOOT_BAGS = strings(b, "items", List.of("lootboxesplus:*"),
                "Loot bag items or mods. Known loot bag mods are also found on their own (loot_bag_detection in features.toml).");
        b.pop();

        b.push("xp_orbs");
        XP_RADIUS = b.comment("XP orbs within this many blocks merge into one.").defineInRange("radius", 2.0, 0.5, 8.0);
        b.pop();

        b.push("item_merging");
        ITEM_MERGE_RADIUS = b.comment("Dropped items within this many blocks merge into one stack.")
                .defineInRange("radius", 1.5, 0.5, 8.0);
        b.pop();

        b.push("item_cleanup");
        CLEANUP_MODE = b.comment("When dropped items are cleared:",
                        "\"manual\" = only when staff run /utilities cleanup",
                        "\"timer\"  = every interval_minutes (staff can still run it by hand)")
                .define("mode", "manual", o -> o instanceof String m && (m.equals("manual") || m.equals("timer")));
        CLEANUP_MINUTES = b.comment("Minutes between sweeps in timer mode.").defineInRange("interval_minutes", 10, 1, 1440);
        CLEANUP_WARNINGS = b.comment("Seconds before each sweep when a chat warning is sent.",
                        "A manual sweep counts down from the largest value; empty = clear at once.")
                .defineListAllowEmpty("warnings_seconds", List.of(60, 30, 10), () -> 10,
                        o -> o instanceof Integer i && i > 0 && i < 3600);
        CLEANUP_SKIP_CLAIMED = b.comment("Leave items on claimed land alone (Elements: Claims, Open Parties and Claims, FTB Chunks).")
                .define("skip_claimed", true);
        CLEANUP_SKIP_NAMED = b.comment("Leave items with a custom name alone.").define("skip_named", true);
        CLEANUP_SKIP_LOOT_BAGS = b.comment("Leave loot bags alone (list under [loot_bags]).").define("skip_loot_bags", true);
        CLEANUP_JUST_DROPPED = b.comment("Leave items a player dropped in the last this-many seconds alone. 0 = off.")
                .defineInRange("just_dropped_seconds", 60, 0, 3600);
        CLEANUP_KEEP = strings(b, "keep_items", List.of(), "Items that are never cleared.");
        b.pop();

        b.push("item_chunk_cap");
        CAP_MAX = b.comment("Most dropped items one chunk can hold. Items of the same kind are merged first, then the oldest are cleared.")
                .defineInRange("max_items", 64, 1, 10000);
        CAP_CHECK_SECONDS = b.comment("Seconds between checks.").defineInRange("check_seconds", 10, 1, 600);
        CAP_JUST_DROPPED = b.comment("Never clear items a player dropped in the last this-many seconds.")
                .defineInRange("just_dropped_seconds", 60, 0, 3600);
        b.pop();

        b.push("valuable_despawn");
        VALUABLE_MINUTES = b.comment("Minutes these items stay on the ground (vanilla: 5).").defineInRange("minutes", 15, 5, 1440);
        VALUABLE_ENCHANTED = b.comment("Count every enchanted item as valuable.").define("enchanted_items", true);
        VALUABLE_ITEMS = strings(b, "items", List.of(
                "minecraft:diamond", "minecraft:diamond_block", "minecraft:netherite_ingot", "minecraft:netherite_block",
                "minecraft:netherite_scrap", "minecraft:ancient_debris", "minecraft:elytra", "minecraft:totem_of_undying",
                "minecraft:nether_star", "minecraft:beacon", "minecraft:enchanted_golden_apple", "minecraft:shulker_box"),
                "Items that stay longer.");
        b.pop();

        b.push("leaf_decay");
        LEAF_MIN_TICKS = b.comment("Shortest wait before a loose leaf decays, in ticks (20 = 1 second).")
                .defineInRange("min_ticks", 10, 1, 1200);
        LEAF_MAX_TICKS = b.comment("Longest wait before a loose leaf decays, in ticks.").defineInRange("max_ticks", 60, 1, 1200);
        b.pop();

        b.push("sapling_planting");
        SAPLING_SECONDS = b.comment("Seconds a sapling lies on grass or dirt before it plants itself.")
                .defineInRange("seconds", 60, 5, 600);
        b.pop();

        b.push("keep_xp");
        KEEP_XP_PERCENT = b.comment("Percent of their XP players keep when they die. The XP orbs dropped on death shrink to match.")
                .defineInRange("percent", 50, 0, 100);
        b.pop();

        b.push("mob_griefing");
        NO_GRIEFING = strings(b, "no_griefing", List.of(),
                "Mobs that can't change blocks, e.g. [\"minecraft:creeper\", \"minecraft:enderman\", \"minecraft:ghast\"].",
                "Creepers and ghasts still explode and hurt players; only the blocks are left alone.");
        b.pop();

        b.comment("Spawner stacking and spawner mobs.").push("spawners");
        SPAWNER_MAX_STACK = b.comment("Most spawners in one stack.").defineInRange("max_stack", 12, 1, 64);
        SPAWNER_MOB_MAX = b.comment("Most mobs in one stacked spawner mob.").defineInRange("mob_stack_max", 100, 1, 10000);
        SPAWNER_LIVE_STACKS = b.comment("Most stacked mobs one spawner can have alive. When they are all full the spawner stops",
                "until every one of them is killed.").defineInRange("live_stacks", 6, 1, 64);
        SPAWNER_CAP_MOBS = b.comment("Most mobs one spawner stack releases per cap_seconds.").defineInRange("cap_mobs", 6, 1, 1000);
        SPAWNER_CAP_SECONDS = b.comment("Length of the output window in seconds.").defineInRange("cap_seconds", 5, 1, 600);
        SPAWNER_SEARCH_RADIUS = b.comment("How far from its spawner a stacked mob still counts as alive for that spawner, in blocks.")
                .defineInRange("search_radius", 48, 8, 128);
        SPAWNER_MIN_DELAY = b.comment("Shortest time between spawn cycles of a stack, in ticks. A stack of N spawners cycles N times as often.")
                .defineInRange("min_delay_ticks", 20, 1, 1200);
        SPAWNER_BREAK_MODE = b.comment("Breaking a stacked spawner with Silk Touch:",
                        "\"both\" = a normal break drops the whole stack, sneak + break drops one",
                        "\"all\"  = always the whole stack",
                        "\"one\"  = always one",
                        "Without Silk Touch a break destroys one spawner of the stack and drops nothing.")
                .define("break_mode", "both", o -> o instanceof String m && (m.equals("both") || m.equals("all") || m.equals("one")));
        SPAWNER_HEAVY_CAP = b.comment("Most heavy mobs (see [mob_stacking]) in one stacked spawner mob.")
                .defineInRange("heavy_mob_cap", 4, 1, 10000);
        SPAWNER_CAPS = strings(b, "mob_caps", List.of("minecraft:warden=4"),
                "Limits for single mobs in spawner mob stacks, \"id=number\". They replace heavy_mob_cap and mob_stack_max for that mob.");
        b.pop();

        b.comment("Create machines as kill sources (only used when Create is installed).").push("create");
        CREATE_PLAYER_KILLS = b.comment("Mobs killed by Create machines drop XP and player-only loot, like a player kill.")
                .define("count_as_player_kill", true);
        CREATE_XP_NUGGETS = b.comment("XP from mobs killed by Create machines drops as Experience Nuggets (3 XP each).")
                .define("xp_as_nuggets", true);
        b.pop();

        b.comment("Natural mob stacking.").push("mob_stacking");
        STACK_MAX = b.comment("Most mobs in one natural stack.").defineInRange("max_stack", 12, 2, 1000);
        STACK_RADIUS = b.comment("Mobs within this many blocks of each other merge.").defineInRange("radius", 5.0, 1.0, 16.0);
        STACK_MERGE_SECONDS = b.comment("Seconds between merge checks.").defineInRange("merge_seconds", 5, 1, 120);
        STACK_NAME_VISIBLE = b.comment("Always show the stack count above the mob. Off: it shows when a player looks at the mob.")
                .define("name_always_visible", false);
        STACK_NEVER = strings(b, "never_stack", List.of(
                "minecraft:villager", "minecraft:wandering_trader", "minecraft:snow_golem", "minecraft:allay",
                "minecraft:wither", "minecraft:ender_dragon", "minecraft:shulker"),
                "Mobs that never stack, player farms included. Spawner mobs still follow the spawner rules.");
        BOSSES_NEVER_STACK = b.comment("Mobs with a boss bar never stack, player farms included.").define("bosses_never_stack", true);
        HEAVY_HEALTH = b.comment("Heavy mobs only stack in player farms (see [farm_stacking]).",
                        "A mob is heavy when its base max health is at least this much, it has a boss bar, it's in #c:bosses",
                        "(skip_boss_tag) or it's on heavy_mobs.")
                .defineInRange("heavy_health", 100, 1, 10000);
        SKIP_BOSS_TAG = b.comment("Mobs in the #c:bosses tag count as heavy.").define("skip_boss_tag", true);
        HEAVY_MOBS = strings(b, "heavy_mobs", List.of("minecraft:elder_guardian"), "Extra mobs that count as heavy.");
        LIGHT_MOBS = strings(b, "light_mobs", List.of(),
                "Mobs listed here never count as heavy, so they stack everywhere like normal mobs.",
                "Add a mob by its id in quotes: \"modid:mob_name\", for example \"minecraft:ravager\".",
                "A whole mod: \"modid:*\". Ids for every mob from other mods are listed in",
                "detected_mobs.toml next to this file. Save, then run /utilities reload.");
        b.pop();

        b.comment("Heavy mobs in player farms (player-claimed land). Spawner mobs follow [spawners] instead.").push("farm_stacking");
        FARM_MAX = b.comment("Most heavy mobs in one stack inside a player farm.").defineInRange("max_stack", 8, 2, 1000);
        FARM_CAPS = strings(b, "mob_caps", List.of("minecraft:warden=4"),
                "Limits for single mobs, \"id=number\". They replace max_stack for that mob.");
        FARM_SPAWN_TYPES = strings(b, "spawn_types", List.of("NATURAL", "MOB_SUMMONED", "EVENT", "PATROL", "TRIGGERED", "REINFORCEMENT"),
                "How a heavy mob came into the world, for it to stack in a farm. Mobs with no spawn type",
                "(player-built Iron Golems and Withers) never stack. Types: NATURAL, CHUNK_GENERATION, SPAWNER, STRUCTURE,",
                "BREEDING, MOB_SUMMONED, JOCKEY, EVENT, CONVERSION, REINFORCEMENT, TRIGGERED, BUCKET, SPAWN_EGG, COMMAND,",
                "DISPENSER, PATROL, TRIAL_SPAWNER.");
        FARM_MOB_SPAWN_TYPES = strings(b, "mob_spawn_types",
                List.of("minecraft:iron_golem=MOB_SUMMONED", "minecraft:elder_guardian=STRUCTURE"),
                "Spawn types for single mobs, \"id=TYPE,TYPE\". They replace spawn_types for that mob.",
                "Iron Golems: only golems villagers summon. Elder Guardians: the ones that came with the monument.");
        ADMIN_CLAIMS_COUNT = b.comment("Admin/server claims count as player farms.").define("admin_claims_count", false);
        NO_CLAIM_MOD = b.comment("With no claim mod installed:",
                        "\"never\"      = heavy mobs never stack",
                        "\"spawn_type\" = heavy mobs stack anywhere, by the spawn types and limits above")
                .define("no_claim_mod", "never", o -> o instanceof String m && (m.equals("never") || m.equals("spawn_type")));
        b.pop();

        b.push("hostile_cap");
        HOSTILE_MAX = b.comment("Natural hostile mobs per 289 loaded chunks (the vanilla way of counting). Vanilla: 70.")
                .defineInRange("max", 40, 0, 1000);
        b.pop();

        b.push("smart_hoppers");
        HOPPER_IDLE_TICKS = b.comment("Ticks an idle hopper waits before checking again (vanilla: every tick).",
                "A block change next to it, or an item landing in it, wakes it straight away.").defineInRange("idle_ticks", 8, 2, 100);
        b.pop();

        b.push("villager_slowdown");
        VILLAGER_EVERY = b.comment("Boxed-in villagers think once every this many ticks.").defineInRange("every_ticks", 4, 2, 20);
        b.pop();

        b.push("redstone_limiter");
        REDSTONE_MAX = b.comment("Redstone updates per chunk per second before the limiter steps in.")
                .defineInRange("max_updates", 500, 50, 100000);
        REDSTONE_ACTION = b.comment("\"slow\" = ignore further updates in that chunk for the rest of the second",
                        "\"break\" = break the redstone component that went over the limit (it drops as an item)")
                .define("action", "slow", o -> o instanceof String a && (a.equals("slow") || a.equals("break")));
        b.pop();

        b.comment("/utilities lag and the lag alert.").push("lag");
        LAG_TOP = b.comment("How many chunks /utilities lag lists in total (3 per page).").defineInRange("top_chunks", 30, 3, 300);
        ALERT_ENTITIES = b.comment("Alert when one chunk has more entities than this.").defineInRange("alert_entities", 200, 10, 100000);
        ALERT_BLOCK_ENTITIES = b.comment("Alert when one chunk has more ticking block entities than this.")
                .defineInRange("alert_block_entities", 400, 10, 100000);
        ALERT_CHECK_SECONDS = b.comment("Seconds between alert checks.").defineInRange("alert_check_seconds", 60, 10, 3600);
        ALERT_COOLDOWN_MINUTES = b.comment("Minutes before the same chunk can alert again.").defineInRange("alert_cooldown_minutes", 30, 1, 1440);
        b.pop();

        SPEC = b.build();
    }

    private Settings() {}

    private static ConfigValue<List<? extends String>> strings(ModConfigSpec.Builder b, String key, List<String> def, String... comment) {
        return b.comment(comment).defineListAllowEmpty(key, def, () -> "", o -> o instanceof String);
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "elements_utilities/settings.toml");
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("elements_utilities").resolve("settings.toml");
    }

    /**
     * Re-reads settings.toml (/utilities reload). NeoForge also picks up a saved file by itself; this makes sure
     * the values in use match the file right now. Values that don't pass their check are left as they are.
     * Returns how many values changed.
     */
    public static int reload() {
        if (!SPEC.isLoaded()) return 0;
        Path path = file();
        if (!Files.exists(path)) return 0;
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(path).build()) {
            cfg.load();
            return apply(SPEC.getValues(), cfg, new ArrayList<>());
        } catch (Exception e) {
            ElementsUtilities.LOGGER.warn("[Utilities] Could not re-read {}: {}", path, e.toString());
            return 0;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int apply(UnmodifiableConfig values, UnmodifiableConfig file, List<String> path) {
        int changed = 0;
        for (UnmodifiableConfig.Entry entry : values.entrySet()) {
            List<String> at = new ArrayList<>(path);
            at.add(entry.getKey());
            Object v = entry.getValue();
            if (v instanceof UnmodifiableConfig section) {
                changed += apply(section, file, at);
            } else if (v instanceof ConfigValue value) {
                Object raw = file.get(at);
                if (raw == null || raw.equals(value.get()) || !value.getSpec().test(raw)) continue;
                value.set(raw);
                changed++;
            }
        }
        return changed;
    }

    /**
     * 1.0.0 to 1.0.1: never_stack still exactly the 1.0.0 default loses the Warden, Iron Golem and Elder Guardian,
     * which stack in player farms now. A copy of the old file is kept. An edited list is left alone.
     */
    public static void upgrade() {
        if (!SPEC.isLoaded()) return;
        List<? extends String> never = STACK_NEVER.get();
        if (new HashSet<>(never).equals(new HashSet<>(NEVER_STACK_1_0_0)) && never.size() == NEVER_STACK_1_0_0.size()) {
            Path path = file();
            try {
                if (Files.exists(path)) Files.copy(path, path.resolveSibling("settings.toml.1.0.0.bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                ElementsUtilities.LOGGER.warn("[Utilities] Could not back up {}: {}", path, e.toString());
                return;
            }
            List<String> updated = new ArrayList<>(never);
            updated.removeAll(MOVED_TO_FARMS);
            STACK_NEVER.set(updated);
            STACK_NEVER.save();
            ElementsUtilities.LOGGER.info("[Utilities] never_stack updated for 1.0.1: Warden, Iron Golem and Elder Guardian now stack "
                    + "in player farms. Old file kept as settings.toml.1.0.0.bak.");
            return;
        }
        List<String> still = MOVED_TO_FARMS.stream().filter(never::contains).toList();
        if (!still.isEmpty()) {
            ElementsUtilities.LOGGER.info("[Utilities] never_stack was edited, so it was left alone. Still on it (never stack, "
                    + "player farms included): {}", String.join(", ", still));
        }
    }
}
