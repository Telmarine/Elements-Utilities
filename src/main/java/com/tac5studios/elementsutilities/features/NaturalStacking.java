package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.core.Mods;
import com.tac5studios.elementsutilities.util.IdList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Natural mob stacking. Every few seconds, identical idle mobs standing close together merge into one mob
 * with a count in its name (up to 12 by default). Bred, tamed, named, leashed and spawner mobs never stack.
 * Heavy mobs (see {@link MobRules}) only stack in player farms, up to the farm limit.
 *
 * <p>Taking one out of a stack: a lead or name tag pulls one out; Carry On (sneak + right-click with an empty hand)
 * picks up one; breeding food pulls out two to breed. Shearing a stack shears all of them.
 * Milking gives one bucket and a chicken stack lays one egg at a time, like a single mob.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class NaturalStacking {

    /** Set on babies born from breeding: they never stack. */
    private static final String BRED = ElementsUtilities.MOD_ID + ":bred";
    /** Most merges per level per check, so a big herd can't cause a spike. */
    private static final int MAX_MERGES = 256;

    private static final IdList NEVER = new IdList();

    private NaturalStacking() {}

    // ---------- merging ----------

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        StackCheck.runQueued(level);
        if (level.getGameTime() % (Settings.STACK_MERGE_SECONDS.get() * 20L) != 0) return;
        boolean merging = Features.on(Feature.NATURAL_MOB_STACKING);
        boolean splitting = StackCheck.active();
        if (!merging && !splitting) return;

        List<Mob> mobs = new ArrayList<>();
        List<Mob> stacks = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob)) continue;
            if (splitting && Stacks.count(mob) > 1) stacks.add(mob);
            if (merging && limit(mob) > 1) mobs.add(mob);
        }
        if (splitting) {
            for (Mob mob : stacks) StackCheck.check(mob);
            StackCheck.logSplits("merge check");
        }
        if (!merging) return;

        double r = Settings.STACK_RADIUS.get();
        int merges = 0;
        for (Mob mob : mobs) {
            if (merges >= MAX_MERGES) break;
            if (!mob.isAlive()) continue;
            int max = limit(mob);
            if (Stacks.count(mob) >= max) continue;
            for (Mob other : level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(r),
                    o -> o != mob && o.isAlive() && o.getType() == mob.getType())) {
                if (!same(mob, other)) continue;
                int cap = Math.min(max, limit(other));
                if (cap <= 1) continue;
                int room = cap - Stacks.count(mob);
                if (room <= 0) break;
                int theirs = Stacks.count(other);
                int moved = Math.min(room, theirs);
                Stacks.setCount(mob, Stacks.count(mob) + moved);
                if (moved == theirs) other.discard();
                else Stacks.setCount(other, theirs - moved);
                merges++;
            }
        }
    }

    /**
     * Most mobs this mob's stack may hold where it stands now: [mob_stacking] max_stack for normal mobs,
     * the farm limit for heavy mobs in a player farm, 0 when it may not stack at all.
     */
    static int limit(Mob mob) {
        if (SpawnerMobs.isSpawnerMob(mob) || mob instanceof AbstractVillager) return 0;
        boolean rules = Features.on(Feature.HEAVY_MOB_RULES);
        MobRules.Info info = rules ? MobRules.info(mob) : null;
        boolean heavy = info != null && info.group() == MobRules.Group.HEAVY;
        if (info != null && !heavy && info.heavyClass()) return 0; // never_stack or boss bar
        if (!heavy && !(mob instanceof Animal || mob instanceof Enemy || mob instanceof WaterAnimal)) return 0;
        if (!idle(mob, heavy)) return 0;
        if (!rules) return NEVER.contains(Settings.STACK_NEVER.get(), mob.getType()) ? 0 : Settings.STACK_MAX.get();
        if (!heavy) return Settings.STACK_MAX.get();
        if (mob instanceof IronGolem golem && golem.isPlayerCreated()) return 0;
        return MobRules.farmAllowed(mob) ? MobRules.farmCap(mob.getType()) : 0;
    }

    /** Not bred, named, leashed, owned, riding, in love, fighting or raiding. Heavy mobs may be persistent (Elder Guardians always are). */
    private static boolean idle(Mob mob, boolean heavy) {
        if (mob.getPersistentData().getBoolean(BRED)) return false;
        if (Stacks.playerNamed(mob) || mob.isLeashed()) return false;
        if (!heavy && mob.isPersistenceRequired()) return false;
        if (mob.isPassenger() || mob.isVehicle()) return false;
        // Separate ifs on purpose: one mob can match several of these types.
        //noinspection IfCanBeSwitch
        if (mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return false;
        if (mob instanceof Animal animal && animal.isInLove()) return false;
        if ((mob instanceof Enemy || heavy) && mob.getTarget() != null) return false; // only idle mobs
        return !(mob instanceof Raider raider && raider.hasActiveRaid());
    }

    /** Same look: baby or adult, sheep colour and fleece, and the variant for mobs that have one. */
    private static boolean same(Mob a, Mob b) {
        if (a.isBaby() != b.isBaby()) return false;
        //noinspection IfCanBeSwitch
        if (a instanceof Slime sa && b instanceof Slime sb && sa.getSize() != sb.getSize()) return false;
        if (a instanceof Sheep sa && b instanceof Sheep sb && (sa.getColor() != sb.getColor() || sa.isSheared() != sb.isSheared())) {
            return false;
        }
        if (a instanceof VariantHolder<?> va && b instanceof VariantHolder<?> vb) return Objects.equals(va.getVariant(), vb.getVariant());
        return true;
    }

    @SubscribeEvent
    public static void onBaby(BabyEntitySpawnEvent e) {
        if (e.getChild() != null) e.getChild().getPersistentData().putBoolean(BRED, true);
    }

    // ---------- taking mobs out of a stack ----------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.isCanceled() || e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(e.getTarget() instanceof Mob mob) || Stacks.count(mob) <= 1) return;
        Player player = e.getEntity();
        ItemStack held = player.getItemInHand(e.getHand());

        if (held.is(Items.LEAD) || held.is(Items.SADDLE) || (held.is(Items.NAME_TAG) && held.has(DataComponents.CUSTOM_NAME))) {
            Stacks.splitOff(mob, 1);
        } else if (held.isEmpty() && player.isShiftKeyDown() && Mods.carryOn()) {
            Stacks.splitOff(mob, 1);
        } else if (mob instanceof Animal animal && animal.isFood(held) && !animal.isBaby() && animal.canFallInLove()) {
            breedTwo(animal, player, held);
        } else if (mob instanceof Sheep sheep && held.getItem() instanceof ShearsItem && sheep.readyForShearing()) {
            shearRest(sheep);
        }
    }

    /** Feeding: one mob comes out and vanilla feeds it; a second one comes out and is fed too, if there's food for it. */
    private static void breedTwo(Animal animal, Player player, ItemStack food) {
        boolean second = player.getAbilities().instabuild || food.getCount() >= 2;
        Mob rest = Stacks.splitOff(animal, 1);
        if (!second || !(rest instanceof Animal partner)) return;
        if (Stacks.count(partner) > 1) Stacks.splitOff(partner, 1);
        partner.setInLove(player);
        if (!player.getAbilities().instabuild) food.shrink(1);
    }

    /** Shearing a stack: vanilla shears the mob itself; the rest of the stack drops its wool too. */
    private static void shearRest(Sheep sheep) {
        int extra = Stacks.count(sheep) - 1;
        Item wool = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(sheep.getColor().getName() + "_wool"));
        for (int i = 0; i < extra; i++) {
            sheep.spawnAtLocation(new ItemStack(wool, 1 + sheep.getRandom().nextInt(3)), 1);
        }
    }
}
