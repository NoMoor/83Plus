package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.CarNormalUtils;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class PickUpTactician extends Tactician {

  private static final float PICK_UP_SPEED = 300f;

  private static final float PICK_UP_Y_OFFSET = 170f;

  public PickUpTactician(EruBot bot) {
    super(bot);
  }

  public static boolean canPickUp(DataPacket input) {
    return true;
  }

  @Override
  boolean execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    BallData relativeBallData = CarNormalUtils.noseNormalLocation(input);

    if (relativeBallData.position.z > 130) {
      bot.botRenderer.setBranchInfo("Got it!");
      output.withThrottle(.02f);

      // TODO: Pass this to the dribbler...
      return true;
    } else if (relativeBallData.position.y < PICK_UP_Y_OFFSET) {
      bot.botRenderer.setBranchInfo("Boost");
      output.withBoost();
    } else if (-relativeBallData.velocity.y < PICK_UP_SPEED) {
      bot.botRenderer.setBranchInfo("Line up");
      output
          .withThrottle(1.0f)
          .withSteer(Angles.flatCorrectionDirection(input.car, input.ball.position));
    } else {
      bot.botRenderer.setBranchInfo("Coast");
      // Coast.
      output.withThrottle(.02f);
    }

    return false;
  }
}
