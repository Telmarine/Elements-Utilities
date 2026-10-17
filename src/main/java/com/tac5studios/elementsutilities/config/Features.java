package com.tac5studios.elementsutilities.config;

import com.tac5studios.elementsutilities.core.Mods;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Master control file: config/elements_utilities/features.toml
 * Every feature has its own switch. A feature that's off does nothing.
 * Switches can be changed while the server runs; /utilities reload re-checks the overlap guard.
 */
public final class Features {

    public static final ModConfigSpec SPEC;

    private static final Map<Feature, BooleanValue> SWITCHES = new EnumMap<>(Feature.class);

    public static final BooleanValue OVERLAP_GUARD;
    public static final ConfigValue<List<? extends String>> OVERLAP_FORCE_ON;
    public static final ConfigValue<List<? extends String>> OVERLAP_MODS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment(
                "Elements: Utilities master switches.",
                "Every feature has its own switch. Settings (numbers, lists) are in settings.toml."
        );

        String section = null;
        for (Feature f : Feature.values()) {
            if (!f.section.equals(section)) {
                if (section != null) b.pop();
                section = f.section;
                b.push(section);
            }
            SWITCHES.put(f, b.comment(f.comment).define(f.key, f.def));
        }
        if (section != null) b.pop();

        b.comment("Turns a feature off when another installed mod already does the same job.",
                "Mods are checked at server start and on /utilities reload.").push("overlap_guard");
        OVERLAP_GUARD = b.comment("Turn the overlap guard on or off.").define("enabled", true);
        OVERLAP_FORCE_ON = b.comment("Features to keep on even when an overlapping mod is installed, e.g. [\"xp_orbs\"].")
                .defineListAllowEmpty("force_on", List.of(), () -> "", o -> o instanceof String);
        OVERLAP_MODS = b.comment("Which mods turn which feature off, as \"feature=modid\".",
                        "Add a line when you find another mod that does the same job.")
                .defineListAllowEmpty("mods", defaultGuards(), () -> "", o -> o instanceof String s && s.contains("="));
        b.pop();

        SPEC = b.build();
    }

    private Features() {}

    private static List<String> defaultGuards() {
        List<String> out = new ArrayList<>();
        for (Feature f : Feature.values()) for (String mod : f.guardMods) out.add(f.key + "=" + mod);
        return out;
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "elements_utilities/features.toml");
    }

    /** The switch alone, ignoring the overlap guard. */
    public static boolean switchedOn(Feature f) {
        return SWITCHES.get(f).get();
    }

    /** True when the feature is switched on and not turned off by the overlap guard. */
    public static boolean on(Feature f) {
        return switchedOn(f) && !Mods.guarded(f);
    }

    public static boolean forcedOn(Feature f) {
        return OVERLAP_FORCE_ON.get().contains(f.key);
    }
}
