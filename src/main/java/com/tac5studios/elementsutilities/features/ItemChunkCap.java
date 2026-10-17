package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Keeps the number of dropped items per chunk under a limit. A chunk over the limit first has its
 * items merged (same item, anywhere in the chunk); if that isn't enough, the oldest items are cleared.
 * Loot bags, named items and items a player just dropped are never touched.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class ItemChunkCap {

    private static int ticks;

    private ItemChunkCap() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (++ticks < Settings.CAP_CHECK_SECONDS.get() * 20) return;
        ticks = 0;
        if (!Features.on(Feature.ITEM_CHUNK_CAP)) return;
        int max = Settings.CAP_MAX.get();
        for (ServerLevel level : e.getServer().getAllLevels()) check(level, max);
    }

    private static void check(ServerLevel level, int max) {
        Long2ObjectOpenHashMap<List<ItemEntity>> byChunk = new Long2ObjectOpenHashMap<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item && item.isAlive()) {
                long chunk = ChunkPos.asLong(item.getBlockX() >> 4, item.getBlockZ() >> 4);
                byChunk.computeIfAbsent(chunk, k -> new ArrayList<>()).add(item);
            }
        }
        for (List<ItemEntity> items : byChunk.values()) {
            if (items.size() > max) shrink(items, max);
        }
    }

    private static void shrink(List<ItemEntity> items, int max) {
        // Oldest first: they're merged into first and cleared first.
        items.sort(Comparator.comparingInt(ItemEntity::getAge).reversed());
        int alive = items.size();
        for (int i = 0; i < items.size() && alive > max; i++) {
            ItemEntity into = items.get(i);
            if (!into.isAlive()) continue;
            for (int j = i + 1; j < items.size() && alive > max; j++) {
                ItemEntity from = items.get(j);
                if (from.isAlive() && removable(from) && DroppedItems.mergeInto(into, from)) alive--;
            }
        }
        for (ItemEntity item : items) {
            if (alive <= max) break;
            if (item.isAlive() && removable(item)) {
                item.discard();
                alive--;
            }
        }
    }

    /** Items the cap may merge or clear: not set to never despawn, not a loot bag, not named, not just dropped. */
    private static boolean removable(ItemEntity item) {
        return item.getAge() != -32768
                && !DroppedItems.lootBag(item.getItem())
                && !DroppedItems.named(item.getItem())
                && !DroppedItems.justDropped(item, Settings.CAP_JUST_DROPPED.get());
    }
}
