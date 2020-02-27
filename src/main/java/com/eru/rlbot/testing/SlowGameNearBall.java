package com.eru.rlbot.testing;

import com.eru.rlbot.bot.flags.GlobalDebugOptions;
import com.eru.rlbot.common.input.DataPacket;
import rlbot.cppinterop.RLBotDll;
import rlbot.gamestate.GameInfoState;
import rlbot.gamestate.GameState;

/**
 * Slows the game when a car is near the ball to evaluate the contacts.
 */
public final class SlowGameNearBall {

  public static void track(DataPacket input) {
    if (carIsNearBall(input) && GlobalDebugOptions.isSlowTimeNearBallEnabled()) {
      setSpeed(GlobalDebugOptions.getSlowedGameSpeed());
    } else {
      setSpeed(10);
    }
  }

  private static boolean carIsNearBall(DataPacket input) {
    double closestCar = input.allCars.stream()
        .mapToDouble(car -> car.position.distance(input.ball.position))
        .min().getAsDouble();
    return closestCar < 500;
  }

  private static void setSpeed(int speed) {
    RLBotDll.setGameState(new GameState()
        .withGameInfoState(new GameInfoState()
            .withGameSpeed(speed / 10.0f))
        .buildPacket());
  }

  private SlowGameNearBall() {
  }
}
