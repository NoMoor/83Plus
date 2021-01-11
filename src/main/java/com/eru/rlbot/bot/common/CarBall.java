package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.Numbers;
import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

/**
 * Utilities for finding points and vectors between the car and the ball.
 */
public final class CarBall {

  /**
   * Returns the nearest point on the car hitbox by drawing a line from the hitbox center the car and then clamping
   * that vector to the hitbox. Note that the closest point is not necessarily on the hitbox.center -> ball.center
   * vector.
   *
   * Credit:
   *   https://samuelpmish.github.io/notes/RocketLeague/car_ball_interaction/
   */
  public static Vector3 nearestPointOnHitBox(Vector3 ballPosition, CarData car) {
    BoundingBox hitBox = car.boundingBox;

    Vector3 ballToBBCenter = ballPosition.minus(hitBox.center);
    Vector3 boundingBoxRelativeBall = ballToBBCenter.dot(car.orientation.getOrientationMatrix());

    Vector3 nearestPointLocal = Vector3.of(
        Numbers.clamp(boundingBoxRelativeBall.x, -BoundingBox.halfLength, BoundingBox.halfLength),
        Numbers.clamp(boundingBoxRelativeBall.y, -BoundingBox.halfWidth, BoundingBox.halfWidth),
        Numbers.clamp(boundingBoxRelativeBall.z, -BoundingBox.halfHeight, BoundingBox.halfHeight)
    );

    return car.orientation.getOrientationMatrix().dot(nearestPointLocal).plus(hitBox.center);
  }

  public static boolean ballIsUpfield(DataPacket input) {
    float teamSide = Math.signum(Goal.ownGoal(input.car.team).center.y);
    return teamSide > 0
        ? input.ball.position.y < input.car.position.y // Orange
        : input.ball.position.y > input.car.position.y; // Blue
  }

  private CarBall() {
  }
}
