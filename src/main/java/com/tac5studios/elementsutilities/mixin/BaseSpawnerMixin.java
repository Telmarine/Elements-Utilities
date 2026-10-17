package com.tac5studios.elementsutilities.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tac5studios.elementsutilities.features.SpawnerControl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spawner stacking hooks into the vanilla spawner (other spawner mods' changes stay in place):
 *  - a stack of N spawners cycles N times as often (never faster than min_delay_ticks);
 *  - when spawner mobs stack, the vanilla "too many mobs nearby" check is skipped, because Utilities keeps its own
 *    limit of stacked mobs per spawner.
 */
@Mixin(BaseSpawner.class)
// unused: Mixin calls these methods. DataFlowIssue: casting this to the target class is how a mixin reaches it.
@SuppressWarnings({"unused", "DataFlowIssue"})
public abstract class BaseSpawnerMixin {

    @Shadow
    private int spawnDelay;

    @Inject(method = "delay", at = @At("RETURN"))
    private void utilities$stackDelay(Level level, BlockPos pos, CallbackInfo ci) {
        spawnDelay = SpawnerControl.delay((BaseSpawner) (Object) this, spawnDelay);
    }

    @ModifyExpressionValue(method = "serverTick", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int utilities$nearby(int nearby, ServerLevel level, BlockPos pos) {
        return SpawnerControl.ownLimit((BaseSpawner) (Object) this) ? 0 : nearby;
    }
}
