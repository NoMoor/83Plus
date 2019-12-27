package com.eru.rlbot.common.input;


import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.gamestate.CarState;
import rlbot.gamestate.DesiredVector3;
import rlbot.gamestate.PhysicsState;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Basic information about the car.
 *
 * This class is here for your convenience, it is NOT part of the framework. You can change it as much
 * as you want, or delete it.
 */
public class CarData {

  /** The location of the car on the field. (0, 0, 0) is center field. */
  public final Vector3 position;

  /** The velocity of the car. */
  public final Vector3 velocity;

  /**
   * The result of calling velocity.flatten().magnitude().
   */
  public final double groundSpeed;

  /** The orientation of the car */
  public final Orientation orientation;

  /** The angular velocity of the car. */
  public final Vector3 angularVelocity;

  public final int playerIndex;

  /** Boost ranges from 0 to 100 */
  public final double boost;

  /** True if the car is driving on the ground, the wall, etc. In other words, true if you can steer. */
  public final boolean hasWheelContact;

  /**
   * True if the car is showing the supersonic and can demolish enemies on contact.
   * This is a close approximation for whether the car is at max speed.
   */
  public final boolean isSupersonic;

  /**
   * 0 for blue team, 1 for orange team.
   */
  public final int team;

  /**
   * This is not really a car-specific attribute, but it's often very useful to know. It's included here
   * so you don't need to pass around DataPacket everywhere.
   */
  public final float elapsedSeconds;
  public final boolean jumped;
  public final boolean doubleJumped;
  public final BoundingBox boundingBox;

  public CarData(rlbot.flat.PlayerInfo playerInfo, float elapsedSeconds, int playerIndex) {
    this.position = Vector3.of(playerInfo.physics().location());
    this.velocity = Vector3.of(playerInfo.physics().velocity());
    this.angularVelocity = Vector3.of(playerInfo.physics().angularVelocity());
    this.playerIndex = playerIndex;

    this.groundSpeed = velocity.flatten().norm();

    this.orientation = Orientation.fromFlatbuffer(playerInfo);
    this.boost = playerInfo.boost();
    this.isSupersonic = playerInfo.isSupersonic();
    this.team = playerInfo.team();
    this.hasWheelContact = playerInfo.hasWheelContact();
    this.elapsedSeconds = elapsedSeconds;

    this.jumped = playerInfo.jumped();
    this.doubleJumped = playerInfo.doubleJumped();

    this.boundingBox = new BoundingBox(position, orientation);
  }

  private CarData(Builder builder) {
    this.position = builder.position;
    this.velocity = builder.velocity;

    this.groundSpeed = velocity.flatten().norm();

    this.orientation = builder.orientation;
    this.angularVelocity = builder.angularVelocity;
    this.boost = builder.boost;
    this.isSupersonic = groundSpeed > Constants.SUPER_SONIC;
    this.team = 1;
    this.hasWheelContact = false;
    this.jumped = false;
    this.doubleJumped = false;
    this.elapsedSeconds = builder.time;
    this.boundingBox = new BoundingBox(position, orientation);
    this.playerIndex = 0;
  }

  public static Builder builder() {
    return new Builder();
  }

  public CarData.Builder toBuilder() {
    Builder builder = new Builder()
        .setPosition(position)
        .setTime(elapsedSeconds)
        .setVelocity(velocity);

    builder.boost = this.boost;
    builder.orientation = this.orientation;
    builder.angularVelocity = this.angularVelocity;
    return builder;
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

  public CarState toCarState() {
    return new CarState()
        .withBoostAmount((float) this.boost)
        .withPhysics(new PhysicsState()
            .withLocation(new DesiredVector3(position.x, position.y, position.z))
            .withVelocity(new DesiredVector3(velocity.x, velocity.y, velocity.z))
            .withRotation(orientation.toEuclidianVector())
            .withAngularVelocity(new DesiredVector3(angularVelocity.x, angularVelocity.y, angularVelocity.z)));
  }

  public static class Builder {
    private float time;
    public double boost;
    public Orientation orientation = Orientation.convert(0, 0, 0);
    public Vector3 velocity = Vector3.zero();
    public Vector3 position = Vector3.zero();
    public Vector3 angularVelocity = Vector3.zero();

    private boolean builderCalled = false;
    public CarData build() {
      if (builderCalled) {
        throw new IllegalStateException("Cannot call build again");
      }

      builderCalled = true;
      return new CarData(this);
    }

    public Builder setVelocity(Vector3 velocity) {
      this.velocity = velocity;
      return this;
    }

    public Builder setPosition(Vector3 position) {
      this.position = position;
      return this;
    }

    public Builder setTime(double time) {
      this.time = (float) time;
      return this;
    }

    public Builder setOrientation(Orientation orientation) {
      this.orientation = orientation;
      return this;
    }
  }
}
