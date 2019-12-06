package com.eru.rlbot.bot.prediction;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;

/** Predicts where the ball will be over some time interval. */
public class BallPredictor {

  private static final float PREDICTION_TIME = 2f;

  /** Gets a list of prediction-slice-like {@link BallData} objects. */
  public static ImmutableList<BallData> makePrediction(BallData ball) {
    BallData currentBall = ball;

    float currentTime = ball.elapsedSeconds;
    float endTime = currentTime + PREDICTION_TIME;

    ImmutableList.Builder<BallData> predictionBuilder = ImmutableList.builder();
    predictionBuilder.add(currentBall);

    while (currentTime < endTime) {
      currentTime += Constants.STEP_SIZE;
      currentBall = generateNextStep(currentBall);
      predictionBuilder.add(currentBall);
    }

    return predictionBuilder.build();
  }

  private static BallData generateNextStep(BallData currentBall) {
    double vX = currentBall.velocity.x;
    if (Math.abs(currentBall.position.x) + Constants.BALL_RADIUS > Constants.HALF_WIDTH) {
      vX *= -Constants.COEFFICIENT_OF_RESITUTION;
    }

    double vY = currentBall.velocity.y;
    if (Math.abs(currentBall.position.y) + Constants.BALL_RADIUS > Constants.HALF_LENGTH) {
      vY *= -Constants.COEFFICIENT_OF_RESITUTION;
    }
    double vZ = currentBall.velocity.z - (Constants.GRAVITY / Constants.STEP_SIZE_COUNT);
    if (currentBall.position.z < Constants.BALL_RADIUS) {
      vZ *= -Constants.COEFFICIENT_OF_RESITUTION;
    }

    Vector3 newVelocity = Vector3.of(vX, vY, vZ);

    return BallData.builder()
        .setPosition(currentBall.position.plus(currentBall.velocity.multiply(Constants.STEP_SIZE)))
        .setVelocity(newVelocity)
        .setSpin(currentBall.spin)
        .setTime(currentBall.elapsedSeconds + Constants.STEP_SIZE)
        .build();
  }
}
