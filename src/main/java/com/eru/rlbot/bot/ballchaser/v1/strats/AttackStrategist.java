package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.ballchaser.v1.tactics.*;
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

    if (DribbleTactician.canDribble(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.DRIBBLE));
    } else if (KickoffTactician.isKickOff(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.KICKOFF));
    } else if (PickUpTactician.canPickUp(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.PICK_UP));
    } else if (CatchTactician.canCatch(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.DRIBBLE));
    } else if (DribbleTactician.canDribble(input)) {
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
