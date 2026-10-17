package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Saplings left lying on grass or dirt plant themselves after a while. Checked once a second per sapling. */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class SaplingPlanting {

    private SaplingPlanting() {}

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof ItemEntity item) || item.tickCount % 20 != 0) return;
        Level level = item.level();
        if (level.isClientSide() || !item.isAlive() || !item.onGround()) return;
        ItemStack stack = item.getItem();
        if (!stack.is(ItemTags.SAPLINGS) || !(stack.getItem() instanceof BlockItem blockItem)) return;
        if (item.getAge() < Settings.SAPLING_SECONDS.get() * 20 || !Features.on(Feature.SAPLING_PLANTING)) return;

        BlockPos pos = item.blockPosition();
        if (!level.getBlockState(pos.below()).is(BlockTags.DIRT)) return;
        if (!level.getBlockState(pos).canBeReplaced()) return;
        BlockState sapling = blockItem.getBlock().defaultBlockState();
        if (!sapling.canSurvive(level, pos)) return;

        level.setBlockAndUpdate(pos, sapling);
        stack.shrink(1);
        if (stack.isEmpty()) item.discard();
        else item.setItem(stack.copy());
    }
}
