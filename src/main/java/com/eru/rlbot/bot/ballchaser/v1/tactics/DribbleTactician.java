package com.eru.rlbot.bot.ballchaser.v1.tactics;

import com.eru.rlbot.bot.common.*;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.Bot;

import java.awt.*;

import static com.eru.rlbot.bot.common.Constants.*;

public class DribbleTactician implements Tactician {

  private static final float OCTANE_BALANCE_POINT = -1.6351f;

  private static final float IDEAL_DRIBBLE_SPEED = 1200;

  private static final float PICK_UP_SPEED = 800f;

  private static final float CATCH_BALL_POINT = OCTANE_BALANCE_POINT;
  private static final float BALL_DIAMETER = BALL_SIZE;

  private static final Vector3 target_left = Goal.opponentGoal(1).left;
  private static final Vector3 target_right = Goal.opponentGoal(1).right;
  private static final Vector3 ownGoal = Goal.opponentGoal(0).center;

  private final BotRenderer renderer;

  DribbleTactician(Bot bot) {
    renderer = BotRenderer.forBot(bot);
  }

  @Override
  public void execute(ControlsOutput output, DataPacket input, Tactic nextTactic) {
    BallData relativeBallData = CarNormalUtils.noseNormalLocation(input);

    if (ballIsOnGround(input)) {
      chip(input, relativeBallData, output);
      angleToBall(input, output);
    } else if (ballIsOnCar(input, relativeBallData)) {
      driveToTarget(input, relativeBallData, output);
    } else {
      catchBall(input, relativeBallData, output);
      relativeAngleToBall(input, relativeBallData, output); // Set up the angle here too.
    }
  }

  private void driveToTarget(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double correctionLeftAngle = Angles.flatCorrectionDirection(input.ball, target_left);
    double correctionRightAngle = Angles.flatCorrectionDirection(input.ball, target_right);

    double momentumAngle = getMomentum(relativeBallData);
    boolean isPointingAtTarget = (correctionLeftAngle + momentumAngle) < 0
        && (correctionRightAngle + momentumAngle) > 0;

    double correctionAngle = Angles.minAbs(correctionLeftAngle, correctionRightAngle);
    // TODO: How to tell if we've turned past the target?

      // Need to turn right.
    boolean tooFarRight = correctionAngle > 0 && correctionLeftAngle + momentumAngle > correctionRightAngle;
    boolean tooFarLeft = correctionAngle < 0 && correctionRightAngle + momentumAngle < correctionLeftAngle;

    boolean needToTurnMore = !isPointingAtTarget && (!tooFarRight || !tooFarLeft);
    boolean canTurnMore = Math.abs(relativeBallData.velocity.x) < getBalancableX(input.car);

    // TODO: Debug why this is still turning a bit too far.
    if (tooFarLeft) {
      moveBallTo(OCTANE_BALANCE_POINT, input, relativeBallData, output);
      output.withSteer(-0.5f); // Hard left.
      renderer.addDebugText("Hard Left");
    } else if (tooFarRight) {
      moveBallTo(OCTANE_BALANCE_POINT, input, relativeBallData, output);
      output.withSteer(0.5f); // Hard right.
      renderer.addDebugText("Hard Right");
    } else if (needToTurnMore && canTurnMore) {
      renderer.addDebugText("Dribble start turn");

      // TODO: Figure out how much more throttle to give.
      moveBallTo(OCTANE_BALANCE_POINT, input, relativeBallData, output); // TODO: Make speed / distance dependent

      // Ball is balanced. Initiate turn.
      double steeringInput = -correctionAngle * 3;

      // Turn opposite the correction angle to get the ball to move the other way.
      output.withSteer(steeringInput);
    } else {
      renderer.addDebugText(isPointingAtTarget ? "GOOOOOAAAAL" : "Stop dribble fall.");

      balanceFrontBack(input, relativeBallData, output);
      balanceLeftRight(input, relativeBallData, output);
    }
  }

  private double getMomentum(BallData relativeBallData) {
    boolean rightWard = relativeBallData.position.x > 0;
    double tiltMagnitude = Math.abs(relativeBallData.position.x);

    double value = 0d;

    // Values in radians
    if (tiltMagnitude < 10) {
      value = tiltMagnitude * .001d;
    } else if (tiltMagnitude < 30) {
      value = tiltMagnitude * .005d;
    } else if (tiltMagnitude < 50d) {
      value = tiltMagnitude * .0075d;
    } else {
      value = tiltMagnitude * .01d;
    }

    if (!rightWard) {
      value = -value;
    }

    renderer.addDebugText("Correction Value: %f", value);

    return value;
  }

  private float getBalancableX(CarData car) {
    return 50f;
  }

  private void relativeAngleToBall(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    if (relativeBallData.position.y < 0 && relativeBallData.velocity.y > 0) {
      // Ball is behind you but catching up. Make slight x adjustment if needed
      if (Math.abs(relativeBallData.velocity.x) > 10) { // #Arbitrary threshold
        double steerAngle = -relativeBallData.velocity.x / 400;
        renderer.addDebugText("Catch turn");
        output.withSteer(steerAngle);
      }
    } else {
      double correctionAngle = Angles.flatCorrectionDirection(input.car, input.ball.position);

      renderer.addDebugText("Catch 2 Turn");
      output.withSteer(correctionAngle);
    }
  }

  private void angleToBall(DataPacket input, ControlsOutput output) {
    double correctionAngle = Angles.flatCorrectionDirection(input.car, input.ball.position);

    if (Math.abs(correctionAngle) > 2.5f) {
      // Prefer turning toward your own goal.
      renderer.addDebugText("Ground Turn to Goal");
      double goalCorrectionAngle = Angles.flatCorrectionDirection(input.car, ownGoal);

      // If the goal and the ball are both far turns, turn toward the goal.
      output.withSteer(Math.abs(goalCorrectionAngle) > 1 ? goalCorrectionAngle : correctionAngle);
    } else if (Math.abs(correctionAngle) > .05) {
      renderer.addDebugText("Ground Turn to ball");
      output.withSteer(correctionAngle);
    }
  }

  private void balanceFrontBack(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    // TODO: Remove or check if we are turning?
    if (isDribblingTooFast(input) && false) {
      slowDown(input, relativeBallData, output);
    } else if (isDribblingTooSlow(input) && false) {
      renderer.addDebugText(Color.ORANGE, "Speed up!");
      speedUp(input, relativeBallData, output);
    } else {
      // Balance
//      renderer.addDebugText(Color.green, "Balance!");
      moveBallTo(OCTANE_BALANCE_POINT, input, relativeBallData, output);
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
      renderer.addDebugText(Color.orange, "Speed up small");
      moveBallTo(10f, input, relativeBallData, output);
    } else {
      renderer.addDebugText(Color.red, "Speed up big");
      moveBallTo(25f, input, relativeBallData, output);
    }
  }

  private void slowDown(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double speedSurplus = input.ball.velocity.flatten().magnitude() - IDEAL_DRIBBLE_SPEED;
    if (speedSurplus < 200) {
      renderer.addDebugText(Color.orange, "Slow small");
      moveBallTo(-25f, input, relativeBallData, output);
    } else {
      renderer.addDebugText(Color.red, "Slow big");
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
          renderer.addDebugText(Color.CYAN, "Wait on it...");
          // The ball will get there eventually… Either break, coast, or low throttle
          output.withThrottle(0.0);
        } else {
          // The ball will get to the new location soon. Speed up to catch it.

          renderer.addDebugText("Overshoot", Color.CYAN);
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
          renderer.addDebugText(Color.RED, "Falling ball: BOOST");
        } else {
          renderer.addDebugText("Move ball back: gas");
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
          renderer.addDebugText(Color.PINK, "Impatient: BOOST");
        } else if (ticks > 1.5f) {
          output.withThrottle(0.4f);
        } else if (ticks > 1f) {
          output.withThrottle(0.02f);
        } else {
          output.withThrottle(0f);
        }
        renderer.addDebugText(Color.CYAN, String.format("Move ball back: ticks %f", ticks));
      }
    }
  }

  private void balanceLeftRight(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double newOffset = Math.abs(relativeBallData.position.x + relativeBallData.velocity.x);
    double previousOffset = Math.abs(relativeBallData.position.x);

    if (newOffset < previousOffset) {
      // Go straight?
      // TODO(10/17/19): Figure out if this is going to overshoot the car and perhaps we should turn the opposite direction.
      renderer.addDebugText("Balance LR: Do NOT turn.");
    } else {
      float turn = relativeBallData.velocity.x / 50f; // Negative x is right.
      renderer.addDebugText("Balance LF: Sharp turn");
      output.withSteer(turn);
    }
  }

  // TODO: Move this to another class.
  // Called when ball is in the air.
  private void catchBall(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double ballSpeed = input.ball.velocity.flatten().magnitude();
    double carSpeed = input.car.velocity.flatten().magnitude();

    double distanceToCatchPoint = relativeBallData.position.y - CATCH_BALL_POINT;

    // How long to get to x/y position of the ball?
    double ticksToBall = distanceToCatchPoint
        / -relativeBallData.velocity.y; // Negate the velocity since moving toward the ball is a relative negative velocity.

    double ballHeightAboveCar = relativeBallData.position.z - (CAR_HEIGHT + BALL_DIAMETER);
    double ballDownVelocity = relativeBallData.velocity.z;

    // How long for the ball to land on the car?
    double ticksToCar = timeToLand(ballHeightAboveCar, ballDownVelocity);

    if (ticksToBall < 0) {
      // We are going to overrun the ball.
      renderer.addDebugText(Color.RED, "Slow Down!");
      output.withThrottle(distanceToCatchPoint > 0 ? 1f : 0f);
    } else if (ballSpeed > carSpeed || ticksToBall > ticksToCar) {
      // The ball is getting away or the ball will hit the ground before we get there.
      renderer.addDebugText(Color.YELLOW, "Behind");
      output.withThrottle(1.0f);

    // Good place to catch the ball. Match speed
    } else if (carSpeed >= ballSpeed && ticksToBall < ticksToCar){
      renderer.addDebugText(Color.YELLOW, "Feather");

      // Throttle to maintain speed
      output.withThrottle(.02);

    } else {
      renderer.addDebugText(Color.BLACK, "Default catch");
      output.withThrottle(0.5f);
    }
    // Direction
//    renderer.addDebugText(String.format("TTC: %f", ticksToCar), Color.white);
//    renderer.addDebugText(String.format("TTB: %f", ticksToBall), Color.white);
  }

  private boolean ballIsOnGround(DataPacket input) {
    boolean initialPosition = input.ball.velocity.z == 0 && input.ball.position.z < 150;
    boolean onGround = input.ball.position.z <= BALL_SIZE;
//    if (onGround || initialPosition) {
//      renderer.addDebugText("Ball on Ground", Color.RED);
//    } else {
//      renderer.addDebugText("Ball in Air", Color.GREEN);
//    }
    return onGround || initialPosition;
  }

  private boolean ballIsOnCar(DataPacket input, BallData relativeBallData) {
    boolean heightOnCar = relativeBallData.position.z > 100 && relativeBallData.position.z < 200;
    boolean locationOnCar = relativeBallData.position.y < CAR_LENGTH
      && relativeBallData.position.y > -CAR_LENGTH;
    boolean striking = Math.abs(relativeBallData.velocity.z) > 300;

//    if (heightOnCar && locationOnCar) {
//      renderer.addDebugText("Ball on Car", Color.GREEN);
//    } else {
//      renderer.addDebugText("Ball not on Car", Color.RED);
//    }
    return heightOnCar && locationOnCar && !striking;
  }

  private void chip(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    if (relativeBallData.position.y > 0 && input.car.velocity.flatten().magnitude() < PICK_UP_SPEED) {
      output.withThrottle(1.0f);
    } else if (relativeBallData.position.y < 0) {
      // Need to turn around.
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
