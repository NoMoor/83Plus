package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.NormalUtils;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.maneuver.FlipHelper;
import com.eru.rlbot.common.StateLogger;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KickoffTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("KickoffTactician");

  private StartLocation location;

  private boolean hasFlipped; // Keeps track of the sequence

  KickoffTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean isKickOff(DataPacket input) {
    return Math.abs(input.ball.position.x) < .1
        && Math.abs(input.ball.position.y) < .1
        && input.ball.velocity.magnitude() < .1
        && input.ball.position.z < 120;
  }

  private enum StartLocation {
    FAR_LEFT, LEFT_CENTER, CENTER, RIGHT_CENTER, FAR_RIGHT
  }

  // TODO: Only update if you are close to a certain spot.
  private void setStartLocation(CarData car) {
    boolean rightPosition = (car.position.y > 0 ^ car.position.x < 0);
    if (Math.abs(car.position.x) < 1) {
      location = StartLocation.CENTER;
    } else if (Math.abs(car.position.y) <= 2560) {
      location = rightPosition ? StartLocation.FAR_RIGHT : StartLocation.FAR_LEFT;
    } else {
      location = rightPosition ? StartLocation.RIGHT_CENTER : StartLocation.LEFT_CENTER;
    }
  }

  @Override
  public void internalExecute(DataPacket input, ControlsOutput output, Tactic tactic) {
    checkTactic(input, tactic);

    if (location == StartLocation.LEFT_CENTER || location == StartLocation.RIGHT_CENTER) {
      centerOffset(input, output);
    } else if (location == StartLocation.CENTER) {
      centerKickOff(input, output);
    } else {
      cornerSpeedFlip(output, input);
    }
    bot.botRenderer.setBranchInfo(location.name());
  }

  @Override
  protected void reset(DataPacket input) {
    setStartLocation(input.car);
    hasFlipped = false;
  }

  private void centerKickOff(DataPacket input, ControlsOutput output) {
    Vector3 targetContact = getTargetContact(input);
    bot.botRenderer.setCarTarget(targetContact);

    double absY = Math.abs(input.car.position.y);

    output.withSteer(Angles.flatCorrectionAngle(input.car, targetContact) * 10);

    if (absY > 4200) {
      // Get Up to speed
      output
          .withBoost()
          .withThrottle(1.0f);
    } else if (absY > 3500) {
      output
          .withBoost()
          .withThrottle(1.0f);

      delegateTo(FlipHelper.builder(bot).build());
    } else if (absY > 800) {
      output
          .withBoost(input.car.groundSpeed < Constants.BOOSTED_MAX_SPEED)
          .withThrottle(1.0f);
    } else if (absY > 600) {
      delegateTo(FlipHelper.builder(bot).build());
    }
  }

  private Vector3 getTargetContact(DataPacket input) {
    Vector3 correction = input.ball.position.minus(Goal.opponentGoal(input.car.team).center)
        .toMagnitude(Constants.BALL_RADIUS);
    return input.ball.position.plus(correction);
  }

  private void centerOffset(DataPacket input, ControlsOutput output) {
    double initJump = 3200; // Turn in / boost
    double releaseJump = initJump - 75; // Jump
    double initFlip = releaseJump - 50; // Diagonal Flip
    double prepLanding = initFlip - 380; // Straighten out
    double releaseBoost = 1300; // Release the boost
    double secondJump = 400; // Jump again

    BallData relativeData = NormalUtils.noseRelativeBall(input);
    JumpManager jumpManager = JumpManager.forCar(input.car);

    if (relativeData.position.y > initJump) {
      // Turn in and boost
      bot.botRenderer.setBranchInfo("Turn in!");
      output
          .withThrottle(1.0f)
          .withBoost()
          .withSteer((location == StartLocation.RIGHT_CENTER ? -1 : 1) * .21f);
    } else if (relativeData.position.y > releaseJump) {
      // Jump
      bot.botRenderer.setBranchInfo("Jump!");
      output
          .withJump()
          .withBoost()
          .withThrottle(1.0f);
    } else if (relativeData.position.y > initFlip) {
      // Release Jump
      bot.botRenderer.setBranchInfo("Release Jump!");
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
          .withPitch(-1F)
          .withBoost();
      hasFlipped = true;
    } else if (relativeData.position.y > releaseBoost) {
      bot.botRenderer.setBranchInfo("Land Cleanly!");
      output
          .withThrottle(1.0f)
          .withBoost(input.car.orientation.getNoseVector().z > -.1 && input.car.orientation.getNoseVector().z < .1);
      Angles3.setControlsFor(input.car, Orientation.fromFlatVelocity(Angles.carBall(input)).getOrientationMatrix(), output);
    } else if (relativeData.position.y > secondJump) {
      bot.botRenderer.setBranchInfo("Prep second jump!");

      // Land cleanly
      output.withThrottle(1.0f);
      correctTowardBall(input, output);
    } else {
      delegateTo(FlipHelper.builder(bot).build());
    }
  }

  private void cornerSpeedFlip(ControlsOutput output, DataPacket input) {
    JumpManager jumpManager = JumpManager.forCar(input.car);
    // Fast flip.
    float carX = Math.abs(input.car.position.x);
    float leftRightMirror = location == StartLocation.FAR_LEFT ? -1 : 1;

    if (carX > 1900) { // Tilt in
      bot.botRenderer.setBranchInfo("Tilt in");
      StateLogger.log(input, "Tilt in");
      output
          .withThrottle(1.0f)
          .withBoost()
          .withSteer(Angles.flatCorrectionAngle(
              input.car,
              input.ball.position.addX(leftRightMirror * -600))
              * 5);
    } else if ((input.car.hasWheelContact || (input.car.position.z < 45 && jumpManager.elapsedJumpTime() < .1))
        && carX > 1600) {
      bot.botRenderer.setBranchInfo("Jump");
      StateLogger.log(input, "Jump");
      output
          .withJump()
          .withBoost()
          .withYaw(leftRightMirror);
    } else if (!jumpManager.hasReleasedJumpInAir() && carX > 1000) {
      bot.botRenderer.setBranchInfo("Release button");
      StateLogger.log(input, "Release");
      output.withBoost();
    } else if (jumpManager.canFlip() && carX > 1000) {
      bot.botRenderer.setBranchInfo("Flip");
      StateLogger.log(input, "Flip");
      output
          .withJump()
          .withBoost()
          .withPitch(-.5) // Front flip
          .withYaw(leftRightMirror * -.6)
          .withThrottle(1.0);
    } else if (jumpManager.isFlipping()) {
      bot.botRenderer.setBranchInfo("Flip Cancel");
      StateLogger.log(input, "Flip Cancel");
      output
          .withBoost()
          .withPitch(1); // Flip cancel.
    } else if (input.car.position.z > 25) {
      bot.botRenderer.setBranchInfo("Land Cleanly");
      StateLogger.log(input, "Land Cleanly");
      output
          .withBoost()
          .withSlide();
      Angles3.setControlsForFlatLanding(input.car, output);
    } else {
      bot.botRenderer.setBranchInfo("Turn toward ball");
      StateLogger.log(input, "Turn toward ball");
      output
          .withThrottle(1.0)
          .withSteer(
              Angles.flatCorrectionAngle(input.car, input.ball.position.minus(Vector3.of(0, -120, 0))) * 5);
    }
  }

  private void correctTowardBall(DataPacket input, ControlsOutput output) {
    output.withSteer(Angles.flatCorrectionAngle(input.car, input.ball.position));
  }
}
