package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.CarBallContactManager;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.KickoffLocations;
import com.eru.rlbot.bot.common.KickoffLocations.KickoffLocation;
import com.eru.rlbot.bot.common.KickoffLocations.KickoffStation;
import com.eru.rlbot.bot.common.RelativeUtils;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.maneuver.DiagonalFlipCancel;
import com.eru.rlbot.bot.maneuver.Flip;
import com.eru.rlbot.bot.utils.Monitor;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.MoreCollectors;
import java.awt.Color;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.flat.TeamInfo;

/**
 * Manages kickoffs for this bot.
 */
public class KickoffTactician extends Tactician {

  private static final Logger logger = LogManager.getLogger("KickoffTactician");

  private KickoffLocation location;

  private Vector3 target;
  private Monitor monitor;

  private boolean done;

  KickoffTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
    location = KickoffLocation.defaultLocation(bot.team);
  }

  public static boolean isKickoffStart(DataPacket input) {
    return isBallInKickoff(input.ball) /*&& input.car.groundSpeed < 10*/;
  }

  private static boolean isBallInKickoff(BallData ball) {
    return Math.abs(ball.position.x) < .1
        && Math.abs(ball.position.y) < .1
        && ball.velocity.magnitude() < 100
        && ball.position.z < 120;
  }

  @Override
  void execute(DataPacket input, Controls output, Tactic tactic) {
    super.execute(input, output, tactic);
    if (monitor != null && CarBallContactManager.isTouched(input)) {
      monitor.trackWhile(false, input.car);
    }
    if (!isBallInKickoff(input.ball)) {
      done = true;
    }
  }

  @Override
  public void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    checkTactic(input, tactic);
    if (target == null) {
      target = input.ball.position;
    }

    if (location.station == KickoffStation.LEFT_CENTER
        || location.station == KickoffStation.RIGHT_CENTER) {
      centerOffset(input, output);
    } else if (location.station == KickoffStation.CENTER) {
      centerKickOff(input, output);
    } else {
      cornerSpeedFlip(output, input);
    }
    bot.botRenderer.setBranchInfo(location.name());
  }

  @Override
  protected void reset(DataPacket input) {
    Optional<KickoffLocation> optionalKickoffLocation = KickoffLocations.getKickoffLocation(input.car);
    location = optionalKickoffLocation.orElse(location);

    if (optionalKickoffLocation.isPresent()) {
      clearDelegate();
      target = selectTarget(input);
      monitor = Monitor.create(input);
    }
  }

  @Override
  public boolean isLocked() {
    return delegate != null || !done;
  }

  private Vector3 selectTarget(DataPacket input) {
    Random random = new Random();
    if (location.station == KickoffStation.RIGHT || location.station == KickoffStation.LEFT) {
      if ((hasTeammates(input) || hasScoreDiff(input, 2)) && random.nextBoolean()) {
        // Aim for the goal.
        Vector3 goalAngle = input.ball.position.minus(Goal.opponentGoal(input.alliance).getSameSidePost(input.car));
        return input.ball.position.plus(goalAngle.toMagnitude(Constants.BALL_RADIUS * 1.3));
        // TODO: Add in a speed flip challenge.
      } else {
        // Hit it around the opponent.
        return input.ball.position
            .addX(Constants.BALL_RADIUS * Math.signum(input.car.position.x))
            .addY(10 * -Math.signum(input.car.position.y));
      }
    } else if (location.station == KickoffStation.CENTER) {
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

  private void centerKickOff(DataPacket input, Controls output) {
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
          RelativeUtils.translateRelative(input.car.position, input.car.orientation.getNoseVector(), target);
      delegateTo(Flip.builder()
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

  private void centerOffset(DataPacket input, Controls output) {
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
          RelativeUtils.translateRelative(input.car.position, input.car.orientation.getNoseVector(), target);

      CarData closestOpponent = closestOpponentToBall(input);

      double pitch = -1;
      double yaw = -relativeTarget.x / (Constants.BALL_RADIUS / 2);
      if (closestOpponent != null) {
        Vector3 relativeOpponent =
            RelativeUtils.translateRelative(input.car.position, input.car.orientation.getNoseVector(), closestOpponent.position);
        // TODO: Check angles.
      }

      delegateTo(Flip.builder()
          .setTarget(target)
          .withFixedPitch(pitch)
          .withFixedYaw(yaw)
          .build());
    }
  }

  private void cornerSpeedFlip(Controls output, DataPacket input) {
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
