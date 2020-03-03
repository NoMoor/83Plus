package com.eru.rlbot.bot.tactics;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.CarBall;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.main.ApolloGuidanceComputer;
import com.eru.rlbot.bot.prediction.NextFramePredictor;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.boost.BoostTracker;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.awt.Color;
import java.util.Optional;

// TODO: What does this do?

/**
 * Not really sure what this class does anymore.
 */
public class JumpFlipTactician extends Tactician {

  private static final float FLICK_TIME = .15f;

  JumpFlipTactician(ApolloGuidanceComputer bot, TacticManager tacticManager) {
    super(bot, tacticManager);
  }

  private static ImmutableList<Pair<Controls, CarData>> plannedControls;
  private static CarData previousCar;

  private static int planningTicks = 0;

  @Override
  void internalExecute(DataPacket input, Controls output, Tactic nextTactic) {
    if (resetTraining(input.car)) {
      plannedControls = null;
      planningTicks = 0;
    }

    if (plannedControls == null) {
      if (planningTicks++ < 20) {
        return;
      }
      plannedControls = planJumpFlip(input.car, input.ball);
    }

    bot.botRenderer.renderPath(plannedControls.stream()
        .filter(pair -> pair.getSecond().elapsedSeconds > input.car.elapsedSeconds)
        .map(Pair::getSecond)
        .map(carData -> carData.position)
        .limit(120)
        .collect(toImmutableList()));
    renderPredictionForFrame(input.car.elapsedSeconds);

    // TODO: Use make ball to check a ball several frames out.
    planFrame(input.car, input.ball, output, JumpManager.forCar(input.car));
  }

  private static float TIME_WINDOW = 1 / 240f;

  private void renderPredictionForFrame(float elapsedSeconds) {
    Optional<Pair<Controls, CarData>> prediction = plannedControls.stream()
        .filter(a -> elapsedSeconds - TIME_WINDOW < a.getSecond().elapsedSeconds)
        .findFirst();

    if (prediction.isPresent()) {
      bot.botRenderer.renderHitBox(Color.BLUE, prediction.get().getSecond());
    }
  }

  private boolean resetTraining(CarData car) {
    boolean jumped = false;
    if (previousCar != null) {
      jumped = previousCar.position.distance(car.position) > 200;
    }
    previousCar = car;
    return jumped;
  }

  private static final double maxTicks = 3.5 * Constants.STEP_SIZE_COUNT;

  private ImmutableList<Pair<Controls, CarData>> planJumpFlip(CarData car, BallData ball) {
    ImmutableList.Builder<Pair<Controls, CarData>> controlsListBuilder = ImmutableList.builder();

    JumpManager jumpManager = JumpManager.copyForCar(car);
    BoostTracker boostTracker = BoostTracker.copyForCar(car);

    int ticks = 0;
    while (car.position.distance(ball.position) > Constants.BALL_RADIUS * 1.5 && ticks < maxTicks) {
      jumpManager.trackInput(car);

      Controls controls = Controls.create();
      planFrame(car, ball, controls, jumpManager);

      CarData nextCar = NextFramePredictor.makePrediction(car, controls, boostTracker, jumpManager);
      jumpManager.trackOutput(car, controls);

      controlsListBuilder.add(Pair.of(controls, car));

      car = nextCar;
      ticks++;
    }

    return controlsListBuilder.build();
  }

  private static void planFrame(CarData car, BallData ball, Controls output, JumpManager jumpManager) {
    if (false) return;
    output.withThrottle(1.0);

    float heightDiff = ball.position.z - car.position.z;
    Optional<Float> jumpTime = Accels.jumpTimeToHeight(heightDiff);

    if (heightDiff < Constants.BALL_RADIUS) {
      if (jumpManager.canFlip()) {
        output.withJump()
            .withPitch(-1.0f);
      } else {
        output.withJump(false);
      }
    } else if (jumpTime.isPresent()) {
      Vector3 carPosition = CarBall.nearestPointOnHitBox(ball.position, car);
      double timeToBall = (ball.position.distance(carPosition) - Constants.BALL_RADIUS) / car.groundSpeed;
      if (timeToBall < FLICK_TIME + jumpTime.get()) {
        output.withJump(!jumpManager.hasMaxJumpHeight());
      }
    }
  }
}
