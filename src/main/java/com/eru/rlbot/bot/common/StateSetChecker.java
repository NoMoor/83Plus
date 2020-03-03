package com.eru.rlbot.bot.common;

import static com.eru.rlbot.bot.common.Constants.NORMAL_EXPECTED;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.vector.Vector3;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks when a car or ball has been state-set.
 */
public final class StateSetChecker {

  private static ConcurrentHashMap<Integer, StateSetChecker> MAP = new ConcurrentHashMap<>();

  public static StateSetChecker forIndex(int index) {
    MAP.computeIfAbsent(index, StateSetChecker::new);
    return MAP.get(index);
  }

  public static StateSetChecker forCar(CarData car) {
    return forIndex(car.serialNumber);
  }

  public static void track(DataPacket input) {
    forCar(input.car).trackForCar(input);
  }

  private Vector3 lastBallPosition;
  private Vector3 lastCarPosition;
  private boolean reset;

  private final int index;

  private StateSetChecker(int index) {
    this.index = index;
  }

  private void trackForCar(DataPacket input) {
    boolean ballJumped = ballHasJumped(input);
    boolean carJumped = carHasJumped(input);
    reset = ballJumped || carJumped;

    lastCarPosition = input.car.position;
    lastBallPosition = input.ball.position;
  }

  public boolean wasDemoed() {
    return reset;
  }

  private boolean carHasJumped(DataPacket input) {
    if (lastCarPosition == null) {
      return true;
    }

    return input.car.position.distance(lastCarPosition) > NORMAL_EXPECTED;
  }

  private boolean ballHasJumped(DataPacket input) {
    if (lastBallPosition == null) {
      return true;
    }

    return input.ball.position.distance(lastBallPosition) > NORMAL_EXPECTED;
  }

  public static boolean wasDemoed(CarData car) {
    return forCar(car).wasDemoed();
  }
}
