package com.eru.rlbot.bot;

import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.CarBall;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.prediction.CarBallCollision;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;

/** Checks the input for touches between the controlling car and the ball. */
public final class CarBallContactManager {

  private static float touchTime = -1;
  private static BallData ballPrediction;

  public static void loadDataPacket(DataPacket input) {
    Vector3 nearestPointWorld = CarBall.nearestPointOnHitBox(input.ball.position, input.car);

    BotRenderer botRenderer = BotRenderer.forIndex(input.playerIndex);

    double distanceToBall = Vector3.from(nearestPointWorld, input.ball.position).norm() - Constants.BALL_COLLISION_RADIUS;
    botRenderer.setBranchInfo("%fuu", distanceToBall);
    botRenderer.setNearestHitboxPoint(nearestPointWorld);


    if (distanceToBall < 2) {
      botRenderer.setTouchIndicator(input);
    }

    // TODO: Configure log4j2
    if (touchTime == -1 && distanceToBall < 0) {
      touchTime = input.car.elapsedSeconds;
      ballPrediction = CarBallCollision.calculateCollision(input.ball, input.car);
    } else if (touchTime != -1) {
      botRenderer.setPredictionDiff(ballPrediction, input.ball);

      touchTime = -1;
      ballPrediction = null;
    }
  }

  private CarBallContactManager() {}
}
