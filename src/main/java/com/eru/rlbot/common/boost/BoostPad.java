package com.eru.rlbot.common.boost;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.vector.Vector3;

/**
 * Representation of one of the boost pads on the field.
 */
public class BoostPad {

    private final Vector3 location;
    private final boolean isFullBoost;
    private boolean isActive;

    BoostPad(Vector3 location, boolean isFullBoost) {
      this.location = location.setZ(Constants.CAR_AT_REST);
      this.isFullBoost = isFullBoost;
    }

    void setActive(boolean active) {
        isActive = active;
    }

  /** The location of the boost pad. */
  public Vector3 getLocation() {
    return location;
  }

    /** True if this is a large boost pad. False if this is a small boost pad. */
    public boolean isLargeBoost() {
        return isFullBoost;
    }

  /** True if this is collectable. False if not. */
  public boolean isActive() {
    return isActive;
  }
}
