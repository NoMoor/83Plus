package com.eru.rlbot.bot.optimizer;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Matrix3;
import com.eru.rlbot.bot.prediction.CarBallCollision;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.BoundingBox;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector3;

public class CarBallOptimizer {

  private static final float AVERAGE_SPEED = 1800;

  // .1 Radian rotation
  private static final double ROTATION_STEP = .01d;
  private static final Matrix3 ROTATION_TRANSFORM = Matrix3.of(
      Vector3.of(Math.cos(ROTATION_STEP), Math.sin(ROTATION_STEP), 0),
      Vector3.of(-Math.sin(ROTATION_STEP), Math.cos(ROTATION_STEP), 0),
      Vector3.of(0, 0, 1));
  private static final Matrix3 ANTI_ROTATION_TRANSFORM = ROTATION_TRANSFORM.inverse();

  public static CarData getOptimalApproach(BallData ball, Vector3 target) {
    // Find a car contact that goes through the given target.

    // TODO: Find the best vertical angle as well...
    final Vector3 targetAngle = target.minus(ball.position).normalized();
    final CarData firstCar = makeCar(ball.position, targetAngle);
    final BallData firstResult = CarBallCollision.calculateCollision(ball, firstCar);
    final double firstOffset = Angles.flatCorrectionAngle(firstResult.velocity, targetAngle);

    // Copy over to variables that will be used for searching.
    Vector3 nextAngle = targetAngle;
    CarData nextCar = firstCar;
    BallData nextResult = firstResult;
    double nextOffset = firstOffset;

    // TODO: Reduce the number of operations here.
    while (Math.signum(firstOffset) != Math.signum(nextOffset)) {
      if (nextOffset < 0) {
        nextAngle = ROTATION_TRANSFORM.dot(nextAngle);
      } else {
        nextAngle = ANTI_ROTATION_TRANSFORM.dot(nextAngle);
      }

      nextCar = makeCar(ball.position, targetAngle);
      nextResult = CarBallCollision.calculateCollision(ball, firstCar);
      nextOffset = Angles.flatCorrectionAngle(firstResult.velocity, targetAngle);
    }

    return nextCar;
  }

  private static CarData makeCar(Vector3 position, Vector3 noseOrientation) {
    Vector3 sideDoor = noseOrientation.cross(Vector3.of(0, 0, noseOrientation.z)).normalized();
    Vector3 roofOrientation = noseOrientation.cross(sideDoor);
    Orientation carOrientation = Orientation.noseRoof(noseOrientation, roofOrientation);
    Vector3 carPosition = position.minus(noseOrientation.toMagnitude(Constants.BALL_RADIUS + BoundingBox.frontToRj));

    return CarData.builder()
        .setOrientation(carOrientation)
        .setVelocity(noseOrientation.toMagnitude(AVERAGE_SPEED))
        .setPosition(carPosition)
        .build();
  }

  private CarBallOptimizer() {
  }
}
