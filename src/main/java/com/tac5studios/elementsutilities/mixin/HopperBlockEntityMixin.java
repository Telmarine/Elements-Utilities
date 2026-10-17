package com.tac5studios.elementsutilities.mixin;

import com.tac5studios.elementsutilities.features.SmartHoppers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

/**
 * Smart hoppers. A hopper that had nothing to push and nothing to pull waits a few ticks before it looks again,
 * instead of checking every tick. A block change next to it or an item landing in it wakes it at once.
 * A busy hopper works exactly like vanilla (one item every 8 ticks).
 */
@Mixin(HopperBlockEntity.class)
// unused: Mixin calls these methods. DataFlowIssue: casting this to the target class is how a mixin reaches it.
@SuppressWarnings({"unused", "DataFlowIssue"})
public abstract class HopperBlockEntityMixin implements SmartHoppers.Sleepy {

    @Shadow
    private int cooldownTime;

    @Unique
    private boolean utilities$asleep;

    @Inject(method = "tryMoveItems", at = @At("RETURN"))
    private static void utilities$sleepWhenIdle(Level level, BlockPos pos, BlockState state, HopperBlockEntity hopper,
                                                BooleanSupplier validator, CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide()) return;
        HopperBlockEntityMixin self = (HopperBlockEntityMixin) (Object) hopper;
        if (cir.getReturnValueZ()) {
            self.utilities$asleep = false; // working again: vanilla's own cooldown applies
            return;
        }
        // Only when the hopper really looked (not on cooldown, not locked by redstone).
        if (self.cooldownTime > 0 || !state.getValue(HopperBlock.ENABLED)) return;
        int idle = SmartHoppers.idleTicks();
        if (idle > 0) {
            self.cooldownTime = idle;
            self.utilities$asleep = true;
        }
    }

    @Inject(method = "entityInside", at = @At("HEAD"))
    private static void utilities$wakeOnItem(Level level, BlockPos pos, BlockState state, Entity entity, HopperBlockEntity hopper,
                                             CallbackInfo ci) {
        if (entity instanceof ItemEntity) ((SmartHoppers.Sleepy) hopper).utilities$wake();
    }

    @Override
    public void utilities$wake() {
        if (utilities$asleep) {
            utilities$asleep = false;
            cooldownTime = 0;
        }
    }
}
