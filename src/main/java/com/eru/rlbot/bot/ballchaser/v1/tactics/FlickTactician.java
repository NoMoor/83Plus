package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.NormalUtils;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;

public class FlickTactician extends Tactician {

  FlickTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  private int boostTicks;
  private int noBoostTicks;

  private boolean mustyFlickLock;
  private int mustyFlickTicks;

  @Override
  void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    if (input.ball.position.z < 135) {
      // Delegate this back.
      tacticManager.setTacticComplete(tactic);
      mustyFlickLock = false;
    }

    BallData relativeBallData = NormalUtils.noseNormal(input);
    if (mustyFlickLock || (relativeBallData.position.y > 60 && relativeBallData.position.y < 70 && relativeBallData.velocity.y > 20)) {
      mustyFlickLock = true;
      mustyFlick(input, output);
    } else if (relativeBallData.position.y > 90) {
      bot.botRenderer.setBranchInfo("Catch up");
      output
          .withThrottle(1f)
          .withBoost();
    } else {
      // Let the ball fall off the front.
      if (relativeBallData.velocity.y < 30) {
        bot.botRenderer.setBranchInfo("Coast %f", relativeBallData.velocity.y);
      } else {
        output.withThrottle(.02f);
      }
    }
  }

  @Override
  public boolean isLocked() {
    return mustyFlickLock;
  }

  private void mustyFlick(DataPacket input, ControlsOutput output) {
    BallData relativeBallData = NormalUtils.noseNormal(input);

    if (noBoostTicks++ < 5) {
      output.withThrottle(1.0f); // Keep driving straight.
      output.withBoost();
      bot.botRenderer.setBranchInfo("Don't boost %d", noBoostTicks);
    } else if (
        input.car.position.z > 80
            && input.car.velocity.z > -10
            && input.car.orientation.getNoseVector().y < -.02
            && (relativeBallData.position.z > 50 && relativeBallData.position.z < 500)
            && (relativeBallData.position.y < 0 && relativeBallData.position.y > -200)) {

      bot.botRenderer.setBranchInfo("Flick");
      // Flick
      output
          .withJump()
          .withPitch(1.0f);
      mustyFlickTicks++;
    } else if (mustyFlickTicks > 1 && mustyFlickTicks < 50) {
      mustyFlickTicks++;
      output.withPitch(1.0f); // Hold pitch
    } else {
      if (input.car.position.z > 150 && input.car.velocity.z < 200) {
        bot.botRenderer.setBranchInfo("Release Jump");
        output.withJump(false);
        output.withPitch(input.car.orientation.getNoseVector().z > -.7 ? -1 : 0);
      } else {
        bot.botRenderer.setBranchInfo("Hold Jump");
        output.withJump();

        recordJump(input);

        if (input.car.hasWheelContact) {
          // Calculate boost ticks.
          boostTicks = Math.max((int) ((relativeBallData.velocity.y - 90)/ 10), 0);
        } else if (boostTicks > 0) {
          if (input.car.orientation.getNoseVector().z > -1) {
            boostTicks--;
            output.withBoost();
          }
          // Keep level while boosting.
          output.withPitch(input.car.orientation.getNoseVector().z < 0 ? 1.0 : 0);
        } else {
          output.withPitch(input.car.orientation.getNoseVector().z > -.7 ? -1 : 0);
        }
      }
    }
  }

  private float recordingTime;
  private void recordJump(DataPacket input) {
    if (input.car.elapsedSeconds - recordingTime < 5) {
      return;
    }

    BallData relativeBallData = NormalUtils.noseNormal(input);
    recordingTime = input.car.elapsedSeconds;
    bot.botRenderer.addAlertText("Jump at %d %d", (int) relativeBallData.position.y, (int) relativeBallData.velocity.y);
  }
}
