package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.core.Mods;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Create machines as kill sources. Damage from any Create machine (saws, drills, crushing wheels, fans, rollers,
 * deployers, ...) counts as a player kill, so mobs drop XP and player-only loot. Their drops are normal item
 * entities, so funnels and belts pick them up. The XP drops as Experience Nuggets (3 XP each), which Create can move.
 * Only does anything when Create is installed.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
// unused: handlers are called by the event bus. resource: Level is AutoCloseable, but the server owns and closes it.
@SuppressWarnings({"unused", "resource"})
public final class CreateFarming {

    private static final ResourceLocation NUGGET = ResourceLocation.fromNamespaceAndPath("create", "experience_nugget");
    private static final int XP_PER_NUGGET = 3;

    private CreateFarming() {}

    private static boolean off() {
        return !Mods.create() || !Features.on(Feature.CREATE_FARMING);
    }

    /** Damage from a Create machine: a Create damage type, or a Create deployer's fake player. */
    static boolean fromCreate(DamageSource source) {
        if (source == null) return false;
        if (source.typeHolder().unwrapKey().map(k -> "create".equals(k.location().getNamespace())).orElse(false)) return true;
        return source.getEntity() != null && source.getEntity().getClass().getName().startsWith("com.simibubi.create.");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingIncomingDamageEvent e) {
        if (e.isCanceled() || !(e.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        if (off() || !Settings.CREATE_PLAYER_KILLS.get() || !fromCreate(e.getSource())) return;
        mob.setLastHurtByPlayer(FakePlayerFactory.getMinecraft(level));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onXp(LivingExperienceDropEvent e) {
        if (e.isCanceled() || !(e.getEntity() instanceof Mob mob) || e.getDroppedExperience() <= 0) return;
        if (off() || !Settings.CREATE_XP_NUGGETS.get() || !fromCreate(mob.getLastDamageSource())) return;
        Item nugget = BuiltInRegistries.ITEM.getOptional(NUGGET).orElse(null);
        if (nugget == null) return;
        int xp = e.getDroppedExperience();
        int nuggets = xp / XP_PER_NUGGET;
        // The leftover XP becomes one more nugget by chance, so on average nothing is lost.
        if (mob.getRandom().nextInt(XP_PER_NUGGET) < xp % XP_PER_NUGGET) nuggets++;
        e.setDroppedExperience(0);
        while (nuggets > 0) {
            int n = Math.min(nuggets, 64);
            mob.spawnAtLocation(new ItemStack(nugget, n));
            nuggets -= n;
        }
    }
}
