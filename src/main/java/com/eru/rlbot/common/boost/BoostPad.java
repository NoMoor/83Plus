package com.eru.rlbot.common.boost;


import com.eru.rlbot.common.vector.Vector3;

/**
 * Representation of one of the boost pads on the field.
 *
 * This class is here for your convenience, it is NOT part of the framework. You can change it as much
 * as you want, or delete it.
 */
public class BoostPad {

    private final Vector3 location;
    private final boolean isFullBoost;
    private boolean isActive;

    BoostPad(Vector3 location, boolean isFullBoost) {
        this.location = location;
        this.isFullBoost = isFullBoost;
    }

    void setActive(boolean active) {
        isActive = active;
    }

    public Vector3 getLocation() {
        return location;
    }

    /** True if this is a large boost pad. False if this is a small boost pad. */
    public boolean isLargeBoost() {
        return isFullBoost;
    }

    public boolean isActive() {
        return isActive;
    }
}
