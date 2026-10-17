package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Players keep a percentage of their XP when they die. The XP dropped on death shrinks by the same share,
 * so dying never creates XP. Does nothing when the keepInventory game rule is on (vanilla keeps all XP then).
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
// unused: handlers are called by the event bus. resource: Level is AutoCloseable, but the server owns and closes it.
@SuppressWarnings({"unused", "resource"})
public final class KeepXp {

    private KeepXp() {}

    private static boolean off(Player player) {
        return !Features.on(Feature.KEEP_XP)
                || Settings.KEEP_XP_PERCENT.get() <= 0
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (!e.isWasDeath() || off(e.getOriginal())) return;
        long kept = totalPoints(e.getOriginal()) * Settings.KEEP_XP_PERCENT.get() / 100;
        if (kept > 0) e.getEntity().giveExperiencePoints((int) Math.min(kept, Integer.MAX_VALUE));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrop(LivingExperienceDropEvent e) {
        if (!(e.getEntity() instanceof Player player) || off(player)) return;
        e.setDroppedExperience(e.getDroppedExperience() * (100 - Settings.KEEP_XP_PERCENT.get()) / 100);
    }

    /** All XP points the player has: full levels plus progress into the current one. */
    private static long totalPoints(Player p) {
        long n = p.experienceLevel;
        long levels;
        if (n <= 16) levels = n * n + 6 * n;
        else if (n <= 31) levels = (5 * n * n - 81 * n + 720) / 2;
        else levels = (9 * n * n - 325 * n + 4440) / 2;
        return levels + Math.round(p.experienceProgress * p.getXpNeededForNextLevel());
    }
}
