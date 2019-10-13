package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import rlbot.Bot;

import java.awt.*;

public class DribbleTactician implements Tactician {

  private static final float PICK_UP_SPEED = 900f;

  private static final float BALANCE_POINT = 30f;
  private static final float CAR_HEIGHT = 25f;
  private static final float CAR_LENGTH = 100f;
  private static final float BALL_DIAMETER = 100f;
  private static final int MAINTAIN_SPEED = 900;

  private final BotRenderer renderer;

  DribbleTactician(Bot bot) {
    renderer = BotRenderer.forBot(bot);
  }

  @Override
  public void execute(ControlsOutput output, DataPacket input, Tactic nextTactic) {
    if (ballIsOnCar(input)) {
      balanceCar(input, output);
    } else if (ballIsOnGround(input)) {
      pickUp(input, output);
      turnToBall(input, output, 2.5);
    } else {
      catchBall(input, output);
      turnToBall(input, output, 2.5);
    }
  }

  private void balanceCar(DataPacket input, ControlsOutput output) {
    double ballSpeed = input.ball.velocity.flatten().magnitude();
    double carSpeed = input.car.velocity.flatten().magnitude();

    double carPos = input.car.position.y;
    double ballPos = input.ball.position.y;

    // Get the ball back on the hood
    if (ballPos > carPos + BALANCE_POINT) {
      renderer.addText("---- Catch Up ----", Color.PINK);
      output.withThrottle(1.0);
      turnToBall(input, output, 1);
    } else if (ballPos < carPos) {
      renderer.addText("Balance....", Color.PINK);
      // coast
      output.withThrottle(0);
      turnToBall(input, output, 1);
    } else {
      renderer.addText("Coast here!!!!!", Color.PINK);
      output.withThrottle((ballSpeed - carSpeed) / (BALANCE_POINT / 2));
      turnToBall(input, output, 1);
    }
  }

  private void turnToBall(DataPacket input, ControlsOutput output, double threshold) {
    Vector2 ballPosition = input.ball.position.flatten();
    CarData myCar = input.car;
    Vector2 carPosition = myCar.position.flatten();
    Vector2 carDirection = myCar.orientation.noseVector.flatten();

    // Subtract the two positions to get a vector pointing from the car to the ball.
    Vector2 carToBall = ballPosition.minus(carPosition);

    // How far does the car need to rotate before it's pointing exactly at the ball?
    double steerCorrectionRadians = carDirection.correctionAngle(carToBall);


    // Update Relative to distance to the ball.
    if (Math.abs(steerCorrectionRadians) > threshold) {
      output.withSteer(-steerCorrectionRadians / (threshold * 2));
      renderer.addText(String.format("Turn %f", steerCorrectionRadians), Color.green);
    }
  }

  // Called when ball is in the air.
  private void catchBall(DataPacket input, ControlsOutput output) {
    double ballSpeed = input.ball.velocity.flatten().magnitude();
    double carSpeed = input.car.velocity.flatten().magnitude();

    double carPos = input.car.position.y;
    double ballPos = input.ball.position.y;

    double ticksToBall = Math.abs(ballPos / carPos) - (carSpeed - ballSpeed);

    double ballHeight = input.ball.position.z;
    double ballDownVelocity = input.ball.velocity.z;

    double carHeight = CAR_HEIGHT + BALL_DIAMETER; // car + ball diameter
    double ticksToCar = (ballHeight - carHeight) / ballDownVelocity;
    // when will the ball hit the ground?

    // How fast is the ball going?

    // The ball is getting away or the ball will hit the ground before we get there.
    if (ballSpeed > carSpeed || ticksToBall > ticksToCar) {
      renderer.addText("Behind", Color.YELLOW);
      output.withThrottle(1.0f);

    // Good place to catch the ball. Match speed
    } else if (carSpeed >= ballSpeed && ticksToBall < ticksToCar){
      renderer.addText("Feather", Color.YELLOW);
      double relativeBallSpeed = carSpeed - ballSpeed;
      double relativePosition = ballPos - (carPos + (BALANCE_POINT * 1.5));

      // Throttle to maintain speed
      double speedRelativeThrottle = carSpeed / MAINTAIN_SPEED;

      double throttleToMatchBall = ((relativePosition - relativeBallSpeed) / relativeBallSpeed) * speedRelativeThrottle;

      output.withThrottle(speedRelativeThrottle + throttleToMatchBall);

    } else {
      renderer.addText("None", Color.BLACK);
      output.withThrottle(0.5f);
    }
    // Direction
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
}
