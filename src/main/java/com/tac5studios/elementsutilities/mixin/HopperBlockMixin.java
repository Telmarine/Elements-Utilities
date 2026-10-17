package com.tac5studios.elementsutilities.mixin;

import com.tac5studios.elementsutilities.features.SmartHoppers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Smart hoppers: a block change next to a sleeping hopper wakes it. */
@Mixin(HopperBlock.class)
@SuppressWarnings("unused") // Mixin calls these methods
public abstract class HopperBlockMixin {

    @Inject(method = "neighborChanged", at = @At("HEAD"))
    private void utilities$wake(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving,
                                CallbackInfo ci) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SmartHoppers.Sleepy hopper) hopper.utilities$wake();
    }
}
