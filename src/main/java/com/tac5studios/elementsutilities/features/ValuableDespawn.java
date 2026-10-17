package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.util.IdList;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Valuable items stay on the ground longer than the vanilla 5 minutes. */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class ValuableDespawn {

    private static final IdList VALUABLE = new IdList();

    private ValuableDespawn() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.loadedFromDisk() || !(e.getEntity() instanceof ItemEntity item) || e.getLevel().isClientSide()) return;
        if (!Features.on(Feature.VALUABLE_DESPAWN)) return;
        ItemStack stack = item.getItem();
        if (stack.isEmpty()) return;
        if (VALUABLE.contains(Settings.VALUABLE_ITEMS.get(), stack) || (Settings.VALUABLE_ENCHANTED.get() && stack.isEnchanted())) {
            item.lifespan = Math.max(item.lifespan, Settings.VALUABLE_MINUTES.get() * 60 * 20);
        }
    }
}
