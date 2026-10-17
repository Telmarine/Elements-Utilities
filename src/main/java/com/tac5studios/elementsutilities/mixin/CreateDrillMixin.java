package com.tac5studios.elementsutilities.mixin;

import com.tac5studios.elementsutilities.features.SpawnerStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * Create drills and saws (placed blocks) can't break a spawner stack. Create breaks blocks without a player,
 * so no break event fires. Only applied when Create is installed; if a Create update renames things,
 * the hook does nothing instead of crashing.
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity")
// unused: Mixin calls these methods. DataFlowIssue: casting this to the target class is how a mixin reaches it.
@SuppressWarnings({"unused", "DataFlowIssue"})
public abstract class CreateDrillMixin {

    @Unique
    private static Field utilities$breakingPos;
    @Unique
    private static boolean utilities$looked;

    @Inject(method = "canBreak", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void utilities$keepStacks(BlockState state, float hardness, CallbackInfoReturnable<Boolean> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        BlockPos pos = utilities$pos(self);
        if (pos != null && SpawnerStacks.isStack(self.getLevel(), pos)) cir.setReturnValue(false);
    }

    @Unique
    private static BlockPos utilities$pos(Object self) {
        if (!utilities$looked) {
            utilities$looked = true;
            for (Class<?> c = self.getClass(); c != null && utilities$breakingPos == null; c = c.getSuperclass()) {
                try {
                    Field f = c.getDeclaredField("breakingPos");
                    f.setAccessible(true);
                    utilities$breakingPos = f;
                } catch (NoSuchFieldException ignored) {
                    // keep looking up the hierarchy
                } catch (RuntimeException ignored) {
                    break;
                }
            }
        }
        try {
            return utilities$breakingPos == null ? null : (BlockPos) utilities$breakingPos.get(self);
        } catch (IllegalAccessException | RuntimeException e) {
            return null;
        }
    }
}
