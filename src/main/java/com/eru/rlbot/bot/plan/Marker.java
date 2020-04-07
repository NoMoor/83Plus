package com.eru.rlbot.bot.plan;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.path.PathPlanner;
import com.eru.rlbot.bot.path.Plan;
import com.eru.rlbot.bot.prediction.BallPrediction;
import com.eru.rlbot.bot.prediction.BallPredictionUtil;
import com.eru.rlbot.bot.tactics.AerialTactician;
import com.eru.rlbot.bot.utils.StopWatch;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Marks which balls can be hit.
 */
public class Marker {

  private static final Logger logger = LogManager.getLogger("Marker");

  private static final ConcurrentHashMap<Integer, Marker> MARKERS = new ConcurrentHashMap<>();

  private final int ownerBot;
  private volatile int index;

  private Marker(int ownerBot) {
    this.ownerBot = ownerBot;
  }

  public static Marker get(int ownerBot) {
    return getInternal(ownerBot);
  }

  private static Marker getInternal(int ownerBot) {
    return MARKERS.computeIfAbsent(ownerBot, Marker::new);
  }

  public void mark(DataPacket input, int botIndex) {
    StopWatch watch = StopWatch.start("Marker");

    CarData car = input.allCars.stream()
        .filter(carData -> carData.serialNumber == botIndex)
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Cannot find car."));

    // Only plan for cars that are on the ground.
    if (!car.hasWheelContact) {
      return;
    }

    BallPredictionUtil.get(ownerBot).getPredictions().stream()
        .limit((long) (BallPredictionUtil.PREDICTION_LIMIT * (ownerBot == botIndex ? .75 : .33)))
        .forEach(ballPrediction -> mark(ballPrediction, car));
    double timeMs = watch.stop() * 1000;
    if (timeMs > 1) {
      logger.debug("Marking for car {} done: {}", botIndex, String.format("%.2fms", timeMs));
    }
  }

  private void mark(BallPrediction ballPrediction, CarData car) {
    BallData ball = ballPrediction.ball;
    BallPrediction.Potential potential = ballPrediction.forCar(car.serialNumber);

    if (ball.position.distance(car.position) / Constants.BOOSTED_MAX_SPEED
        > ballPrediction.ball.time - car.elapsedSeconds) {

      // The ball is too far away to hit.
      return;
    }

    Optional<Plan> result = PathPlanner.getGroundPath(car, ball);
    if (result.isPresent()) {
      potential.addPlan(result.get());
    } else {
      Optional<Plan> airResult = AerialTactician.doAerialPlanning(car, ball);
      airResult.ifPresent(potential::addPlan);
    }
  }

  private int getPredictionIndex(int size) {
    return index++ % size;
  }

  public void markNext(DataPacket input) {
    mark(input, getPredictionIndex(input.allCars.size()));
  }
}
