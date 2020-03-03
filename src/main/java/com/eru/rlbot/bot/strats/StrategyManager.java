package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.StateSetChecker;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector2;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of the strategists.
 */
public class StrategyManager {

  // Delay ~1 frame after a jump to allow ball propagation.
  private static final float RESET_DELAY = .01f;
  private static final float STRATEGY_UPDATE_INTERVAL = .25F;

  // DO NOT MODIFY
  private Map<Strategy.Type, Strategist> strategists = new HashMap<>();

  private Strategist active;

  private ApolloGuidanceComputer bot;

  private float resetTime;
  private float lastStrategyUpdateTime;

  public StrategyManager(ApolloGuidanceComputer bot) {
    this.bot = bot;
    for (Strategy.Type type : Strategy.Type.values()) {
      strategists.put(type, Strategy.strategistForBot(type, bot));
    }
    active = strategists.get(Strategy.Type.ATTACK);
  }

  public Controls executeStrategy(DataPacket input) {
    checkReset(input);

    boolean timedUpdate = lastStrategyUpdateTime == 0
        || input.car.elapsedSeconds - lastStrategyUpdateTime > STRATEGY_UPDATE_INTERVAL;

    if (active == null || active.isComplete() || (timedUpdate && !active.tacticManager.isTacticLocked())) {
      lastStrategyUpdateTime = input.car.elapsedSeconds;

      Strategist newStrategist = getNewStrategist();
      if (active != null) {
        active.abort();
      }

      newStrategist.assign(input);
      active = newStrategist;
    }

    Controls output = active.execute(input);
    bot.botRenderer.setStrategy(active);

    return output;
  }

  /**
   * Called every x ticks to get the best strategy.
   */
  private Strategist getNewStrategist() {
    return strategists.get(Strategy.Type.ATTACK);
  }

  private boolean checkReset(DataPacket input) {
    if (active != null && StateSetChecker.wasDemoed(input.car)) {
      active.abort();
      active = null;
      resetTime = input.car.elapsedSeconds;
    }

    // Use a reset delay for unit tests to make sure the car isn't shaking.
    if (resetTime != 0 && resetTime + RESET_DELAY > input.car.elapsedSeconds) {
      return true;
    }

    resetTime = 0;
    return false;
  }

  // TODO: Move this elsewhere.
  private static Comparator<? super BoostPad> selectBoost(DataPacket input) {
    Vector2 noseVector = input.car.orientation.getNoseVector().flatten();
    Vector2 flatPosition = input.car.position.flatten();

    return (a, b) -> {
      // Angle diff in radians
      int angleValue = (int) (Math.abs(noseVector.correctionAngle(a.getLocation().flatten()))
          - Math.abs(noseVector.correctionAngle(b.getLocation().flatten())));
      // 750 units is worth a u-turn.
      int distanceValue = (int) (flatPosition.distance(a.getLocation().flatten())
          - flatPosition.distance(b.getLocation().flatten())) / 2000;
      return angleValue + distanceValue;
    };
  }
}
