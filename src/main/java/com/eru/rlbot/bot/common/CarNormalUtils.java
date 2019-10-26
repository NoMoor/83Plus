package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

public class CarNormalUtils {

  private static final Vector3 NOSE_NORTH = Vector3.of(0, 1, 0);

  private static DataPacket cacheKey;
  private static BallData cacheValue;

  /** Returns the ball position / velocity relative to the car position. */
  // Context: https://stackoverflow.com/questions/14607640/rotating-a-vector-in-3d-space
  public static BallData noseNormalLocation(DataPacket input) {
    if (input == cacheKey) {
      return cacheValue;
    } else {
      cacheKey = input;
      cacheValue = new BallData(
          translateRelative(input.car.position, input.ball.position, input.car.orientation.getNoseVector()),
          translateRelative(input.car.velocity, input.ball.velocity, input.car.orientation.getNoseVector()));
    }

    return cacheValue;
  }

  private static Vector3 translateRelative(Vector3 source, Vector3 target, Vector3 referenceOrientation) {
    Vector3 relativeVector = target.minus(source);

    // Translate the vector relative to the reference orientation.
    // This always returns a positive number....
    double translationAngle = referenceOrientation.flatten().correctionAngle(NOSE_NORTH.flatten()); // Negate to row correction angle to north.

    double relativeX = (Math.cos(translationAngle) * relativeVector.x) - (Math.sin(translationAngle) * relativeVector.y);
    double relativeY = (Math.sin(translationAngle) * relativeVector.x) + (Math.cos(translationAngle) * relativeVector.y);
    double relativeZ = relativeVector.z;

    return Vector3.of(relativeX, relativeY, relativeZ);
  }
}
