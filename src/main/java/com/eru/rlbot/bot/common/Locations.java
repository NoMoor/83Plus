package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;

// TODO: Clean up this class.

/**
 * Static utilities for deriving different logical locations.
 */
public final class Locations {

  public static Vector3 toInsideLeftGoal(CarData car, Vector3 target) {
    return Vector3.from(target, Goal.opponentGoal(car.team).leftInside);
  }

  public static Vector3 toInsideRightGoal(CarData car, Vector3 target) {
    return Vector3.from(target, Goal.opponentGoal(car.team).rightInside);
  }

  private static Vector3 toOutsideLeftGoal(CarData car, Vector3 target) {
    return Vector3.from(target, Goal.ownGoal(car.team).leftWide);
  }

  private static Vector3 toOutsideRightGoal(CarData car, Vector3 position) {
    return Vector3.from(position, Goal.ownGoal(car.team).rightWide);
  }

  public static Vector3 ballToOppGoalCenter(DataPacket input) {
    return Vector3.from(input.ball.position, Goal.opponentGoal(input.car.team).center);
  }

  public static Vector3 carToBall(DataPacket input) {
    return Vector3.from(input.car.position, input.ball.position);
  }

  public static boolean isOpponentSideOfBall(DataPacket input) {
    double correctionAngle = minCarTargetGoalCorrection(input, Moment.from(input.ball));

    boolean carFacingOwnGoal = Math.abs(correctionAngle) > Math.PI * .5;

    return carFacingOwnGoal && ballIsInFrontOfCar(input);
  }

  public static boolean ballIsInFrontOfCar(DataPacket input) {
    BallData relativeBallLocation = RelativeUtils.noseRelativeBall(input);

    return relativeBallLocation.position.y > 0;
  }

  public static double minCarTargetGoalCorrection(DataPacket input, Moment targetContactPoint) {
    Vector2 ballToGoalRight = toInsideRightGoal(input.car, targetContactPoint.position).flatten();
    Vector2 ballToGoalLeft = toInsideLeftGoal(input.car, targetContactPoint.position).flatten();

    Vector2 carToBall = carToBall(input).flatten();

    double leftCorrectAngle = ballToGoalLeft.correctionAngle(carToBall);
    double rightCorrectAngle = ballToGoalRight.correctionAngle(carToBall);

    if (leftCorrectAngle > 0 && rightCorrectAngle < 0) {
      return 0;
    }

    return Angles.minAbs(leftCorrectAngle, rightCorrectAngle);
  }

  public static double minCarTargetNotGoalCorrection(DataPacket input, Moment targetContactPoint) {
    Vector2 ballToGoalRight = toOutsideRightGoal(input.car, targetContactPoint.position).flatten();
    Vector2 ballToGoalLeft = toOutsideLeftGoal(input.car, targetContactPoint.position).flatten();

    Vector2 carToBall = carToBall(input).flatten();

    double leftCorrectAngle = Math.max(ballToGoalLeft.correctionAngle(carToBall), 0);
    double rightCorrectAngle = Math.min(ballToGoalRight.correctionAngle(carToBall), 0);

    if (leftCorrectAngle == 0 || rightCorrectAngle == 0) {
      return 0;
    }

    return Angles.minAbs(leftCorrectAngle, rightCorrectAngle);
  }

  public static Vector3 farPost(DataPacket input) {
    Goal ownGoal = Goal.ownGoal(input.car.team);

    double leftPostDistance = input.ball.position.distance(ownGoal.leftInside);
    double rightPostDistance = input.ball.position.distance(ownGoal.rightInside);
    return leftPostDistance > rightPostDistance ? ownGoal.leftInside : ownGoal.rightInside;
  }

  public static double minBallGoalCorrection(DataPacket input) {
    return minBallGoalCorrection(input.car, input.ball);
  }

  public static double minBallGoalCorrection(CarData car, BallData ball) {
    Vector2 ballToGoalRight = toOutsideRightGoal(car, ball.position).flatten();
    Vector2 ballToGoalLeft = toOutsideLeftGoal(car, ball.position).flatten();

    Vector2 ballRoll = ball.velocity.flatten();

    double leftCorrectAngle = ballToGoalLeft.correctionAngle(ballRoll);
    double rightCorrectAngle = ballToGoalRight.correctionAngle(ballRoll);

    if (leftCorrectAngle > 0 && rightCorrectAngle < 0) {
      return 0;
    }

    return Angles.minAbs(leftCorrectAngle, rightCorrectAngle);
  }

  private Locations() {}
}
