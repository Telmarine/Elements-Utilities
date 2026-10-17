package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Redstone clock limiter. Counts updates from redstone components (anything that gives off a signal) per chunk per second.
 * Over the limit, the rest of that second's updates in the chunk are ignored ("slow"),
 * or the component that went over the limit is broken and dropped ("break").
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class RedstoneLimiter {

    private static final Map<ServerLevel, Counter> COUNTERS = new WeakHashMap<>();

    private static final class Counter {
        long second = -1;
        final Long2IntOpenHashMap perChunk = new Long2IntOpenHashMap();
        /** Components to break at the end of the tick (breaking them in the middle of an update is unsafe). */
        final Set<BlockPos> toBreak = new HashSet<>();
    }

    private RedstoneLimiter() {}

    @SubscribeEvent
    public static void onNotify(BlockEvent.NeighborNotifyEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        BlockState state = e.getState();
        if (!state.isSignalSource() || !Features.on(Feature.REDSTONE_LIMITER)) return;

        Counter counter = COUNTERS.computeIfAbsent(level, l -> new Counter());
        long second = level.getGameTime() / 20;
        if (second != counter.second) {
            counter.second = second;
            counter.perChunk.clear();
        }
        BlockPos pos = e.getPos();
        long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        int count = counter.perChunk.addTo(chunk, 1) + 1;
        if (count <= Settings.REDSTONE_MAX.get()) return;

        if ("break".equals(Settings.REDSTONE_ACTION.get())) {
            if (count == Settings.REDSTONE_MAX.get() + 1) counter.toBreak.add(pos.immutable());
        }
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        Counter counter = COUNTERS.get(level);
        if (counter == null || counter.toBreak.isEmpty()) return;
        for (BlockPos pos : counter.toBreak) {
            if (!level.getBlockState(pos).isSignalSource()) continue;
            ElementsUtilities.LOGGER.info("[Utilities] Redstone limiter broke a fast clock at {} in {}.", pos.toShortString(),
                    level.dimension().location());
            level.destroyBlock(pos, true);
        }
        counter.toBreak.clear();
    }
}
