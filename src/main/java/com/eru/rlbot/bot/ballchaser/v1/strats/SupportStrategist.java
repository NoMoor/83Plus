package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.ballchaser.v1.tactics.Tactic;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.Bot;

/** Strategy responsible for patience. */
public class SupportStrategist extends Strategist {

  private static final Vector3 LEFT_MID = Vector3.of(3584, 0, 73);
  private static final Vector3 RIGHT_MID = Vector3.of(-3584, 0, 73);

  public SupportStrategist(EruBot bot) {
    super(bot);
  }

  @Override
  public boolean assign(DataPacket input) {
    // TODO: If we are supporting, pick up boost nearby.

    if (input.car.boost < 50) {
      tacticManager.setTactic(new Tactic(input.car.position.x > 0 ? LEFT_MID : RIGHT_MID, Tactic.Type.ROTATE));
    } else {
      // TODO: Update to work with both teams.
      tacticManager.setTactic(new Tactic(input.ball.position.minus(Vector3.of(0, 1500, 0)), Tactic.Type.ROTATE));
    }

    return true;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.SUPPORT;
  }
}
