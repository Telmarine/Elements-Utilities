package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.messages.Msg;
import com.tac5studios.elementsutilities.util.Text;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stacked mobs, shared by natural stacking and spawner mobs. A stack is one real mob that stands for several;
 * the count lives in the mob's saved data and shows in its name. Only that one mob is ever hurt. When it dies
 * it's a normal death (drops, XP, Butchery carcasses), and a fresh copy carrying the rest of the count takes its place.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
// unused: handlers are called by the event bus. resource: Level is AutoCloseable, but the server owns and closes it.
@SuppressWarnings({"unused", "resource"})
public final class Stacks {

    static final String COUNT = ElementsUtilities.MOD_ID + ":stack";
    /** Set when the custom name is the stack name, so it isn't mistaken for a player's name tag. */
    static final String OUR_NAME = ElementsUtilities.MOD_ID + ":stack_name";

    private static final Pattern CODE = Pattern.compile("&#[0-9A-Fa-f]{6}|&[0-9a-fk-orA-FK-OR]");

    /** Saved fields that must not carry over to a copy. */
    private static final String[] RESET = {
            "UUID", "Health", "DeathTime", "HurtTime", "HurtByTimestamp", "Fire", "Motion", "FallDistance", "active_effects",
            "AbsorptionAmount", "Passengers", "leash", "InLove", "LoveCause", "Brain", "Saddle"
    };

    private Stacks() {}

    public static int count(Entity e) {
        int n = e.getPersistentData().getInt(COUNT);
        return Math.max(1, n);
    }

    /** Sets the count and the name. A count of 1 removes the stack name. */
    public static void setCount(Mob mob, int count) {
        CompoundTag data = mob.getPersistentData();
        if (count <= 1) {
            data.remove(COUNT);
            if (data.getBoolean(OUR_NAME)) {
                data.remove(OUR_NAME);
                mob.setCustomName(null);
                mob.setCustomNameVisible(false);
            }
            return;
        }
        data.putInt(COUNT, count);
        data.putBoolean(OUR_NAME, true);
        mob.setCanPickUpLoot(false); // anything a stack picked up would be copied to every mob in it
        mob.setCustomName(name(mob, count));
        mob.setCustomNameVisible(Settings.STACK_NAME_VISIBLE.get());
    }

    /** True when the mob has a name a player gave it (not our stack name). */
    public static boolean playerNamed(Mob mob) {
        return mob.hasCustomName() && !mob.getPersistentData().getBoolean(OUR_NAME);
    }

    /** "12x Cow": the template from messages.toml with the mob's own (translated) name in place of {mob}. */
    private static Component name(Mob mob, int count) {
        String template = Msg.raw("stack.name", "count", count);
        int at = template.indexOf("{mob}");
        if (at < 0) return Text.color(template);
        String before = template.substring(0, at);
        MutableComponent out = Text.color(before);
        // The mob name takes the style that's active where {mob} stands.
        List<Component> probe = Text.color(lastColor(before) + "#").getSiblings();
        Style style = probe.isEmpty() ? Style.EMPTY : probe.getFirst().getStyle();
        out.append(Component.translatable(mob.getType().getDescriptionId()).setStyle(style));
        out.append(Text.color(template.substring(at + "{mob}".length())));
        return out;
    }

    /** The last & color code in a piece of text, so the mob name keeps the template's color. */
    private static String lastColor(String s) {
        Matcher m = CODE.matcher(s);
        StringBuilder last = new StringBuilder();
        while (m.find()) {
            String code = m.group();
            boolean color = code.startsWith("&#") || "0123456789abcdefABCDEFrR".indexOf(code.charAt(1)) >= 0;
            if (color) last.setLength(0);
            last.append(code);
        }
        return last.toString();
    }

    /**
     * A fresh copy of the mob (same type, variant, equipment and saved data, full health, new id) holding {@code count},
     * placed where the mob stands. Returns null if the copy couldn't be made.
     */
    public static Mob copy(Mob src, int count) {
        if (!(src.level() instanceof ServerLevel level)) return null;
        Entity made = src.getType().create(level);
        if (!(made instanceof Mob copy)) return null;
        CompoundTag tag = new CompoundTag();
        src.saveWithoutId(tag);
        for (String key : RESET) tag.remove(key);
        dropGuaranteedEquipment(tag);
        try {
            copy.load(tag);
        } catch (RuntimeException e) {
            ElementsUtilities.LOGGER.warn("[Utilities] Could not copy a stacked {}: {}", src.getType().getDescriptionId(), e.toString());
            return null;
        }
        copy.moveTo(src.getX(), src.getY(), src.getZ(), src.getYRot(), src.getXRot());
        copy.setHealth(copy.getMaxHealth());
        copy.getPersistentData().remove(OUR_NAME);
        if (src.getPersistentData().getBoolean(OUR_NAME)) copy.setCustomName(null);
        setCount(copy, count);
        if (!level.addFreshEntity(copy)) return null;
        return copy;
    }

    /**
     * Items a mob always drops (picked up or held) stay with the dying mob only: the copy gets an empty slot,
     * or every death in the stack would drop another one.
     */
    private static void dropGuaranteedEquipment(CompoundTag tag) {
        clearGuaranteed(tag, "HandItems", "HandDropChances");
        clearGuaranteed(tag, "ArmorItems", "ArmorDropChances");
        if (tag.contains("body_armor_drop_chance") && tag.getFloat("body_armor_drop_chance") > 1.0F) {
            tag.remove("body_armor_item");
        }
    }

    private static void clearGuaranteed(CompoundTag tag, String items, String chances) {
        ListTag list = tag.getList(items, Tag.TAG_COMPOUND);
        ListTag odds = tag.getList(chances, Tag.TAG_FLOAT);
        for (int i = 0; i < list.size() && i < odds.size(); i++) {
            if (odds.getFloat(i) > 1.0F) list.set(i, new CompoundTag());
        }
    }

    /**
     * Turns a stack into a single mob by moving the rest of the count to a copy next to it.
     * Used before a lead, name tag, Carry On or breeding is applied to the mob. Returns the copy, or null.
     */
    public static Mob splitOff(Mob mob, int keep) {
        int n = count(mob);
        if (n <= keep) return null;
        Mob rest = copy(mob, n - keep);
        if (rest == null) return null;
        setCount(mob, keep);
        return rest;
    }

    /** A converting mob (zombie to drowned, skeleton to stray, ...) keeps its stack count and spawner tag. */
    @SubscribeEvent
    public static void onConvert(LivingConversionEvent.Post e) {
        if (!(e.getEntity() instanceof Mob from) || !(e.getOutcome() instanceof Mob to)) return;
        CompoundTag src = from.getPersistentData();
        CompoundTag dst = to.getPersistentData();
        for (String key : new String[] {SpawnerMobs.SPAWNER_POS, SpawnerMobs.SPAWNER_DIM}) {
            Tag tag = src.get(key);
            if (tag != null) dst.put(key, tag.copy());
        }
        if (src.getBoolean(OUR_NAME)) {
            dst.putBoolean(OUR_NAME, true); // so setCount can replace the copied stack name
            setCount(to, count(from));
        }
        if (SpawnerMobs.isSpawnerMob(to)) SpawnerMobs.afterConvert(to);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent e) {
        if (e.isCanceled() || !(e.getEntity() instanceof Mob mob) || mob.level().isClientSide()) return;
        int n = count(mob);
        if (n <= 1) return;
        if (e.getSource().is(DamageTypes.GENERIC_KILL)) return; // /kill clears the whole stack
        copy(mob, n - 1);
    }
}
