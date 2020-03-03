package com.eru.rlbot.bot.prediction;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.flags.PerBotDebugOptions;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.common.Lists;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MoreCollectors;
import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CarLocationPredictor {

  private static final Logger logger = LogManager.getLogger("CarLocationPredictor");

  private static final ConcurrentHashMap<Integer, CarLocationPredictor> MAP = new ConcurrentHashMap<>();

  private static final LinkedHashMap<Integer, CarLocationPrediction> predictions = new LinkedHashMap<>();

  private final int playerIndex;
  private final int team;

  public CarLocationPredictor(int playerIndex, int team) {
    this.playerIndex = playerIndex;
    this.team = team;
  }

  public static void track(DataPacket input) {
    forCar(input.car).trackInternal(input);
  }

  public static CarLocationPredictor forCar(CarData car) {
    return MAP.computeIfAbsent(car.serialNumber, i -> new CarLocationPredictor(car.serialNumber, car.team));
  }

  public ImmutableList<CarLocationPrediction> allies() {
    return predictions.values().stream()
        .filter(prediction -> prediction.getTeamIndex() == team)
        .collect(toImmutableList());
  }

  public ImmutableList<CarLocationPrediction> opponents() {
    return predictions.values().stream()
        .filter(prediction -> prediction.getTeamIndex() != team)
        .collect(toImmutableList());
  }

  public CarLocationPrediction forOpponent(CarData car) {
    return predictions.values().stream()
        .filter(predictions -> predictions.getPlayerIndex() == car.serialNumber)
        .collect(MoreCollectors.onlyElement());
  }

  private void trackInternal(DataPacket input) {
    if (input.gameInfo.isRoundActive()) {
      for (CarData car : input.allCars) {
        // Skip self.
        if (car == input.car) {
          continue;
        }

        CarLocationPrediction prediction =
            predictions.computeIfAbsent(car.serialNumber, index -> new CarLocationPrediction(input.serialNumber));

        prediction.updatePrediction(car);
      }
    }

    if (PerBotDebugOptions.get(input.serialNumber).isRenderOpponentPaths()) {
      predictions.values().forEach(CarLocationPrediction::renderPrediction);
    }
  }

  private static final float PREDICTION_LENGTH = 1.0f;

  public static class CarLocationPrediction {

    private final int owningPlayerIndex;

    private volatile CarData previousCar;
    private volatile ImmutableList<CarPrediction.PredictionNode> predictions;

    /**
     * Constructed with the index of the player that owns this prediction. This is different from the player index of
     * the car being predicted.
     */
    public CarLocationPrediction(int owningPlayerIndex) {
      this.owningPlayerIndex = owningPlayerIndex;
    }

    public int getPlayerIndex() {
      return previousCar.serialNumber;
    }

    public int getTeamIndex() {
      return previousCar.team;
    }

    private static final int PRUNE_DENSITY = 10;
    private static final ImmutableList<Color> PREDICTION_COLORS = ImmutableList.of(
        Color.GREEN,
        Color.GREEN.darker(),
        Color.GREEN.darker().darker(),
        Color.GREEN.darker().darker().darker(),
        Color.GREEN.darker().darker().darker().darker());

    public void renderPrediction() {
      if (predictions == null || predictions.isEmpty()) {
        return;
      }

      ImmutableList<CarPrediction.PredictionNode> prunedPath = Lists.everyNth(predictions, PRUNE_DENSITY);
      ImmutableList.Builder<Pair<Color, ImmutableList<Vector3>>> splitPathBuilder = ImmutableList.builder();

      int colorIndex = 0;
      float timeIncrement = PREDICTION_LENGTH / (PREDICTION_COLORS.size() - 1);
      float timeBoundary = timeIncrement;
      ImmutableList.Builder<Vector3> nextNodes = ImmutableList.builder();
      for (CarPrediction.PredictionNode node : prunedPath) {
        float predictionLength = node.absoluteTime - previousCar.elapsedSeconds;

        if (predictionLength > timeBoundary) {
          splitPathBuilder.add(Pair.of(PREDICTION_COLORS.get(colorIndex++), nextNodes.build()));
          nextNodes = ImmutableList.builder();
          timeBoundary += timeIncrement;
        }

        nextNodes.add(node.position);
      }

      BotRenderer.forIndex(owningPlayerIndex).renderPaths(splitPathBuilder.build());
    }

    public void updatePrediction(CarData car) {
      predictions = CarPrediction.noInputs(car, PREDICTION_LENGTH);
      previousCar = car;
    }

    public ImmutableList<CarPrediction.PredictionNode> getPredictions() {
      return predictions;
    }
  }
}
