package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.common.Moment;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableMap;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

// TODO: Needs to know where the ball is and where the goal is
// Relies on other low level tactical units to do the movement but this tactician is responsible for planning a shot on
// goal.
public class TakeTheShotTactician extends Tactician {

  private static final ImmutableMap<Double, Double> STRIKING_SPEEDS = ImmutableMap.<Double, Double>builder()
      .put(0.0, 1750.0)
      .put(1000.0, 1750.0)
      .put(2000.0, 1750.0)
      .put(3000.0, 1700.0)
      .put(4000.0, 1850.0)
      .put(5000.0, 2000.0)
      .put(6000.0, 2150.0)
      .put(7000.0, 2300.0)
      .build();

  TakeTheShotTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean takeTheShot(DataPacket input) {
    if (input.allCars.size() == 1) {
      return true;
    }

    BallData relativeBallData = NormalUtils.noseRelativeBall(input);

    // The ball must mostly be in front of the car.
    if (Math.abs(relativeBallData.position.x) * 2 > relativeBallData.position.y) {
      return false;
    }

    // TODO: Replace this with another time to ball equation.
    float timeToBall = timeToBall(relativeBallData, input.car);

    // Am the closest car to the ball.
    for (int i = 0 ; i < input.allCars.size() ; i++) {
      CarData carData = input.allCars.get(i);
      if (carData == input.car) {
        continue;
      }

      BallData relativeBallDataI = NormalUtils.noseRelativeBall(input, i);

      float oppTimeToBall = timeToBall(relativeBallDataI, carData);

      if (oppTimeToBall < timeToBall) {
        return false;
      }
    }

    return true;
  }

  public static Moment shotTarget(DataPacket input) {
    // Assume we are hitting the ball where it is.
    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();

    if (ballPredictionOptional.isPresent()) {

      // TODO: Take into account angle change.
      BallPrediction ballPrediction = ballPredictionOptional.get();
      Vector2 flatCarPosition = input.car.position.flatten();

      for (int i = 0 ; i < ballPrediction.slicesLength() ; i++) {
        PredictionSlice slice = ballPrediction.slices(i);

        Vector3 slicePosition = Vector3.of(slice.physics().location());

        // TODO: Update to include boost status.
        float timeToLocation = Accels.boostedTimeToDistance(
            input.car.velocity.flatten().norm(),
            flatCarPosition.distance(slicePosition.flatten()) - Constants.BALL_RADIUS - Constants.CAR_LENGTH);

        // TODO: Account for more time to swing out.
        if (timeToLocation < slice.gameSeconds() - input.car.elapsedSeconds) {
          // Target Acquired.
          return new Moment(slice);
        }
      }
    }

    return new Moment(input.ball.position, input.ball.velocity);
  }

  private static float timeToBall(BallData relativeBallData, CarData carData) {
    return carData.boost > 40
        ? Accels.boostedTimeToDistance(carData.velocity.flatten().norm(), relativeBallData.position.flatten().norm())
        : Accels.timeToDistance(carData.velocity.flatten().norm(), relativeBallData.position.flatten().norm());
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    bot.botRenderer.setIntersectionTarget(tactic.getTargetPosition());

    // TODO: We should only do this if we have an open goal.
    if (Locations.isOpponentSideOfBall(input)) {
      getGoalSide(input, output, tactic);
    } else if (tactic.getTargetPosition().z < 200) {
      rollingBall(input, output, tactic);
    } else if (tactic.getTargetPosition().z < 530) {
      jumpingBall(input, output, tactic);
    }
  }

  private void getGoalSide(DataPacket input, ControlsOutput output, Tactic tactic) {
    bot.botRenderer.setBranchInfo("Get goal side.");
    Vector3 rotationPost = Goal.ownGoal(bot.team).getSameSidePost(input.car);
    Vector3 carToGoal = rotationPost.minus(input.car.position);

    double rotationNorm = Math.min(carToGoal.flatten().norm(), NormalUtils.noseRelativeBall(input).position.y + 2000);
    Vector3 rotationDirection = carToGoal.scaledToMagnitude(rotationNorm);

    Vector3 rotationTarget = input.car.position.plus(rotationDirection);

    double steeringAngle = Angles.flatCorrectionDirection(input.car, rotationTarget);

    output
        .withSteer(steeringAngle)
        .withThrottle(1.0f);

    if (rotationTarget.distance(input.car.position) > 1000) {
      output.withBoost();
    }
  }

  private float jumpVelocity;
  private boolean secondJumpLock; // ???
  private void jumpingBall(DataPacket input, ControlsOutput output, Tactic tactic) {
    bot.botRenderer.setBranchInfo("Jumping Ball");

    double distanceToTarget = input.car.position.flatten().distance(tactic.getTargetPosition().flatten());
    double velocity = input.car.velocity.flatten().norm();

    double timeToTarget = distanceToTarget / velocity;

    if (input.car.hasWheelContact) {
      output.withSteer(Angles.flatCorrectionDirection(input.car, input.ball.position));
      output.withThrottle(1.0f);

      if (input.car.velocity.flatten().norm() < Constants.BOOSTED_MAX_SPEED) {
        output.withBoost();
      }

//      Vector3 goalTarget = Goal.opponentGoal(input.car.team).center;
//      double shotDistance = tactic.getTargetPosition().distance(goalTarget);
//      double shotYOffset = goalTarget.z - tactic.getTargetPosition().z;
//
//      // TODO: Update 5000 this to be distance from tactic targetMoment to 'shot' targetMoment.
////      double zOffset = getRoughUnderCut(shotDistance, shotYOffset, input.car.velocity.flatten().norm());
      double zOffset = 0;
      double targetHeight = tactic.getTargetPosition().z - zOffset;

      Optional<Float> zTimeToTarget = Accels.jumpTimeToHeight(targetHeight);

      // Adjust the z based on the speed since longer shots can hit lower on the ball.
      if (!zTimeToTarget.isPresent()) {
        bot.botRenderer.setBranchInfo("Cannot jump that high %d", (int) targetHeight);
      } else if (zTimeToTarget.get() > timeToTarget) {
        output.withJump();
      }
    } else {
      boolean secondJumping = false;
      if (!JumpManager.hasMaxJumpHeight()) {
        // First Jump
        output
            .withJump()
            .withThrottle(1.0);
      } else if (!JumpManager.hasReleasedJumpInAir()) {
        // No Jump
        output.withThrottle(1.0);
      } else {
        double zVelocity = input.car.velocity.z;
        double zDistance = tactic.getTargetPosition().z - input.car.position.z;

        Optional<Float> floatingTimeToTarget = Accels.floatingTimeToTarget(zVelocity, zDistance);

        if (secondJumpLock) {
          output
              .withThrottle(1.0);
        } else if (!floatingTimeToTarget.isPresent() && input.ball.velocity.norm() == 0) {
          // Do we need to go higher?
          secondJumping = true;
          output
              .withJump()
              .withThrottle(1.0);
          secondJumpLock = true;
        }
      }

      if (!secondJumping || !JumpManager.canFlip()) {
        // Pitch to targetMoment the ball
        Vector3 noseVector = input.ball.position.minus(input.car.position).normalized();

        float carNoseZ = input.car.orientation.getNoseVector().z;
        float carNoseDz = input.car.angularVelocity.z;

        // TODO: Update to be the angle that we will hit the ball instead of the current angle.
        if (noseVector.z - (carNoseZ + carNoseDz) > 0.1) {
          // Need to tilt back
          output.withPitch(.5);
        } else if (noseVector.z - (carNoseZ + carNoseDz) < 0.1) {
          output.withPitch(-.5);
        }
      }
    }
  }

  private void rollingBall(DataPacket input, ControlsOutput output, Tactic tactic) {
    shoot(input, output, tactic);
  }

  private static double timeToShot = 0;
  private static double shotStartTime = 0;
  private void shoot(DataPacket input, ControlsOutput output, Tactic tactic) {

    double targetSpeed = getTargetSpeed(input.team, tactic.getTargetPosition());

    // Determine where to aim the car
    double shotCorrectionAngle = Locations.minCarTargetGoalCorrection(input, tactic.targetMoment);
    // TODO: Adjust the correction Angle based on ball speed.

    bot.botRenderer.setBranchInfo("Shoot %f", shotCorrectionAngle);

    Vector3 targetOffset = getTargetOffset(input.car, tactic.targetMoment, shotCorrectionAngle);
    Vector3 correctedTarget = tactic.targetMoment.position.plus(targetOffset);

    double correctionDirection = Angles.flatCorrectionDirection(input.car, correctedTarget);

    // TODO: Scale the steering angle with how close you are to the ball
    output.withSteer(correctionDirection * STEERING_SENSITIVITY);

    BallData noseNormalBall = NormalUtils.noseRelativeBall(input);
    double absSteeringAngle = Math.abs(output.getSteer());
    // TODO: take into account angular velocity.
    if (absSteeringAngle > 1) {
      // Need to slide / turn
      output
          .withThrottle(1.0f)
          .withSlide();

      // Boost to finish the turn
      if (absSteeringAngle < 1.4) {
        output.withBoost();
      }
    } else if (noseNormalBall.velocity.norm() < targetSpeed) { // TODO: Take into account the relative ball speed.
      output
          .withThrottle(1.0f);
      if (absSteeringAngle < .4) {
        output.withBoost();
      }
    } else if (noseNormalBall.velocity.norm() < targetSpeed + 20f) {
      output.withThrottle(.02f);
    } else {
      // Coast.
    }

    // Render elements
    bot.botRenderer.setCarTarget(correctedTarget);
    trackShotTime(input, targetSpeed, correctedTarget);
  }

  private static final double P_GAIN = 15d * -Constants.BALL_RADIUS;
  private static final double D_GAIN = .2d;
  private static final double STEERING_SENSITIVITY = 15d;
  private Vector3 getTargetOffset(CarData car, Moment target, double correctionAngle) {
    // TODO: Account for targetMoment speed.
    double xCorrection = Math.sin(correctionAngle) * P_GAIN;
    double xDampeningCorrection = -Math.sin(correctionAngle) * car.velocity.flatten().norm() * D_GAIN;

    double yCorrection = -Constants.BALL_RADIUS;

    // Rotate to be relative to where the car is.
    Vector3 carBall = Angles.carTarget(car, target.position);
    double rotationAngle = carBall.flatten().correctionAngle(Vector2.NORTH);
    return Angles.rotate(new Vector2(xCorrection + xDampeningCorrection, yCorrection), rotationAngle).asVector3();
  }

  private void trackShotTime(DataPacket input, double targetSpeed, Vector3 ballTarget) {
    if (input.ball.velocity.norm() > 300) {
      shotStartTime = 0;
      timeToShot = 0;
    } else if (timeToShot == 0) {
      timeToShot = Accels.minTimeToDistance(
          input.car, Angles.carTarget(input.car, ballTarget).flatten().norm(), targetSpeed);
      shotStartTime = input.car.elapsedSeconds;
    } else if (shotStartTime != 0 && Angles.carBall(input).flatten().norm() < 150 && input.ball.velocity.norm() == 0) {
      double shotTime = input.car.elapsedSeconds - shotStartTime;
      bot.botRenderer.addAlertText("Hit time %f off by %f", shotTime, timeToShot - shotTime);
      shotStartTime = 0;
      timeToShot = 0;
    }
  }

  // TODO: Update these speeds to account for rolling / relative speed which takes away from delta V from impact.
  private static double getTargetSpeed(int team, Vector3 position) {
    return getRoughSpeed(position.distance(Goal.opponentGoal(team).center));
  }

  private static double getRoughSpeed(double distance) {
    return STRIKING_SPEEDS.getOrDefault(distance - distance % 1000, 2000d);
  }
}
