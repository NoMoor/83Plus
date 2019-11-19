package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Goal;
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

  // Used for any flick to indicate that the car has jumped.
  private boolean jumpLock;

  private boolean mustyFlickLock;
  private int mustyFlickTicks;

  private boolean mognusFlickLock = true;
  private int mognusFlickTicks;
  private float rotationDirection;

  private static boolean flickChooser = false;

  @Override
  public boolean isLocked() {
    return mustyFlickLock || mognusFlickLock;
  }

  @Override
  void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    if (input.ball.position.z < 135) {
      // Delegate this back.
      tacticManager.setTacticComplete(tactic);
    }

    bot.botRenderer.addDebugText("Flick Chooser %b", flickChooser);

    BallData relativeBallData = NormalUtils.noseRelativeBall(input);
    if (!jumpLock && Math.abs(relativeBallData.position.x) > 15) {
      output
          .withSteer(relativeBallData.position.x / -25)
          .withThrottle(1.0f);
      return;
    }

    if (!mustyFlickLock && (mognusFlickLock || flickChooser)) {
      // Do the other flick next time.
      if (!mognusFlickLock) {
        flickChooser = false;
      }
      mognusFlickLock = true;
      bot.botRenderer.addDebugText("Mognus Flick");
      mognusFlick(input, output);
    } else {
      // Do the other flick next time.
      if (!mustyFlickLock) {
        flickChooser = true;
      }
      mustyFlickLock = true;
      bot.botRenderer.addDebugText("Musty Flick");
      mustyFlick(input, output);
    }
  }

  private void mognusFlick(DataPacket input, ControlsOutput output) {
    BallData relativeBallData = NormalUtils.noseRelativeBall(input);
    if (jumpLock || (relativeBallData.position.y > 35 && relativeBallData.position.y < 45 && relativeBallData.velocity.y > 10)) {
      jumpLock = true;

      // Keep steady
      if (noBoostTicks++ < 5) {
        output.withThrottle(1.0f); // Keep driving straight.
        output.withBoost();
        bot.botRenderer.setBranchInfo("Don't boost %d", noBoostTicks);
      } else if (
          (relativeBallData.position.y < -10 && relativeBallData.position.y > -200)
              && (Math.abs(relativeBallData.position.x) < 30)
              && (relativeBallData.position.z > 50 && relativeBallData.position.z < 160)) {

        bot.botRenderer.setBranchInfo("Flick");
        // Flick
        output
            .withJump()
            .withPitch(1.0f);
        mognusFlickTicks++;
      } else if (mognusFlickTicks > 1 && mognusFlickTicks < 50) {
        mognusFlickTicks++;
        output.withPitch(1.0f); // Hold pitch
      } else if (input.car.position.z > 150 && input.car.velocity.z < 200) {
          bot.botRenderer.setBranchInfo("Release Jump");
        output.withJump(false);
        output.withYaw(rotationDirection);
      } else {
        // Start the jump
        bot.botRenderer.setBranchInfo("Hold Jump");
        output.withJump();

        recordJump(input);

        if (input.car.hasWheelContact) {
          // Calculate boost ticks.
          boostTicks = Math.max((int) relativeBallData.velocity.y, 0);

          // Spin the opposite direction of the way you'd need to turn so the flick hits the ball back toward the goal.
          rotationDirection =
              (float) Math.signum(-Angles.flatCorrectionDirection(input.ball, Goal.opponentGoal(bot.team).center));
        } else if (boostTicks > 0) {
          boostTicks--;
          output.withBoost();

          // Keep level while boosting.
          output.withPitch(input.car.orientation.getNoseVector().z < 0 ? 1.0 : 0);
        } else {
          output.withYaw(rotationDirection); // Control the spin...
        }
      }
    } else if (relativeBallData.position.y > 50) {
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

  private void mustyFlick(DataPacket input, ControlsOutput output) {
    BallData relativeBallData = NormalUtils.noseRelativeBall(input);
    if (jumpLock || (relativeBallData.position.y > 60 && relativeBallData.position.y < 70 && relativeBallData.velocity.y > 20)) {
      jumpLock = true;

      // Keep steady
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
            if (input.car.orientation.getNoseVector().z > 0) {
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

  private float recordingTime;
  private void recordJump(DataPacket input) {
    if (input.car.elapsedSeconds - recordingTime < 5) {
      return;
    }

    BallData relativeBallData = NormalUtils.noseRelativeBall(input);
    recordingTime = input.car.elapsedSeconds;
    bot.botRenderer.addAlertText("Jump at %d %d", (int) relativeBallData.position.y, (int) relativeBallData.velocity.y);
  }
}
