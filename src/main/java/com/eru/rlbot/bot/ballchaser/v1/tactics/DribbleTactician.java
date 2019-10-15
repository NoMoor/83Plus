package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.CarNormalUtils;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import rlbot.Bot;

import java.awt.*;

import static com.eru.rlbot.bot.common.Constants.*;

public class DribbleTactician implements Tactician {

  private static final float IDEAL_DRIBBLE_SPEED = 1000;

  private static final float PICK_UP_SPEED = 900f;

  private static final float SPEED_UP_BALANCE_POINT = CAR_LENGTH / 2f;
  private static final float STEADY_BALANCE_POINT = CAR_LENGTH / 5.4f;
  private static final float BALL_DIAMETER = BALL_SIZE;

  private final BotRenderer renderer;
  private boolean catchUp;

  DribbleTactician(Bot bot) {
    renderer = BotRenderer.forBot(bot);
  }

  @Override
  public void execute(ControlsOutput output, DataPacket input, Tactic nextTactic) {
    BallData relativeBallData = CarNormalUtils.noseNormalLocation(input);

    if (ballIsOnCar(input)) {
      balanceCar(input, relativeBallData, output);
    } else if (ballIsOnGround(input)) {
      pickUp(input, output);
      turnToBall(relativeBallData, output);
    } else {
      catchBall(input, relativeBallData, output);
      turnToBall(relativeBallData, output);
    }
  }

  private void balanceCar(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    if (catchUp ||
      (relativeBallData.position.y > STEADY_BALANCE_POINT * 3
           && getThrottleAccel(input.car.velocity.flatten().magnitude()) > relativeBallData.velocity.y)) {
      catchUp = relativeBallData.position.y < STEADY_BALANCE_POINT * 3;
      renderer.addText("Catch Up!!!!!", Color.RED);
      output.withThrottle(1.0F);
      output.withBoost();
      turnToBall(relativeBallData, output);
    } else if (relativeBallData.position.y < -STEADY_BALANCE_POINT * 3.5) { // TODO: Make this speed dependent
      renderer.addText("Break!!!", Color.RED);
      // coast
      output.withThrottle(-1);
      turnToBall(relativeBallData, output);
    } else {
      output.withThrottle(getBalanceThrottle(relativeBallData, input.car));
      turnToBall(relativeBallData, output);
    }
  }

  private float getBalanceThrottle(BallData relativeBallData, CarData carData) {
    double nosewardVelocity = relativeBallData.velocity.y;
    double carVelocity = carData.velocity.flatten().magnitude();
    double immediateThrottle = getThrottleAccel(carVelocity);

    // Ball is coming toward the car. Break. TODO: In the future, figure out how fast we want to go here.
    if (Math.abs(nosewardVelocity) < 20
        && IDEAL_DRIBBLE_SPEED - 50 < carVelocity
        && carVelocity < IDEAL_DRIBBLE_SPEED + 50) {
      renderer.addText("Perfect speed", Color.GREEN);
      return .02f;
    }

    if (nosewardVelocity < -100) {
      renderer.addText("Break!!!!!", Color.RED);
      return -1f;
    } else if (nosewardVelocity < -60) {
      renderer.addText("Coast: Slow", Color.ORANGE);
      return 0f;
    } else if (carVelocity + nosewardVelocity > IDEAL_DRIBBLE_SPEED) {
      renderer.addText("Coast: Slow Ball down", Color.GREEN);
      return relativeBallData.position.y > -IDEAL_DRIBBLE_SPEED ? 1f : 0.02f;
    } if (carVelocity + nosewardVelocity < IDEAL_DRIBBLE_SPEED
          && nosewardVelocity <= immediateThrottle / 2.5
          && relativeBallData.position.y < SPEED_UP_BALANCE_POINT) {
      renderer.addText("Coast: Let Slip", Color.GREEN);
      return nosewardVelocity < 30 ? 0 : 0.02f; // If the ball isn't going fast, drop throttle and let slow down.
    } else if (nosewardVelocity > 0) {
      if (nosewardVelocity > immediateThrottle) {
        renderer.addText("Slow up...", Color.RED);
        return 1f;
      } else if (nosewardVelocity < (immediateThrottle / 2)) {
        renderer.addText("Coast: Let Slip2", Color.GREEN);
        return (carVelocity < IDEAL_DRIBBLE_SPEED) ? 0.3f : 0.5f;
      } else if (nosewardVelocity > (immediateThrottle / 2)) {
        renderer.addText("Coast: Slow Catch", Color.ORANGE);
        return .5f;
      }
    }

    renderer.addText("Coast:", Color.GREEN);
    return 0.02f;
  }

  private double getThrottleAccel(double carVelocity) {
    if (carVelocity < 1400) {
      return 1600 - carVelocity;
    } else if (carVelocity < 1410) {
      return (carVelocity - 1400) * 20;
    } else if (carVelocity >= 1410 && carVelocity < 2300){
      // Cannot throttle faster.
      return 0;
    }
    throw new IllegalArgumentException("What happened?!");
  }

  private void turnToBall(BallData relativeBallData, ControlsOutput output) {
    if (CAR_WIDTH / 2 < Math.abs(relativeBallData.position.x)) {
      float turn = (relativeBallData.position.x / (CAR_WIDTH / 2));
      renderer.addText(String.format("Turn %s %f", turn < 0 ? "LEFT" : "RIGHT", turn), Color.PINK);
      output.withSteer(turn);
    }
  }

  // Called when ball is in the air.
  private void catchBall(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double ballSpeed = input.ball.velocity.flatten().magnitude();
    double carSpeed = input.car.velocity.flatten().magnitude();

    double distanceToCatchPoint = relativeBallData.position.y - SPEED_UP_BALANCE_POINT;

    // How long to get to x/y position of the ball?
    double ticksToBall = distanceToCatchPoint
        / -relativeBallData.velocity.y; // Negate the velocity since moving toward the ball is a relative negative velocity.

    double ballHeightAboveCar = relativeBallData.position.z - (CAR_HEIGHT + BALL_DIAMETER);
    double ballDownVelocity = relativeBallData.velocity.z;

    // How long for the ball to land on the car?
    double ticksToCar = timeToLand(ballHeightAboveCar, ballDownVelocity);

    if (ticksToBall < 0) {
      // We are going to overrun the ball.
      renderer.addText("Slow Down!", Color.RED);
      output.withThrottle(distanceToCatchPoint > 0 ? 1f : 0f);
    } else if (ballSpeed > carSpeed || ticksToBall > ticksToCar) {
      // The ball is getting away or the ball will hit the ground before we get there.
      renderer.addText("Behind", Color.YELLOW);
      output.withThrottle(1.0f);

    // Good place to catch the ball. Match speed
    } else if (carSpeed >= ballSpeed && ticksToBall < ticksToCar){
      renderer.addText("Feather", Color.YELLOW);

      // Throttle to maintain speed
      output.withThrottle(.02);

    } else {
      renderer.addText("None", Color.BLACK);
      output.withThrottle(0.5f);
    }
    // Direction
    renderer.addText(String.format("TTC: %f", ticksToCar), Color.white);
    renderer.addText(String.format("TTB: %f", ticksToBall), Color.white);
  }

  private boolean ballIsOnGround(DataPacket input) {
    boolean onGround = input.ball.position.z < 150;
    if (onGround) {
      renderer.addText("Ball on Ground", Color.RED);
    } else {
      renderer.addText("Ball in Air", Color.GREEN);
    }
    return onGround;
  }

  private boolean ballIsOnCar(DataPacket input) {
    boolean heightOnCar = input.ball.position.z > 100 && input.ball.position.z < 200;
    boolean locationOnCar = input.ball.position.y < input.car.position.y + CAR_LENGTH
      && input.ball.position.y > input.car.position.y - CAR_LENGTH;

    if (heightOnCar && locationOnCar) {
      renderer.addText("Ball on Car", Color.GREEN);
    }
    return heightOnCar && locationOnCar;
  }

  private void pickUp(DataPacket input, ControlsOutput output) {
    if (input.ball.position.y > input.car.position.y && input.car.velocity.flatten().magnitude() < PICK_UP_SPEED) {
      output.withThrottle(1.0f);
    }
  }

  private double timeToLand(double height, double verticalSpeed) {
    double firstTerm = verticalSpeed / GRAVITY;
    double secondTerm = Math.sqrt((verticalSpeed * verticalSpeed) + (2 * GRAVITY * height)) / GRAVITY;

    return Math.max(firstTerm + secondTerm, firstTerm - secondTerm);
  }

  private double timeToCatch(double distance, double acceleration) {
    return Math.sqrt((2 * acceleration * distance)) / acceleration;
  }
}
