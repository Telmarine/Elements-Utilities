package com.tac5studios.elementsutilities.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Merged items keep the younger age and the longer pickup delay, the same as vanilla merging. */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {

    @Accessor("age")
    void utilities$setAge(int age);

    @Accessor("pickupDelay")
    int utilities$getPickupDelay();
}
