package com.tac5studios.elementsutilities.config;

import java.util.List;

/**
 * Every feature, with its switch in features.toml and the mods that already do the same job.
 * When one of those mods is installed, the overlap guard turns the feature off
 * (overlap_guard.force_on keeps it on). The mod lists can be changed in features.toml.
 */
public enum Feature {

    // [spawners]
    SPAWNER_STACKING("spawners", "spawner_stacking", true,
            "Spawners of the same mob stack in one block (up to 12). Silk-touched spawners keep their mob."),
    SPAWNER_MOB_STACKING("spawners", "spawner_mob_stacking", true,
            "Mobs from one spawner merge into stacks of up to 100."),
    SPAWNER_MOB_NO_AI("spawners", "spawner_mob_no_ai", true,
            "Spawner mobs don't walk, attack or target. Water, fans, knockback and gravity still move them."),
    SPAWNER_NO_LOOT_BAGS("spawners", "spawner_no_loot_bags", true,
            "Spawner mobs never drop loot bags."),
    CREATE_FARMING("spawners", "create_farming", true,
            "Create machines count as kill sources and XP turns into Experience Nuggets. Only works when Create is installed."),

    // [mobs]
    NATURAL_MOB_STACKING("mobs", "natural_mob_stacking", true,
            "Identical idle mobs standing together merge into one mob (up to 12).",
            "mobstacker"),
    HOSTILE_CAP("mobs", "hostile_cap", true,
            "Cap natural hostile spawns per loaded chunks. Spawner mobs and passive mobs aren't affected.",
            "incontrol"),
    VILLAGER_SLOWDOWN("mobs", "villager_slowdown", true,
            "Villagers boxed into small spaces think less often. Trading, restocking and breeding work the same."),
    MOB_GRIEFING("mobs", "mob_griefing", true,
            "Per-mob block griefing switches instead of the all-or-nothing game rule.",
            "elements_claims"),

    // [items]
    XP_ORBS("items", "xp_orbs", true,
            "Nearby XP orbs merge into one orb.",
            "clumps"),
    LOOT_BAG_DETECTION("items", "loot_bag_detection", true,
            "Known loot bag mods found at start count as loot bags (LootBoxesPlus, Mob Loot Bags, Dungeon Loot Bags, Treasure Bags, "
                    + "Loot Bags Renewal, LootBags+, Mark's LootBags). Add others to [loot_bags] in settings.toml."),
    ITEM_MERGING("items", "item_merging", true,
            "Dropped items of the same kind near each other merge into one stack."),
    ITEM_CLEANUP("items", "item_cleanup", true,
            "Clear dropped items with a chat countdown, by staff command (default) or on a timer. See settings.toml."),
    ITEM_CHUNK_CAP("items", "item_chunk_cap", false,
            "Limit how many dropped items one chunk can hold."),
    VALUABLE_DESPAWN("items", "valuable_despawn", false,
            "Items on a list stay on the ground longer before they despawn."),

    // [blocks]
    SMART_HOPPERS("blocks", "smart_hoppers", true,
            "Idle hoppers stop checking every tick and wake up when a neighbour changes."),
    REDSTONE_LIMITER("blocks", "redstone_limiter", false,
            "Slow down or break very fast redstone clocks."),
    LEAF_DECAY("blocks", "leaf_decay", true,
            "Leaves with no log nearby decay within a few seconds.",
            "leavesbegone"),
    SAPLING_PLANTING("blocks", "sapling_planting", false,
            "Saplings left lying on grass or dirt plant themselves."),
    BONE_MEAL("blocks", "bone_meal", true,
            "Bone meal works on sugar cane, cactus, nether wart and vines."),
    DOUBLE_DOORS("blocks", "double_doors", true,
            "Opening one door of a matching pair opens both.",
            "doubledoors", "quark"),
    RIGHT_CLICK_HARVEST("blocks", "right_click_harvest", true,
            "Right-click a fully grown crop to harvest and replant it.",
            "rightclickharvest", "quark"),

    // [players]
    KEEP_XP("players", "keep_xp", true,
            "Players keep part of their XP when they die (percentage in settings.toml)."),

    // [staff]
    LAG_REPORT("staff", "lag_report", true,
            "/utilities lag lists the busiest chunks."),
    LAG_ALERT("staff", "lag_alert", false,
            "Tell online staff when a chunk passes the lag threshold.");

    public final String section;
    public final String key;
    public final boolean def;
    public final String comment;
    /** Mod ids that already do this job. */
    public final List<String> guardMods;

    Feature(String section, String key, boolean def, String comment, String... guardMods) {
        this.section = section;
        this.key = key;
        this.def = def;
        this.comment = comment;
        this.guardMods = List.of(guardMods);
    }

    public static Feature byKey(String key) {
        for (Feature f : values()) if (f.key.equals(key)) return f;
        return null;
    }
}
