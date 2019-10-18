package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.CarNormalUtils;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import rlbot.Bot;

public class KickoffTactician implements Tactician {

  public static final float NOSE_DOWN = -1.0f;
  private final BotRenderer botRenderer;
  private boolean flipLock;

  private StartLocation location;

  KickoffTactician(Bot bot) {
    botRenderer = BotRenderer.forBot(bot);
  }

  public static boolean isKickOff(DataPacket input) {
    return Math.abs(input.ball.position.x) < 1
        && Math.abs(input.ball.position.x) < 1
        && input.ball.velocity.magnitude() < 10;
  }

  private enum StartLocation {
    LEFT, LEFT_CENTER, CENTER, RIGHT_CENTER, RIGHT;
  }

  private void setStartLocation(CarData car) {
    if (car.groundSpeed > 4) {
      return;
    }

    StartLocation newLocation = null;
    if (Math.abs(car.position.x) < 1) {
      newLocation = StartLocation.CENTER;
    } else if (Math.abs(car.position.y) > 3840) {
      newLocation = car.position.x > 0 ? StartLocation.RIGHT : StartLocation.LEFT;
    } else {
      newLocation = car.position.x > 0 ? StartLocation.RIGHT_CENTER : StartLocation.LEFT_CENTER;
    }
    if (location == null || newLocation != location) {
      location = newLocation;
    }
  }

  @Override
  public void execute(ControlsOutput output, DataPacket input, Tactic nextTactic) {
    setStartLocation(input.car);
    botRenderer.setBranchInfo(String.format("%s", location));

    centerStart(output, input);
  }

  private void centerStart(ControlsOutput output, DataPacket input) {
    BallData relativeData = CarNormalUtils.noseNormalLocation(input);

    tryFlipLock(input, relativeData);

    if (flipLock) {
      if (JumpManager.canFlip() && (Math.abs(relativeData.position.x) * 1.7 > Math.abs(relativeData.position.y))) {
        output.withPitch(NOSE_DOWN);
        output.withYaw(relativeData.position.x);
        output.withJump();
        flipLock = false;
      } else {
        output.withJump(input.car.position.z <= Constants.CAR_AT_REST);
        output.withYaw(getLocationYaw());
      }
    } else {
      if (!input.car.hasWheelContact) {
        output.withPitch(NOSE_DOWN);
        if (Math.abs(relativeData.position.x) > 100) {
          output.withRoll(getLocationYaw());
          output.withYaw(-getLocationYaw());
        }
      }
      if (input.car.groundSpeed < 200) {
        output.withSteer(getLocationTurnIn());
      } else {
        fullThrottle(input, output);
      }
    }

    if (Math.abs(output.getSteer()) < 1
        || (Math.abs(input.car.orientation.noseVector.z) < .5 && relativeData.position.y > 0)) {
      output.withBoost();
    }
  }

  private float getLocationTurnIn() {
    if (location == null) return 0f;
    switch (location) {
      case LEFT:
      case LEFT_CENTER:
        return 1.0f;
      case RIGHT_CENTER:
        return -1.0f;
      case RIGHT:
      default:
      case CENTER:
        return 0f;
    }
  }

  private float getLocationYaw() {
    if (location == null) return 1.0f;
    switch (location) {
      case LEFT:
      case LEFT_CENTER:
      case CENTER:
        return 1.0f;
      case RIGHT_CENTER:
      case RIGHT:
      default:
        return -1.0f;
    }
  }

  private void tryFlipLock(DataPacket input, BallData relativeData) {
    if (flipLock) {
      // Check reset
      flipLock = input.car.velocity.flatten().magnitude() > 100f;
    } else if (Math.abs(relativeData.position.x) < 200 && input.car.velocity.flatten().magnitude() > 1000) {
      flipLock = true;
    }
  }

  private void fullThrottle(DataPacket input, ControlsOutput output) {
    output.withSteer(Angles.flatCorrectionDirection(input.car, input.ball.position));
    output.withThrottle(1.0);
  }
}
