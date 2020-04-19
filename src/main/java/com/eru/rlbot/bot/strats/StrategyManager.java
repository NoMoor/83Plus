package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.StateSetChecker;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.plan.Marker;
import com.eru.rlbot.bot.tactics.KickoffTactician;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Keeps track of the strategists.
 */
public class StrategyManager {

  private static final Logger logger = LogManager.getLogger("StrategyManager");

  // Delay ~1 frame after a jump to allow ball propagation.
  private static final float RESET_DELAY = .02f;
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
    if (checkReset(input)) {
      return Controls.create();
    }

    boolean timedUpdate = lastStrategyUpdateTime == 0
        || input.car.elapsedSeconds - lastStrategyUpdateTime > STRATEGY_UPDATE_INTERVAL;

    Marker.get(input.serialNumber).markNext(input);

    if (KickoffTactician.isKickoffStart(input)) {
      if (active == null || active.getType() != Strategy.Type.ATTACK) {
        active = strategists.get(Strategy.Type.ATTACK);
        active.assign(input);
      }
    } else if (active == null) {
      lastStrategyUpdateTime = input.car.elapsedSeconds;

      Strategist newStrategist = getNewStrategist();
      if (active != null) {
        active.abort();
      }

      newStrategist.assign(input);
      active = newStrategist;
    } else if (active.isComplete(input)) {
      Strategist newStrategist = strategists.get(active.getDelegate());

      logger.debug("New Strategy: {} Completed: {}", active.getType(), newStrategist.getType());
      active.abort();
      newStrategist.assign(input);
      active = newStrategist;
    } else if (timedUpdate && !active.tacticManager.isTacticLocked()) {
      active.assign(input);
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
}
