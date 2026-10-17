package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.core.Mods;
import com.tac5studios.elementsutilities.mixin.ItemEntityAccessor;
import com.tac5studios.elementsutilities.util.IdList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Shared checks for dropped items. */
public final class DroppedItems {

    private static final IdList LOOT_BAGS = new IdList();
    /** Vanilla's markers for display items (ItemEntity.INFINITE_PICKUP_DELAY / INFINITE_LIFETIME). */
    private static final int NEVER_PICK_UP = 32767;
    private static final int NEVER_DESPAWN = -32768;

    private DroppedItems() {}

    public static boolean lootBag(ItemStack stack) {
        if (LOOT_BAGS.contains(Settings.LOOT_BAGS.get(), stack)) return true;
        return Features.on(Feature.LOOT_BAG_DETECTION)
                && Mods.lootBagMod(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    public static boolean named(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_NAME);
    }

    /** Dropped by a player less than this many seconds ago. */
    public static boolean justDropped(ItemEntity item, int seconds) {
        return seconds > 0 && item.getAge() >= 0 && item.getAge() < seconds * 20 && item.getOwner() instanceof Player;
    }

    /**
     * Merges {@code from} into {@code into} when both are the same item and the total fits in one stack
     * (the same rule vanilla uses). Returns true when merged; {@code from} is then removed.
     */
    public static boolean mergeInto(ItemEntity into, ItemEntity from) {
        ItemStack a = into.getItem();
        ItemStack b = from.getItem();
        if (a.isEmpty() || b.isEmpty() || !ItemEntity.areMergable(a, b)) return false;
        if (!Objects.equals(into.getTarget(), from.getTarget())) return false;
        // Never merge the special "never despawn" / "never pick up" items other mods use for displays.
        int intoDelay = ((ItemEntityAccessor) into).utilities$getPickupDelay();
        int fromDelay = ((ItemEntityAccessor) from).utilities$getPickupDelay();
        if (intoDelay == NEVER_PICK_UP || fromDelay == NEVER_PICK_UP || into.getAge() == NEVER_DESPAWN || from.getAge() == NEVER_DESPAWN) {
            return false;
        }
        into.setItem(a.copyWithCount(a.getCount() + b.getCount()));
        into.setPickUpDelay(Math.max(intoDelay, fromDelay));
        ((ItemEntityAccessor) into).utilities$setAge(Math.min(into.getAge(), from.getAge()));
        from.discard();
        return true;
    }
}
