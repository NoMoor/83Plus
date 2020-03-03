package com.eru.rlbot.bot.prediction;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;

/**
 * Predicts where the car will be in a given time.
 */
public final class CarPrediction {

  /**
   * Makes a prediction assuming the user will have no inputs. This means that the car will continue to coast (not slow)
   * on the ground and fall in the air and will not change direction.
   */
  public static ImmutableList<PredictionNode> noInputs(CarData car, float timeToSimulate) {
    ImmutableList.Builder<PredictionNode> pathBuilder = ImmutableList.builder();
    float time = 0;

    Vector3 velocity = car.velocity;
    Vector3 position = car.position;
    while (time + Constants.STEP_SIZE < timeToSimulate) {

      position = position.plus(velocity.multiply(Constants.STEP_SIZE));

      if (position.z > Constants.CAR_AT_REST) {
        velocity = velocity.addZ(Constants.NEG_GRAVITY * Constants.STEP_SIZE);
      } else {
        // Zero out the z direction.
        velocity = velocity.addZ(-velocity.z);
      }

      time += Constants.STEP_SIZE;

      pathBuilder.add(new PredictionNode(position, car.elapsedSeconds + time));
    }

    return pathBuilder.build();
  }

  /** The result of a prediction. */
  public static class PredictionNode {

    public final Vector3 position;
    public final float absoluteTime;

    PredictionNode(Vector3 position, float absoluteTime) {
      this.position = position;
      this.absoluteTime = absoluteTime;
    }

    public Vector3 getPosition() {
      return position;
    }

    public float getAbsoluteTime() {
      return absoluteTime;
    }
  }

  private CarPrediction() {}
}
