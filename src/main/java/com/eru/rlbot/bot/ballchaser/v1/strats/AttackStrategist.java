package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.ballchaser.v1.tactics.*;
import com.eru.rlbot.common.input.DataPacket;

/** Responsible for dribbling, shooting, and passing. */
public class AttackStrategist extends Strategist {

  AttackStrategist(EruBot bot) {
    super(bot);
  }

  public static boolean shouldAttack(DataPacket input) {
    return true;
  }

  @Override
  public boolean assign(DataPacket input) {
    if (tacticManager.isTacticLocked()) {
      // Let the tactic finish it's motion.
      return true;
    }

    if (DribbleTactician.canDribble(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.DRIBBLE));
    } else if (KickoffTactician.isKickOff(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.KICKOFF));
    } else if (PickUpTactician.canPickUp(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.PICK_UP));
    } else if (CatchTactician.canCatch(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.DRIBBLE));
    } else {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.HIT_BALL));
    }

    return true;
  }

  @Override
  public Strategy.Type getType() {
    return Strategy.Type.ATTACK;
  }
}
