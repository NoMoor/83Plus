package com.eru.rlbot.testing;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.flags.GlobalDebugOptions;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import rlbot.cppinterop.RLBotDll;
import rlbot.gamestate.BallState;
import rlbot.gamestate.GameState;
import rlbot.gamestate.PhysicsState;

/**
 * Manager for a kickoff game.
 *
 * <p>When enabled, the ball is automatically placed in the goal of the team that loses the kickoff. Losing is defined
 * as the ball being on or heading toward their side of the field.
 */
public final class KickoffGame {

  private static float kickOffTime;
  private static int controllingPlayer = -1;

  /**
   * Tracks and scores the game.
   */
  public static void track(DataPacket input) {
    if (!GlobalDebugOptions.isKickoffGameEnabled()) {
      return;
    }

    if (controllingPlayer == -1) {
      controllingPlayer = input.car.serialNumber;
    } else if (controllingPlayer != input.car.serialNumber) {
      return;
    }

    BotRenderer.forIndex(controllingPlayer).renderStateSetWarning();

    if (!input.gameInfo.isKickoffPause() && kickOffTime == 0 && input.ball.position.magnitude() < 200) {
      kickOffTime = input.gameInfo.secondsElapsed();
    }

    boolean isNotNearCenter = input.ball.position.magnitude() > 3000;
    boolean isNotNearGoal = Math.abs(input.ball.position.x) > 100
        || Math.abs(input.ball.position.y) < Constants.HALF_LENGTH - 200;
    boolean timeHasEllapsed = kickOffTime != 0 && input.gameInfo.secondsElapsed() - kickOffTime > 4;

    if ((isNotNearCenter && isNotNearGoal) || timeHasEllapsed) {
      scorePoint(input);
      kickOffTime = 0;
    }
  }

  /**
   * Checks which side won and scores a goal for them.
   */
  private static void scorePoint(DataPacket input) {
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

  private KickoffGame() {}
}
