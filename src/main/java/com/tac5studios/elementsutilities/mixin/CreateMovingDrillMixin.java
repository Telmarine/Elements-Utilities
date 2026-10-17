package com.tac5studios.elementsutilities.mixin;

import com.tac5studios.elementsutilities.features.SpawnerStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Drills and saws on moving Create contraptions can't break a spawner stack. Only applied when Create is installed. */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour")
@SuppressWarnings("unused") // Mixin calls these methods
public abstract class CreateMovingDrillMixin {

    @Inject(method = "canBreak", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void utilities$keepStacks(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (SpawnerStacks.isStack(level, pos)) cir.setReturnValue(false);
    }
}
