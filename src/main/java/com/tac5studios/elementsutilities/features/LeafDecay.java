package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Faster leaf decay. Loose leaves (no log within 6 blocks, not placed by a player) get a decay time a few
 * seconds out; when it comes, the leaf gets the normal vanilla random tick, which drops its items and removes it.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class LeafDecay {

    /** Most leaves decayed per level per tick, so a huge tree can't cause a spike. */
    private static final int PER_TICK = 64;

    /** Per level: block position -> game time when it decays. */
    private static final Map<ServerLevel, Long2LongOpenHashMap> QUEUE = new WeakHashMap<>();

    private LeafDecay() {}

    /** Called from the leaves' scheduled tick (see LeavesBlockMixin). */
    public static void check(ServerLevel level, BlockPos pos) {
        if (!Features.on(Feature.LEAF_DECAY)) return;
        if (held(level.getBlockState(pos))) return;
        int min = Settings.LEAF_MIN_TICKS.get();
        int max = Math.max(min, Settings.LEAF_MAX_TICKS.get());
        long due = level.getGameTime() + min + level.random.nextInt(max - min + 1);
        QUEUE.computeIfAbsent(level, l -> new Long2LongOpenHashMap()).putIfAbsent(pos.asLong(), due);
    }

    /** Leaves that won't decay: player-placed, or still connected to a log. Anything else isn't leaves at all. */
    private static boolean held(BlockState state) {
        return !(state.getBlock() instanceof LeavesBlock)
                || state.getValue(LeavesBlock.PERSISTENT)
                || state.getValue(LeavesBlock.DISTANCE) < LeavesBlock.DECAY_DISTANCE;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        Long2LongOpenHashMap queue = QUEUE.get(level);
        if (queue == null || queue.isEmpty()) return;
        long now = level.getGameTime();
        int done = 0;
        ObjectIterator<Long2LongMap.Entry> it = queue.long2LongEntrySet().fastIterator();
        while (it.hasNext() && done < PER_TICK) {
            Long2LongMap.Entry entry = it.next();
            if (entry.getLongValue() > now) continue;
            // Read the key before remove(): the fast iterator reuses its entry object, which points elsewhere after a removal.
            BlockPos pos = BlockPos.of(entry.getLongKey());
            it.remove();
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (held(state)) continue;
            state.randomTick(level, pos, level.random);
            done++;
        }
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload e) {
        if (e.getLevel() instanceof ServerLevel level) QUEUE.remove(level);
    }
}
