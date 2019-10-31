package com.eru.rlbot.bot.ballchaser.v1.strats;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.ballchaser.v1.tactics.*;
import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.DllHelper;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.Optional;

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

    Optional<BallPrediction> ballPredictionOptional = DllHelper.getBallPrediction();
    if (KickoffTactician.isKickOff(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.KICKOFF));
    } else if (TakeTheShotTactician.takeTheShot(input) && ballPredictionOptional.isPresent()) {
      BallPrediction ballPrediction = ballPredictionOptional.get();
      Vector3 target = Vector3.of(ballPrediction.slices(0).physics().location());

      // TODO: Put this back in but it doesn't work when using rlbottraining.
//      for (int i = 0 ; i < ballPrediction.slicesLength() ; i = Math.min(i + 5, ballPrediction.slicesLength() - 1)) {
//        PredictionSlice slice = ballPrediction.slices(i);
//
//        Vector3 slicePosition = Vector3.of(slice.physics().location());
//
//        if (slicePosition.z < 300) {
//          float timeToLocation =
//              Accels.timeToDistance(
//                  input.car.velocity.flatten().norm(),
//                  input.car.position.flatten().distance(slicePosition.flatten()));
//          if (timeToLocation < slice.gameSeconds() - input.car.elapsedSeconds) {
//            // Target Acquired.
//            target = slicePosition;
//            break;
//          }
//        }
//      }

      tacticManager.setTactic(new Tactic(target, Tactic.Type.STRIKE));
    } else if (DribbleTactician.canDribble(input)) {
      tacticManager.setTactic(new Tactic(input.ball.position, Tactic.Type.DRIBBLE));
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
