package com.eru.rlbot.bot.strats;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.DllHelper;
import com.eru.rlbot.bot.common.Path;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.DataPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.flat.BallPrediction;
import rlbot.flat.PredictionSlice;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BallPredictionUtil {

  private static final Logger logger = LogManager.getLogger("BallPredictionUtil");

  private static List<ExaminedBallData> examinedBallData = new LinkedList<>();

  public static List<ExaminedBallData> getPredictions() {
    return examinedBallData;
  }

  public static ExaminedBallData getFirstHittableLocation() {
    return examinedBallData.isEmpty() ? null : examinedBallData.stream().filter(ExaminedBallData::hasPath).findFirst().orElse(null);
  }

  public static boolean refresh(DataPacket input) {
    Optional<BallPrediction> predictionOptional = DllHelper.getBallPrediction();
    if (predictionOptional.isPresent()) {
      BallPrediction prediction = predictionOptional.get();
      if (prediction.slicesLength() > 0) {
        PredictionSlice nextSlice = prediction.slices(0);
        if (hasBeenTouched(nextSlice)) {
          examinedBallData = stream(prediction)
              .map(BallData::fromPredictionSlice)
              .map(ExaminedBallData::new)
              .collect(Collectors.toList());
          return true;
        }
      }
    }

    Iterator<ExaminedBallData> ballIterator = examinedBallData.iterator();
    while (ballIterator.hasNext()) {
      ExaminedBallData next = ballIterator.next();
      if (next.ball.elapsedSeconds >= input.ball.elapsedSeconds) {
        break;
      }

      ballIterator.remove();
    }

    return false;
  }

  private static boolean hasBeenTouched(PredictionSlice nextSlice) {
    BallData prediction = BallData.fromPredictionSlice(nextSlice);
    Iterator<ExaminedBallData> ballIterator = examinedBallData.iterator();
    while (ballIterator.hasNext()) {
      ExaminedBallData nextBall = ballIterator.next();
      if (nextBall.ball.elapsedSeconds >= prediction.elapsedSeconds) {
        if (prediction.elapsedSeconds + Constants.STEP_SIZE * 1 < nextBall.ball.elapsedSeconds) {
          // This prediction is off-cycle of the ones we have. Don't worry about it.
          return false;
        }

        return !prediction.fuzzyEquals(nextBall.ball);
        // Logging to check the diffs when the ball prediction is refreshed.
//        logger.warn(StateLogger.format(nextBall.ball) + " time: " + nextBall.ball.elapsedSeconds);
//        logger.warn(StateLogger.format(prediction) + " time: " + prediction.elapsedSeconds);
      }
      ballIterator.remove();
    }

    return true;
  }

  private static Stream<PredictionSlice> stream(BallPrediction prediction) {
    return IntStream.range(0, prediction.slicesLength())
        .mapToObj(prediction::slices);
  }

  public static class ExaminedBallData {

    private List<Path> paths = new ArrayList<>();
    private Optional<Boolean> hittable = Optional.empty();
    public final BallData ball;

    public ExaminedBallData(BallData ball) {
      this.ball = ball;
    }

    // TODO: This should be per pathing strategy
    public Optional<Boolean> isHittable() {
      return hittable;
    }

    public void addPath(Path path) {
      paths.add(path);
    }

    public boolean hasPath() {
      return paths.size() > 1;
    }

    public Path getPath() {
      return paths.get(0);
    }
  }
}
