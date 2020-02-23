package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.CarBallContactManager;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.Locations;
import com.eru.rlbot.bot.common.Monitor;
import com.eru.rlbot.bot.common.NormalUtils;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.maneuver.DiagonalFlipCancel;
import com.eru.rlbot.bot.maneuver.FlipHelper;
import com.eru.rlbot.common.StateLogger;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KickoffTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("KickoffTactician");

  private Locations.KickoffLocation location;

  private boolean hasFlipped; // Keeps track of the sequence
  private Vector3 target;
  private Monitor monitor;

  KickoffTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
    location = Locations.KickoffLocation.defaultLocation(bot.team);
  }

  public static boolean isKickOff(DataPacket input) {
    return Math.abs(input.ball.position.x) < .1
        && Math.abs(input.ball.position.y) < .1
        && input.ball.velocity.magnitude() < 100
        && input.ball.position.z < 120;
  }

  @Override
  void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    super.execute(input, output, tactic);
    if (monitor != null) {
      monitor.trackWhile(CarBallContactManager.isTouched(), input.car);
    }
  }

  @Override
  public void internalExecute(DataPacket input, ControlsOutput output, Tactic tactic) {
    checkTactic(input, tactic);
    if (target == null) {
      target = input.ball.position;
    }

    if (location.station == Locations.KickoffStation.LEFT_CENTER
        || location.station == Locations.KickoffStation.RIGHT_CENTER) {
      centerOffset(input, output);
    } else if (location.station == Locations.KickoffStation.CENTER) {
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
      target = selectTarget(input);
      monitor = Monitor.create(input);
    }
  }

  @Override
  public boolean isLocked() {
    return delegate != null;
  }

  private Vector3 selectTarget(DataPacket input) {
    Random random = new Random();
    if (location.station == Locations.KickoffStation.RIGHT || location.station == Locations.KickoffStation.LEFT) {
      if (random.nextBoolean() || true) {
        // Aim for the goal.
        Vector3 goalAngle = input.ball.position.minus(Goal.opponentGoal(input.team).getSameSidePost(input.car));
        return input.ball.position.plus(goalAngle.toMagnitude(Constants.BALL_RADIUS));
      } else {
        // Hit it around the opponent.
        return input.ball.position
            .addX(Constants.BALL_RADIUS * Math.signum(input.car.position.x))
            .addY(10 * -Math.signum(input.car.position.y));
      }
    } else if (location.station == Locations.KickoffStation.CENTER) {
      // -1 or 1
      int xInt = -1 + (random.nextInt(2) * 2);

      return input.ball.position.addX(xInt * Constants.BALL_RADIUS * 1.2);
    } else {
      // -1 or 1
      if (random.nextInt(10) >= 7) {
        // Hook
        return input.ball.position
            .addX(-location.pushModifier * Constants.BALL_RADIUS * 2)
            .addY(Math.signum(location.location.y) * Constants.BALL_RADIUS);
      } else {
        // PUsh
        return input.ball.position.addX(location.pushModifier * Constants.BALL_RADIUS / 3);
      }
    }
  }

  private void centerKickOff(DataPacket input, ControlsOutput output) {
    bot.botRenderer.setCarTarget(target);

    double absY = Math.abs(input.car.position.y);

    if (absY > 4300) {
      // Get Up to speed
      output
          .withBoost()
          .withThrottle(1.0f);
    } else if (absY > 3500) {
      output
          .withBoost()
          .withThrottle(1.0f);

      delegateTo(DiagonalFlipCancel.builder()
          .setTarget(target)
          .build());
    } else if (absY > 800) {
      output
          .withSteer(Angles.flatCorrectionAngle(input.car, target) * 10)
          .withThrottle(1);
    } else {
      CarData closestOpponent = closestOpponentToBall(input);
      if (closestOpponent != null) {
        // Need to get higher than the opponent.
        Vector3 oppBall = input.ball.position.minus(closestOpponent.position);
        Vector3 selfBall = input.ball.position.minus(input.car.position);
        double value = (slope(oppBall) - slope(selfBall)) / 10;
        double aggressiveness = Angles3.clip(value, 0, 1);

        if (oppBall.magnitude() - selfBall.magnitude() < 200) {
          delegateTo(FlipHelper.builder()
              .setAggressiveness(aggressiveness)
              .setTarget(target)
              .build());
        }
      } else {
        delegateTo(FlipHelper.builder()
            .setAggressiveness(1)
            .setTarget(target)
            .build());
      }
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

  private void centerOffset(DataPacket input, ControlsOutput output) {
    bot.botRenderer.renderTarget(target);

    double absY = Math.abs(input.car.position.y);

    if (absY > 3250) {
      // Get Up to speed
      output
          .withSteer(location.turnModifier * .15)
          .withBoost()
          .withThrottle(1.0f);
    } else if (absY > 2500) {
      output
          .withBoost()
          .withThrottle(1.0f);

      delegateTo(DiagonalFlipCancel.builder()
          .setTarget(target)
          .build());
    } else if (absY > 500) {
      output
          .withSteer(Angles.flatCorrectionAngle(input.car, target) * 10)
          .withSlide()
          .withThrottle(1);
    } else {
      BallData relativeBall = NormalUtils.noseRelativeBall(input);
      delegateTo(FlipHelper.builder()
          .setTarget(target)
          .withFixedPitch(-1)
          .withFixedYaw(relativeBall.position.x)
          .build());
    }
  }

  private void cornerSpeedFlip(ControlsOutput output, DataPacket input) {
    JumpManager jumpManager = JumpManager.forCar(input.car);
    // Fast flip.
    float carX = Math.abs(input.car.position.x);
    double carTarget = input.car.position.distance(target);

    bot.botRenderer.renderTarget(target);

    if (carX > 1950) { // Tilt in
      bot.botRenderer.setBranchInfo("Tilt in");
      StateLogger.log(input, "Tilt in");

      double correctionAngle = Angles.flatCorrectionAngle(input.car, target);
      // Aim offset depending on the side you are on.
      correctionAngle += (DiagonalFlipCancel.MIN_DRIFT * -Math.signum(input.car.position.x));
      output
          .withThrottle(1.0f)
          .withBoost()
          .withSteer(correctionAngle * 10);
    } else if ((input.car.hasWheelContact || (input.car.position.z < 45 && jumpManager.getElapsedJumpTime() < .1))
        && carX > 1600) {
      bot.botRenderer.setBranchInfo("Jump");
      StateLogger.log(input, "Jump");
      delegateTo(DiagonalFlipCancel.builder()
          .setTarget(target)
          .build());
    } else if (carTarget > 470) {
      output
          .withBoost(!input.car.isSupersonic)
          .withThrottle(1.0)
          .withSteer(Angles.flatCorrectionAngle(input.car, target) * 5);
    } else {
      BallData relativeBall = NormalUtils.noseRelativeBall(input);
      delegateTo(FlipHelper.builder()
          .setTarget(target)
          .withFixedPitch(-1)
          .withFixedYaw(-relativeBall.position.x)
          .build());
    }
  }

  private void correctTowardBall(DataPacket input, ControlsOutput output) {
    output.withSteer(Angles.flatCorrectionAngle(input.car, input.ball.position));
  }
}
