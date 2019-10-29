package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.NormalUtils;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class PickUpTactician extends Tactician {

  private static final float PICK_UP_SPEED = 600f;

  private static final float PICK_UP_Y_OFFSET = 170f;

  public PickUpTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean canPickUp(DataPacket input) {
    BallData relativeBallData = NormalUtils.noseNormal(input);
    CarData relativeCarData = NormalUtils.rollNormal(input);

    boolean canPickUp = input.ball.position.z < 170
        && input.car.position.distance(input.ball.position) < 1000
        && input.car.boost > 40
        && relativeBallData.position.y > 0 // Ball is infront of the car
        && Math.abs(relativeBallData.position.x) < 300
        && relativeCarData.position.y > 0;

    return canPickUp;
  }

  @Override
  void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    BallData relativeBallData = NormalUtils.noseNormal(input);

    if (relativeBallData.position.z > 130) {
      bot.botRenderer.setBranchInfo("Got it!");
      output.withThrottle(.02f);

      tacticManager.changeTactic(tactic, Tactic.Type.DRIBBLE);
      bot.botRenderer.addAlertText("Start dribble");
    } else if (Math.abs(relativeBallData.position.x) < 5 && relativeBallData.position.y < PICK_UP_Y_OFFSET) {
      bot.botRenderer.setBranchInfo("Boost");
      output
          .withBoost()
          .withThrottle(1.0f);
    } else if (relativeBallData.position.norm() < 500 && relativeBallData.velocity.norm() > PICK_UP_SPEED) {
      bot.botRenderer.setBranchInfo("Slow down");
      float speedDiff = -relativeBallData.velocity.y - PICK_UP_SPEED;
      if (speedDiff < 200) {
        output.withThrottle(0);
      } else if (speedDiff > 200) {
        output.withThrottle(-1f);
      }
      output.withSteer(Angles.flatCorrectionDirection(input.car, input.ball.position));
    } else if (-relativeBallData.velocity.y < PICK_UP_SPEED || relativeBallData.position.norm() > 500) {
      bot.botRenderer.setBranchInfo("Get to the ball");
      output
          .withThrottle(1.0f)
          .withSteer(Angles.flatCorrectionDirection(input.car, input.ball.position));
    } else {
      bot.botRenderer.setBranchInfo("Coast %f", -relativeBallData.velocity.y);
      // Coast.
      output.withThrottle(.02f);
      output.withSteer(Angles.flatCorrectionDirection(input.car, input.ball.position));
    }
  }
}
