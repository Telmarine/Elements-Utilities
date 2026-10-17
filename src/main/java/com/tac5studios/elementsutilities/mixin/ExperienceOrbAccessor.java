package com.tac5studios.elementsutilities.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla keeps several equal orbs in one entity as a count; merging needs to read and reset it. */
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbAccessor {

    @Accessor("count")
    int utilities$getCount();

    @Accessor("count")
    void utilities$setCount(int count);
}
