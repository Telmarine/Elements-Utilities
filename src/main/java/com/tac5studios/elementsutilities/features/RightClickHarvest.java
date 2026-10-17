package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

/**
 * Right-click a fully grown crop to harvest it and replant it. One seed from the drops pays for the replant.
 * Works on vanilla crops, nether wart, cocoa and modded crops built on the vanilla crop block.
 * Runs late and also fires a block-break check, so claim mods can stop it. Sneaking skips it.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class RightClickHarvest {

    private RightClickHarvest() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.isCanceled() || e.getHand() != InteractionHand.MAIN_HAND || e.getUseBlock() == TriState.FALSE) return;
        if (!(e.getEntity() instanceof ServerPlayer player) || !(e.getLevel() instanceof ServerLevel level)) return;
        if (player.isShiftKeyDown() || player.isSpectator() || player.getMainHandItem().is(Items.BONE_MEAL)) return;
        BlockPos pos = e.getPos();
        BlockState state = level.getBlockState(pos);
        BlockState replant = replant(state);
        if (replant == null || !Features.on(Feature.RIGHT_CLICK_HARVEST)) return;

        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) return;

        List<ItemStack> drops = Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
        if (!takeSeed(drops, state.getBlock().asItem())) return; // no seed to replant with: leave it to vanilla

        level.setBlock(pos, replant, Block.UPDATE_ALL);
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) Block.popResource(level, pos, drop);
        }
        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.swing(InteractionHand.MAIN_HAND, true);
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** The replanted state for a fully grown crop, or null when it isn't one. */
    private static BlockState replant(BlockState state) {
        return switch (state.getBlock()) {
            case CropBlock crop -> crop.isMaxAge(state) ? crop.getStateForAge(0) : null;
            case NetherWartBlock ignored ->
                    state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE ? state.setValue(NetherWartBlock.AGE, 0) : null;
            case CocoaBlock ignored ->
                    state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE ? state.setValue(CocoaBlock.AGE, 0) : null;
            default -> null;
        };
    }

    private static boolean takeSeed(List<ItemStack> drops, Item seed) {
        for (ItemStack drop : drops) {
            if (drop.is(seed)) {
                drop.shrink(1);
                return true;
            }
        }
        return false;
    }
}
