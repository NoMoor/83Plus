package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Arrays;
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

  public static Optional<KickoffLocation> getKickoffLocation(CarData car) {
    if (car.velocity.magnitude() > 40)
      return Optional.empty();

    KickoffLocation closestLocation = Arrays.stream(KickoffLocation.values())
        .min(Comparator.comparing(location -> car.position.distance(location.location.asVector3())))
        .get();

    if (closestLocation.location.distance(car.position.flatten()) > 50) {
      return Optional.empty();
    }
    return Optional.of(closestLocation);
  }

  private static final double POS = 1.0;
  private static final double NEG = -1.0;
  private static final double SIT = 0;

  public enum KickoffLocation {
    CENTER_BLUE(Vector2.of(CENTER_X, -CENTER_Y), KickoffStation.CENTER, SIT),
    LEFT_CENTER_BLUE(Vector2.of(CENTER_MID_X, -CENTER_MID_Y), KickoffStation.LEFT_CENTER, POS),
    RIGHT_CENTER_BLUE(Vector2.of(-CENTER_MID_X, -CENTER_MID_Y), KickoffStation.RIGHT_CENTER, NEG),
    LEFT_BLUE(Vector2.of(FAR_X, -FAR_Y), KickoffStation.LEFT, POS),
    RIGHT_BLUE(Vector2.of(-FAR_X, -FAR_Y), KickoffStation.RIGHT, NEG),

    CENTER_ORANGE(Vector2.of(CENTER_X, CENTER_Y), KickoffStation.CENTER, SIT),
    LEFT_CENTER_ORANGE(Vector2.of(-CENTER_MID_X, CENTER_MID_Y), KickoffStation.LEFT_CENTER, NEG),
    RIGHT_CENTER_ORANGE(Vector2.of(CENTER_MID_X, CENTER_MID_Y), KickoffStation.RIGHT_CENTER, POS),
    LEFT_ORANGE(Vector2.of(-FAR_X, FAR_Y), KickoffStation.LEFT, NEG),
    RIGHT_ORANGE(Vector2.of(FAR_X, FAR_Y), KickoffStation.RIGHT, POS);

    public final Vector2 location;
    public final KickoffStation station;
    public final double pushModifier;
    public final double turnModifier;

    KickoffLocation(Vector2 location, KickoffStation station, double pushModifier) {
      this.location = location;
      this.station = station;
      this.turnModifier = station.turnModifier;
      this.pushModifier = pushModifier;
    }

    public static KickoffLocation defaultLocation(int team) {
      return team == 0 ? CENTER_BLUE : CENTER_ORANGE;
    }
  }

  public enum KickoffStation {
    LEFT(1), LEFT_CENTER(1), CENTER(0), RIGHT_CENTER(-1), RIGHT(-1);

    private final double turnModifier;

    KickoffStation(double turnModifier) {
      this.turnModifier = turnModifier;
    }
  }
}
