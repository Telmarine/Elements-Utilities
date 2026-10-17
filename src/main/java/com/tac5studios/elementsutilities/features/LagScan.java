package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.mixin.LevelTickersAccessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Counts entities, ticking block entities and hoppers per loaded chunk. Only runs on demand or for the alert check. */
public final class LagScan {

    /** One chunk's counts. */
    public static final class ChunkStat {
        public final ResourceKey<Level> dimension;
        public final int chunkX, chunkZ;
        public int entities, blockEntities, hoppers;
        final Object2IntOpenHashMap<String> types = new Object2IntOpenHashMap<>();

        ChunkStat(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        public int score() {
            return entities + blockEntities;
        }

        /** The most common entity type in the chunk, e.g. "minecraft:zombie x42", or "" when there are no entities. */
        public String topEntity() {
            String best = "";
            int most = 0;
            for (Object2IntMap.Entry<String> e : types.object2IntEntrySet()) {
                if (e.getIntValue() > most) {
                    most = e.getIntValue();
                    best = e.getKey();
                }
            }
            return most == 0 ? "" : best + " x" + most;
        }

        public String key() {
            return dimension.location() + "@" + chunkX + "," + chunkZ;
        }
    }

    private LagScan() {}

    /** Every loaded chunk with at least one entity or ticking block entity, busiest first. */
    public static List<ChunkStat> scan(MinecraftServer server) {
        List<ChunkStat> all = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) all.addAll(scan(level));
        all.sort(Comparator.comparingInt(ChunkStat::score).reversed());
        return all;
    }

    private static List<ChunkStat> scan(ServerLevel level) {
        Long2ObjectOpenHashMap<ChunkStat> chunks = new Long2ObjectOpenHashMap<>();
        for (Entity entity : level.getAllEntities()) {
            ChunkStat stat = stat(chunks, level, entity.getBlockX() >> 4, entity.getBlockZ() >> 4);
            stat.entities++;
            stat.types.addTo(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), 1);
        }
        for (TickingBlockEntity ticker : ((LevelTickersAccessor) level).utilities$getTickers()) {
            if (ticker.isRemoved()) continue;
            BlockPos pos = ticker.getPos();
            ChunkStat stat = stat(chunks, level, pos.getX() >> 4, pos.getZ() >> 4);
            stat.blockEntities++;
            if ("minecraft:hopper".equals(ticker.getType())) stat.hoppers++;
        }
        return new ArrayList<>(chunks.values());
    }

    private static ChunkStat stat(Long2ObjectOpenHashMap<ChunkStat> chunks, ServerLevel level, int x, int z) {
        return chunks.computeIfAbsent(ChunkPos.asLong(x, z), k -> new ChunkStat(level.dimension(), x, z));
    }
}
