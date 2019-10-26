package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.ballchaser.v1.tactics.DribbleTactician;
import com.eru.rlbot.bot.ballchaser.v1.tactics.KickoffTactician;
import com.eru.rlbot.bot.ballchaser.v1.tactics.Tactic;
import com.eru.rlbot.bot.ballchaser.v1.tactics.TacticManager;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

/** Responsible for dribbling, shooting, and passing. */
public class AttackStrategist implements Strategist {

  private final TacticManager tacticManager;
  private final EruBot bot;

  AttackStrategist(EruBot bot) {
    this.bot = bot;
    this.tacticManager = new TacticManager(bot);
  }

  public static boolean shouldAttack(DataPacket input) {
    return true;
  }

  @Override
  public boolean assign(DataPacket input) {
    tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.WAVE_DASH));
    if (true) return true;

    if (KickoffTactician.isKickOff(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.KICKOFF));
    } if (DribbleTactician.canDribble(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.DRIBBLE));
    } else {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.HIT_BALL));
    }

    return true;
  }

  @Override
  public boolean isComplete(DataPacket input) {
    return false;
  }

  @Override
  public void abort() {
    tacticManager.clearTactics();
  }

  @Override
  public ControlsOutput execute(DataPacket input) {
    // TODO: Decide how / when to set new tactics.
    assign(input);

    ControlsOutput output = new ControlsOutput();
    tacticManager.execute(input, output);
    return output;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.ATTACK;
  }
}
