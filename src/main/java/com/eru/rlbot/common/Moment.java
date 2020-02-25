package com.eru.rlbot.common;

import com.eru.rlbot.bot.strats.BallPredictionUtil;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.base.Objects;
import rlbot.flat.PredictionSlice;

/** Represents a subject that will happen at a given time. */
public class Moment {

  public final Type type;
  public final Vector3 velocity;
  public final Vector3 position;
  public final float time;

  public Moment(PredictionSlice predictionSlice) {
    this(
        predictionSlice.physics().location(),
        predictionSlice.physics().velocity(),
        predictionSlice.gameSeconds(),
        Type.BALL);
  }

  public Moment(Vector3 position, Vector3 velocity) {
    this(position, velocity, 0, Type.BALL);
  }

  public Moment(Vector3 position, Type type) {
    this(position, Vector3.zero(), 0, type);
  }

  public Moment(Vector3 position, Vector3 velocity, float time, Type type) {
    this.position = position;
    this.velocity = velocity;
    this.time = time;
    this.type = type;
  }

  public Moment(rlbot.flat.Vector3 location, rlbot.flat.Vector3 velocity, float time, Type type) {
    this.position = Vector3.of(location);
    this.velocity = Vector3.of(velocity);
    this.time = time;
    this.type = type;
  }

  private Moment(BallData ball) {
    this(ball.position, ball.velocity, ball.time, Type.BALL);
  }

  public static Moment from(BallData ball) {
    return new Moment(ball);
  }

  public static Moment from(BallPredictionUtil.ExaminedBallData ballToHit) {
    return from(ballToHit.ball);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof Moment) {
      Moment m = (Moment) o;
      return Objects.equal(m.position, this.position)
          && Objects.equal(m.velocity, this.velocity)
          && Objects.equal(m.type, this.type)
          && Objects.equal(m.time, this.time);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(velocity, position, time, type);
  }

  public enum Type {
    BALL,
    CAR,
    WAY_POINT,
    SMALL_BOOST,
    LARGE_BOOST;

    boolean isBoost() {
      return this == SMALL_BOOST || this == LARGE_BOOST;
    }

    boolean isBall() {
      return this == BALL;
    }
  }
}
