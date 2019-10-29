package com.eru.rlbot.bot.ballchaser.v1.tactics;

import static com.eru.rlbot.bot.common.Constants.*;

import com.eru.rlbot.bot.EruBot;
import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.NormalUtils;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.output.ControlsOutput;
import java.awt.*;

public class CatchTactician extends Tactician {

  private static final float CATCH_BALL_POINT = OCTANE_BALANCE_POINT;
  private static final float BALL_DIAMETER = BALL_SIZE;

  CatchTactician(EruBot bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  public static boolean canCatch(DataPacket input) {
    BallData relativeCarData = NormalUtils.noseNormal(input);

    return (input.ball.position.z > 100 || input.ball.velocity.z > 100)
        && (ballDownTime(relativeCarData) > carToBallTime(relativeCarData));
  }

  @Override
  public void execute(DataPacket input, ControlsOutput output, Tactic nextTactic) {
    BallData relativeBallData = NormalUtils.noseNormal(input);
    catchBall(input, relativeBallData, output);
    relativeAngleToBall(input, relativeBallData, output); // Set up the angle here too.

    // TODO: if the ball is on the car, hand off to the dribbler
  }

  private void relativeAngleToBall(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    if (relativeBallData.position.y < 0 && relativeBallData.velocity.y > 0) {
      // Ball is behind you but catching up. Make slight x adjustment if needed
      if (Math.abs(relativeBallData.velocity.x) > 10) { // #Arbitrary threshold
        double steerAngle = -relativeBallData.velocity.x / 400;
        bot.botRenderer.addDebugText("Catch turn");
        output.withSteer(steerAngle);
      }
    } else {
      double correctionAngle = Angles.flatCorrectionDirection(input.car, input.ball.position);

      bot.botRenderer.addDebugText("Catch 2 Turn");
      output.withSteer(correctionAngle);
    }
  }

  // TODO: Move this to another class.
  // Called when ball is in the air.
  private void catchBall(DataPacket input, BallData relativeBallData, ControlsOutput output) {
    double ballSpeed = input.ball.velocity.flatten().magnitude();
    double carSpeed = input.car.velocity.flatten().magnitude();

    double distanceToCatchPoint = relativeBallData.position.y - CATCH_BALL_POINT;

    // How long to get to x/y position of the ball?
    double ticksToBall = carToBallTime(relativeBallData);

    double ballHeightAboveCar = relativeBallData.position.z - (CAR_HEIGHT + BALL_DIAMETER);
    double ballDownVelocity = relativeBallData.velocity.z;

    // How long for the ball to land on the car?
    double ticksToCar = timeToLand(ballHeightAboveCar, ballDownVelocity);

    if (ticksToBall < 0) {
      // We are going to overrun the ball.
      bot.botRenderer.addDebugText(Color.RED, "Slow Down!");
      output.withThrottle(distanceToCatchPoint > 0 ? 1f : 0f);
    } else if (ballSpeed > carSpeed || ticksToBall > ticksToCar) {
      // The ball is getting away or the ball will hit the ground before we get there.
      bot.botRenderer.addDebugText(Color.YELLOW, "Behind");
      output.withThrottle(1.0f);

      // Good place to catch the ball. Match speed
    } else if (carSpeed >= ballSpeed && ticksToBall < ticksToCar){
      bot.botRenderer.addDebugText(Color.YELLOW, "Feather");

      // Throttle to maintain speed
      output.withThrottle(.02);

    } else {
      bot.botRenderer.addDebugText(Color.BLACK, "Default catch");
      output.withThrottle(0.5f);
    }
    // Direction
    // bot.botRenderer.addDebugText(String.format("TTC: %f", ticksToCar), Color.white);
    // bot.botRenderer.addDebugText(String.format("TTB: %f", ticksToBall), Color.white);
  }

  private static double carToBallTime(BallData relativeBallData) {
    double distanceToCatchPoint = relativeBallData.position.y - CATCH_BALL_POINT;

    // Negate the velocity since moving toward the ball is a relative negative velocity.
    return distanceToCatchPoint / -relativeBallData.velocity.y;
  }

  private static double ballDownTime(BallData relativeBallData) {
    double ballHeightAboveCar = relativeBallData.position.z - (CAR_HEIGHT + BALL_DIAMETER);
    double ballDownVelocity = relativeBallData.velocity.z;

    // How long for the ball to land on the car?
    return timeToLand(ballHeightAboveCar, ballDownVelocity);
  }

  private static double timeToLand(double height, double verticalSpeed) {
    double firstTerm = verticalSpeed / GRAVITY;
    double secondTerm = Math.sqrt((verticalSpeed * verticalSpeed) + (2 * GRAVITY * height)) / GRAVITY;

    return Math.max(firstTerm + secondTerm, firstTerm - secondTerm);
  }

  private static double timeToCatch(double distance, double acceleration) {
    return Math.sqrt((2 * acceleration * distance)) / acceleration;
  }
}
