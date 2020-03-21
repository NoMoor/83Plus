package com.eru.rlbot.bot.tactics.kickoff;

import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

/**
 * Kickoff tactic info.
 */
public class KickoffTactic {

  public final Vector3 target;
  public final Type type;
  public final KickoffLocations.KickoffLocation location;

  public KickoffTactic(KickoffLocations.KickoffLocation location, Vector3 target, Type type) {
    this.location = location;
    this.target = target;
    this.type = type;
  }

  public static KickoffTactic defaultTactic(DataPacket input) {
    return create(KickoffLocations.defaultLocation(input.car.team), input.ball.position, Type.CHALLENGE);
  }

  public String getDescriptor() {
    return String.format("%s %s", location, type);
  }

  public enum Type {
    PUSH,
    HOOK,
    SHOOT,
    FAKE,
    CHALLENGE,
    GRAB_BOOST
  }

  public static KickoffTactic create(KickoffLocations.KickoffLocation location, Vector3 target, Type type) {
    return new KickoffTactic(location, target, type);
  }
}
