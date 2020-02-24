package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.CarBallContactManager;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.Locations;
import com.eru.rlbot.bot.common.Monitor;
import com.eru.rlbot.bot.common.NormalUtils;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.bot.maneuver.DiagonalFlipCancel;
import com.eru.rlbot.bot.maneuver.FlipHelper;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.MoreCollectors;
import java.awt.Color;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.flat.TeamInfo;

public class KickoffTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("KickoffTactician");

  private Locations.KickoffLocation location;

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
    if (monitor != null && CarBallContactManager.isTouched(input)) {
      monitor.trackWhile(false, input.car);
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
      if ((hasTeammates(input) || hasScoreDiff(input, 2)) && random.nextBoolean()) {
        // Aim for the goal.
        Vector3 goalAngle = input.ball.position.minus(Goal.opponentGoal(input.team).getSameSidePost(input.car));
        return input.ball.position.plus(goalAngle.toMagnitude(Constants.BALL_RADIUS * 1.3));
        // TODO: Add in a speed flip challenge.
      } else {
        // Hit it around the opponent.
        return input.ball.position
            .addX(Constants.BALL_RADIUS * Math.signum(input.car.position.x))
            .addY(10 * -Math.signum(input.car.position.y));
      }
    } else if (location.station == Locations.KickoffStation.CENTER) {
      // -1 or 1
      int xInt = -1 + (random.nextInt(2) * 2);

      return input.ball.position.addX(xInt * Constants.BALL_RADIUS / 4);
    } else {
      // -1 or 1
      if (random.nextInt(10) >= 7) {
        // Hook
        return input.ball.position
            .addX(-location.pushModifier * Constants.BALL_RADIUS / 3)
            .addY(Math.signum(location.location.y) * Constants.BALL_RADIUS);
      } else {
        // PUsh
        return input.ball.position.addX(location.pushModifier * Constants.BALL_RADIUS / 4);
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
      Vector3 relativeTarget =
          NormalUtils.translateRelative(input.car.position, input.car.orientation.getNoseVector(), target);
      delegateTo(FlipHelper.builder()
          .setTarget(target)
          .withFixedPitch(-1)
          .withFixedYaw(-relativeTarget.x)
          .build());
    }
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
      Vector3 relativeTarget =
          NormalUtils.translateRelative(input.car.position, input.car.orientation.getNoseVector(), target);

      CarData closestOpponent = closestOpponentToBall(input);

      double pitch = -1;
      double yaw = -relativeTarget.x / (Constants.BALL_RADIUS / 2);
      if (closestOpponent != null) {
        Vector3 relativeOpponent =
            NormalUtils.translateRelative(input.car.position, input.car.orientation.getNoseVector(), closestOpponent.position);
        // TODO: Check angles.
      }

      delegateTo(FlipHelper.builder()
          .setTarget(target)
          .withFixedPitch(pitch)
          .withFixedYaw(yaw)
          .build());
    }
  }

  private void cornerSpeedFlip(ControlsOutput output, DataPacket input) {
    JumpManager jumpManager = JumpManager.forCar(input.car);
    // Fast flip.
    float carX = Math.abs(input.car.position.x);
    double carTarget = input.car.position.distance(target);

    bot.botRenderer.renderTarget(Color.RED, target);

    double jumpSpeed = 875;
    if (input.car.groundSpeed < jumpSpeed) { // Tilt in
      double correctionAngle = 0;
      bot.botRenderer.setBranchInfo("Tilt in");

      output
          .withThrottle(1.0f)
          .withBoost()
          .withSteer(correctionAngle * 10);
    } else if ((input.car.hasWheelContact || (input.car.position.z < 45 && jumpManager.getElapsedJumpTime() < .1))
        && carX > 1600) {

      // TODO: Select target here based on where the opponents are.
      bot.botRenderer.setBranchInfo("Jump");
      delegateTo(DiagonalFlipCancel.builder()
          .setTarget(target)
          .build());
    } else if (carTarget > 470) {
      output
          .withBoost(!input.car.isSupersonic)
          .withThrottle(1.0)
          .withSteer(Angles.flatCorrectionAngle(input.car, target) * 10);
    } else {
      output
          .withBoost(!input.car.isSupersonic)
          .withThrottle(1.0)
          .withSteer(Angles.flatCorrectionAngle(input.car, target) * 10);
    }
  }

  // TODO: Move these to a utility class.
  private boolean hasTeammates(DataPacket input) {
    return input.allCars.stream()
        .anyMatch(car -> car != input.car && car.team == input.car.team);
  }

  private boolean hasScoreDiff(DataPacket input, int scoreDifference) {
    TeamInfo ownTeam = input.teamInfos.stream()
        .filter(team -> input.car.team == team.teamIndex())
        .collect(MoreCollectors.onlyElement());
    TeamInfo otherTeam = input.teamInfos.stream()
        .filter(team -> ownTeam != team)
        .collect(MoreCollectors.onlyElement());

    return Math.abs(ownTeam.score() - otherTeam.score()) > scoreDifference;
  }
}
