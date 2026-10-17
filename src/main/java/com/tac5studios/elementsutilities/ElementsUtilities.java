package com.tac5studios.elementsutilities;

import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Elements: Utilities — server performance and the small tweaks server management mods don't cover.
 * Dedicated servers only. Players install nothing.
 *
 * <p>Everything else starts from server events (each class subscribes itself):
 *   messages.Msg        read messages.toml
 *   core.Mods           check installed mods and set the overlap guard
 *   perms.Perm          register permission nodes
 *   commands.Commands   /utilities
 *   features.*          one class per feature, each checks its own switch
 */
@Mod(value = ElementsUtilities.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class ElementsUtilities {

    public static final String MOD_ID = "elements_utilities";
    public static final Logger LOGGER = LoggerFactory.getLogger("ElementsUtilities");

    public ElementsUtilities(ModContainer container) {
        // config/elements_utilities/features.toml, settings.toml
        Features.register(container);
        Settings.register(container);
        LOGGER.info("[Utilities] Elements: Utilities loading.");
    }
}
