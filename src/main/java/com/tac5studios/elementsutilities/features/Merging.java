package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.mixin.ExperienceOrbAccessor;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * XP orb merging and ground item merging. When a new orb or item appears, it is added to one that's
 * already lying nearby instead of becoming another entity. Items already on the ground when the
 * server starts are left as they are.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
// unused: handlers are called by the event bus. resource: Level is AutoCloseable, but the server owns and closes it.
@SuppressWarnings({"unused", "resource"})
public final class Merging {

    private Merging() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.isCanceled() || e.loadedFromDisk() || e.getLevel().isClientSide()) return;
        if (e.getEntity() instanceof ExperienceOrb orb) {
            if (Features.on(Feature.XP_ORBS) && mergeOrb(orb)) e.setCanceled(true);
        } else if (e.getEntity() instanceof ItemEntity item) {
            if (Features.on(Feature.ITEM_MERGING) && mergeItem(item)) e.setCanceled(true);
        }
    }

    private static boolean mergeOrb(ExperienceOrb orb) {
        double r = Settings.XP_RADIUS.get();
        for (ExperienceOrb other : orb.level().getEntitiesOfClass(ExperienceOrb.class, orb.getBoundingBox().inflate(r),
                o -> o != orb && o.isAlive())) {
            long total = points(other) + points(orb);
            if (total > Integer.MAX_VALUE) continue;
            other.value = (int) total;
            ((ExperienceOrbAccessor) other).utilities$setCount(1);
            return true;
        }
        return false;
    }

    private static long points(ExperienceOrb orb) {
        return (long) orb.value * Math.max(1, ((ExperienceOrbAccessor) orb).utilities$getCount());
    }

    private static boolean mergeItem(ItemEntity item) {
        if (item.getItem().isEmpty()) return false;
        double r = Settings.ITEM_MERGE_RADIUS.get();
        for (ItemEntity other : item.level().getEntitiesOfClass(ItemEntity.class, item.getBoundingBox().inflate(r),
                o -> o != item && o.isAlive())) {
            if (DroppedItems.mergeInto(other, item)) return true;
        }
        return false;
    }
}
