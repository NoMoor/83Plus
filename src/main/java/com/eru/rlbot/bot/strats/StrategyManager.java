package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.DemoChecker;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class StrategyManager {

  // Delay ~1 frame after a jump to allow ball propagation.
  private static final float RESET_DELAY = .1f;

  private static final float STRATEGY_UPDATE_INTERVAL = .25F;

  private static boolean grabBoostStrat = false;
  private static boolean wallRideStrat = false;
  private static boolean hitBallStrat = true;

  // DO NOT MODIFY
  private Map<Strategy.Type, Strategist> strategists = new HashMap<>();

  private Strategist active;

  private Agc bot;

  private float resetTime;
  private float lastStrategyUpdateTime;

  public StrategyManager(Agc bot) {
    this.bot = bot;
    for(Strategy.Type type : Strategy.Type.values()) {
      strategists.put(type, Strategy.strategistForBot(type, bot));
    }
    active = strategists.get(Strategy.Type.ATTACK);
  }

  public ControlsOutput executeStrategy(DataPacket input) {
    checkReset(input);

    boolean timedUpdate = lastStrategyUpdateTime == 0
        || input.car.elapsedSeconds - lastStrategyUpdateTime > STRATEGY_UPDATE_INTERVAL;
    boolean strategistUpdate = active == null || active.isComplete();
    boolean strategistLocked = active != null && active.tacticManager.isTacticLocked();

    if (!strategistLocked && (strategistUpdate || timedUpdate)) {
      lastStrategyUpdateTime = input.car.elapsedSeconds;
      updateStrategy(input);
    }

    ControlsOutput output = active.execute(input);

    bot.botRenderer.setStrategy(active);
    return output;
  }

  /** Called every x ticks to get the best strategy. */
  private void updateStrategy(DataPacket input) {
    Strategist newStrategist;
    if (DefendStrategist.shouldDefend(input)) {
      newStrategist = strategists.get(Strategy.Type.DEFEND);
    } else if (AttackStrategist.shouldAttack(input)) {
      newStrategist = strategists.get(Strategy.Type.ATTACK);
    } else {
      newStrategist = strategists.get(Strategy.Type.SUPPORT);
    }

    if (active != null) {
      active.abort();
    }

    active = newStrategist;
    active.assign(input);
  }

  private boolean checkReset(DataPacket input) {
    if (active != null && DemoChecker.wasDemoed()) {
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
