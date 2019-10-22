package com.eru.rlbot.common.input;


import com.eru.rlbot.bot.common.Matrix3;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.PlayerInfo;

/**
 * The car's orientation in space, a.k.a. what direction it's pointing.
 *
 * This class is here for your convenience, it is NOT part of the framework. You can change it as much
 * as you want, or delete it.
 */
public class CarOrientation extends Matrix3 {

    /** The direction that the front of the car is facing */
    public final Vector3 noseVector;

    /** The direction the roof of the car is facing. (0, 0, 1) means the car is upright. */
    public final Vector3 roofVector;

    /** The direction that the right side of the car is facing. */
    public final Vector3 rightVector;

    public CarOrientation(Vector3 noseVector, Vector3 roofVector) {
        super(noseVector, roofVector, noseVector.crossProduct(roofVector));
        this.noseVector = noseVector;
        this.roofVector = roofVector;
        this.rightVector = noseVector.crossProduct(roofVector);
    }

    public static CarOrientation fromFlatbuffer(PlayerInfo playerInfo) {
        return convert(
            playerInfo.physics().rotation().pitch(),
            playerInfo.physics().rotation().yaw(),
            playerInfo.physics().rotation().roll());
    }

    /**
     * All params are in radians.
     */
    private static CarOrientation convert(double pitch, double yaw, double roll) {

        double noseX = -1 * Math.cos(pitch) * Math.cos(yaw);
        double noseY = Math.cos(pitch) * Math.sin(yaw);
        double noseZ = Math.sin(pitch);

        double roofX = Math.cos(roll) * Math.sin(pitch) * Math.cos(yaw) + Math.sin(roll) * Math.sin(yaw);
        double roofY = Math.cos(yaw) * Math.sin(roll) - Math.cos(roll) * Math.sin(pitch) * Math.sin(yaw);
        double roofZ = Math.cos(roll) * Math.cos(pitch);

        return new CarOrientation(Vector3.of(noseX, noseY, noseZ), Vector3.of(roofX, roofY, roofZ));
    }
}
