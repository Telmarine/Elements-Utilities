package com.tac5studios.elementsutilities.mixin;

import com.tac5studios.elementsutilities.features.LeafDecay;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaves update their distance to the nearest log in a scheduled tick. Right after that update,
 * leaves that lost their log are handed to LeafDecay so they fall within seconds instead of minutes.
 */
@Mixin(LeavesBlock.class)
@SuppressWarnings("unused") // Mixin calls these methods
public abstract class LeavesBlockMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void utilities$afterTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        LeafDecay.check(level, pos);
    }
}
