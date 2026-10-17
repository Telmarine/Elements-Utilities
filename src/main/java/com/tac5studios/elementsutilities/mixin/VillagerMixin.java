package com.tac5studios.elementsutilities.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tac5studios.elementsutilities.features.VillagerSlowdown;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Villager slowdown. A villager boxed into a small space (a trading hall cell) runs its brain only every few ticks.
 * Movement, trading, restocking and level-ups aren't part of the brain tick and run as normal.
 */
@Mixin(Villager.class)
// unused: Mixin calls these methods. DataFlowIssue: casting this to the target class is how a mixin reaches it.
@SuppressWarnings({"unused", "DataFlowIssue"})
public abstract class VillagerMixin {

    @WrapOperation(method = "customServerAiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void utilities$slowBrain(Brain<?> brain, ServerLevel level, LivingEntity entity, Operation<Void> original) {
        if (VillagerSlowdown.shouldTick((Villager) (Object) this)) original.call(brain, level, entity);
    }
}
