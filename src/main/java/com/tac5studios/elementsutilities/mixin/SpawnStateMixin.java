package com.tac5studios.elementsutilities.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tac5studios.elementsutilities.features.HostileCap;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hostile mob cap. Vanilla allows 70 monsters per 289 loaded chunks; this swaps in the configured number,
 * counted the same way. Spawner mobs aren't counted (they are persistent, which vanilla skips).
 */
@Mixin(NaturalSpawner.SpawnState.class)
@SuppressWarnings("unused") // Mixin calls these methods
public abstract class SpawnStateMixin {

    @ModifyExpressionValue(method = "canSpawnForCategory",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/MobCategory;getMaxInstancesPerChunk()I"))
    private int utilities$hostileCap(int original, MobCategory category) {
        return category == MobCategory.MONSTER ? HostileCap.cap(original) : original;
    }
}
