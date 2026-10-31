package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lists every mob from other mods (any id outside minecraft:), grouped by mod, with its base health and the
 * stacking group it falls in now, in config/elements_utilities/detected_mobs.toml. Rewritten at start and on reload.
 * Changes nothing by itself: owners copy ids from it into settings.toml.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class MobDetection {

    private MobDetection() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        run(e.getServer());
    }

    public static void run(MinecraftServer server) {
        if (!Features.on(Feature.MOB_DETECTION)) return;
        ServerLevel level = server.overworld();
        Map<String, List<String>> byMod = new TreeMap<>();
        int mobs = 0, heavy = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id.getNamespace().equals("minecraft") || !DefaultAttributes.hasSupplier(type)) continue;
            if (!isMob(type, level)) continue;
            MobRules.Info info = MobRules.info(type);
            byMod.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>())
                    .add("\"" + id + "\" = \"" + label(info) + "\"");
            mobs++;
            if (info.heavyClass()) heavy++;
        }
        write(byMod);
        ElementsUtilities.LOGGER.info("[Utilities] Mob detection: {} mod(s) with mobs, {} mob(s), {} heavy, boss or never-stack. "
                + "See detected_mobs.toml.", byMod.size(), mobs, heavy);
    }

    /** Makes one (never added to the world) to learn its class, for the boss bar check. Non-mobs are left out. */
    private static boolean isMob(EntityType<?> type, ServerLevel level) {
        try {
            Entity made = type.create(level);
            if (made == null) return false;
            MobRules.rememberClass(type, made.getClass());
            boolean mob = made instanceof Mob;
            made.discard();
            return mob;
        } catch (RuntimeException | LinkageError e) {
            ElementsUtilities.LOGGER.debug("[Utilities] Mob detection skipped {}: {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), e.toString());
            return true; // still a living type; list it from what the registry says
        }
    }

    private static String label(MobRules.Info info) {
        String group = info.group().name().toLowerCase(java.util.Locale.ROOT);
        String health = MobRules.healthText(info.health()) + " health";
        String reason = info.reason().endsWith("health") ? info.reason() : info.reason() + ", " + health;
        return group + " (" + reason + ")";
    }

    private static void write(Map<String, List<String>> byMod) {
        StringBuilder out = new StringBuilder();
        out.append("# Elements: Utilities - mobs from other mods, found at server start and on /utilities reload.\n");
        out.append("# This file is rewritten each time, so edits here do nothing. Copy ids from it into settings.toml:\n");
        out.append("# light_mobs, heavy_mobs, never_stack, [farm_stacking] mob_caps / mob_spawn_types, [spawners] mob_caps.\n");
        out.append("# Groups: normal = stacks everywhere, heavy = player farms only, boss / never = never stacks.\n");
        byMod.forEach((mod, lines) -> {
            out.append("\n[").append(mod.matches("[A-Za-z0-9_-]+") ? mod : "\"" + mod + "\"").append("]\n");
            lines.forEach(l -> out.append(l).append('\n'));
        });
        Path path = FMLPaths.CONFIGDIR.get().resolve(ElementsUtilities.MOD_ID).resolve("detected_mobs.toml");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            ElementsUtilities.LOGGER.warn("[Utilities] Could not write {}: {}", path, e.toString());
        }
    }
}
