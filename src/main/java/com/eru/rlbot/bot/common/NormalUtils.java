package com.eru.rlbot.bot.common;

import com.eru.rlbot.bot.main.EruBot;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import java.util.HashMap;
import java.util.Map;

public class NormalUtils {

  public static final Vector3 NOSE_NORTH = Vector3.of(0, 1, 0);

  private DataPacket cacheKey;
  private BallData cacheBall;
  private CarData cacheCar;

  private static final Map<Integer, NormalUtils> CACHE = new HashMap<>();
  private int index;

  public static NormalUtils from(int index) {
    if (!CACHE.containsKey(index)) {
      CACHE.put(index, new NormalUtils(index));
    }

    return CACHE.get(index);
  }

  public static NormalUtils from(EruBot bot) {
    return from(bot.getIndex());
  }

  public static NormalUtils from(DataPacket input) {
    return from(input.playerIndex);
  }

  private NormalUtils(int index) {
    this.index = index;
  }

  public static BallData noseRelativeBall(DataPacket input) {
    return from(input).noseNormalInternal(input);
  }

  public static BallData noseRelativeBall(DataPacket input, int index) {
    return from(index).noseNormalInternal(input);
  }

  /** Returns the ball position / velocity relative to the car position. */
  // Context: https://stackoverflow.com/questions/14607640/rotating-a-vector-in-3d-space
  private BallData noseNormalInternal(DataPacket input) {
    if (input == cacheKey) {
      return cacheBall;
    }

    update(input);
    return cacheBall;
  }


  public static CarData rollNormal(DataPacket input) {
    return from(input).rollNormalInternal(input);
  }

  /** Returns the car position / velocity relative to the ball velocity/postiion. */
  public CarData rollNormalInternal(DataPacket input) {
    if (input == cacheKey) {
      return cacheCar;
    }
    update(input);
    return cacheCar;
  }

  private void update(DataPacket input) {
    CarData car = input.allCars.get(index);

    cacheKey = input;
    cacheBall = BallData.builder()
        .setPosition(translateRelative(car.position, input.ball.position, car.orientation.getNoseVector()))
        .setVelocity(translateRelative(car.velocity, input.ball.velocity, car.orientation.getNoseVector()))
        .setTime(input.car.elapsedSeconds)
        .isLive()
        .isRelative()
        .build();

    Vector3 ballRoll = input.ball.velocity;
    if (ballRoll.norm() < 10) {
      // The ball is stationary enough. Normalize to the car.
      ballRoll = car.position.minus(input.ball.position).normalized();
    }

    // TODO: Set other car values.
    cacheCar = new CarData.Builder()
        .setPosition(translateRelative(input.ball.position, car.position, ballRoll))
        .setVelocity(translateRelative(input.ball.velocity, car.velocity, ballRoll))
        .build();
  }

  // TODO: Update this to use dot products...
  private static Vector3 translateRelative(Vector3 source, Vector3 target, Vector3 referenceOrientation) {
    Vector3 relativeVector = target.minus(source);

    // Translate the vector relative to the reference orientation.
    // This always returns a positive number....
    double translationAngle = referenceOrientation.flatten().correctionAngle(NOSE_NORTH.flatten()); // Negate to row correction angle to north.

    double relativeX = (Math.cos(translationAngle) * relativeVector.x) - (Math.sin(translationAngle) * relativeVector.y);
    double relativeY = (Math.sin(translationAngle) * relativeVector.x) + (Math.cos(translationAngle) * relativeVector.y);
    double relativeZ = relativeVector.z;

    return Vector3.of(relativeX, relativeY, relativeZ);
  }
}
