package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.List;

/**
 * Splits stacks that aren't allowed (1.0.1):
 *  - a heavy, boss or never-stack stack outside a player farm splits into single mobs
 *  - a stack over its limit (farm or spawner mob_caps, heavy_mob_cap) splits down to the limit
 *  - normal mobs are never split here
 * Checked when a stack loads from disk (next tick), at every merge check, and by /utilities stacks check.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class StackCheck {

    /** Stacks that loaded from disk, checked on the next tick of their level (not while they join). */
    private static final Map<ServerLevel, List<Mob>> QUEUE = new WeakHashMap<>();
    /** Stacks split since the last log line, by mob id. */
    private static final Map<String, Integer> SPLITS = new TreeMap<>();

    private StackCheck() {}

    public static boolean active() {
        return Features.on(Feature.HEAVY_MOB_RULES);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.isCanceled() || !e.loadedFromDisk() || !(e.getLevel() instanceof ServerLevel level)) return;
        if (!(e.getEntity() instanceof Mob mob) || Stacks.count(mob) <= 1) return;
        QUEUE.computeIfAbsent(level, l -> new ArrayList<>()).add(mob);
    }

    static void runQueued(ServerLevel level) {
        List<Mob> queued = QUEUE.get(level);
        if (queued == null || queued.isEmpty()) return;
        List<Mob> now = new ArrayList<>(queued);
        queued.clear();
        if (!active()) return;
        logSplits("merge check"); // keep merge check splits out of the chunk load line
        for (Mob mob : now) check(mob);
        logSplits("chunk load");
    }

    /** Checks one stack and splits it when it isn't allowed. Returns true when it was split. */
    static boolean check(Mob mob) {
        if (!mob.isAlive() || mob.isRemoved() || Stacks.count(mob) <= 1) return false;
        int size = allowedSize(mob);
        if (Stacks.count(mob) <= size) return false;
        if (Stacks.splitInto(mob, size) <= 0) return false;
        SPLITS.merge(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(), 1, Integer::sum);
        return true;
    }

    /** The biggest stack this mob may be part of where it stands. Normal mobs keep whatever they have. */
    private static int allowedSize(Mob mob) {
        if (SpawnerMobs.isSpawnerMob(mob)) return MobRules.spawnerCap(mob);
        MobRules.Info info = MobRules.info(mob);
        if (info.stacksNaturally()) return Integer.MAX_VALUE;
        if (info.group() != MobRules.Group.HEAVY) return 1; // never_stack or boss bar
        int limit = NaturalStacking.limit(mob);
        if (limit > 1) return limit;
        // Not allowed to stack right now. A heavy farm stack that's only busy (fighting, leashed, named) keeps its size.
        return MobRules.farmAllowed(mob) ? MobRules.farmCap(mob.getType()) : 1;
    }

    /** One log line for everything split since the last one. */
    static void logSplits(String when) {
        if (SPLITS.isEmpty()) return;
        int total = SPLITS.values().stream().mapToInt(Integer::intValue).sum();
        ElementsUtilities.LOGGER.info("[Utilities] Split {} stack(s) that weren't allowed ({}): {}", total, when, describe(SPLITS));
        SPLITS.clear();
    }

    static String describe(Map<String, Integer> counts) {
        StringBuilder out = new StringBuilder();
        counts.forEach((id, n) -> out.append(out.isEmpty() ? "" : ", ").append(id).append(" x").append(n));
        return out.toString();
    }

    /** Result of /utilities stacks check. */
    public record Report(int checked, Map<String, Integer> split) {
        public int total() {
            return split.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    /** Checks every stack in every loaded chunk now. */
    public static Report checkAll(MinecraftServer server) {
        logSplits("merge check"); // keep earlier splits out of this report
        int checked = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<Mob> stacks = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Mob mob && Stacks.count(mob) > 1) stacks.add(mob);
            }
            checked += stacks.size();
            for (Mob mob : stacks) check(mob);
        }
        Map<String, Integer> split = new TreeMap<>(SPLITS);
        logSplits("/utilities stacks check");
        return new Report(checked, split);
    }
}
