package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;

/**
 * Bone meal on sugar cane, cactus, nether wart and vines. Works from players and dispensers.
 * Sugar cane and cactus grow one block (up to the vanilla height of 3), nether wart grows one stage,
 * vines grow one block down.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class BoneMeal {

    private static final int MAX_HEIGHT = 3;

    private BoneMeal() {}

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent e) {
        if (e.isCanceled() || !(e.getLevel() instanceof ServerLevel level)) return;
        BlockState state = e.getState();
        Block block = state.getBlock();
        boolean ours = block == Blocks.SUGAR_CANE || block == Blocks.CACTUS || block == Blocks.NETHER_WART || block == Blocks.VINE;
        if (!ours || !Features.on(Feature.BONE_MEAL)) return;

        boolean grew;
        if (block == Blocks.NETHER_WART) grew = growWart(level, e.getPos(), state);
        else if (block == Blocks.VINE) grew = growVine(level, e.getPos());
        else grew = growColumn(level, e.getPos(), block);

        // Cancelling stops vanilla; applyBonemeal then returns our result and plays the particles.
        e.setCanceled(true);
        e.setSuccessful(grew);
        if (grew) e.getStack().shrink(1);
    }

    private static boolean growWart(ServerLevel level, BlockPos pos, BlockState state) {
        int age = state.getValue(NetherWartBlock.AGE);
        if (age >= NetherWartBlock.MAX_AGE) return false;
        level.setBlock(pos, state.setValue(NetherWartBlock.AGE, age + 1), Block.UPDATE_CLIENTS);
        return true;
    }

    /** Sugar cane and cactus: add one block on top of the column. */
    private static boolean growColumn(ServerLevel level, BlockPos pos, Block block) {
        BlockPos bottom = pos;
        while (level.getBlockState(bottom.below()).is(block)) bottom = bottom.below();
        BlockPos top = pos;
        while (level.getBlockState(top.above()).is(block)) top = top.above();
        int height = top.getY() - bottom.getY() + 1;
        BlockPos target = top.above();
        if (height >= MAX_HEIGHT || !level.isEmptyBlock(target)) return false;
        BlockState grown = block.defaultBlockState();
        if (!grown.canSurvive(level, target)) return false;
        level.setBlockAndUpdate(target, grown);
        return true;
    }

    /** Vines: copy the lowest vine one block down. */
    private static boolean growVine(ServerLevel level, BlockPos pos) {
        BlockPos bottom = pos;
        while (level.getBlockState(bottom.below()).is(Blocks.VINE)) bottom = bottom.below();
        BlockPos target = bottom.below();
        if (!level.isEmptyBlock(target)) return false;
        BlockState grown = level.getBlockState(bottom).setValue(VineBlock.UP, false);
        if (!grown.canSurvive(level, target)) return false;
        level.setBlockAndUpdate(target, grown);
        return true;
    }
}
