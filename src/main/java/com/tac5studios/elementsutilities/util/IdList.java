package com.tac5studios.elementsutilities.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A list of ids from the config: full ids ("minecraft:diamond") or a whole mod ("lootboxesplus:*").
 * Rebuilt only when the config list changes.
 */
public final class IdList {

    private List<? extends String> source;
    private Set<String> ids = Set.of();
    private Set<String> mods = Set.of();

    private void refresh(List<? extends String> list) {
        if (list == source) return;
        Set<String> i = new HashSet<>();
        Set<String> m = new HashSet<>();
        for (String s : list) {
            String t = s.trim();
            if (t.endsWith(":*")) m.add(t.substring(0, t.length() - 2));
            else if (!t.isEmpty()) i.add(t.contains(":") ? t : "minecraft:" + t);
        }
        ids = i;
        mods = m;
        source = list;
    }

    public boolean contains(List<? extends String> list, ResourceLocation id) {
        refresh(list);
        return id != null && (ids.contains(id.toString()) || mods.contains(id.getNamespace()));
    }

    public boolean contains(List<? extends String> list, Item item) {
        return contains(list, BuiltInRegistries.ITEM.getKey(item));
    }

    public boolean contains(List<? extends String> list, ItemStack stack) {
        return !stack.isEmpty() && contains(list, stack.getItem());
    }

    public boolean contains(List<? extends String> list, EntityType<?> type) {
        return contains(list, BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }
}
