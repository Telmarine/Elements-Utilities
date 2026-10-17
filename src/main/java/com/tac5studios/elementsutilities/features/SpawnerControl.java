package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/** Spawner block hooks used by BaseSpawnerMixin. Spawner minecarts and other mods' spawners are left alone. */
public final class SpawnerControl {

    private SpawnerControl() {}

    private static SpawnerBlockEntity block(BaseSpawner spawner) {
        var owner = spawner.getOwner();
        return owner != null && owner.left().orElse(null) instanceof SpawnerBlockEntity be ? be : null;
    }

    /** The delay until the next cycle: the vanilla delay divided by the stack size. */
    public static int delay(BaseSpawner spawner, int vanilla) {
        SpawnerBlockEntity be = block(spawner);
        if (be == null || !Features.on(Feature.SPAWNER_STACKING)) return vanilla;
        int n = SpawnerStacks.count(be);
        return n <= 1 ? vanilla : Math.max(Settings.SPAWNER_MIN_DELAY.get(), vanilla / n);
    }

    /** True when Utilities, not vanilla's nearby-mob count, limits this spawner. */
    public static boolean ownLimit(BaseSpawner spawner) {
        return block(spawner) != null && Features.on(Feature.SPAWNER_MOB_STACKING);
    }
}
