package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;

/** Natural hostile mob cap (see SpawnStateMixin). */
public final class HostileCap {

    private HostileCap() {}

    /** The monster cap per 289 chunks: the configured one when the feature is on, vanilla's otherwise. */
    public static int cap(int vanilla) {
        return Features.on(Feature.HOSTILE_CAP) ? Settings.HOSTILE_MAX.get() : vanilla;
    }
}
