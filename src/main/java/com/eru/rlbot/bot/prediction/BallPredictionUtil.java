package com.eru.rlbot.bot.prediction;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Teams;
import com.eru.rlbot.common.DllHelper;
import com.eru.rlbot.common.StateLogger;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.google.common.collect.Iterables;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;

/**
 * Keep track of which ball locations have been examined.
 */
public class BallPredictionUtil {

  private static final Logger logger = LogManager.getLogger("BallPredictionUtil");
  private static final int PREDICTION_FPS = 60;
  private static final long PREDICTION_LIMIT = 3 * PREDICTION_FPS;

  private static ConcurrentHashMap<Integer, BallPredictionUtil> MAP = new ConcurrentHashMap<>();

  private final int serialNumber;

  private BallPredictionUtil(int serialNumber) {
    this.serialNumber = serialNumber;
  }

  private volatile List<com.eru.rlbot.bot.prediction.BallPrediction> balls = new LinkedList<>();

  public List<com.eru.rlbot.bot.prediction.BallPrediction> getPredictions() {
    return balls;
  }

  public com.eru.rlbot.bot.prediction.BallPrediction getFirstHittableLocation() {
    if (balls.isEmpty()) {
      return null;
    }

    Optional<com.eru.rlbot.bot.prediction.BallPrediction> firstHittable = balls.stream()
        .filter(ball -> ball.forCar(serialNumber).isHittable())
        .findFirst();

    return firstHittable.orElse(null);
  }

  private boolean refreshInternal(BallData ball) {
    Optional<BallPrediction> predictionOptional = DllHelper.getBallPrediction();
    if (!predictionOptional.isPresent()) {
      return false;
    }

    BallPrediction prediction = predictionOptional.get();
    if (prediction.slicesLength() > 0) {
      PredictionSlice nextSlice = prediction.slices(0);
      if (hasBeenTouched(nextSlice)) {
        balls = stream(prediction)
            .limit(PREDICTION_LIMIT)
            .map(BallData::fromPredictionSlice)
            .map(com.eru.rlbot.bot.prediction.BallPrediction::new)
            .collect(Collectors.toList());
        return true;
      }
    }

    Iterator<com.eru.rlbot.bot.prediction.BallPrediction> ballIterator = balls.iterator();
    while (ballIterator.hasNext()) {
      com.eru.rlbot.bot.prediction.BallPrediction next = ballIterator.next();
      if (next.ball.time >= ball.time) {
        break;
      }

      ballIterator.remove();
    }

    if (PREDICTION_LIMIT > balls.size()) {
      float lastTime = Iterables.getLast(balls).ball.time;

      stream(prediction)
          .filter(predictionSlice -> predictionSlice.gameSeconds() > lastTime)
          .limit(PREDICTION_LIMIT - balls.size())
          .map(BallData::fromPredictionSlice)
          .map(com.eru.rlbot.bot.prediction.BallPrediction::new)
          .forEach(balls::add);
    }

    return false;
  }

  private boolean hasBeenTouched(PredictionSlice nextSlice) {
    BallData prediction = BallData.fromPredictionSlice(nextSlice);
    Iterator<com.eru.rlbot.bot.prediction.BallPrediction> ballIterator = balls.iterator();
    while (ballIterator.hasNext()) {
      com.eru.rlbot.bot.prediction.BallPrediction nextBall = ballIterator.next();
      if (nextBall.ball.time >= prediction.time) {
        if (prediction.time + Constants.STEP_SIZE * 1 < nextBall.ball.time) {
          // This prediction is off-cycle of the ones we have. Don't worry about it.
          return false;
        }

        boolean isTheSame = prediction.fuzzyEquals(nextBall.ball);
        float lastTouchTime = Teams.getBallTouchTime().gameSeconds();
        if (!isTheSame && nextBall.ball.time - lastTouchTime > .5) {
          // Logging to check the diffs when the ball prediction is refreshed.
          logger.debug(
              " time: {} last touch: {} data {}",
              nextBall.ball.time,
              Teams.getBallTouchTime().gameSeconds(),
              StateLogger.format(nextBall.ball));
        }
        return !isTheSame;
      }
      ballIterator.remove();
    }

    return true;
  }

  public static BallPredictionUtil get(int serialNumber) {
    return MAP.computeIfAbsent(serialNumber, BallPredictionUtil::new);
  }

  public static BallPredictionUtil get(CarData car) {
    return get(car.serialNumber);
  }

  public static boolean refresh(DataPacket input) {
    return get(input.car).refreshInternal(input.ball);
  }

  private static Stream<PredictionSlice> stream(BallPrediction prediction) {
    return IntStream.range(0, prediction.slicesLength())
        .mapToObj(prediction::slices);
  }
}
