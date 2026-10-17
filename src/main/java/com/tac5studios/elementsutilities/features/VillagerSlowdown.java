package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Villager slowdown (see VillagerMixin). "Boxed in" means every side of the villager is blocked at foot or head
 * height. That's checked once every 5 seconds per villager, not every tick.
 */
@SuppressWarnings("resource") // Level is AutoCloseable, but the server owns and closes it
public final class VillagerSlowdown {

    private static final int RECHECK_TICKS = 100;

    /** Per villager: game time of the last check, and the answer. */
    private static final Map<Villager, long[]> CHECKS = new WeakHashMap<>();

    private VillagerSlowdown() {}

    public static boolean shouldTick(Villager villager) {
        if (!Features.on(Feature.VILLAGER_SLOWDOWN)) return true;
        int every = Settings.VILLAGER_EVERY.get();
        if ((villager.tickCount + villager.getId()) % every == 0) return true;
        return !boxed(villager);
    }

    private static boolean boxed(Villager villager) {
        long now = villager.level().getGameTime();
        long[] check = CHECKS.get(villager);
        if (check == null || now - check[0] >= RECHECK_TICKS) {
            check = new long[] {now, enclosed(villager) ? 1 : 0};
            CHECKS.put(villager, check);
        }
        return check[1] == 1;
    }

    private static boolean enclosed(Villager villager) {
        Level level = villager.level();
        BlockPos feet = villager.blockPosition();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos low = feet.relative(d);
            BlockPos high = low.above();
            boolean blocked = !level.getBlockState(low).getCollisionShape(level, low).isEmpty()
                    || !level.getBlockState(high).getCollisionShape(level, high).isEmpty();
            if (!blocked) return false;
        }
        return true;
    }
}
