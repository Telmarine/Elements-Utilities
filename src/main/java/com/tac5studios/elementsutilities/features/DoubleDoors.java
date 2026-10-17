package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Opening one door of a matching pair opens the other one too. Works for any door that extends the vanilla
 * door block (most modded doors do). Runs late, so a claim mod that blocks the click stops both doors.
 * Sneaking opens just the one door.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class DoubleDoors {

    private DoubleDoors() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.isCanceled() || e.getHand() != InteractionHand.MAIN_HAND || e.getUseBlock() == TriState.FALSE) return;
        Player player = e.getEntity();
        Level level = e.getLevel();
        if (level.isClientSide() || player.isShiftKeyDown()) return;
        BlockState state = level.getBlockState(e.getPos());
        if (!(state.getBlock() instanceof DoorBlock door) || !door.type().canOpenByHand()) return;
        if (!Features.on(Feature.DOUBLE_DOORS)) return;

        Direction facing = state.getValue(DoorBlock.FACING);
        DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
        boolean open = state.getValue(DoorBlock.OPEN);
        BlockPos side = e.getPos().relative(hinge == DoorHingeSide.RIGHT ? facing.getCounterClockWise() : facing.getClockWise());
        BlockPos otherPos = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? side : side.below();
        BlockState other = level.getBlockState(otherPos);

        // A real pair: same door, same facing, opposite hinge, same open state.
        if (other.getBlock() != door || other.getValue(DoorBlock.FACING) != facing
                || other.getValue(DoorBlock.HINGE) == hinge || other.getValue(DoorBlock.OPEN) != open) {
            return;
        }
        door.setOpen(player, level, other, otherPos, !open);
    }
}
