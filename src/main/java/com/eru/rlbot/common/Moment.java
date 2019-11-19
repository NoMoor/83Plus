package com.eru.rlbot.common;

import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.PredictionSlice;

/** Represents a targetMoment that will happen at a given time. */
public class Moment {

  public final Vector3 velocity;
  public final Vector3 position;
  public final float time;

  public Moment(PredictionSlice predictionSlice) {
    this(
        predictionSlice.physics().location(),
        predictionSlice.physics().velocity(),
        predictionSlice.gameSeconds());
  }

  public Moment(Vector3 position, Vector3 velocity) {
    this.position = position;
    this.velocity = velocity;
    this.time = 300;
  }

  public Moment(Vector3 position, Vector3 velocity, float time) {
    this.position = position;
    this.velocity = velocity;
    this.time = time;
  }

  public Moment(rlbot.flat.Vector3 location, rlbot.flat.Vector3 velocity, float time) {
    this.position = Vector3.of(location);
    this.velocity = Vector3.of(velocity);
    this.time = time;
  }

  public Moment(BallData ball) {
    this.position = ball.position;
    this.velocity = ball.velocity;
    this.time = 300;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof Moment) {
      Moment m = (Moment) o;
      return m.position.equals(this.position)
          && m.velocity.equals(this.velocity)
          && m.time == this.time;
    }

    return false;
  }
}
