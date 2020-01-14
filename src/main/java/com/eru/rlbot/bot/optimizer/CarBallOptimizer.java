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
import com.google.common.collect.Range;
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
    logger.log(Level.WARN, "Optimization time: " + (System.nanoTime() - startTime));
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
    Vector3 sideDoor = noseOrientation.cross(Vector3.of(0, 0, 1)).normalize();
    Vector3 roofOrientation = sideDoor.cross(noseOrientation);
    Orientation carOrientation = Orientation.noseRoof(noseOrientation, roofOrientation);
    Vector3 carPosition = ball.position.minus(noseOrientation.toMagnitude(Constants.BALL_RADIUS + BoundingBox.frontToRj));

    return CarData.builder()
        .setOrientation(carOrientation)
        .setVelocity(noseOrientation.toMagnitude(AVERAGE_SPEED))
        .setPosition(carPosition)
        .setTime(ball.elapsedSeconds)
        .build();
  }

  public static CarData getOptimalApproach(BallData ball, Vector3 target, CarData car) {
    // Optimize x offset.
    Range<Float> xOffsetRange = Range.closed(-BoundingBox.halfWidth - 20, BoundingBox.halfWidth + 20);
    float currXOffset = 0;
    float previousXStepSize = 5;

    while (previousXStepSize > X_PRECISION && xOffsetRange.contains(currXOffset)) {
      float prevXOffset = currXOffset;
      double score = getXGradient(ball, car, target, currXOffset);
      currXOffset -= X_EPSILON * score;
      previousXStepSize = Math.abs(currXOffset - prevXOffset);
    }

    currXOffset = Angles3.clip(currXOffset, xOffsetRange.lowerEndpoint(), xOffsetRange.upperEndpoint());

    car = adjustCarX(car, currXOffset);

    // Optimize Z offset.
    // Do not adjust through walls.
    float floorOffset = Constants.CAR_AT_REST - car.position.z;
    float ballBottomOffset = (ball.position.z - Constants.BALL_RADIUS) - car.position.z;
    float ceilingOffset = (Constants.FIELD_HEIGHT - BoundingBox.height) - car.position.z;
    float ballTopOffset = (ball.position.z + Constants.BALL_RADIUS) - car.position.z;
    Range<Float> zOffsetRange =
        Range.closed(Math.max(floorOffset, ballBottomOffset), Math.min(ceilingOffset, ballTopOffset));

    float currZOffset = 0;
    float previousZStepSize = 5;

    while (previousZStepSize > Z_PRECISION && zOffsetRange.contains(currZOffset)) {
      float prevZOffset = currZOffset;
      double score = getZGradient(ball, car, target, currZOffset);
      currZOffset -= Z_EPSILON * score;
      previousZStepSize = Math.abs(currZOffset - prevZOffset);
    }

    currZOffset = Angles3.clip(currZOffset, zOffsetRange.lowerEndpoint(), zOffsetRange.upperEndpoint());

    car = adjustCarZ(car, currZOffset);

    // Optimize y offset.
    Range<Double> aOffsetRange = Range.closed(-Math.PI, Math.PI);
    double currAOffset = 0;
    double previousAStepSize = 1;

    while (previousAStepSize > A_PRECISION && aOffsetRange.contains(currAOffset)) {
      double prevAOffset = currAOffset;
      double score = getAGradient(car, ball, target, currAOffset);
      currAOffset -= A_EPSILON * score;
      previousAStepSize = Math.abs(currAOffset - prevAOffset);
    }

    car = adjustCarA(car, ball, currAOffset);

    return car;
  }

  private static final double A_PRECISION = .025f;
  private static final double A_EPSILON = .05f;
  private static final double A_GAMMA = .1f;

  private static double getAGradient(CarData car, BallData ball, Vector3 target, double currAOffset) {
    BallData resultA = CarBallCollision.calculateCollision(ball, adjustCarA(car, ball, currAOffset));
    double aScore = score(resultA, target);

    BallData resultB = CarBallCollision.calculateCollision(ball, adjustCarA(car, ball, currAOffset + A_GAMMA));
    double bScore = score(resultB, target);

    return (bScore - aScore) / A_GAMMA;
  }

  private static final double X_PRECISION = .1; // How accurate to get x.
  private static final double X_EPSILON = 10; // Proportionate step size to take
  private static final double X_GAMMA = 10; // How small of an area to evaluate x gradient

  private static final double Z_PRECISION = .1; // How accurate to get Z.
  private static final double Z_EPSILON = 10; // Proportionate step size to take
  private static final double Z_GAMMA = 15; // How small of an area to evaluate Z gradient

  private static double getXGradient(BallData ball, CarData car, Vector3 target, float currXOffset) {
    BallData resultA = CarBallCollision.calculateCollision(ball, adjustCarX(car, currXOffset));
    double aScore = score(resultA, target);

    BallData resultB = CarBallCollision.calculateCollision(ball, adjustCarX(car, currXOffset + X_GAMMA));
    double bScore = score(resultB, target);

    return (bScore - aScore) / X_GAMMA;
  }

  private static double getZGradient(BallData ball, CarData car, Vector3 target, float currZOffset) {
    BallData resultA = CarBallCollision.calculateCollision(ball, adjustCarZ(car, currZOffset));
    double aScore = score(resultA, target);

    BallData resultB = CarBallCollision.calculateCollision(ball, adjustCarZ(car, currZOffset + Z_GAMMA));
    double bScore = score(resultB, target);

    return (aScore - bScore) / Z_GAMMA; // TODO: Why are these switched?
  }

  private static CarData adjustCarA(CarData car, BallData ball, double currAOffset) {
    Matrix3 rotate = Angles3.rotationMatrix(currAOffset);

    Orientation newCarOrientation =
        Orientation.fromOrientationMatrix(rotate.dot(car.orientation.getOrientationMatrix()));

    Vector3 carBall = ball.position.minus(car.position);
    Vector3 rotatedCarBall = rotate.dot(carBall);

    return car.toBuilder()
        .setOrientation(newCarOrientation)
        .setPosition(car.position.plus(carBall).minus(rotatedCarBall))
        .setVelocity(rotate.dot(car.velocity))
        .build();
  }

  private static CarData adjustCarX(CarData car, double dx) {
    Vector3 newPosition = car.position.plus(car.orientation.getLeftVector().toMagnitude(dx));

    return car.toBuilder()
        .setPosition(newPosition)
        .build();
  }

  private static CarData adjustCarZ(CarData car, double dy) {
    Vector3 newPosition = car.position.plus(car.orientation.getRoofVector().toMagnitude(dy));

    return car.toBuilder()
        .setPosition(newPosition)
        .build();
  }

  private static double score(BallData ballData, Vector3 target) {
    Vector3 ballTarget = target.minus(ballData.position);

    double flatAngleOffset = ballTarget.flatten().correctionAngle(ballData.velocity.flatten());

    double flatDistance = ballTarget.magnitude();
    double groundSpeed = ballData.velocity.flatten().norm();
    double timeToTarget = flatDistance / groundSpeed;

    double heightOffset = heightOffset(ballData.velocity.z, timeToTarget);
    double verticalAngleOffset = Math.atan((ballTarget.z - heightOffset) / ballTarget.magnitude());

    return Math.abs(flatAngleOffset) + Math.abs(verticalAngleOffset);
  }

  private static double heightOffset(double zVelocity, double time) {
    double position = 0;
    while (time > 0) {
      time -= Constants.STEP_SIZE;
      position += zVelocity;
      zVelocity -= Constants.GRAVITY * Constants.STEP_SIZE;
    }
    return position;
  }

  private CarBallOptimizer() {
  }
}
