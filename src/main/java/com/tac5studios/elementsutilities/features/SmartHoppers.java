package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;

/** Smart hoppers (see HopperBlockEntityMixin and HopperBlockMixin). */
public final class SmartHoppers {

    /** Added to every hopper block entity by the mixin. */
    public interface Sleepy {
        void utilities$wake();
    }

    private SmartHoppers() {}

    /** Ticks an idle hopper waits, or 0 when the feature is off. */
    public static int idleTicks() {
        return Features.on(Feature.SMART_HOPPERS) ? Settings.HOPPER_IDLE_TICKS.get() : 0;
    }
}
