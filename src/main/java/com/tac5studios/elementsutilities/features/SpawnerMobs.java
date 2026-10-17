package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Mobs from spawners.
 *  - Tagged with the spawner that made them, and kept from despawning (vanilla then leaves them out of the mob cap).
 *  - No AI: their goals and brain are cleared, but they still fall, get pushed by water and fans and take knockback.
 *  - Stacking: each spawn cycle adds a random number of mobs (1 up to the number of stacked spawners) to the
 *    spawner's stacked mobs, up to 100 per stacked mob, at most 6 mobs per 5 seconds per spawner. A spawner keeps
 *    at most 6 stacked mobs alive; once all 6 are full it stops until every one of them is dead.
 *  - Never drop loot bags.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
// unused: handlers are called by the event bus. resource: Level is AutoCloseable, but the server owns and closes it.
@SuppressWarnings({"unused", "resource"})
public final class SpawnerMobs {

    static final String SPAWNER_POS = ElementsUtilities.MOD_ID + ":spawner_pos";
    static final String SPAWNER_DIM = ElementsUtilities.MOD_ID + ":spawner_dim";

    /** Per level, per spawner position: the last game tick a cycle was handled. */
    private static final Map<ServerLevel, Long2LongOpenHashMap> LAST_CYCLE = new WeakHashMap<>();
    /** Per level, per spawner position: start tick of the output window and mobs released in it. */
    private static final Map<ServerLevel, Map<Long, long[]>> WINDOWS = new WeakHashMap<>();
    /** Recent spawner mob deaths, so loot bags dropped outside the normal drop list can be caught too. */
    private static final List<Death> DEATHS = new ArrayList<>();

    private record Death(ServerLevel level, Vec3 pos, long tick) {}

    private SpawnerMobs() {}

    public static boolean isSpawnerMob(Entity e) {
        return e.getPersistentData().contains(SPAWNER_POS);
    }

    private static boolean fromSpawner(Entity e, ServerLevel level, BlockPos spawner) {
        CompoundTag data = e.getPersistentData();
        return data.getLong(SPAWNER_POS) == spawner.asLong() && data.getString(SPAWNER_DIM).equals(level.dimension().location().toString());
    }

    // ---------- spawning ----------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFinalizeSpawn(FinalizeSpawnEvent e) {
        if (e.getSpawnType() != MobSpawnType.SPAWNER || e.isSpawnCancelled()) return;
        BlockEntity be = e.getSpawner() == null ? null : e.getSpawner().left().orElse(null);
        if (!(be instanceof SpawnerBlockEntity spawner) || !(be.getLevel() instanceof ServerLevel level)) return;
        Mob mob = e.getEntity();
        BlockPos pos = be.getBlockPos();

        mob.getPersistentData().putLong(SPAWNER_POS, pos.asLong());
        mob.getPersistentData().putString(SPAWNER_DIM, level.dimension().location().toString());
        mob.setPersistenceRequired();

        if (!Features.on(Feature.SPAWNER_MOB_STACKING)) return;
        if (!cycle(level, spawner, mob)) e.setSpawnCancelled(true);
    }

    /** Runs one spawn cycle. Returns true when {@code mob} should spawn as a new stacked mob. */
    private static boolean cycle(ServerLevel level, SpawnerBlockEntity spawner, Mob mob) {
        BlockPos pos = spawner.getBlockPos();
        long now = level.getGameTime();
        Long2LongOpenHashMap last = LAST_CYCLE.computeIfAbsent(level, l -> new Long2LongOpenHashMap());
        if (last.get(pos.asLong()) == now && last.containsKey(pos.asLong())) return false; // the rest of this cycle's mobs
        last.put(pos.asLong(), now);

        int mobMax = Settings.SPAWNER_MOB_MAX.get();
        int liveMax = Settings.SPAWNER_LIVE_STACKS.get();
        List<Mob> live = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(Settings.SPAWNER_SEARCH_RADIUS.get()),
                m -> m.isAlive() && fromSpawner(m, level, pos));

        if (SpawnerStacks.paused(spawner)) {
            if (!live.isEmpty()) return false;
            SpawnerStacks.setPaused(spawner, false);
        }

        int amount = 1 + level.random.nextInt(SpawnerStacks.count(spawner));
        amount = Math.min(amount, budget(level, pos, now));
        if (amount <= 0) return false;
        int released = 0;

        // Fill the fullest stacks first, so stacks finish one at a time.
        live.sort(Comparator.comparingInt(Stacks::count).reversed());
        for (Mob stack : live) {
            if (amount <= 0) break;
            int room = mobMax - Stacks.count(stack);
            if (room <= 0) continue;
            int add = Math.min(room, amount);
            Stacks.setCount(stack, Stacks.count(stack) + add);
            amount -= add;
            released += add;
        }

        boolean newStack = amount > 0 && live.size() < liveMax;
        if (newStack) {
            Stacks.setCount(mob, Math.min(amount, mobMax));
            released += Math.min(amount, mobMax);
        }
        spend(level, pos, released);

        int stacks = live.size() + (newStack ? 1 : 0);
        boolean allFull = stacks >= liveMax && live.stream().allMatch(m -> Stacks.count(m) >= mobMax)
                && (!newStack || Stacks.count(mob) >= mobMax);
        if (allFull) SpawnerStacks.setPaused(spawner, true);
        return newStack;
    }

    /** Mobs this spawner may still release in the current output window. */
    private static int budget(ServerLevel level, BlockPos pos, long now) {
        long[] w = window(level, pos, now);
        return (int) Math.max(0, Settings.SPAWNER_CAP_MOBS.get() - w[1]);
    }

    private static void spend(ServerLevel level, BlockPos pos, int mobs) {
        window(level, pos, level.getGameTime())[1] += mobs;
    }

    private static long[] window(ServerLevel level, BlockPos pos, long now) {
        Map<Long, long[]> windows = WINDOWS.computeIfAbsent(level, l -> new java.util.HashMap<>());
        long[] w = windows.computeIfAbsent(pos.asLong(), k -> new long[] {now, 0});
        if (now - w[0] >= Settings.SPAWNER_CAP_SECONDS.get() * 20L) {
            w[0] = now;
            w[1] = 0;
        }
        return w;
    }

    // ---------- no AI ----------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.isCanceled() || e.getLevel().isClientSide()) return;
        if (e.getEntity() instanceof Mob mob && isSpawnerMob(mob)) {
            if (Features.on(Feature.SPAWNER_MOB_NO_AI)) stripAi(mob);
        } else if (e.getEntity() instanceof ItemEntity item && !DEATHS.isEmpty()) {
            if (Features.on(Feature.SPAWNER_NO_LOOT_BAGS) && DroppedItems.lootBag(item.getItem()) && nearRecentDeath(item)) {
                e.setCanceled(true);
            }
        }
    }

    /** A spawner mob that converted into another mob stays without AI. */
    static void afterConvert(Mob mob) {
        mob.setPersistenceRequired();
        if (Features.on(Feature.SPAWNER_MOB_NO_AI)) stripAi(mob);
    }

    /** Clears goals and brain behaviours. Physics (gravity, water, fans, knockback) stays. Goals come back on load, so this runs on every join. */
    private static void stripAi(Mob mob) {
        mob.goalSelector.removeAllGoals(g -> true);
        mob.targetSelector.removeAllGoals(g -> true);
        mob.getBrain().removeAllBehaviors();
        mob.setTarget(null);
    }

    // ---------- loot bags ----------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent e) {
        if (!isSpawnerMob(e.getEntity()) || !Features.on(Feature.SPAWNER_NO_LOOT_BAGS)) return;
        e.getDrops().removeIf(item -> DroppedItems.lootBag(item.getItem()));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity().level() instanceof ServerLevel level) || !isSpawnerMob(e.getEntity())) return;
        if (!Features.on(Feature.SPAWNER_NO_LOOT_BAGS)) return;
        long now = level.getGameTime();
        DEATHS.removeIf(d -> now - d.tick() > 2 || d.level() != level);
        DEATHS.add(new Death(level, e.getEntity().position(), now));
    }

    private static boolean nearRecentDeath(ItemEntity item) {
        long now = item.level().getGameTime();
        for (Death d : DEATHS) {
            if (d.level() == item.level() && now - d.tick() <= 2 && d.pos().distanceToSqr(item.position()) <= 9) return true;
        }
        return false;
    }
}
