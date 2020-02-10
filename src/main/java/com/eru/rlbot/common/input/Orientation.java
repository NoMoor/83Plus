package com.eru.rlbot.common.input;


import com.eru.rlbot.bot.common.Matrix3;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import rlbot.flat.PlayerInfo;
import rlbot.gamestate.DesiredRotation;

/**
 * The car's orientation in space, a.k.a. what direction it's pointing.
 *
 * This class is here for your convenience, it is NOT part of the framework. You can change it as much
 * as you want, or delete it.
 */
public class Orientation {

  private final Matrix3 orientation;

  public static Orientation fromOrientationMatrix(Matrix3 orientationMatrix) {
    return new Orientation(orientationMatrix.column(0), orientationMatrix.column(1), orientationMatrix.column(2));
  }

  public Orientation(Vector3 noseVector, Vector3 leftVector, Vector3 roofVector) {
    orientation = Matrix3.of(noseVector, leftVector, roofVector);
  }

  public static Orientation noseRoof(Vector3 noseVector, Vector3 roofVector) {
    return new Orientation(noseVector, noseVector.cross(roofVector).multiply(-1), roofVector);
  }

  public static Orientation fromFlatbuffer(PlayerInfo playerInfo) {
    return convert(
        playerInfo.physics().rotation().pitch(),
        playerInfo.physics().rotation().yaw(),
        playerInfo.physics().rotation().roll());
  }

  public static Orientation fromFlatOrientation(CarData car) {
    return convert(0, car.orientation.toEuclidianVector().yaw, 0);
  }

  public static Orientation fromFlatVelocity(CarData car) {
    return fromFlatVelocity(car.velocity);
  }

  public static Orientation fromFlatVelocity(Vector3 velocity) {
    return convert(0, Vector2.WEST.correctionAngle(velocity.flatten()), 0);
  }

  /**
   * The direction that the front of the car is facing
   */
  public Vector3 getNoseVector() {
    return orientation.row(0);
  }

  /**
   * The direction that the left side of the car is facing.
   */
  public final Vector3 getLeftVector() {
    return orientation.row(1);
  }

  /**
   * The direction the roof of the car is facing. (0, 0, 1) means the car is upright.
   */
  public final Vector3 getRoofVector() {
    return orientation.row(2);
  }

  /**
   * Returns the 3x3 matrix representing the nose, right, and roof vectors respectively.
   */
  public Matrix3 getOrientationMatrix() {
    return orientation.transpose();
  }

  /** All params are in radians. */
  public static Orientation convert(double pitch, double yaw, double roll) {
    double noseX = Math.cos(pitch) * Math.cos(yaw);
    double noseY = Math.cos(pitch) * Math.sin(yaw);
    double noseZ = Math.sin(pitch);

    double leftX = Math.cos(yaw) * Math.sin(pitch) * Math.sin(roll) - Math.cos(roll) * Math.sin(yaw);
    double leftY = Math.sin(yaw) * Math.sin(pitch) * Math.sin(roll) + Math.cos(roll) * Math.cos(yaw);
    double leftZ = -Math.cos(pitch) * Math.sin(roll);

    double roofX = -Math.cos(roll) * Math.cos(yaw) * Math.sin(pitch) - Math.sin(roll) * Math.sin(yaw);
    double roofY = Math.cos(yaw) * Math.sin(roll) - Math.cos(roll) * Math.sin(pitch) * Math.sin(yaw);
    double roofZ = Math.cos(roll) * Math.cos(pitch);

    return new Orientation(
        Vector3.of(noseX, noseY, noseZ),
        Vector3.of(leftX, leftY, leftZ),
        Vector3.of(roofX, roofY, roofZ));
  }

  public DesiredRotation toEuclidianVector() {
    Matrix3 orientationMatrix = this.getOrientationMatrix();

    double pitch = -Math.asin(orientationMatrix.row(2).x);  //Pitch
    double yaw, roll;

    if (pitch == 1) {
      // Gimbal lock: pitch = -90
      yaw = 0.0;
      roll = Math.atan2(-orientationMatrix.row(0).y, -orientationMatrix.row(0).z);
    } else if (pitch == -1) {
      // Gimbal lock: pitch = 90
      yaw = 0.0;
      roll = Math.atan2(orientationMatrix.row(0).y, orientationMatrix.row(0).z);
    } else {
      // General solution
      yaw = Math.atan2(orientationMatrix.row(1).x, orientationMatrix.row(0).x);
      roll = Math.atan2(orientationMatrix.row(2).y, orientationMatrix.row(2).z);
    }
    return new DesiredRotation((float) pitch, (float) yaw, (float) roll);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Orientation)) {
      return false;
    }

    Orientation other = (Orientation) o;
    return this.orientation.equals(other.orientation);
  }
}
