package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.Optional;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;

public class Locations {

  public static Vector3 toInsideLeftGoal(CarData car, Vector3 target) {
    return Vector3.from(target, Goal.opponentGoal(car.team).leftInside);
  }

  public static Vector3 toInsideRightGoal(CarData car, Vector3 target) {
    return Vector3.from(target, Goal.opponentGoal(car.team).rightInside);
  }

  private static Vector3 toOutsideLeftGoal(CarData car, Vector3 target) {
    return Vector3.from(target, Goal.ownGoal(car.team).leftOutside);
  }

  private static Vector3 toOutsideRightGoal(CarData car, Vector3 position) {
    return Vector3.from(position, Goal.ownGoal(car.team).rightOutside);
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
    BallData relativeBallLocation = NormalUtils.noseRelativeBall(input);

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

  public static Vector3 getFirstPossibleTouch(DataPacket input) {
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();

    if (ballPredictionOptional.isPresent()) {
      BallPrediction ballPrediction = ballPredictionOptional.get();
      Vector2 flatCarPosition = input.car.position.flatten();

      for (int i = 0 ; i < ballPrediction.slicesLength() ; i = Math.min(i + 5, ballPrediction.slicesLength() - 1)) {
        PredictionSlice slice = ballPrediction.slices(i);

        Vector3 slicePosition = Vector3.of(slice.physics().location());

        double timeToLocation = Accels.minTimeToDistance(input.car, flatCarPosition.distance(slicePosition.flatten())).time;
        if (timeToLocation < slice.gameSeconds() - input.car.elapsedSeconds) {
          // Target Acquired.
          return slicePosition;
        }
      }
    }

    return input.ball.position;
  }

  private static final double CENTER_X = 0;
  private static final double CENTER_Y = 4608;
  private static final double CENTER_MID_X = 256;
  private static final double CENTER_MID_Y = 3840;
  private static final double FAR_X = 2048;
  private static final double FAR_Y = 2560;

  private static final Vector2 CENTER_BLUE = Vector2.of(CENTER_X, -CENTER_Y);
  private static final Vector2 LEFT_CENTER_BLUE = Vector2.of(CENTER_MID_X, -CENTER_MID_Y);
  private static final Vector2 RIGHT_CENTER_BLUE = Vector2.of(-CENTER_MID_X, -CENTER_MID_Y);
  private static final Vector2 LEFT_BLUE = Vector2.of(FAR_X, -FAR_Y);
  private static final Vector2 RIGHT_BLUE = Vector2.of(-FAR_X, -FAR_Y);

  private static final Vector2 CENTER_ORANGE = Vector2.of(CENTER_X, CENTER_Y);
  private static final Vector2 LEFT_CENTER_ORANGE = Vector2.of(-CENTER_MID_X, CENTER_MID_Y);
  private static final Vector2 RIGHT_CENTER_ORANGE = Vector2.of(CENTER_MID_X, CENTER_MID_Y);
  private static final Vector2 LEFT_ORANGE = Vector2.of(-FAR_X, FAR_Y);
  private static final Vector2 RIGHT_ORANGE = Vector2.of(FAR_X, FAR_Y);

  private static final ImmutableMap<Integer, ImmutableList<Pair<Vector2, KickoffLocation>>> KICKOFF_LOCATIONS =
      ImmutableMap.<Integer, ImmutableList<Pair<Vector2, KickoffLocation>>>builder()
          .put(0, ImmutableList.<Pair<Vector2, KickoffLocation>>builder()
              .add(Pair.of(CENTER_BLUE, KickoffLocation.CENTER))
              .add(Pair.of(LEFT_CENTER_BLUE, KickoffLocation.LEFT_CENTER))
              .add(Pair.of(LEFT_BLUE, KickoffLocation.LEFT))
              .add(Pair.of(RIGHT_CENTER_BLUE, KickoffLocation.RIGHT_CENTER))
              .add(Pair.of(RIGHT_BLUE, KickoffLocation.RIGHT))
              .build())
          .put(1, ImmutableList.<Pair<Vector2, KickoffLocation>>builder()
              .add(Pair.of(CENTER_ORANGE, KickoffLocation.CENTER))
              .add(Pair.of(LEFT_CENTER_ORANGE, KickoffLocation.LEFT_CENTER))
              .add(Pair.of(LEFT_ORANGE, KickoffLocation.LEFT))
              .add(Pair.of(RIGHT_CENTER_ORANGE, KickoffLocation.RIGHT_CENTER))
              .add(Pair.of(RIGHT_ORANGE, KickoffLocation.RIGHT))
              .build())
          .build();

  public static Optional<KickoffLocation> getKickoffLocation(CarData car) {
    if (car.velocity.magnitude() > 40)
      return Optional.empty();

    Pair<Vector2, KickoffLocation> closestLocation = KICKOFF_LOCATIONS.get(car.team).stream()
        .min(Comparator.comparing(pair -> car.position.distance(pair.getFirst().asVector3())))
        .get();

    if (closestLocation.getFirst().distance(car.position.flatten()) > 50) {
      return Optional.empty();
    }
    return Optional.of(closestLocation.getSecond());
  }

  public enum KickoffLocation {
    LEFT, LEFT_CENTER, CENTER, RIGHT_CENTER, RIGHT
  }
}
