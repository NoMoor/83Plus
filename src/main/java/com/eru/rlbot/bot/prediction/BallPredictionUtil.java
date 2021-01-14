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
import rlbot.flat.PredictionSlice;
import rlbot.flat.Touch;
import rlbot.flat.Vector3;

/**
 * Keep track of which ball locations have been examined.
 */
public class BallPredictionUtil {

  private static final Logger logger = LogManager.getLogger("BallPredictionUtil");

  public static final int PREDICTION_TIME_LIMIT = 6;
  public static final int PREDICTION_FPS = 60;
  public static final long PREDICTION_LIMIT = PREDICTION_TIME_LIMIT * PREDICTION_FPS;

  private static final ConcurrentHashMap<Integer, BallPredictionUtil> MAP = new ConcurrentHashMap<>();
  private static boolean wasTouched;

  private final int serialNumber;

  private BallPredictionUtil(int serialNumber) {
    this.serialNumber = serialNumber;
  }

  private volatile List<BallPrediction> balls = new LinkedList<>();

  public List<BallPrediction> getPredictions() {
    return balls;
  }

  public Optional<ChallengeData> getChallengeData() {
    if (balls.isEmpty()) {
      logger.debug("Cannot find prediction slices");
      return Optional.empty();
    }

    Optional<BallPrediction> hittableByAnyone = balls.stream()
        .filter(BallPrediction::isHittableBySomeone)
        .findFirst();

    if (!hittableByAnyone.isPresent()) {
      logger.debug("Not hittable");
      return Optional.empty();
    }

    BallPrediction firstTouch = hittableByAnyone.get();
    int touchedByTeam = firstTouch.ableToReachTeams().get(0);

    BallPrediction hittableByOtherTeam = balls.stream()
        .filter(ballPrediction -> ballPrediction.isHittableByTeam(Teams.otherTeam(touchedByTeam)))
        .findFirst().orElseGet(() -> Iterables.getLast(balls));

    return Optional.of(new ChallengeData(firstTouch, hittableByOtherTeam, touchedByTeam));
  }

  public BallPrediction getTarget() {
    if (balls.isEmpty()) {
      return null;
    }

    Optional<BallPrediction> firstHittable = balls.stream()
        .filter(ball -> ball.forCar(serialNumber).isHittable())
        .findFirst();

    return firstHittable.orElseGet(() -> Iterables.getLast(balls));
  }

  private boolean refreshInternal(BallData ball) {
    Optional<rlbot.flat.BallPrediction> predictionOptional = DllHelper.getBallPrediction();
    if (!predictionOptional.isPresent()) {
      return false;
    }

    rlbot.flat.BallPrediction prediction = predictionOptional.get();
    if (prediction.slicesLength() > 0) {
      PredictionSlice nextSlice = prediction.slices(0);
      if (hasBeenTouched(nextSlice)) {
        balls = stream(prediction)
            .limit(PREDICTION_LIMIT)
            .filter(BallPredictionUtil::isInBounds)
            .map(BallData::fromPredictionSlice)
            .map(BallPrediction::new)
            .collect(Collectors.toList());
        return true;
      }
    }

    Iterator<BallPrediction> ballIterator = balls.iterator();
    while (ballIterator.hasNext()) {
      BallPrediction next = ballIterator.next();
      if (next.ball.time >= ball.time) {
        break;
      }

      ballIterator.remove();
    }

    if (!balls.isEmpty() && PREDICTION_LIMIT > balls.size()) {
      float lastTime = Iterables.getLast(balls).ball.time;

      stream(prediction)
          .filter(predictionSlice -> predictionSlice.gameSeconds() > lastTime)
          .filter(BallPredictionUtil::isInBounds)
          .limit(PREDICTION_LIMIT - balls.size())
          .map(BallData::fromPredictionSlice)
          .map(BallPrediction::new)
          .forEach(balls::add);
    }

    return false;
  }

  private static boolean isInBounds(PredictionSlice predictionSlice) {
    Vector3 location = predictionSlice.physics().location();
    return Math.abs(location.x()) < Constants.HALF_WIDTH && Math.abs(location.y()) < Constants.HALF_LENGTH &&
        location.z() >= (Constants.BALL_RADIUS - 10) && location.z() < Constants.FIELD_HEIGHT;
  }

  private boolean hasBeenTouched(PredictionSlice nextSlice) {
    BallData prediction = BallData.fromPredictionSlice(nextSlice);
    Iterator<BallPrediction> ballIterator = balls.iterator();
    while (ballIterator.hasNext()) {
      BallPrediction nextBall = ballIterator.next();
      if (nextBall.ball.time >= prediction.time) {
        if (prediction.time + Constants.STEP_SIZE * 1 < nextBall.ball.time) {
          // This prediction is off-cycle of the ones we have. Don't worry about it.
          return false;
        }

        boolean isTheSame = prediction.fuzzyEquals(nextBall.ball);
        Touch touch = Teams.getBallTouchTime();
        float lastTouchTime = touch != null ? touch.gameSeconds() : 0;
        if (!isTheSame && nextBall.ball.time - lastTouchTime > .5) {
          // Logging to check the diffs when the ball prediction is refreshed.
          logger.debug(
              " time: {} last touch: {} data {}",
              nextBall.ball.time,
              lastTouchTime,
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

  public boolean wasTouched() {
    return wasTouched;
  }

  public static boolean refresh(DataPacket input) {
    wasTouched = get(input.car).refreshInternal(input.ball);
    return wasTouched;
  }

  private static Stream<rlbot.flat.PredictionSlice> stream(rlbot.flat.BallPrediction prediction) {
    return IntStream.range(0, prediction.slicesLength())
        .mapToObj(prediction::slices);
  }

  public static class ChallengeData {

    public final BallPrediction firstTouch;
    public final Optional<BallPrediction> firstTouchByOtherTeam;
    public final int controllingTeam;

    ChallengeData(BallPrediction firstTouch, BallPrediction firstTouchByOtherTeam, int team) {
      this.firstTouch = firstTouch;
      this.firstTouchByOtherTeam = Optional.ofNullable(firstTouchByOtherTeam);
      this.controllingTeam = team;
    }
  }
}
