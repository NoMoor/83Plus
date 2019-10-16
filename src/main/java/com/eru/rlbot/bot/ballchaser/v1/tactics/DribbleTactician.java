package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.CarNormalUtils;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector2;
import rlbot.Bot;

import java.awt.*;

import static com.eru.rlbot.bot.common.Constants.*;

public class DribbleTactician implements Tactician {

  private static final float IDEAL_DRIBBLE_SPEED = 1400;

  private static final float PICK_UP_SPEED = 800f;

  private static final float SPEED_UP_BALANCE_POINT = CAR_LENGTH / 2f;
  private static final float BALL_DIAMETER = BALL_SIZE;

  private final BotRenderer renderer;

  DribbleTactician(Bot bot) {
    renderer = BotRenderer.forBot(bot);
  }

  @Override
  public void execute(ControlsOutput output, DataPacket input, Tactic nextTactic) {
    BallData relativeBallData = CarNormalUtils.noseNormalLocation(input);

    if (ballIsOnGround(input)) {
      pickUp(input, relativeBallData, output);
      angleToBall(input, output);
    } else if (ballIsOnCar(relativeBallData)) {
      balanceFrontBack(input, relativeBallData, output);
      // TODO: Pick a target and take it to the target.
      balanceLeftRight(input, relativeBallData, output);
    } else {
      catchBall(input, relativeBallData, output);
      angleToBall(input, output); // Set up the angle here too.
    }
  }

  private void angleToBall(DataPacket input, ControlsOutput output) {
    // Subtract the two positions to get a vector pointing from the car to the ball.
    Vector2 carToBall = input.ball.position.flatten().minus(input.car.position.flatten());

    // How far does the car need to rotate before it's pointing exactly at the ball?
    double correctionAngle = input.car.orientation.noseVector.flatten().correctionAngle(carToBall);

    if (Math.abs(correctionAngle) > .05) {
      renderer.addText(String.format("Turn %s %f", correctionAngle < 0 ? "LEFT" : "RIGHT", correctionAngle), Color.PINK);
      output.withSteer(-correctionAngle);
    }
  }

  private void balanceFrontBack(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    if (isDribblingTooFast(input)) {
      renderer.addText("Slow down!", Color.RED);
      slowDown(input, relativeBallData, output);
    } else if (isDribblingTooSlow(input)) {
      renderer.addText("Speed up!", Color.ORANGE);
      speedUp(input, relativeBallData, output);
    } else {
      // Balance
      renderer.addText("Balance!", Color.green);
      moveBallTo(5, input, relativeBallData, output);
    }
  }

  private boolean isDribblingTooSlow(DataPacket input) {
    return input.ball.velocity.flatten().magnitude() < IDEAL_DRIBBLE_SPEED - 50;
  }

  private boolean isDribblingTooFast(DataPacket input) {
    return input.ball.velocity.flatten().magnitude() > IDEAL_DRIBBLE_SPEED + 50;
  }

  private void speedUp(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double speedDeficit = IDEAL_DRIBBLE_SPEED - input.ball.velocity.flatten().magnitude();
    if (speedDeficit < 200) {
      renderer.addText("A little ....", Color.orange);
      moveBallTo(20f, input, relativeBallData, output);
    } else {
      renderer.addText("A lot ....", Color.red);
      moveBallTo(30f, input, relativeBallData, output);
    }
  }

  private void slowDown(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double speedSurplus = input.ball.velocity.flatten().magnitude() - IDEAL_DRIBBLE_SPEED;
    if (speedSurplus < 200) {
      renderer.addText("A little ....", Color.orange);
      moveBallTo(-25f, input, relativeBallData, output);
    } else {
      renderer.addText("A lot ....", Color.red);
      moveBallTo(-40f, input, relativeBallData, output);
    }
  }

  private void moveBallTo(float targetLocation, DataPacket input, BallData relativeBallData, ControlsOutput output) {
    // #Future improvements
    // * Add predictions about where the ball will be.
    // * Include vDiff due to gravity acceleration.

    float diffLocation = targetLocation - relativeBallData.position.y; // Positive means move the ball forward.

    float relativeBallVelocity = relativeBallData.velocity.y;
    float momentOfAcceleration = Constants.acceleration(input.car.velocity.flatten().magnitude());

    if (diffLocation > 0) {
      // Goal: Move the ball forward.
      if (relativeBallVelocity > 0) {
        // The ball is moving forward. Check how quickly.
        if (diffLocation > relativeBallVelocity) {
          renderer.addText("Wait on it...", Color.CYAN);
          // The ball will get there eventually… Either break, coast, or low throttle
          output.withThrottle(0.02);
        } else {
          // The ball will get to the new location soon. Speed up to catch it.

          renderer.addText("Overshoot", Color.CYAN);
          // How much will we overshoot in one second.
          double overShoot = relativeBallVelocity - diffLocation;
          // Determine how much acceleration we need.
          output.withThrottle(momentOfAcceleration / overShoot);
          // TODO: Does this need to be able to boost?
        }
      } else {
        // The ball is moving backward but needs to go forward. Need to slow down.
        if (relativeBallVelocity < Constants.COASTING_ACCELERATION) { // TODO: Tune this.
          output.withThrottle(-1f);
        } else {
          // Don’t really have other options. Perhaps swerve?
          output.withThrottle(0f);
        }
      }
    } else {
      // Goal: Move the ball backward
      if (relativeBallVelocity > 0) {
        // The ball is moving forward.
        if (relativeBallVelocity > momentOfAcceleration) {
          output.withBoost();
        } else {
          float vDiff = relativeBallVelocity - momentOfAcceleration;
          output.withThrottle(vDiff / diffLocation); // Both negative = positive throttle.
        }
      } else {
        // The ball is moving backward. Check how quickly.
        float ticks = diffLocation / relativeBallVelocity;
        if (ticks < .5f) {
          output.withThrottle(-1f); // Break.
        } else if ((ticks > .1f && momentOfAcceleration < 200f) || ticks > 3f) {
          output.withBoost();
        } else if (ticks > 1.5f) {
          output.withThrottle(0.4f);
        } else if (ticks > 1f) {
          output.withThrottle(0.02f);
        } else {
          output.withThrottle(0f);
        }
        renderer.addText(String.format("ticks %f", ticks), Color.CYAN);
      }
    }
  }

  private void balanceLeftRight(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double newOffset = Math.abs(relativeBallData.position.x + relativeBallData.velocity.x);
    double previousOffset = Math.abs(relativeBallData.position.x);

    if (newOffset < previousOffset) {
      // Go straight?
      // TODO: Figure out if this is going to overshoot the car and perhaps we should turn the opposite direction.
      renderer.addText("Do NOT turn.", Color.PINK);
    } else {
      float turn = relativeBallData.velocity.x / 50f;
      renderer.addText(String.format("Turn %s %f", turn < 0 ? "LEFT" : "RIGHT", turn), Color.PINK);
      output.withSteer(turn);
    }
  }

  // TODO: Move this to another class.
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
//    renderer.addText(String.format("TTC: %f", ticksToCar), Color.white);
//    renderer.addText(String.format("TTB: %f", ticksToBall), Color.white);
  }

  private boolean ballIsOnGround(DataPacket input) {
    boolean initialPosition = input.ball.velocity.z == 0 && input.ball.position.z < 150;
    boolean onGround = input.ball.position.z <= BALL_SIZE;
//    if (onGround || initialPosition) {
//      renderer.addText("Ball on Ground", Color.RED);
//    } else {
//      renderer.addText("Ball in Air", Color.GREEN);
//    }
    return onGround || initialPosition;
  }

  private boolean ballIsOnCar(BallData relativeBallData) {
    boolean heightOnCar = relativeBallData.position.z > 100 && relativeBallData.position.z < 200;
    boolean locationOnCar = relativeBallData.position.y < CAR_LENGTH
      && relativeBallData.position.y > -CAR_LENGTH;

//    if (heightOnCar && locationOnCar) {
//      renderer.addText("Ball on Car", Color.GREEN);
//    } else {
//      renderer.addText("Ball not on Car", Color.RED);
//    }
    return heightOnCar && locationOnCar;
  }

  private void pickUp(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    if (relativeBallData.position.y > input.car.position.y && input.car.velocity.flatten().magnitude() < PICK_UP_SPEED) {
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
