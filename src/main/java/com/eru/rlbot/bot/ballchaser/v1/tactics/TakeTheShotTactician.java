package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableMap;
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
    if (input.ball.position.z > 500) {
      return false;
    }

    BallData relativeBallData = NormalUtils.noseNormal(input);
    if (Math.abs(relativeBallData.position.x) > 400) {
      return false;
    }

    if (input.allCars.size() == 1) {
      return true;
    }

    float timeToBall = timeToBall(relativeBallData, input.car);

    for (int i = 0 ; i < input.allCars.size() ; i++) {
      CarData carData = input.allCars.get(i);
      if (carData == input.car) {
        continue;
      }

      BallData relativeBallDataI = NormalUtils.noseNormal(input, i);

      float oppTimeToBall = timeToBall(relativeBallDataI, carData);

      if (oppTimeToBall < timeToBall) {
        return false;
      }
    }

    return true;
  }

  private static float timeToBall(BallData relativeBallData, CarData carData) {
    return carData.boost > 20
        ? Accels.boostedTimeToDistance(carData.velocity.flatten().norm(), relativeBallData.position.flatten().norm())
        : Accels.timeToDistance(carData.velocity.flatten().norm(), relativeBallData.position.flatten().norm());
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    if (tactic.getTarget().z < 100) {
      rollingBall(input, output, tactic);
    } else if (tactic.getTarget().z < 530) {
      jumpingBall(input, output, tactic);
    }

//    if (input.ball.position.z > 100 && input.ball.position.z < 125) {
//      bot.botRenderer.setBranchInfo("Striking speed: %f", input.car.velocity.norm());
//    }
  }

  private float jumpVelocity;
  private boolean secondJumpLock; // ???
  private void jumpingBall(DataPacket input, ControlsOutput output, Tactic tactic) {
    bot.botRenderer.setBranchInfo("Jumping Ball");

    double distanceToTarget = input.car.position.flatten().distance(tactic.getTarget().flatten());
    double velocity = input.car.velocity.flatten().norm();

    double timeToTarget = distanceToTarget / velocity;

    if (input.car.hasWheelContact) {
      output.withSteer(Angles.flatCorrectionDirection(input.car, input.ball.position));
      output.withThrottle(1.0f);

      if (input.car.velocity.flatten().norm() < Constants.BOOSTED_MAX_SPEED) {
        output.withBoost();
      }

      Vector3 goalTarget = Goal.opponentGoal(input.car.team).center;
      double shotDistance = tactic.getTarget().distance(goalTarget);
      double shotYOffset = goalTarget.z - tactic.getTarget().z;

      // TODO: Update 5000 this to be distance from tactic target to 'shot' target.
//      double zOffset = getRoughUnderCut(shotDistance, shotYOffset, input.car.velocity.flatten().norm());
      double zOffset = 0;
      double targetHeight = tactic.getTarget().z - zOffset;

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
        double zDistance = tactic.getTarget().z - input.car.position.z;

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
        // Pitch to target the ball
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

  private double getRoughUnderCut(double shotDistance, double shotYOffset, double carSpeed) {
    double ballVelocity = carSpeed * .6; // Fill in constant.

    // TODO: What angle to fly the distance required?

    return (shotDistance / 1000) * 7 + (shotYOffset / 7);
  }

  private void rollingBall(DataPacket input, ControlsOutput output, Tactic tactic) {
    bot.botRenderer.setBranchInfo("Rolling Ball %d", (int) input.car.velocity.flatten().norm());

    double distance = tactic.getTarget().distance(Goal.opponentGoal(input.car.team).center);

    output.withSteer(Angles.flatCorrectionDirection(input.car, input.ball.position));

    double targetSpeed = getRoughSpeed(distance);
    if (input.car.velocity.norm() < targetSpeed) { // TODO: Take into account the relative ball speed.
      output
          .withThrottle(1.0f)
          .withBoost();
    } else if (input.car.velocity.norm() > targetSpeed + 100f) {
      // Coast.
    } else {
      output.withThrottle(.02f);
    }
  }

  private static double getRoughSpeed(double distance) {
    return STRIKING_SPEEDS.getOrDefault(distance - distance % 1000, 2000d);
  }
}
