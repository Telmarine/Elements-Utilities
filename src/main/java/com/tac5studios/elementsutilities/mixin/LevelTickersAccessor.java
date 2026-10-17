package com.tac5studios.elementsutilities.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** The lag report counts ticking block entities per chunk. */
@Mixin(Level.class)
public interface LevelTickersAccessor {

    @Accessor("blockEntityTickers")
    List<TickingBlockEntity> utilities$getTickers();
}
