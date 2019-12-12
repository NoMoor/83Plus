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
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CarBallOptimizer {

  private static final Logger logger = LogManager.getLogger();

  private static final float AVERAGE_SPEED = 1200;

  private static final ImmutableList<Double> STEP_SIZES = ImmutableList.of(.25, .05, .01);
  public static CarData getOptimalApproach(BallData ball, Vector3 target) {
    long startTime = System.nanoTime();
    Vector3 workingAngle = target.minus(ball.position).normalized();
    for (double nextStepSize : STEP_SIZES) {
      workingAngle = refineApproach(ball, target, workingAngle, nextStepSize);
    }
    logger.log(Level.WARN, "Optimization time: " + (System.nanoTime() - startTime));
    return makeCar(ball.position, workingAngle);
  }

  private static Vector3 refineApproach(BallData ball, Vector3 target, Vector3 previousAngle, double granularity) {
    // TODO: Find the best vertical angle as well...
    final Vector3 targetAngle = target.minus(ball.position).normalized();

    Vector3 nextAngle = previousAngle;

    BallData prevResult = CarBallCollision.calculateCollision(ball, makeCar(ball.position, previousAngle));
    double previousOffset = Angles.flatCorrectionAngle(prevResult.velocity, previousAngle);
    double nextOffset = previousOffset;

    Matrix3 rotationMatrix = rotationMatrix(granularity);
    Matrix3 rotationMatrixInverse = rotationMatrix.inverse();

    while (Math.signum(previousOffset) == Math.signum(nextOffset)) {
      // Advance the previous values forward one step.
      previousOffset = nextOffset;
      previousAngle = nextAngle;

      // Move the next values forward one step.
      if (nextOffset < 0) {
        nextAngle = rotationMatrix.dot(nextAngle);
      } else {
        nextAngle = rotationMatrixInverse.dot(nextAngle);
      }

      BallData nextResult = CarBallCollision.calculateCollision(ball, makeCar(ball.position, nextAngle));
      nextOffset = Angles.flatCorrectionAngle(nextResult.velocity, targetAngle);
    }

    return Math.abs(previousOffset) < Math.abs(nextOffset) ? previousAngle : nextAngle;
  }

  private static Matrix3 rotationMatrix(double radians) {
    return Matrix3.of(
        Vector3.of(Math.cos(radians), Math.sin(radians), 0),
        Vector3.of(-Math.sin(radians), Math.cos(radians), 0),
        Vector3.of(0, 0, 1));
  }

  private static CarData makeCar(Vector3 position, Vector3 noseOrientation) {
    // TODO: Fix these multiply by -1 issues...
    Vector3 sideDoor = noseOrientation.cross(Vector3.of(0, 0, noseOrientation.z)).normalized().multiply(-1);
    Vector3 roofOrientation = noseOrientation.cross(sideDoor).multiply(-1);
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
