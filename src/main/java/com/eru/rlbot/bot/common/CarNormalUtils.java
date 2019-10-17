package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

public class CarNormalUtils {

  private static final Vector3 NOSE_NORTH = new Vector3(0, 1, 0);

  // Context: https://stackoverflow.com/questions/14607640/rotating-a-vector-in-3d-space
  public static BallData noseNormalLocation(DataPacket input) {
    return new BallData(
        translateRelative(input.car.position, input.ball.position, input.car.orientation.noseVector),
        translateRelative(input.car.velocity, input.ball.velocity, input.car.orientation.noseVector));
  }

  private static Vector3 translateRelative(Vector3 source, Vector3 target, Vector3 referenceOrientation) {
    Vector3 relativeVector = target.minus(source);

    // Translate the vector relative to the reference orientation.
    // This always returns a positive number....
    double translationAngle = referenceOrientation.flatten().correctionAngle(NOSE_NORTH.flatten()); // Negate to get correction angle to north.

    double relativeX = (Math.cos(translationAngle) * relativeVector.x) - (Math.sin(translationAngle) * relativeVector.y);
    double relativeY = (Math.sin(translationAngle) * relativeVector.x) + (Math.cos(translationAngle) * relativeVector.y);
    double relativeZ = relativeVector.z;

    return new Vector3(relativeX, relativeY, relativeZ);

//    return new Vector3(
//        translationAngle > 0 ? relativeX : relativeY,
//        translationAngle > 0 ? relativeY : relativeX,
//        relativeZ);
  }
}
