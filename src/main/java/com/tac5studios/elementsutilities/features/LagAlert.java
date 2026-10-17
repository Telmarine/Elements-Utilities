package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.messages.Msg;
import com.tac5studios.elementsutilities.perms.Perm;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Optional lag alert: one staff-only chat line when a chunk passes the entity or block entity threshold.
 * Each chunk has a long cooldown so it can't flood chat. Never goes to public chat or Discord.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class LagAlert {

    /** Chunk key -> server time (ms) of its last alert. */
    private static final Map<String, Long> LAST = new HashMap<>();
    private static int ticks;

    private LagAlert() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (++ticks < Settings.ALERT_CHECK_SECONDS.get() * 20) return;
        ticks = 0;
        if (!Features.on(Feature.LAG_ALERT)) return;
        MinecraftServer server = e.getServer();
        long now = System.currentTimeMillis();
        long cooldown = Settings.ALERT_COOLDOWN_MINUTES.get() * 60_000L;
        LAST.values().removeIf(t -> now - t > cooldown);
        for (LagScan.ChunkStat stat : LagScan.scan(server)) {
            if (stat.entities <= Settings.ALERT_ENTITIES.get() && stat.blockEntities <= Settings.ALERT_BLOCK_ENTITIES.get()) continue;
            if (LAST.containsKey(stat.key())) continue;
            LAST.put(stat.key(), now);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!Perm.has(p, Perm.LAG_ALERT)) continue;
                Msg.send(p, "lag.alert",
                        "dim", stat.dimension.location(),
                        "x", stat.chunkX * 16 + 8, "z", stat.chunkZ * 16 + 8,
                        "entities", stat.entities, "block_entities", stat.blockEntities);
            }
        }
    }
}
