package com.tac5studios.elementsutilities.perms;

import com.tac5studios.elementsutilities.ElementsUtilities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Permission nodes, named "utilities.<path>". They go through the NeoForge PermissionAPI, so they work
 * with Elements: Nexus ranks, any other permission mod, or plain OP levels.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class Perm {

    public enum Who {
        PLAYER(0), STAFF(2), ADMIN(3);

        final int opLevel;

        Who(int opLevel) {
            this.opLevel = opLevel;
        }
    }

    private record Entry(PermissionNode<Boolean> node, Who who) {}

    private static final Map<String, Entry> NODES = new LinkedHashMap<>();

    /** /utilities lag */
    public static final String LAG = node("lag", Who.STAFF);
    /** Receive lag alerts. */
    public static final String LAG_ALERT = node("lag.alert", Who.STAFF);
    /** /utilities cleanup */
    public static final String CLEANUP = node("cleanup", Who.STAFF);
    /** /utilities reload */
    public static final String RELOAD = node("reload", Who.ADMIN);

    private Perm() {}

    private static String node(String path, Who who) {
        PermissionNode<Boolean> n = new PermissionNode<>("utilities", path, PermissionTypes.BOOLEAN,
                (player, uuid, ctx) -> player != null && player.hasPermissions(who.opLevel));
        n.setInformation(Component.literal("utilities." + path), Component.literal("Default: " + switch (who) {
            case PLAYER -> "everyone";
            case STAFF -> "OP level 2+";
            case ADMIN -> "OP level 3+";
        }));
        NODES.put(n.getNodeName(), new Entry(n, who));
        return n.getNodeName();
    }

    @SubscribeEvent
    @SuppressWarnings("unused") // called by the event bus
    public static void onGatherNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(NODES.values().stream().map(Entry::node).toArray(PermissionNode<?>[]::new));
        ElementsUtilities.LOGGER.info("[Utilities] {} permission nodes registered.", NODES.size());
    }

    public static boolean has(CommandSourceStack source, String node) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            Entry e = NODES.get(node);
            Who who = e == null ? Who.ADMIN : e.who();
            return source.hasPermission(Math.max(who.opLevel, 2));
        }
        return has(player, node);
    }

    public static boolean has(ServerPlayer player, String node) {
        Entry e = NODES.get(node);
        if (e == null) return false;
        try {
            return PermissionAPI.getPermission(player, e.node());
        } catch (RuntimeException ex) {
            ElementsUtilities.LOGGER.warn("[Utilities] Permission check failed for {}: {}", node, ex.toString());
            return player.hasPermissions(e.who().opLevel);
        }
    }
}
