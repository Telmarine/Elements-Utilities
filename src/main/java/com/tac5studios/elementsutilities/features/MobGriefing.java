package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.util.IdList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;

/**
 * Per-mob block griefing. Mobs on the no_griefing list can't change blocks (creeper craters, endermen
 * picking up blocks, ghast fireball fires, and so on); everything else follows the mobGriefing game rule.
 * A projectile (e.g. a ghast fireball) counts as the mob that fired it.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class MobGriefing {

    private static final IdList BLOCKED = new IdList();

    private MobGriefing() {}

    @SubscribeEvent
    public static void onGrief(EntityMobGriefingEvent e) {
        Entity entity = e.getEntity();
        if (!e.canGrief() || !Features.on(Feature.MOB_GRIEFING)) return;
        if (blocked(entity) || (entity instanceof Projectile p && p.getOwner() != null && blocked(p.getOwner()))) {
            e.setCanGrief(false);
        }
    }

    private static boolean blocked(Entity entity) {
        return BLOCKED.contains(Settings.NO_GRIEFING.get(), entity.getType());
    }
}
