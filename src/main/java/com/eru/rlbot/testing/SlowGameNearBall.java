package com.eru.rlbot.testing;

import com.eru.rlbot.bot.flags.GlobalDebugOptions;
import com.eru.rlbot.common.input.DataPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.cppinterop.RLBotDll;
import rlbot.gamestate.GameInfoState;
import rlbot.gamestate.GameState;

/**
 * Slows the game when a car is near the ball to evaluate the contacts.
 */
public final class SlowGameNearBall {

  private static final Logger logger = LogManager.getLogger("FramePrediction");

  private static final int FULL_SPEED = 10;

  private static volatile int lastSetSpeed = FULL_SPEED;

  /**
   * Tracks if a car is close to the ball and - if enabled - adjusts the game speed.
   */
  public static void track(DataPacket input) {
    if (carIsNearBall(input) && GlobalDebugOptions.isSlowTimeNearBallEnabled()) {
      setSpeed(GlobalDebugOptions.getSlowedGameSpeed());
    } else if (lastSetSpeed != FULL_SPEED) {
      setSpeed(FULL_SPEED);
    }
  }

  private static boolean carIsNearBall(DataPacket input) {
    double closestCar = input.allCars.stream()
        .mapToDouble(car -> car.position.distance(input.ball.position))
        .min().getAsDouble();
    return closestCar < 500 || true;
  }

  private synchronized static void setSpeed(int speed) {
    if (lastSetSpeed == speed) {
      return;
    }

    lastSetSpeed = speed;
    float speedDecimal = speed / 10.0f;

    logger.warn("Setting game speed to %.0f%%", speedDecimal * 100);
    RLBotDll.setGameState(new GameState()
        .withGameInfoState(new GameInfoState()
            .withGameSpeed(speedDecimal))
        .buildPacket());
  }

  private SlowGameNearBall() {
  }
}
