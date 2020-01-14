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

  public final boolean isLiveData;

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
    this.isLiveData = true;
  }

  private CarData(Builder builder) {
    this.position = builder.position;
    this.velocity = builder.velocity;

    this.groundSpeed = velocity.flatten().norm();

    this.orientation = builder.orientation;
    this.angularVelocity = builder.angularVelocity;
    this.boost = builder.boost;
    this.isSupersonic = groundSpeed > Constants.SUPER_SONIC;
    this.team = builder.team;
    this.hasWheelContact = builder.hasWheelContact;
    this.jumped = builder.jumped;
    this.doubleJumped = builder.doubleJumped;
    this.elapsedSeconds = builder.time;
    this.boundingBox = new BoundingBox(position, orientation);
    this.playerIndex = builder.playerIndex;
    this.isLiveData = false;
  }

  public static Builder builder() {
    return new Builder();
  }

  public CarData.Builder toBuilder() {
    return new Builder()
        .setPosition(position)
        .setTime(elapsedSeconds)
        .setVelocity(velocity)
        .setBoost(boost)
        .setOrientation(orientation)
        .setAngularVelocity(angularVelocity)
        .setHasWheelContact(hasWheelContact)
        .setTeam(team)
        .setJumped(jumped)
        .setDoubleJumped(doubleJumped)
        .setPlayerIndex(playerIndex);
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
    private boolean jumped;
    private boolean doubleJumped;
    private float time;
    private double boost;
    private boolean hasWheelContact;
    private Orientation orientation = Orientation.convert(0, 0, 0);
    private Vector3 velocity = Vector3.zero();
    private Vector3 position = Vector3.zero();
    private Vector3 angularVelocity = Vector3.zero();
    private boolean builderCalled = false;
    private int team;
    private int playerIndex;

    public CarData build() {
      if (builderCalled) {
        throw new IllegalStateException("Cannot call build again");
      }

      builderCalled = true;
      return new CarData(this);
    }

    public Builder setVelocity(Vector3 value) {
      this.velocity = value;
      return this;
    }

    public Builder setPosition(Vector3 value) {
      this.position = value;
      return this;
    }

    public Builder setTime(double value) {
      this.time = (float) value;
      return this;
    }

    public Builder setBoost(double value) {
      this.boost = (float) value;
      return this;
    }

    public Builder setOrientation(Orientation value) {
      this.orientation = value;
      return this;
    }

    public Builder setHasWheelContact(boolean value) {
      this.hasWheelContact = value;
      return this;
    }

    public Builder setAngularVelocity(Vector3 value) {
      this.angularVelocity = value;
      return this;
    }

    public Builder setTeam(int team) {
      this.team = team;
      return this;
    }

    public Builder setJumped(boolean jumped) {
      this.jumped = jumped;
      return this;
    }

    public Builder setDoubleJumped(boolean doubleJumped) {
      this.doubleJumped = doubleJumped;
      return this;
    }

    public Builder setPlayerIndex(int playerIndex) {
      this.playerIndex = playerIndex;
      return this;
    }
  }
}
