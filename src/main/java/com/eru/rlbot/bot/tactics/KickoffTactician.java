package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.main.EruBot;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.NormalUtils;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;

public class KickoffTactician extends Tactician {

  private static final float NOSE_DOWN = -1.0f;

  private boolean flipLock;

  private Tactic tactic;

  private StartLocation location;
  private boolean secondFlipLock;

  private boolean hasFlipped; // Keeps track of the sequence

  KickoffTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean isKickOff(DataPacket input) {
    return Math.abs(input.ball.position.x) < .1
        && Math.abs(input.ball.position.y) < .1
        && input.ball.velocity.norm() < .1
        && input.ball.position.z < 120;
  }

  private enum StartLocation {
    LEFT, LEFT_CENTER, CENTER, RIGHT_CENTER, RIGHT;
  }

  private void setStartLocation(CarData car) {
    boolean rightPosition = (car.position.y > 0 ^ car.position.x > 0);
    if (Math.abs(car.position.x) < 1) {
      location = StartLocation.CENTER;
    } else if (Math.abs(car.position.y) <= 2560) {
      location = rightPosition ? StartLocation.RIGHT : StartLocation.LEFT;
    } else {
      location = rightPosition ? StartLocation.RIGHT_CENTER : StartLocation.LEFT_CENTER;
    }

    flipLock = false;
    secondFlipLock = false;
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    if (nextTactic != tactic) {
      tactic = nextTactic;
      setStartLocation(input.car);
    }
    bot.botRenderer.setBranchInfo(String.format("%s", location));

    switch (location) {
      case LEFT:
      case RIGHT:
      case LEFT_CENTER:
      case RIGHT_CENTER:
      case CENTER:
        centerKickOff(input, output);
        break;
    }

//    mustyKicks(output, input);

    // dumbKickoff(output, input);
  }

  private void centerKickOff(DataPacket input, ControlsOutput output) {
    Vector3 targetContact = getTargetContact(input);
    bot.botRenderer.setCarTarget(targetContact);

    if (input.car.groundSpeed > 1800) {
      bot.botRenderer.setBranchInfo("Second flip.");
      output
          .withBoost()
          .withThrottle(1.0f);

      BallData relativeBall = NormalUtils.noseRelativeBall(input);
      if (relativeBall.position.y < 600) {
        tacticManager.preemptTactic(Tactic.builder()
          .setSubject(targetContact)
          .setTacticType(Tactic.TacticType.FLIP)
          .setSubjectType(Tactic.SubjectType.BALL)
          .build());
      }
    } else if (input.car.groundSpeed < 1400 && input.car.hasWheelContact) {
      // Get Up to speed
      output
          .withBoost()
          .withThrottle(1.0f);
    } else if (input.car.groundSpeed > 1400 && input.car.groundSpeed < 1600) {
      output
          .withBoost()
          .withThrottle(1.0f);
      tacticManager.preemptTactic(Tactic.builder()
          .setSubject(targetContact)
          .setTacticType(Tactic.TacticType.FLIP)
          .build());
    }
    output.withSteer(Angles.flatCorrectionAngle(input.car, targetContact) * 10);
  }

  private Vector3 getTargetContact(DataPacket input) {
    // TODO: Update this based on car / ball physics
    Vector3 correction = input.ball.position.minus(Goal.opponentGoal(input.car.team).center)
        .toMagnitude(Constants.BALL_RADIUS);
    return input.ball.position.plus(correction);
  }

  private void mustyKicks(ControlsOutput output, DataPacket input) {
    BallData relativeData = NormalUtils.noseRelativeBall(input);

    if (secondFlipLock) {
      if (input.car.hasWheelContact) {
        output.withJump();
      } else if (JumpManager.canFlip()) {
        secondFlipLock = false;
        output
            .withJump()
            .withYaw(location == StartLocation.RIGHT_CENTER ? -1 : 1);
      } else {
        bot.botRenderer.setBranchInfo("Wait...");
      }
    } else if (location == StartLocation.LEFT_CENTER || location == StartLocation.RIGHT_CENTER) {
      double initJump = 3200; // Turn in / boost
      double releaseJump = initJump - 75; // Jump
      double initFlip = releaseJump - 50; // Diagonal Flip
      double prepLanding = initFlip - 400; // Straighten out
      double releaseBoost = 1300; // Release the boost
      double secondJump = 400; // Jump again

      if (relativeData.position.y > initJump) {
        // Turn in and boost
        output
            .withThrottle(1.0f)
            .withBoost()
            .withSteer((location == StartLocation.RIGHT_CENTER ? -1 : 1) * .21f);
      } else if (relativeData.position.y > releaseJump) {
        // Jump
        output
            .withJump()
            .withBoost()
            .withThrottle(1.0f);
      } else if (relativeData.position.y > initFlip) {
        // Release Jump
        output
            .withThrottle(1.0f)
            .withBoost();
        hasFlipped = false;
      } else if (relativeData.position.y > prepLanding) {
        // Diagonal Flip
        bot.botRenderer.setBranchInfo("Diagonal Flip!");
        output
            .withThrottle(1.0f)
            .withJump(!hasFlipped)
            .withYaw(location == StartLocation.RIGHT_CENTER ? 1 : -1)
            .withPitch(-.5F)
            .withBoost();
        hasFlipped = true;
      } else if (relativeData.position.y > releaseBoost) {
        bot.botRenderer.setBranchInfo("Land Cleanly!");
        output
            .withThrottle(1.0f)
            .withBoost();
        if (Math.abs(relativeData.position.x) > 2) {
          output
              .withYaw(relativeData.position.x);
//              .withSteer(-relativeData.position.x);
        }
        if (Math.abs(input.car.orientation.getLeftVector().z) > .05) {
          output.withRoll(input.car.orientation.getLeftVector().z);
        }
        if (input.car.orientation.getNoseVector().z > .01) {
          output.withPitch(-10 * input.car.orientation.getNoseVector().z);
        }
      } else if (relativeData.position.y > secondJump) {
        // Land cleanly
        output.withThrottle(1.0f);
        correctTowardBall(input, output);
      } else {
        secondFlipLock = true;
      }
    } else {
      correctTowardBall(input, output);
      output.withBoost();
      output.withThrottle(1.0f);

      if (relativeData.position.y < 400) {
        secondFlipLock = true;
      }
    }
  }

  // Do not modify...

  private void dumbKickoff(ControlsOutput output, DataPacket input) {
    BallData relativeData = NormalUtils.noseRelativeBall(input);

    tryFlipLock(input, relativeData);

    if (flipLock) {
      if (JumpManager.canFlip() && relativeData.position.y < getFlipDistance()) {
        output.withPitch(NOSE_DOWN);
        output.withJump();
        flipLock = false;
        bot.botRenderer.setBranchInfo("Flip");
      } else {
        output.withJump(input.car.position.z <= Constants.CAR_AT_REST + 2);
        output.withThrottle(1.0f);
        bot.botRenderer.setBranchInfo("Jump");
      }
    } else {
      if (!input.car.hasWheelContact) {
        if (input.car.orientation.getNoseVector().z > .1) { // Nose vector is normalized
          output.withPitch(NOSE_DOWN);
        }
      } else if (input.car.groundSpeed < 200) {
        output.withSteer(getLocationTurnIn());
        output.withThrottle(1.0f);
        bot.botRenderer.setBranchInfo("Turn in.");
      } else {
        fullThrottle(input, output);
        bot.botRenderer.setBranchInfo("Turn to ball.");
      }

      if (Math.abs(output.getSteer()) < 1
          && input.car.orientation.getNoseVector().z > -.03
          && input.car.orientation.getNoseVector().z < .1
          && relativeData.position.y > 0) {
        output.withBoost();
      }
    }
  }

  private float getFlipDistance() {
    if (location == null) return 4000f;
    switch (location) {
      case LEFT:
      case RIGHT:
        return 2000f;
      case LEFT_CENTER:
      case RIGHT_CENTER:
        return 2500f;
      default:
      case CENTER:
        return 4000f;
    }
  }

  private float getLocationTurnIn() {
    if (location == null) return 0f;
    switch (location) {
      case LEFT:
      case LEFT_CENTER:
        return 1.0f;
      case RIGHT_CENTER:
        return -1.0f;
      case RIGHT:
      default:
      case CENTER:
        return 0f;
    }
  }

  private float getLocationYaw() {
    if (location == null) return 1.0f;
    switch (location) {
      case LEFT:
      case LEFT_CENTER:
      case CENTER:
        return 1.0f;
      case RIGHT_CENTER:
      case RIGHT:
      default:
        return -1.0f;
    }
  }

  private void tryFlipLock(DataPacket input, BallData relativeData) {
    // Do not alter flip lock in the air.
    if (!input.car.hasWheelContact) {
      return;
    }

    if (input.car.velocity.flatten().norm() > 1400 && relativeData.position.y > 2000) {
      // First flip
      flipLock = true;
    } else if (relativeData.position.y < 700){
      // Second flip
      flipLock = true;
    }
  }

  private void correctTowardBall(DataPacket input, ControlsOutput output) {
    double steer = Angles.flatCorrectionAngle(input.car, input.ball.position);
    if (Math.abs(steer) > .05 && Math.abs(steer) < .1) {
      steer = .1 * steer < 0 ? -1 : 1;
    } else if (Math.abs(steer) < .05) {
      steer = 0;
    }
    output.withSteer(steer);
  }

  private void fullThrottle(DataPacket input, ControlsOutput output) {
    correctTowardBall(input, output);
    output.withThrottle(1.0);
  }
}
