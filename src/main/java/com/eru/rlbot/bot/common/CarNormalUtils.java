package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

public class CarNormalUtils {

  // Context: https://stackoverflow.com/questions/14607640/rotating-a-vector-in-3d-space
  public static BallData noseNormalLocation(DataPacket input) {
    return new BallData(
        translateRelative(input.car.position, input.ball.position, input.car.orientation.noseVector),
        translateRelative(input.car.velocity, input.ball.velocity, input.car.orientation.noseVector));
  }

  private static Vector3 translateRelative(Vector3 source, Vector3 target, Vector3 referenceOrientation) {
    Vector3 relativeVector = target.minus(source);

    // Translate the vector relative to the reference orientation.
    double translationAngle = referenceOrientation.angle(noseNorth(source));
    double relativeX = (Math.cos(translationAngle) * relativeVector.x) - (Math.sin(translationAngle) * relativeVector.y);
    double relativeY = (Math.sin(translationAngle) * relativeVector.x) + (Math.cos(translationAngle) * relativeVector.y);
    double relativeZ = relativeVector.z;

    return new Vector3(relativeX, relativeY, relativeZ);
  }

  private static Vector3 noseNorth(Vector3 position) {
    // 100 is arbitrary to point 'up the positive' axis.
    return new Vector3(position.x, position.y + 10000, position.z);
  }
}
