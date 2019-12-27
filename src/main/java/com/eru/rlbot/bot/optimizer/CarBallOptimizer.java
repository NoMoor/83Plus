package com.eru.rlbot.bot.optimizer;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
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
    // The ball and target are close enough, just hit it toward the target.
    if (ball.position.distance(target) < 1000) {
      return makeCar(ball, target.minus(ball.position).normalize());
    }

    long startTime = System.nanoTime();
    Vector3 workingAngle = target.minus(ball.position).normalize();
    for (double nextStepSize : STEP_SIZES) {
      workingAngle = refineApproach(ball, target, workingAngle, nextStepSize);
    }
    logger.log(Level.DEBUG, "Optimization time: " + (System.nanoTime() - startTime));
    return makeCar(ball, workingAngle);
  }

  private static Vector3 refineApproach(BallData ball, Vector3 target, Vector3 previousAngle, double granularity) {
    // TODO: Find the best vertical angle as well...
    final Vector3 targetAngle = target.minus(ball.position).normalize();

    Vector3 nextAngle = previousAngle;

    BallData prevResult = CarBallCollision.calculateCollision(ball, makeCar(ball, previousAngle));
    double previousOffset = Angles.flatCorrectionAngle(prevResult.velocity, previousAngle);
    double nextOffset = previousOffset;

    Matrix3 rotationMatrix = Angles3.rotationMatrix(granularity);
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

      BallData nextResult = CarBallCollision.calculateCollision(ball, makeCar(ball, nextAngle));
      nextOffset = Angles.flatCorrectionAngle(nextResult.velocity, targetAngle);
    }

    return Math.abs(previousOffset) < Math.abs(nextOffset) ? previousAngle : nextAngle;
  }

  private static CarData makeCar(BallData ball, Vector3 noseOrientation) {
    // TODO: Fix these multiply by -1 issues...
    Vector3 sideDoor = noseOrientation.cross(Vector3.of(0, 0, noseOrientation.z)).normalize().multiply(-1);
    Vector3 roofOrientation = noseOrientation.cross(sideDoor).multiply(-1);
    Orientation carOrientation = Orientation.noseRoof(noseOrientation, roofOrientation);
    Vector3 carPosition = ball.position.minus(noseOrientation.toMagnitude(Constants.BALL_RADIUS + BoundingBox.frontToRj));

    return CarData.builder()
        .setOrientation(carOrientation)
        .setVelocity(noseOrientation.toMagnitude(AVERAGE_SPEED))
        .setPosition(carPosition)
        .setTime(ball.elapsedSeconds)
        .build();
  }

  private CarBallOptimizer() {
  }
}
