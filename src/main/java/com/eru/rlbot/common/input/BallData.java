package com.eru.rlbot.common.input;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.vector.Vector3;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import rlbot.flat.BallInfo;
import rlbot.flat.PredictionSlice;

/**
 * Immutable information about the ball data.
 */
public class BallData {

  public final Vector3 position;
  public final Vector3 velocity;
  public final Vector3 spin;
  public final float time;

  /**
   * True if this is where the ball is currently, either in absolute or relative terms.
   */
  public final boolean isLiveData;

  /**
   * False if this ball data is a transformation of ball data. True otherwise.
   */
  public final boolean isAbsolute;

  /**
   * Constructs a {@link BallData} object from the flat buffer data.
   */
  public BallData(final BallInfo ball, final float time) {
    this.position = Vector3.of(ball.physics().location());
    this.velocity = Vector3.of(ball.physics().velocity());
    this.spin = Vector3.of(ball.physics().angularVelocity());
    this.time = time;
    this.isLiveData = true;
    this.isAbsolute = true;
  }

  public static BallData fromPredictionSlice(PredictionSlice predictionSlice) {
    return new BallData(predictionSlice);
  }

  private BallData(PredictionSlice predictionSlice) {
    this.position = Vector3.of(predictionSlice.physics().location());
    this.velocity = Vector3.of(predictionSlice.physics().velocity());
    this.spin = Vector3.of(predictionSlice.physics().angularVelocity());
    this.time = predictionSlice.gameSeconds();
    this.isLiveData = false;
    this.isAbsolute = true;
  }

  private BallData(Builder builder) {
    this.position = builder.position;
    this.velocity = builder.velocity;
    this.spin = builder.spin;
    this.time = builder.time;
    this.isLiveData = builder.isLiveData;
    this.isAbsolute = !builder.isRelativeData;
  }

  /**
   * Returns a new empty builder for {@link BallData}.
   */
  public static Builder builder() {
    return new Builder();
  }

  public static String csvHeader(String label) {
    return Stream.of("x", "y", "z", "vx", "vy", "vz")
        .collect(Collectors.joining(String.format("%s,", label))) + label + ",";
  }

  public String toCsv() {
    return String.format(
        "%f,%f,%f,%f,%f,%f,",
        position.x,
        position.y,
        position.z,
        velocity.x,
        velocity.y,
        velocity.z);
  }

  public boolean fuzzyEquals(BallData ball) {
    double stepDrift = Math.max(Math.abs(time - ball.time) / Constants.STEP_SIZE, 1);

    return Math.abs(time - ball.time) < (Constants.STEP_SIZE * 3)
        && spin.isWithin(.5 * stepDrift).of(ball.spin)
        && position.isWithin(20 * stepDrift).of(ball.position)
        && velocity.isWithin(30 * stepDrift).of(ball.velocity);
  }

  /**
   * A builder for {@link BallData}.
   */
  public final static class Builder {
    private Vector3 position = Vector3.zero();
    private Vector3 velocity = Vector3.zero();
    private Vector3 spin = Vector3.zero();
    private float time;
    private boolean isLiveData;
    private boolean isRelativeData;

    public Builder setPosition(Vector3 position) {
      this.position = position;
      return this;
    }

    public Builder setVelocity(Vector3 velocity) {
      this.velocity = velocity;
      return this;
    }

    public Builder setSpin(Vector3 spin) {
      this.spin = spin;
      return this;
    }

    public Builder setTime(float time) {
      this.time = time;
      return this;
    }

    public Builder isLive() {
      this.isLiveData = true;
      return this;
    }

    public Builder isRelative() {
      this.isRelativeData = true;
      return this;
    }

    public BallData build() {
      return new BallData(this);
    }
  }
}
