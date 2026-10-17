package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.messages.Msg;
import com.tac5studios.elementsutilities.util.Claims;
import com.tac5studios.elementsutilities.util.IdList;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Clears dropped items with a chat countdown first. By default only when staff run /utilities cleanup;
 * with mode = "timer" it also runs every few minutes.
 * Claimed land, named items, loot bags, items a player just dropped and the keep list are left alone (each one a setting).
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class ItemCleanup {

    private static final IdList KEEP = new IdList();

    /** Seconds until the next sweep; -1 when none is counting down. */
    private static int secondsLeft = -1;
    /** True while a sweep started by a command is counting down. */
    private static boolean manual;
    private static int tick;

    private ItemCleanup() {}

    public static boolean timerMode() {
        return "timer".equals(Settings.CLEANUP_MODE.get());
    }

    public static boolean counting() {
        return secondsLeft >= 0;
    }

    /**
     * Starts a sweep by command. Counts down from the largest warning time, or clears at once when there are none.
     * Returns -1 if a sweep is already counting down, otherwise the seconds until it runs (0 = it already ran).
     */
    public static int start(MinecraftServer server) {
        if (counting()) return -1;
        int wait = Settings.CLEANUP_WARNINGS.get().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (wait <= 0) {
            run(server);
            return 0;
        }
        manual = true;
        secondsLeft = wait;
        broadcast(server, "cleanup.warning", "seconds", secondsLeft);
        return wait;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (++tick < 20) return;
        tick = 0;
        if (!Features.on(Feature.ITEM_CLEANUP) || (!manual && !timerMode())) {
            secondsLeft = -1;
            manual = false;
            return;
        }
        int interval = Settings.CLEANUP_MINUTES.get() * 60;
        if (!manual && (secondsLeft < 0 || secondsLeft > interval)) secondsLeft = interval;
        secondsLeft--;
        if (secondsLeft > 0) {
            if (Settings.CLEANUP_WARNINGS.get().contains(secondsLeft)) {
                broadcast(e.getServer(), "cleanup.warning", "seconds", secondsLeft);
            }
            return;
        }
        run(e.getServer());
    }

    /** Sweeps and announces it. In timer mode the next countdown starts from the full interval. */
    private static void run(MinecraftServer server) {
        int cleared = sweep(server);
        broadcast(server, "cleanup.done", "count", cleared);
        manual = false;
        secondsLeft = timerMode() ? Settings.CLEANUP_MINUTES.get() * 60 : -1;
    }

    /** Clears every dropped item that isn't protected. Returns how many item stacks were removed. */
    public static int sweep(MinecraftServer server) {
        int cleared = 0;
        boolean skipClaimed = Settings.CLEANUP_SKIP_CLAIMED.get();
        for (ServerLevel level : server.getAllLevels()) {
            Long2BooleanOpenHashMap claimed = new Long2BooleanOpenHashMap();
            List<ItemEntity> doomed = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ItemEntity item) || !item.isAlive() || protectedItem(item)) continue;
                if (skipClaimed) {
                    BlockPos pos = item.blockPosition();
                    long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
                    if (claimed.computeIfAbsent(chunk, k -> Claims.isClaimed(level, pos))) continue;
                }
                doomed.add(item);
            }
            for (ItemEntity item : doomed) item.discard();
            cleared += doomed.size();
        }
        return cleared;
    }

    private static boolean protectedItem(ItemEntity item) {
        ItemStack stack = item.getItem();
        if (stack.isEmpty()) return false;
        if (item.getAge() == -32768) return true; // display items other mods set to never despawn
        if (Settings.CLEANUP_SKIP_NAMED.get() && DroppedItems.named(stack)) return true;
        if (Settings.CLEANUP_SKIP_LOOT_BAGS.get() && DroppedItems.lootBag(stack)) return true;
        if (KEEP.contains(Settings.CLEANUP_KEEP.get(), stack)) return true;
        return DroppedItems.justDropped(item, Settings.CLEANUP_JUST_DROPPED.get());
    }

    private static void broadcast(MinecraftServer server, String key, Object... pairs) {
        if (!Msg.on(key)) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) Msg.send(p, key, pairs);
    }
}
