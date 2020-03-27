package com.eru.rlbot.common;

import com.eru.rlbot.bot.prediction.BallPrediction;
import com.eru.rlbot.common.boost.BoostPad;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import rlbot.flat.PredictionSlice;

/**
 * Represents an event that will happen at a given time.
 */
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

  private Moment(BoostPad boostPad) {
    this(boostPad.getLocation(), boostPad.isLargeBoost() ? Type.LARGE_BOOST : Type.SMALL_BOOST);
  }

  private Moment(Builder builder) {
    this(builder.position, builder.velocity, builder.time, builder.type);
  }

  public static Moment from(Vector3 wayPoint) {
    return new Moment(wayPoint, Type.WAY_POINT);
  }

  public static Moment from(BoostPad pad) {
    return new Moment(pad);
  }

  public static Moment from(BallData ball) {
    return new Moment(ball);
  }

  public static Moment from(BallPrediction ballToHit) {
    return from(ballToHit.ball);
  }

  public static Moment from(CarData car) {
    return new Moment(car.position, car.velocity, car.elapsedSeconds, Type.CAR);
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

  public BallData toBall() {
    Preconditions.checkArgument(type == Moment.Type.BALL);
    return BallData.builder()
        .setPosition(position)
        .setTime(time)
        .setVelocity(velocity)
        .build();
  }

  public Moment.Builder toBuilder() {
    return builder()
        .setPosition(position)
        .setTime(time)
        .setType(type)
        .setVelocity(velocity);
  }

  public static Builder builder() {
    return new Builder();
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

  public static class Builder {

    private Vector3 position;
    private float time;
    private Type type;
    private Vector3 velocity;

    public Builder setPosition(Vector3 position) {
      this.position = position;
      return this;
    }

    public Builder setTime(float time) {
      this.time = time;
      return this;
    }

    public Builder setType(Type type) {
      this.type = type;
      return this;
    }

    public Builder setVelocity(Vector3 velocity) {
      this.velocity = velocity;
      return this;
    }

    public Moment build() {
      return new Moment(this);
    }
  }
}
