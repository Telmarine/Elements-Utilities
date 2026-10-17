package com.tac5studios.elementsutilities.util;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.core.Mods;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Is a block on claimed land? Asks every installed claim mod by reflection, so none of them is needed to build or run.
 * A check that fails counts as claimed, so nothing is cleared by mistake.
 */
public final class Claims {

    private static final String ELEMENTS_API = "com.tac5studios.elementsclaims.api.ClaimsAPI";
    private static final String OPAC_API = "xaero.pac.common.server.api.OpenPACServerAPI";
    private static final String FTB_API = "dev.ftb.mods.ftbchunks.api.FTBChunksAPI";
    private static final String FTB_POS = "dev.ftb.mods.ftblibrary.math.ChunkDimPos";

    private static boolean elementsWarned, opacWarned, ftbWarned;

    private Claims() {}

    public static boolean isClaimed(Level level, BlockPos pos) {
        return (Mods.elementsClaims() && elements(level, pos))
                || (Mods.opac() && opac(level, pos))
                || (Mods.ftbChunks() && ftb(level, pos));
    }

    private static boolean elements(Level level, BlockPos pos) {
        try {
            return Boolean.TRUE.equals(Reflect.callStatic(ELEMENTS_API, "isClaimed", level, pos));
        } catch (RuntimeException e) {
            if (!elementsWarned) {
                elementsWarned = true;
                ElementsUtilities.LOGGER.warn("[Utilities] Elements: Claims check failed: {}", e.toString());
            }
            return true;
        }
    }

    private static boolean opac(Level level, BlockPos pos) {
        try {
            Object api = Reflect.callStatic(OPAC_API, "get", level.getServer());
            Object claims = Reflect.call(api, "getServerClaimsManager");
            return Reflect.call(claims, "get", level.dimension().location(), pos.getX() >> 4, pos.getZ() >> 4) != null;
        } catch (RuntimeException e) {
            if (!opacWarned) {
                opacWarned = true;
                ElementsUtilities.LOGGER.warn("[Utilities] Open Parties and Claims check failed: {}", e.toString());
            }
            return true;
        }
    }

    private static boolean ftb(Level level, BlockPos pos) {
        try {
            Object api = Reflect.callStatic(FTB_API, "api");
            if (!Boolean.TRUE.equals(Reflect.call(api, "isManagerLoaded"))) return true;
            Object manager = Reflect.call(api, "getManager");
            return Reflect.call(manager, "getChunk", Reflect.create(FTB_POS, level, pos)) != null;
        } catch (RuntimeException e) {
            if (!ftbWarned) {
                ftbWarned = true;
                ElementsUtilities.LOGGER.warn("[Utilities] FTB Chunks check failed: {}", e.toString());
            }
            return true;
        }
    }
}
