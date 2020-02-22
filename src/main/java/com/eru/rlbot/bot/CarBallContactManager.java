package com.eru.rlbot.bot;

import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.CarBall;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.prediction.CarBallCollision;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Checks the input for touches between the controlling car and the ball. */
public final class CarBallContactManager {

  private static final Logger logger = LogManager.getLogger("Prediction");
  private static boolean headersLogged = false;

  private static float touchTime = -1;
  private static BallData ballPrediction;
  private static CarData previousCarData;
  private static BallData previousBallData;

  public static void loadDataPacket(DataPacket input) {
    Vector3 nearestPointWorld = CarBall.nearestPointOnHitBox(input.ball.position, input.car);

    BotRenderer botRenderer = BotRenderer.forIndex(input.playerIndex);

    double distanceToBall = Vector3.from(nearestPointWorld, input.ball.position).magnitude() - Constants.BALL_COLLISION_RADIUS;
    botRenderer.setNearestHitboxPoint(nearestPointWorld);


    if (distanceToBall < 2) {
      botRenderer.setTouchIndicator(input);
    }

    if (touchTime == -1 && distanceToBall < 2) {
      touchTime = input.car.elapsedSeconds;
      ballPrediction = CarBallCollision.calculateCollision(input.ball, input.car);
      previousBallData = input.ball;
      previousCarData = input.car;
    } else if (touchTime != -1) {
      botRenderer.setPredictionDiff(ballPrediction, input.ball);
      logResult(ballPrediction, input);

      touchTime = -1;
      ballPrediction = null;
    }
  }

  private static void logResult(BallData ballPrediction, DataPacket input) {
    if (!headersLogged) {
      headersLogged = true;
      logger.log(Level.INFO, CarData.csvHeader("cprev")
          + BallData.csvHeader("bprev")
          + BallData.csvHeader("bpred")
          + BallData.csvHeader("bact"));
    }

    String logMessage = previousCarData.toCsv() +
        previousBallData.toCsv() +
        ballPrediction.toCsv() +
        input.ball.toCsv();

    logger.log(Level.INFO, logMessage);
  }

  private CarBallContactManager() {}
}
