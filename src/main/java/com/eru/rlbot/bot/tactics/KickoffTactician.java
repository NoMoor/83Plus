package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.Locations;
import com.eru.rlbot.bot.common.NormalUtils;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.maneuver.DiagonalFlipCancel;
import com.eru.rlbot.bot.maneuver.FlipHelper;
import com.eru.rlbot.common.StateLogger;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Comparator;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KickoffTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("KickoffTactician");

  private Locations.KickoffLocation location = Locations.KickoffLocation.CENTER;

  private boolean hasFlipped; // Keeps track of the sequence

  KickoffTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean isKickOff(DataPacket input) {
    return Math.abs(input.ball.position.x) < .1
        && Math.abs(input.ball.position.y) < .1
        && input.ball.velocity.magnitude() < 100
        && input.ball.position.z < 120;
  }

  @Override
  public void internalExecute(DataPacket input, ControlsOutput output, Tactic tactic) {
    checkTactic(input, tactic);

    if (location == Locations.KickoffLocation.LEFT_CENTER || location == Locations.KickoffLocation.RIGHT_CENTER) {
      centerOffset(input, output);
    } else if (location == Locations.KickoffLocation.CENTER) {
      centerKickOff(input, output);
    } else {
      cornerSpeedFlip(output, input);
    }
    bot.botRenderer.setBranchInfo(location.name());
  }

  @Override
  protected void reset(DataPacket input) {
    Optional<Locations.KickoffLocation> optionalKickoffLocation = Locations.getKickoffLocation(input.car);
    location = optionalKickoffLocation.orElse(location);

    if (optionalKickoffLocation.isPresent()) {
      hasFlipped = false;
      clearDelegate();
    }
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

      delegateTo(FlipHelper.builder(bot)
          .setAggressiveness(1)
          .build());
    } else if (absY > 700) {
      output
          .withBoost(input.car.groundSpeed < Constants.BOOSTED_MAX_SPEED && input.car.noseIsBetween(-.2, .1))
          .withThrottle(1.0f);
    } else {
      CarData closestOpponent = closestOpponentToBall(input);
      double aggressiveness = 1;
      if (closestOpponent != null) {
        // Need to get higher than the opponent.
        Vector3 oppBall = input.ball.position.minus(closestOpponent.position);
        Vector3 selfBall = input.ball.position.minus(input.car.position);
        double value = (slope(oppBall) - slope(selfBall)) / 10;
        aggressiveness = Angles3.clip(value, 0, 1);
      }

      delegateTo(FlipHelper.builder(bot)
          .setAggressiveness(aggressiveness)
          .build());
    }
  }

  private double slope(Vector3 v) {
    return v.y / Math.abs(v.x);
  }

  private CarData closestOpponentToBall(DataPacket input) {
    return input.allCars.stream()
        .filter(car -> input.car.team != car.team)
        .min(Comparator.comparing(car -> car.position.distance(input.ball.position)))
        .orElse(null);
  }

  private Vector3 getTargetContact(DataPacket input) {
    Vector3 correction = input.ball.position.minus(Goal.opponentGoal(input.car.team).center)
        .toMagnitude(Constants.BALL_RADIUS);
    return input.ball.position.plus(correction);
  }

  private void centerOffset(DataPacket input, ControlsOutput output) {
    double initJump = 3100; // Turn in / boost
    double releaseJump = initJump - 75; // Jump
    double initFlip = releaseJump - 50; // Diagonal Flip
    double prepLanding = initFlip - 380; // Straighten out
    double releaseBoost = 1300; // Release the boost
    double secondJump = 700; // Jump again

    BallData relativeData = NormalUtils.noseRelativeBall(input);
    JumpManager jumpManager = JumpManager.forCar(input.car);

    if (relativeData.position.y > initJump) {
      // Turn in and boost
      bot.botRenderer.setBranchInfo("Turn in!");
      output
          .withThrottle(1.0f)
          .withSteer((location == Locations.KickoffLocation.RIGHT_CENTER ? -1 : 1) * .20f)
          .withBoost();
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
          .withYaw(location == Locations.KickoffLocation.RIGHT_CENTER ? 1 : -1)
          .withPitch(-1F)
          .withBoost();
      hasFlipped = true;
    } else if (relativeData.position.y > releaseBoost || !input.car.hasWheelContact) {
      bot.botRenderer.setBranchInfo("Land Cleanly!");
      output
          .withThrottle(1.0f)
          .withBoost(input.car.orientation.getNoseVector().z > -.1 && input.car.orientation.getNoseVector().z < .1);
      Angles3.setControlsFor(input.car, Orientation.fromFlatVelocity(Angles.carBall(input)).getOrientationMatrix(), output);
    } else {
      bot.botRenderer.setBranchInfo("Prep second jump!");

      // Land cleanly
      output.withThrottle(1.0f)
          .withBoost();
      correctTowardBall(input, output);
      delegateTo(FlipHelper.builder(bot)
          .setAggressiveness(1.0)
          .build());
    }
  }

  private void cornerSpeedFlip(ControlsOutput output, DataPacket input) {
    JumpManager jumpManager = JumpManager.forCar(input.car);
    // Fast flip.
    float carX = Math.abs(input.car.position.x);
    float leftRightMirror = location == Locations.KickoffLocation.LEFT ? -1 : 1;

    if (carX > 1900) { // Tilt in
      bot.botRenderer.setBranchInfo("Tilt in");
      StateLogger.log(input, "Tilt in");
      output
          .withThrottle(1.0f)
          .withBoost()
          .withSteer(Angles.flatCorrectionAngle(
              input.car,
              input.ball.position.addX(leftRightMirror * -DiagonalFlipCancel.MIN_DRIFT * 1.2)) * 5);
    } else if ((input.car.hasWheelContact || (input.car.position.z < 45 && jumpManager.elapsedJumpTime() < .1))
        && carX > 1600) {
      bot.botRenderer.setBranchInfo("Jump");
      StateLogger.log(input, "Jump");
      delegateTo(DiagonalFlipCancel.builder()
          .setTarget(input.ball.position)
          .build());
    } else {
      Vector3 ballContactTarget = input.ball.position.addY(Math.signum(input.car.position.y) * Constants.BALL_RADIUS);
      bot.botRenderer.renderTarget(ballContactTarget);
      output
          .withSteer(Angles.flatCorrectionAngle(input.car, ballContactTarget) * 10)
          .withThrottle(1.0);
    }
  }

  private void correctTowardBall(DataPacket input, ControlsOutput output) {
    output.withSteer(Angles.flatCorrectionAngle(input.car, input.ball.position));
  }
}
