package com.eru.rlbot.bot.tactics;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.CarBallContactManager;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.RelativeUtils;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.maneuver.DiagonalFlipCancel;
import com.eru.rlbot.bot.maneuver.Flip;
import com.eru.rlbot.bot.tactics.kickoff.KickoffLocations;
import com.eru.rlbot.bot.tactics.kickoff.KickoffLocations.KickoffLocation;
import com.eru.rlbot.bot.tactics.kickoff.KickoffLocations.KickoffStation;
import com.eru.rlbot.bot.tactics.kickoff.KickoffTactic;
import com.eru.rlbot.bot.utils.Monitor;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.boost.BoostManager;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
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

  private KickoffTactic kickoffTactic;
  private Monitor monitor;
  private boolean done;

  KickoffTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean isKickoffStart(DataPacket input) {
    return isBallInKickoff(input.ball) /*&& input.car.groundSpeed < 10*/;
  }

  private static boolean isBallInKickoff(BallData ball) {
    return Math.abs(ball.position.x) < .1
        && Math.abs(ball.position.y) < .1
        && ball.position.z > 50 // This check is to see if state setting is being triggered.
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
    if (kickoffTactic == null) {
      kickoffTactic = KickoffTactic.defaultTactic(input);
    }

    bot.botRenderer.setBranchInfo("%s %d", kickoffTactic.type, kickoffTactic.location.priority);

    if (kickoffTactic.type == KickoffTactic.Type.FAKE) {
      return;
    } else if (kickoffTactic.type == KickoffTactic.Type.GRAB_BOOST) {
      double steeringCorrection = Angles.flatCorrectionAngle(input.car, kickoffTactic.target);
      output
          .withThrottle(1.0)
          .withSlide(input.car.groundSpeed > 500 && Math.abs(steeringCorrection) > 1)
          .withSteer(steeringCorrection)
          .withBoost(Math.abs(steeringCorrection) < 2);

      if (Math.abs(steeringCorrection) < .25) {
        delegateTo(Flip.builder()
            .setTarget(kickoffTactic.target)
            .flipEarly()
            .build());
      }
      return;
    }

    if (kickoffTactic.location.station == KickoffStation.LEFT_CENTER
        || kickoffTactic.location.station == KickoffStation.RIGHT_CENTER) {
      centerOffset(input, output);
    } else if (kickoffTactic.location.station == KickoffStation.CENTER) {
      centerKickOff(input, output);
    } else {
      cornerSpeedFlip(output, input);
    }
    bot.botRenderer.setBranchInfo(kickoffTactic.getDescriptor());
  }

  @Override
  protected void reset(DataPacket input) {
    Optional<KickoffLocation> optionalKickoffLocation = KickoffLocations.getKickoffLocation(input.car);

    if (optionalKickoffLocation.isPresent()) {
      clearDelegate();

      kickoffTactic = selectTactic(input, optionalKickoffLocation.get());
      monitor = Monitor.create(input);
    }
  }

  @Override
  public boolean isLocked() {
    return delegate != null || !done;
  }

  private KickoffTactic selectTactic(DataPacket input, KickoffLocation location) {
    if (!hasPriority(input, location)) {
      ImmutableList<CarData> nonPriorityTeammates = getTeammates(input).stream()
          .map(car -> Pair.of(car, KickoffLocations.getKickoffLocation(car)))
          .filter(pair -> pair.getSecond().isPresent())
          .sorted(Comparator.comparing(pair -> pair.getSecond().get().priority))
          .skip(1)
          .map(Pair::getFirst)
          .collect(toImmutableList());

      Vector3 target = BoostManager.getLargeBoosts().stream()
          .map(BoostPad::getLocation)
          .filter(boost -> nonPriorityTeammates.stream()
              .noneMatch(teammate -> boost.distance(teammate.position) < input.car.position.distance(boost)))
          .min(Comparator.comparing(boostLocation -> boostLocation.distance(input.car.position)))
          .get();
      return KickoffTactic.create(location, target, KickoffTactic.Type.GRAB_BOOST);
    } else {
      boolean hasTeammates = hasTeammates(input);
      Random random = new Random();
      if (location.station == KickoffStation.RIGHT || location.station == KickoffStation.LEFT) {
        boolean otherSideCovered = getTeammates(input).stream()
            .map(KickoffLocations::getKickoffLocation)
            .filter(Optional::isPresent)
            .map(optional -> optional.get().station)
            .anyMatch(station -> station == KickoffStation.RIGHT || station == KickoffStation.LEFT);

        if (!otherSideCovered && (hasTeammates || hasScoreDiff(input, 2)) && random.nextBoolean()) {
          // Aim for the goal.
          Vector3 goalAngle = input.ball.position.minus(Goal.opponentGoal(input.alliance).getNearPost(input.car));
          Vector3 target = input.ball.position.plus(goalAngle.toMagnitude(Constants.BALL_RADIUS * 1.3));
          return KickoffTactic.create(location, target, KickoffTactic.Type.SHOOT);
        } else if (true) {
          // Hit it around the opponent.
          Vector3 target = input.ball.position
              .addX(.8 * Constants.BALL_RADIUS * Math.signum(input.car.position.x))
              .addY(95 * Math.signum(input.car.position.y));
          return KickoffTactic.create(location, target, KickoffTactic.Type.CHALLENGE);
        } else {// Hit it around the opponent.
          Vector3 target = input.ball.position
              .addX(Constants.BALL_RADIUS * Math.signum(input.car.position.x))
              .addY(10 * -Math.signum(input.car.position.y));
          return KickoffTactic.create(location, target, KickoffTactic.Type.PUSH);
        }
      } else if (location.station == KickoffStation.CENTER) {
        // -1 or 1
        int xInt = -1 + (random.nextInt(2) * 2);
        Vector3 target = input.ball.position.addX(xInt * Constants.BALL_RADIUS / 4);
        return KickoffTactic.create(location, target, xInt < 0 ? KickoffTactic.Type.HOOK : KickoffTactic.Type.PUSH);
      } else {
        // -1 or 1
        if (random.nextInt(10) >= 7) {
          // Hook
          Vector3 target = input.ball.position
              .addX(-location.pushModifier * Constants.BALL_RADIUS / 3)
              .addY(Math.signum(location.location.y) * Constants.BALL_RADIUS);
          return KickoffTactic.create(location, target, KickoffTactic.Type.HOOK);
        } else {
          // PUsh
          Vector3 target = input.ball.position.addX(location.pushModifier * Constants.BALL_RADIUS / 4);
          return KickoffTactic.create(location, target, KickoffTactic.Type.PUSH);
        }
      }
    }
  }

  private void centerKickOff(DataPacket input, Controls output) {
    bot.botRenderer.setCarTarget(kickoffTactic.target);

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
          .setTarget(kickoffTactic.target)
          .build());
    } else if (absY > 800) {
      output
          .withSteer(Angles.flatCorrectionAngle(input.car, kickoffTactic.target) * 10)
          .withThrottle(1);
    } else {
      Vector3 relativeTarget =
          RelativeUtils.translateRelative(input.car.position, input.car.orientation.getNoseVector(), kickoffTactic.target);
      delegateTo(Flip.builder()
          .setTarget(kickoffTactic.target)
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
    bot.botRenderer.renderTarget(kickoffTactic.target);

    double absY = Math.abs(input.car.position.y);

    if (absY > 3250) {
      // Get Up to speed
      output
          .withSteer(kickoffTactic.location.turnModifier * .165)
          .withBoost()
          .withThrottle(1.0f);
    } else if (absY > 2500) {
      output
          .withBoost()
          .withThrottle(1.0f);

      delegateTo(DiagonalFlipCancel.builder()
          .setTarget(kickoffTactic.target)
          .build());
    } else if (absY > 500) {
      output
          .withSteer(Angles.flatCorrectionAngle(input.car, kickoffTactic.target) * 10)
          .withSlide()
          .withThrottle(1);
    } else {
      Vector3 relativeTarget =
          RelativeUtils.translateRelative(input.car.position, input.car.orientation.getNoseVector(), kickoffTactic.target);

      CarData closestOpponent = closestOpponentToBall(input);

      double pitch = -1;
      double yaw = -relativeTarget.x / (Constants.BALL_RADIUS / 2);
      if (closestOpponent != null) {
        Vector3 relativeOpponent =
            RelativeUtils.translateRelative(input.car.position, input.car.orientation.getNoseVector(), closestOpponent.position);
        // TODO: Check angles.
      }

      delegateTo(Flip.builder()
          .setTarget(kickoffTactic.target)
          .withFixedPitch(pitch)
          .withFixedYaw(yaw)
          .build());
    }
  }

  private void cornerSpeedFlip(Controls output, DataPacket input) {
    JumpManager jumpManager = JumpManager.forCar(input.car);
    // Fast flip.
    float carX = Math.abs(input.car.position.x);
    double carTarget = input.car.position.distance(kickoffTactic.target);

    bot.botRenderer.renderTarget(Color.RED, kickoffTactic.target);

    double jumpSpeed = 800;
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
          .setTarget(kickoffTactic.target)
          .build());
    } else if (input.car.angularVelocity.flatten().magnitude() > .2) {
      output
          .withBoost(!input.car.isSupersonic)
          .withThrottle(1.0)
          .withSteer(Angles.flatCorrectionAngle(input.car, kickoffTactic.target) * 10);
    } else {
      delegateTo(Flip.builder()
          .setTarget(kickoffTactic.target)
          .build());
    }
  }

  private ImmutableList<CarData> getTeammates(DataPacket input) {
    return input.allCars.stream()
        .filter(car -> car != input.car)
        .filter(car -> car.team == input.car.team)
        .collect(toImmutableList());
  }

  private boolean hasPriority(DataPacket input, KickoffLocation myLocation) {
    Optional<KickoffLocation> min = getTeammates(input).stream()
        .map(KickoffLocations::getKickoffLocation)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .min(Comparator.comparing(KickoffLocation::getPriority));

    if (!min.isPresent()) {
      return getTeammates(input).isEmpty();
    }

    int teammatePrio = min.get().priority;
    return teammatePrio > myLocation.priority;
  }

  private boolean hasTeammates(DataPacket input) {
    return input.allCars.stream()
        .filter(car -> car != input.car)
        .anyMatch(car -> car.team == input.car.team);
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
