package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

public class Locations {

  public static Vector3 toInsideLeftGoal(DataPacket input, Vector3 target) {
    return Goal.opponentGoal(input.car.team).leftInside.minus(target);
  }

  public static Vector3 toInsideRightGoal(DataPacket input, Vector3 target) {
    return Goal.opponentGoal(input.car.team).rightInside.minus(target);
  }

  private static Vector3 toOutsideLeftGoal(DataPacket input, Vector3 target) {
    return Goal.ownGoal(input.car.team).leftOutside.minus(target);
  }

  private static Vector3 toOutsideRightGoal(DataPacket input, Vector3 position) {
    return Goal.ownGoal(input.car.team).rightOutside.minus(position);
  }

  public static Vector3 ballToOppGoalCenter(DataPacket input) {
    return Goal.opponentGoal(input.car.team).center.minus(input.ball.position);
  }

  public static Vector3 carToBall(DataPacket input) {
    return input.ball.position.minus(input.car.position);
  }

  public static boolean isOpponentSideOfBall(DataPacket input) {
    double correctionAngle = minCarTargetGoalCorrection(input, new Moment(input.ball));

    boolean carFacingOwnGoal = Math.abs(correctionAngle) > Math.PI * .5;

    return carFacingOwnGoal && ballIsInFrontOfCar(input);
  }

  public static boolean ballIsInFrontOfCar(DataPacket input) {
    BallData relativeBallLocation = NormalUtils.noseRelativeBall(input);

    return relativeBallLocation.position.y > 0;
  }

  public static double minCarTargetGoalCorrection(DataPacket input, Moment targetContactPoint) {
    Vector2 ballToGoalRight = toInsideRightGoal(input, targetContactPoint.position).flatten();
    Vector2 ballToGoalLeft = toInsideLeftGoal(input, targetContactPoint.position).flatten();

    Vector2 carToBall = carToBall(input).flatten();

    double leftCorrectAngle = ballToGoalLeft.correctionAngle(carToBall);
    double rightCorrectAngle = ballToGoalRight.correctionAngle(carToBall);

    if (leftCorrectAngle > 0 && rightCorrectAngle < 0) {
      return 0;
    }

    return Angles.minAbs(leftCorrectAngle, rightCorrectAngle);
  }

  public static double minCarTargetNotGoalCorrection(DataPacket input, Moment targetContactPoint) {
    Vector2 ballToGoalRight = toOutsideRightGoal(input, targetContactPoint.position).flatten();
    Vector2 ballToGoalLeft = toOutsideLeftGoal(input, targetContactPoint.position).flatten();

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

    double leftPostDistance = input.ball.position.distance(ownGoal.left);
    double rightPostDistance = input.ball.position.distance(ownGoal.right);
    return leftPostDistance > rightPostDistance ? ownGoal.left : ownGoal.right;
  }

  public static double minBallGoalCorrection(DataPacket input) {
    Vector2 ballToGoalRight = toOutsideRightGoal(input, input.ball.position).flatten();
    Vector2 ballToGoalLeft = toOutsideLeftGoal(input, input.ball.position).flatten();

    Vector2 ballRoll = input.ball.velocity.flatten();

    double leftCorrectAngle = ballToGoalLeft.correctionAngle(ballRoll);
    double rightCorrectAngle = ballToGoalRight.correctionAngle(ballRoll);

    if (leftCorrectAngle > 0 && rightCorrectAngle < 0) {
      return 0;
    }

    return Angles.minAbs(leftCorrectAngle, rightCorrectAngle);
  }

  private Locations() {}

  public static Vector3 getFirstPossibleTouch(DataPacket input) {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();

    if (ballPredictionOptional.isPresent()) {
      BallPrediction ballPrediction = ballPredictionOptional.get();
      Vector2 flatCarPosition = input.car.position.flatten();

      for (int i = 0 ; i < ballPrediction.slicesLength() ; i = Math.min(i + 5, ballPrediction.slicesLength() - 1)) {
        PredictionSlice slice = ballPrediction.slices(i);

        Vector3 slicePosition = Vector3.of(slice.physics().location());

        float timeToLocation = Accels.minTimeToDistance(input.car, flatCarPosition.distance(slicePosition.flatten()));
        if (timeToLocation < slice.gameSeconds() - input.car.elapsedSeconds) {
          // Target Acquired.
          return slicePosition;
        }
      }
    }

    return input.ball.position;
  }
}
