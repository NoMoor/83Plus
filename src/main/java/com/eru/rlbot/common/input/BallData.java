package com.eru.rlbot.common.input;

import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallInfo;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Immutable information about the ball data.
 */
public class BallData {

    public final Vector3 position;
    public final Vector3 velocity;
    public final Vector3 spin;
    public final float elapsedSeconds;

    /** True if this is where the ball is currently, either in absolute or relative terms. */
    public final boolean isLiveData;

    /** False if this ball data is a transformation of ball data. True otherwise. */
    public final boolean isAbsolute;

    /** Constructs a {@link BallData} object from the flat buffer data. */
    public BallData(final BallInfo ball, final float elapsedTime) {
        this.position = Vector3.of(ball.physics().location());
        this.velocity = Vector3.of(ball.physics().velocity());
        this.spin = Vector3.of(ball.physics().angularVelocity());
        this.elapsedSeconds = elapsedTime;
        this.isLiveData = true;
        this.isAbsolute = true;
    }

    private BallData(Builder builder) {
        this.position = builder.position;
        this.velocity = builder.velocity;
        this.spin = builder.spin;
        this.elapsedSeconds = builder.elapsedSeconds;
        this.isLiveData = builder.isLiveData;
        this.isAbsolute = !builder.isRelativeData;
    }

    /** Returns a new empty builder for {@link BallData}. */
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

    /** A builder for {@link BallData}. */
    public final static class Builder {
        private Vector3 position;
        private Vector3 velocity;
        private Vector3 spin;
        private float elapsedSeconds;
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

        public Builder setTime(float elapsedSeconds) {
            this.elapsedSeconds = elapsedSeconds;
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
