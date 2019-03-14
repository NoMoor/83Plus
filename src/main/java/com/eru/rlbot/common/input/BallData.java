package com.eru.rlbot.common.input;


import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.BallInfo;

/**
 * Basic information about the ball.
 *
 * This class is here for your convenience, it is NOT part of the framework. You can change it as much
 * as you want, or delete it.
 */
public class BallData {
    public final Vector3 position;
    public final Vector3 velocity;
    public final Vector3 spin;

    public BallData(final BallInfo ball) {
        this.position = new Vector3(ball.physics().location());
        this.velocity = new Vector3(ball.physics().velocity());
        this.spin = new Vector3(ball.physics().angularVelocity());
    }
}
