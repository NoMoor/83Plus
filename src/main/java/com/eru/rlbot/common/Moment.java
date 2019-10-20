package com.eru.rlbot.common;

import com.eru.rlbot.common.vector.Vector3;

/** Represents a target that will happen at a given time. */
public class Moment {

  public final Vector3 velocity;
  public final Vector3 position;
  public final float time;

  public Moment(Vector3 position) {
    this(position, new Vector3(), 300);
  }

  public Moment(Vector3 position, Vector3 velocity, float time) {
    this.position = position;
    this.velocity = velocity;
    this.time = time;
  }

  public Moment(rlbot.flat.Vector3 location, rlbot.flat.Vector3 velocity, float time) {
    this.position = new Vector3(location);
    this.velocity = new Vector3(velocity);
    this.time = time;
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
