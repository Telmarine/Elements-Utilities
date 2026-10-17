package com.tac5studios.elementsutilities.core;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Looks at which mods are loaded, only at server start and on /utilities reload,
 * and decides which features the overlap guard turns off. Nothing is looked up while the server runs.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class Mods {

    /** Features turned off by the overlap guard. */
    private static volatile Set<Feature> guarded = Collections.emptySet();
    /** Which installed mods caused each guard (for the log and /utilities reload). */
    private static volatile Map<Feature, List<String>> found = Collections.emptyMap();

    private static volatile boolean create, carryOn, elementsClaims, opac, ftbChunks;

    /** Loot bag mods the loot_bag_detection feature knows about: mod id -> name for the log. */
    private static final Map<String, String> LOOT_BAG_MODS = Map.of(
            "lootboxesplus", "LootBoxesPlus",
            "mob_loot_bags", "Mob Loot Bags",
            "dungeonlootbags", "Dungeon Loot Bags",
            "treasurebags", "Treasure Bags",
            "lootbags", "Loot Bags Renewal / LootBags+",
            "markslootbags", "Mark's LootBags");
    /** The known loot bag mods that are installed. */
    private static volatile Set<String> lootBagMods = Collections.emptySet();

    private Mods() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @SuppressWarnings("unused") // the event bus needs the parameter
    public static void onAboutToStart(ServerAboutToStartEvent e) {
        detect();
        log();
    }

    /** Re-check the loaded mods against features.toml. */
    public static synchronized void detect() {
        ModList mods = ModList.get();
        create = mods.isLoaded("create");
        carryOn = mods.isLoaded("carryon");
        elementsClaims = mods.isLoaded("elements_claims");
        opac = mods.isLoaded("openpartiesandclaims");
        ftbChunks = mods.isLoaded("ftbchunks");
        Set<String> bags = new HashSet<>();
        for (String id : LOOT_BAG_MODS.keySet()) if (mods.isLoaded(id)) bags.add(id);
        lootBagMods = Set.copyOf(bags);

        Map<Feature, List<String>> hits = new EnumMap<>(Feature.class);
        for (String line : Features.OVERLAP_MODS.get()) {
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            Feature f = Feature.byKey(line.substring(0, eq).trim());
            String mod = line.substring(eq + 1).trim();
            if (f == null) {
                ElementsUtilities.LOGGER.warn("[Utilities] overlap_guard.mods: unknown feature in \"{}\".", line);
                continue;
            }
            if (!mod.isEmpty() && mods.isLoaded(mod)) hits.computeIfAbsent(f, k -> new ArrayList<>()).add(name(mod));
        }

        Set<Feature> off = EnumSet.noneOf(Feature.class);
        if (Features.OVERLAP_GUARD.get()) {
            for (Feature f : hits.keySet()) if (!Features.forcedOn(f)) off.add(f);
        }
        found = hits;
        guarded = off;
    }

    /** Write the overlap guard and loot bag results to the server log. */
    public static void log() {
        if (!lootBagMods.isEmpty() && Features.on(Feature.LOOT_BAG_DETECTION)) {
            List<String> names = lootBagMods.stream().map(LOOT_BAG_MODS::get).sorted().toList();
            ElementsUtilities.LOGGER.info("[Utilities] Loot bag mods found: {}. Their items count as loot bags.", String.join(", ", names));
        }
        found.forEach((f, names) -> {
            if (guarded.contains(f)) {
                ElementsUtilities.LOGGER.info("[Utilities] {} found: {} is turned off. Add \"{}\" to overlap_guard.force_on to keep it.",
                        String.join(", ", names), f.key, f.key);
            } else if (Features.OVERLAP_GUARD.get()) {
                ElementsUtilities.LOGGER.info("[Utilities] {} found: {} is kept on (overlap_guard.force_on).",
                        String.join(", ", names), f.key);
            }
        });
    }

    private static String name(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getDisplayName())
                .orElse(modId);
    }

    /** True when this namespace belongs to an installed loot bag mod. */
    public static boolean lootBagMod(String namespace) {
        return lootBagMods.contains(namespace);
    }

    public static boolean guarded(Feature f) {
        return guarded.contains(f);
    }

    public static int guardedCount() {
        return guarded.size();
    }

    public static boolean create() {
        return create;
    }

    public static boolean carryOn() {
        return carryOn;
    }

    public static boolean elementsClaims() {
        return elementsClaims;
    }

    public static boolean opac() {
        return opac;
    }

    public static boolean ftbChunks() {
        return ftbChunks;
    }
}
