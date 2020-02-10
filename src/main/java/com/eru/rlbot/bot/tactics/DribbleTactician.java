package com.eru.rlbot.bot.tactics;

import static com.eru.rlbot.bot.common.Constants.*;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Goal;
import com.eru.rlbot.bot.common.NormalUtils;
import com.eru.rlbot.bot.main.Agc;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import java.awt.*;

public class DribbleTactician extends Tactician {

  private static final float IDEAL_DRIBBLE_SPEED = 1200;

  private static final float PICK_UP_SPEED = 800f;

  private static final Vector3 target_left = Goal.opponentGoal(0).leftInside;
  private static final Vector3 target_right = Goal.opponentGoal(0).rightInside;
  private static final Vector3 ownGoal = Goal.ownGoal(0).center;

  DribbleTactician(Agc bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean canDribble(DataPacket input) {
//    return input.car.position.flatten().distance(input.ball.position.flatten()) < 400
//        && input.ball.position.z > input.car.position.distance(input.ball.position);
    return ballIsOnCar(input) || ballCouldBeOnCar(input);
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic tactic) {
    BallData relativeBallData = NormalUtils.noseRelativeBall(input);

    if (input.car.velocity.magnitude() > 1550 && Math.abs(relativeBallData.velocity.y) < 50) {
      tacticManager.preemptTactic(tactic.withType(Tactic.TacticType.FLICK));
    } else if (ballIsOnGround(input)) {
      chip(input, relativeBallData, output);
      angleToBall(input, output);
    } else if (ballIsOnCar(input)) {
      driveToTarget(input, relativeBallData, output);
    } else if (ballCouldBeOnCar(input)) {
      output.withBoost();
    }

//    output.withSteer(0); // TODO: Don't turn left/right.
  }

  private float getBalancePoint(DataPacket input) {
    double speed = input.car.groundSpeed;

    // This should probably be continuous
    if (speed < 500) {
      return OCTANE_BALANCE_POINT + 15;
    } else if (speed < 1000) {
      return OCTANE_BALANCE_POINT + 20;
    } else if (speed < 1600) {
      return OCTANE_BALANCE_POINT + 35;
    } else {
      return OCTANE_BALANCE_POINT + 45;
    }
  }

  private void driveToTarget(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double correctionLeftAngle = -Angles.flatCorrectionAngle(input.ball, target_left);
    double correctionRightAngle = -Angles.flatCorrectionAngle(input.ball, target_right);

    double momentumAngle = getMomentum(relativeBallData);
    boolean isPointingAtTarget = (correctionLeftAngle + momentumAngle) < 0
        && (correctionRightAngle + momentumAngle) > 0;

    double correctionAngle = Angles.minAbs(correctionLeftAngle, correctionRightAngle);
    // TODO: How to tell if we've turned past the subject?

    // Need to turn right.
    boolean tooFarRight = correctionAngle > 0 && correctionLeftAngle + momentumAngle > correctionRightAngle;
    boolean tooFarLeft = correctionAngle < 0 && correctionRightAngle + momentumAngle < correctionLeftAngle;

    boolean needToTurnMore = !isPointingAtTarget && (!tooFarRight || !tooFarLeft);
    boolean canTurnMore = Math.abs(relativeBallData.velocity.x) < getBalancableX(input.car);

    // TODO: Debug why this is still turning a bit too far.
//    if (tooFarLeft) {
//      moveBallTo(getBalancePoint(input), input, relativeBallData, output);
//      output.withSteer(0.5f); // Hard Right.
//      bot.botRenderer.addAlertText("Hard Right");
//    } else if (tooFarRight) {
//      moveBallTo(getBalancePoint(input), input, relativeBallData, output);
//      output.withSteer(-0.5f); // Hard Left.
//      bot.botRenderer.addAlertText("Hard Left");
//    } else

//    if (needToTurnMore && canTurnMore) {
//      bot.botRenderer.addAlertText("Dribble start turn");
//
//      // TODO: Figure out how much more throttle to give.
//      moveBallTo(getBalancePoint(input) - 10, input, relativeBallData, output); // TODO: Make speed / distance dependent
//
//      // Ball is balanced. Initiate turn.
//      double steeringInput = correctionAngle / 5;
//
//      // Turn opposite the correction angle to get the ball to move the other way.
//      output.withSteer(steeringInput);
//    } else {
      balanceFrontBack(input, relativeBallData, output);
      balanceLeftRight(input, relativeBallData, output);
//    }
  }

  private double getMomentum(BallData relativeBallData) {
    boolean rightWard = relativeBallData.position.x > 0;
    double tiltMagnitude = Math.abs(relativeBallData.position.x);

    double value;

    // Values in radians
    if (tiltMagnitude < 10) {
      value = tiltMagnitude * .0001d;
    } else if (tiltMagnitude < 30) {
      value = tiltMagnitude * .001d;
    } else if (tiltMagnitude < 50d) {
      value = tiltMagnitude * .0075d;
    } else {
      value = tiltMagnitude * .01d;
    }

    if (!rightWard) {
      value = -value;
    }

    bot.botRenderer.addDebugText("Correction Value: %f", value);

    return value;
  }

  private float getBalancableX(CarData car) {
    return 50f;
  }

  private void angleToBall(DataPacket input, ControlsOutput output) {
    double correctionAngle = Angles.flatCorrectionAngle(input.car, input.ball.position);

    if (Math.abs(correctionAngle) > 2.5f) {
      // Prefer turning toward your own goal.
      bot.botRenderer.addDebugText("Ground Turn to Goal");
      double goalCorrectionAngle = Angles.flatCorrectionAngle(input.car, ownGoal);

      // If the goal and the ball are both far turns, turn toward the goal.
      output.withSteer(Math.abs(goalCorrectionAngle) > 1 ? goalCorrectionAngle : correctionAngle);
    } else if (Math.abs(correctionAngle) > .05) {
      bot.botRenderer.addDebugText("Ground Turn to ball");
      output.withSteer(correctionAngle);
    }
  }

  private void balanceFrontBack(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    // TODO: Remove or check if we are turning?
    if (isDribblingTooFast(input) && false) {
      slowDown(input, relativeBallData, output);
    } else if (isDribblingTooSlow(input) && false) {
      bot.botRenderer.addDebugText(Color.ORANGE, "Speed up!");
      speedUp(input, relativeBallData, output);
    } else {
      // Balance
//      bot.botRenderer.addDebugText(Color.green, "Balance!");
      moveBallTo(getBalancePoint(input), input, relativeBallData, output);
    }
  }

  private boolean isDribblingTooSlow(DataPacket input) {
    return input.ball.velocity.flatten().norm() < IDEAL_DRIBBLE_SPEED - 50;
  }

  private boolean isDribblingTooFast(DataPacket input) {
    return input.ball.velocity.flatten().norm() > IDEAL_DRIBBLE_SPEED + 50;
  }

  private void speedUp(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double speedDeficit = IDEAL_DRIBBLE_SPEED - input.ball.velocity.flatten().norm();
    if (speedDeficit < 400) {
      bot.botRenderer.addDebugText(Color.orange, "Speed up small");
      moveBallTo(25f, input, relativeBallData, output);
    } else {
      bot.botRenderer.addDebugText(Color.red, "Speed up big");
      moveBallTo(45f, input, relativeBallData, output);
    }
  }

  private void slowDown(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double speedSurplus = input.ball.velocity.flatten().norm() - IDEAL_DRIBBLE_SPEED;
    if (speedSurplus < 200) {
      bot.botRenderer.addDebugText(Color.orange, "Slow small");
      moveBallTo(-25f, input, relativeBallData, output);
    } else {
      bot.botRenderer.addDebugText(Color.red, "Slow big");
      moveBallTo(-40f, input, relativeBallData, output);
    }
  }

  private void moveBallTo(float targetLocation, DataPacket input, BallData relativeBallData, ControlsOutput output) {
    // #Future improvements
    // * Add predictions about where the ball will be.
    // * Include vDiff due to gravity acceleration.

    float diffLocation = targetLocation - relativeBallData.position.y; // Positive means move the ball forward.

    float relativeBallVelocity = relativeBallData.velocity.y;
    float momentOfAcceleration = Accels.acceleration(input.car.velocity.flatten().norm());

    if (diffLocation > 0) {
      // Goal: Move the ball forward.
      if (relativeBallVelocity > 0) {
        // The ball is moving forward. Check how quickly.
        if (diffLocation > relativeBallVelocity) {
          bot.botRenderer.setBranchInfo("Wait on it...");
          // The ball will get there eventually… Either break, coast, or low throttle
          output.withThrottle(0.0);
        } else {
          // The ball will get to the new location soon. Speed up to catch it.

          bot.botRenderer.setBranchInfo("Overshoot");
          // How much will we overshoot in one second.
          double overShoot = relativeBallVelocity - diffLocation;
          // Determine how much acceleration we need.
          output.withThrottle(momentOfAcceleration / overShoot);
          // TODO: Does this need to be able to boost?
        }
      } else {
        // The ball is moving backward but needs to go forward. Need to slow down.
        if (relativeBallVelocity < 200) { // TODO: Tune this.
          bot.botRenderer.setBranchInfo("Break");
          output.withThrottle(-1f);
        } else {
          // Don’t really have other options. Perhaps swerve?
          bot.botRenderer.setBranchInfo("Coast");
          output.withThrottle(0f);
        }
      }
    } else {
      // Goal: Move the ball backward
      if (relativeBallVelocity > 0) {
        // The ball is moving forward.
        if (relativeBallVelocity > momentOfAcceleration) {
          output.withBoost();
          bot.botRenderer.setBranchInfo("Falling ball: BOOST");
        } else {
          bot.botRenderer.setBranchInfo("Move ball back: gas");
          float vDiff = relativeBallVelocity - momentOfAcceleration;
          output.withThrottle(vDiff / diffLocation); // Both negative = positive throttle.
        }
      } else {
        // The ball is moving backward. Check how quickly.
        float ticks = diffLocation / relativeBallVelocity;
        if (ticks < .5f) {
          output.withThrottle(-1f); // Break.
        } else if ((ticks > .5f && momentOfAcceleration < 200f) || ticks > 5f) {
          output.withBoost();
          bot.botRenderer.setBranchInfo("Impatient: BOOST");
        } else if (ticks > 1.5f) {
          output.withThrottle(0.4f);
        } else if (ticks > 1f) {
          output.withThrottle(0.02f);
        } else {
          output.withThrottle(0f);
        }
        bot.botRenderer.setBranchInfo("Move ball to");
      }
    }
  }

  private void balanceLeftRight(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double newOffset = Math.abs(relativeBallData.position.x + relativeBallData.velocity.x);
    double previousOffset = Math.abs(relativeBallData.position.x);

    if (newOffset < previousOffset) {
      // Go straight?
      // TODO(10/17/19): Figure out if this is going to overshoot the car and perhaps we should turn the opposite direction.
      bot.botRenderer.addDebugText("Balance LR: Do NOT turn.");
    } else {
      float turn = -relativeBallData.velocity.x / 50f; // Negative x is right.
      bot.botRenderer.addDebugText("Balance LF: Sharp turn");
      output.withSteer(turn);
    }
  }

  private boolean ballIsOnGround(DataPacket input) {
    boolean initialPosition = input.ball.velocity.z == 0 && input.ball.position.z < 150;
    boolean onGround = input.ball.position.z <= BALL_RADIUS;
//    if (onGround || initialPosition) {
//      bot.botRenderer.addDebugText("Ball on Ground", Color.RED);
//    } else {
//      bot.botRenderer.addDebugText("Ball in Air", Color.GREEN);
//    }
    return onGround || initialPosition;
  }

  private static boolean ballIsOnCar(DataPacket input) {
    BallData relativeBallData = NormalUtils.noseRelativeBall(input);

    boolean heightOnCar = relativeBallData.position.z > 100 && relativeBallData.position.z < 200;
    boolean locationOnCar = relativeBallData.position.y < CAR_LENGTH
        && relativeBallData.position.y > -CAR_LENGTH;
    boolean striking = Math.abs(relativeBallData.velocity.z) > 300;

    return heightOnCar && locationOnCar && !striking;
  }

  private static boolean ballCouldBeOnCar(DataPacket input) {
    BallData relativeBallData = NormalUtils.noseRelativeBall(input);

    boolean heightOnCar = relativeBallData.position.z > 100 && relativeBallData.position.z < 300;
    boolean rightInFrontOfCar = relativeBallData.position.y > 0 && relativeBallData.position.y < 200;

    boolean velocityIsCatchable = relativeBallData.velocity.y < 100
        || (relativeBallData.velocity.y < 300 && input.car.boost > 15);

    return heightOnCar && rightInFrontOfCar && velocityIsCatchable;
  }

  private void chip(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    if (relativeBallData.position.y > 0 && input.car.velocity.flatten().norm() < PICK_UP_SPEED) {
      output.withThrottle(1.0f);
    } else if (relativeBallData.position.y < 0) {
      // Need to turn around.
      output.withThrottle(1.0f);
    }
  }
}
