package com.eru.rlbot.bot.tactics;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.RelativeUtils;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.Controls;

/**
 * Picks up the ball for dribbling.
 */
public class PickUpTactician extends Tactician {

  private static final float PICK_UP_SPEED = 600f;

  private static final float PICK_UP_Y_OFFSET = 170f;

  public PickUpTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean canPickUp(DataPacket input) {
    BallData relativeBallData = RelativeUtils.noseRelativeBall(input);
    CarData relativecar = RelativeUtils.rollNormal(input);

    return input.ball.position.z < 170
        && input.car.position.distance(input.ball.position) < 1000
        && input.car.boost > 40
        && relativeBallData.position.y > 0 // Ball is in-front of the car
        && Math.abs(relativeBallData.position.x) < 300
        && relativecar.position.y > 0;
  }

  @Override
  void internalExecute(DataPacket input, Controls output, Tactic tactic) {
    CarData relativecar = RelativeUtils.rollNormal(input);

    if (relativecar.position.y > 0) {
      // Ball is rolling toward the car.
      doStationaryPickup(input, output, tactic);
    } else {
      // Ball is rolling away from the car.
      doRollingPickup(input, output, tactic);
    }

  }

  private void doRollingPickup(DataPacket input, Controls output, Tactic tactic) {

  }

  private void doStationaryPickup(DataPacket input, Controls output, Tactic tactic) {
    BallData relativeBallData = RelativeUtils.noseRelativeBall(input);

    if (relativeBallData.position.z > 130) {
      bot.botRenderer.setBranchInfo("Got it!");
      output.withThrottle(.02f);

      tacticManager.changeTactic(tactic, Tactic.TacticType.DRIBBLE);
    } else if (Math.abs(relativeBallData.position.x) < 5 && relativeBallData.position.y < PICK_UP_Y_OFFSET) {
      bot.botRenderer.setBranchInfo("Boost");
      output
          .withBoost()
          .withThrottle(1.0f);
    } else if (relativeBallData.position.magnitude() < 500 && relativeBallData.velocity.magnitude() > PICK_UP_SPEED) {
      bot.botRenderer.setBranchInfo("Slow down");
      float speedDiff = -relativeBallData.velocity.y - PICK_UP_SPEED;
      if (speedDiff < 200) {
        output.withThrottle(0);
      } else if (speedDiff > 200) {
        output.withThrottle(-1f);
      }
      output.withSteer(Angles.flatCorrectionAngle(input.car, input.ball.position));
    } else if (-relativeBallData.velocity.y < PICK_UP_SPEED || relativeBallData.position.magnitude() > 500) {
      bot.botRenderer.setBranchInfo("Get to the ball");
      output
          .withThrottle(1.0f)
          .withSteer(Angles.flatCorrectionAngle(input.car, input.ball.position));
    } else {
      bot.botRenderer.setBranchInfo("Coast %f", -relativeBallData.velocity.y);
      // Coast.
      output.withThrottle(.02f);
      output.withSteer(Angles.flatCorrectionAngle(input.car, input.ball.position));
    }
  }
}
