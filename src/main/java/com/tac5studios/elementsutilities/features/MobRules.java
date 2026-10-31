package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.util.Claims;
import com.tac5studios.elementsutilities.util.IdList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.neoforged.neoforge.common.Tags;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Which mobs may stack where (1.0.1).
 *  - NEVER: on never_stack. BOSS: has a boss bar while bosses_never_stack is on. Neither ever stacks outside spawners.
 *  - HEAVY: high base health, boss bar, #c:bosses or heavy_mobs. Stacks only in player farms (player-claimed land).
 *  - NORMAL: everything else, stacks everywhere by [mob_stacking].
 * Results are remembered per mob type until a setting they depend on changes or /utilities reload runs.
 */
public final class MobRules {

    public enum Group { NORMAL, HEAVY, BOSS, NEVER }

    public record Info(Group group, String reason, double health) {
        public boolean stacksNaturally() {
            return group == Group.NORMAL;
        }

        public boolean heavyClass() {
            return group != Group.NORMAL;
        }
    }

    private static final Map<EntityType<?>, Info> INFO = new HashMap<>();
    /** Mob class per type, seen on a live mob or made for detection. The boss bar check needs it. */
    private static final Map<EntityType<?>, Class<?>> CLASSES = new HashMap<>();
    private static final Map<Class<?>, Boolean> BOSS_BAR = new HashMap<>();

    private static final IdList NEVER = new IdList(), HEAVY = new IdList(), LIGHT = new IdList();
    private static final IdMap FARM_CAPS = new IdMap(), SPAWNER_CAPS = new IdMap(), FARM_TYPES = new IdMap();

    /** The settings the remembered results were worked out from. */
    private static Object[] basis = new Object[0];

    private MobRules() {}

    /** Forget every remembered result (on /utilities reload). */
    public static synchronized void clear() {
        INFO.clear();
        BOSS_BAR.clear();
        basis = new Object[0];
    }

    private static void checkBasis() {
        Object[] now = {Settings.STACK_NEVER.get(), Settings.HEAVY_MOBS.get(), Settings.LIGHT_MOBS.get(),
                Settings.HEAVY_HEALTH.get(), Settings.BOSSES_NEVER_STACK.get(), Settings.SKIP_BOSS_TAG.get()};
        if (now.length != basis.length) {
            INFO.clear();
            basis = now;
            return;
        }
        for (int i = 0; i < now.length; i++) {
            if (now[i] != basis[i] && !now[i].equals(basis[i])) {
                INFO.clear();
                basis = now;
                return;
            }
        }
    }

    // ---------- groups ----------

    public static synchronized Info info(Mob mob) {
        rememberClass(mob.getType(), mob.getClass());
        return info(mob.getType());
    }

    public static synchronized Info info(EntityType<?> type) {
        checkBasis();
        Info known = INFO.get(type);
        if (known != null) return known;
        Info made = work(type);
        INFO.put(type, made);
        return made;
    }

    /** The first class seen for a type; a result worked out before it was known (no boss bar check) is redone. */
    static synchronized void rememberClass(EntityType<?> type, Class<?> cls) {
        if (CLASSES.putIfAbsent(type, cls) == null) INFO.remove(type);
    }

    private static Info work(EntityType<?> type) {
        double health = baseHealth(type);
        Class<?> cls = CLASSES.get(type);
        boolean bar = cls != null && hasBossBar(cls);
        if (NEVER.contains(Settings.STACK_NEVER.get(), type)) return new Info(Group.NEVER, "never_stack", health);
        if (bar && Settings.BOSSES_NEVER_STACK.get()) return new Info(Group.BOSS, "boss bar", health);
        if (LIGHT.contains(Settings.LIGHT_MOBS.get(), type)) return new Info(Group.NORMAL, "light_mobs", health);
        if (HEAVY.contains(Settings.HEAVY_MOBS.get(), type)) return new Info(Group.HEAVY, "heavy_mobs", health);
        if (health >= Settings.HEAVY_HEALTH.get()) return new Info(Group.HEAVY, healthText(health) + " health", health);
        if (bar) return new Info(Group.HEAVY, "boss bar", health);
        if (Settings.SKIP_BOSS_TAG.get() && type.is(Tags.EntityTypes.BOSSES)) return new Info(Group.HEAVY, "boss tag", health);
        return new Info(Group.NORMAL, healthText(health) + " health", health);
    }

    static String healthText(double health) {
        return health == Math.rint(health) ? String.valueOf((long) health) : String.format(Locale.ROOT, "%.1f", health);
    }

    /** Base max health of a mob type, 0 when it has none. */
    @SuppressWarnings("unchecked")
    public static double baseHealth(EntityType<?> type) {
        try {
            if (!DefaultAttributes.hasSupplier(type)) return 0;
            return DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type).getBaseValue(Attributes.MAX_HEALTH);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** True when the class (or a parent below Mob) holds a boss bar. */
    private static boolean hasBossBar(Class<?> cls) {
        Boolean known = BOSS_BAR.get(cls);
        if (known != null) return known;
        boolean found = false;
        try {
            for (Class<?> c = cls; c != null && c != Mob.class && c != Object.class && !found; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (BossEvent.class.isAssignableFrom(f.getType())) {
                        found = true;
                        break;
                    }
                }
            }
        } catch (LinkageError | RuntimeException e) {
            ElementsUtilities.LOGGER.debug("[Utilities] Boss bar check failed for {}: {}", cls.getName(), e.toString());
        }
        BOSS_BAR.put(cls, found);
        return found;
    }

    // ---------- farms ----------

    /** True when a heavy mob may stack where it stands: farm stacking on, a player farm, an allowed spawn type. */
    public static boolean farmAllowed(Mob mob) {
        if (!Features.on(Feature.FARM_STACKING)) return false;
        MobSpawnType spawn = mob.getSpawnType();
        if (spawn == null || !spawnTypeAllowed(mob.getType(), spawn)) return false;
        if (!Claims.anyMod()) return "spawn_type".equals(Settings.NO_CLAIM_MOD.get());
        return Claims.isPlayerClaim(mob.level(), mob.blockPosition(), Settings.ADMIN_CLAIMS_COUNT.get());
    }

    private static boolean spawnTypeAllowed(EntityType<?> type, MobSpawnType spawn) {
        String own = FARM_TYPES.get(Settings.FARM_MOB_SPAWN_TYPES.get(), type);
        if (own != null) {
            for (String t : own.split(",")) if (t.trim().equalsIgnoreCase(spawn.name())) return true;
            return false;
        }
        for (String t : Settings.FARM_SPAWN_TYPES.get()) if (t.trim().equalsIgnoreCase(spawn.name())) return true;
        return false;
    }

    // ---------- limits ----------

    /** Most mobs in one farm stack of this type. */
    public static int farmCap(EntityType<?> type) {
        return FARM_CAPS.number(Settings.FARM_CAPS.get(), type, Settings.FARM_MAX.get());
    }

    /** Most mobs in one spawner mob stack of this mob. */
    public static int spawnerCap(Mob mob) {
        int max = Settings.SPAWNER_MOB_MAX.get();
        if (Features.on(Feature.HEAVY_MOB_RULES) && info(mob).heavyClass()) max = Settings.SPAWNER_HEAVY_CAP.get();
        return SPAWNER_CAPS.number(Settings.SPAWNER_CAPS.get(), mob.getType(), max);
    }

    /**
     * A config list of "id=value" lines (ids or "modid:*"). Rebuilt only when the list changes.
     * A full id wins over a whole mod.
     */
    static final class IdMap {
        private List<? extends String> source;
        private Map<String, String> ids = Map.of();
        private Map<String, String> mods = Map.of();
        private final Set<String> warned = new HashSet<>();

        synchronized String get(List<? extends String> list, EntityType<?> type) {
            if (list != source) {
                Map<String, String> i = new HashMap<>();
                Map<String, String> m = new HashMap<>();
                for (String line : list) {
                    int eq = line.indexOf('=');
                    if (eq <= 0) {
                        warn(line);
                        continue;
                    }
                    String id = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    if (id.endsWith(":*")) m.put(id.substring(0, id.length() - 2), value);
                    else i.put(id.contains(":") ? id : "minecraft:" + id, value);
                }
                ids = i;
                mods = m;
                source = list;
            }
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            String v = ids.get(key.toString());
            return v != null ? v : mods.get(key.getNamespace());
        }

        int number(List<? extends String> list, EntityType<?> type, int def) {
            String v = get(list, type);
            if (v == null) return def;
            try {
                return Math.max(1, Integer.parseInt(v));
            } catch (NumberFormatException e) {
                warn(BuiltInRegistries.ENTITY_TYPE.getKey(type) + "=" + v);
                return def;
            }
        }

        private void warn(String line) {
            if (warned.add(line)) ElementsUtilities.LOGGER.warn("[Utilities] settings.toml: can't read \"{}\" (use \"id=value\").", line);
        }
    }
}
