package com.eru.rlbot.testing;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.flags.Flags;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import rlbot.cppinterop.RLBotDll;
import rlbot.gamestate.BallState;
import rlbot.gamestate.GameInfoState;
import rlbot.gamestate.GameState;
import rlbot.gamestate.PhysicsState;

public class KickoffGameSetter {

  private static float kickOffTime;

  public static void track(DataPacket input) {
    if (input.ball.position.magnitude() < 500 && carIsNearBall(input) && Flags.slow_time_near_ball) {
      setSpeed(.1);
    } else {
      setSpeed(1);
    }

    if (!Flags.kickoff_game_enabled) {
      return;
    }

    if (!input.gameInfo.isKickoffPause() && kickOffTime == 0 && input.ball.position.magnitude() < 200) {
      kickOffTime = input.gameInfo.secondsElapsed();
    }

    boolean isNotNearCenter = input.ball.position.magnitude() > 3000;
    boolean isNotNearGoal = Math.abs(input.ball.position.x) > 100
        || Math.abs(input.ball.position.y) < Constants.HALF_LENGTH - 200;
    boolean timeHasEllapsed = kickOffTime != 0 && input.gameInfo.secondsElapsed() - kickOffTime > 4;

    if ((isNotNearCenter && isNotNearGoal) || timeHasEllapsed) {
      renderJudgement(input);
      kickOffTime = 0;
    }
  }

  private static boolean carIsNearBall(DataPacket input) {
    double closestCar = input.allCars.stream()
        .mapToDouble(car -> car.position.distance(input.ball.position))
        .min().getAsDouble();
    return closestCar < 500;
  }

  private static void setSpeed(double speed) {
    RLBotDll.setGameState(new GameState()
        .withGameInfoState(new GameInfoState()
            .withGameSpeed((float) speed))
        .buildPacket());
  }

  private static void renderJudgement(DataPacket input) {
    float loosingDirection = input.ball.position.y + input.ball.velocity.y;
    Optional<Map.Entry<Integer, Boolean>> loosingTeam = input.allCars.stream()
        .collect(Collectors.groupingBy(car -> car.team))
        .entrySet().stream()
        .collect(toImmutableMap(
            Map.Entry::getKey,
            entry -> entry.getValue().stream()
                .allMatch(car -> car.isDemolished)))
        .entrySet().stream()
        .filter(Map.Entry::getValue)
        .findFirst();

    if (loosingTeam.isPresent()) {
      loosingDirection = loosingTeam.get().getKey() == 0 ? -1 : 1;
    }

    RLBotDll.setGameState(new GameState()
        .withBallState(new BallState()
            .withPhysics(new PhysicsState()
                .withLocation(Vector3.of(
                    0,
                    Math.signum(loosingDirection) * Constants.HALF_LENGTH,
                    Constants.BALL_RADIUS)
                    .toDesired())
                .withVelocity(Vector3.of(
                    0,
                    loosingDirection,
                    Constants.BALL_RADIUS)
                    .toDesired())))
        .buildPacket());
  }

  private KickoffGameSetter() {
  }
}
