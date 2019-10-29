package com.eru.rlbot.common.input;


import com.eru.rlbot.common.vector.Vector3;

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

    /** The result of calling velocity.flatten().norm(). */
    public final double groundSpeed;

    /** The orientation of the car */
    public final CarOrientation orientation;

    /** The angular velocity of the car. */
    public final Vector3 angularVelocity;

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

    public CarData(rlbot.flat.PlayerInfo playerInfo, float elapsedSeconds) {
        this.position = Vector3.of(playerInfo.physics().location());
        this.velocity = Vector3.of(playerInfo.physics().velocity());
        this.angularVelocity = Vector3.of(playerInfo.physics().angularVelocity());

        this.groundSpeed = velocity.flatten().magnitude();

        this.orientation = CarOrientation.fromFlatbuffer(playerInfo);
        this.boost = playerInfo.boost();
        this.isSupersonic = playerInfo.isSupersonic();
        this.team = playerInfo.team();
        this.hasWheelContact = playerInfo.hasWheelContact();
        this.elapsedSeconds = elapsedSeconds;
    }

    private CarData(Builder builder) {
        this.position = builder.position;
        this.velocity = builder.velocity;

        this.groundSpeed = velocity.flatten().magnitude();

        this.orientation = builder.orientation;
        this.angularVelocity = builder.angularVelocity;
        this.boost = builder.boost;
        this.isSupersonic = groundSpeed > 2200;
        this.team = 1;
        this.hasWheelContact = false;
        this.elapsedSeconds = builder.time;
    }

    public static class Builder {
        public float time;
        public double boost;
        public CarOrientation orientation;
        public Vector3 velocity;
        public Vector3 position;
        public Vector3 angularVelocity;

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
    }
}
