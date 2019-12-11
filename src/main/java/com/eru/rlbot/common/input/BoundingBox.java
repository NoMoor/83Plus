package com.eru.rlbot.common.input;

import com.eru.rlbot.bot.common.Matrix3;
import com.eru.rlbot.common.vector.Vector3;

public class BoundingBox {

  public static final Vector3 RJ_OFFSET = Vector3.of(13.88, 0, 20.75);

  public static final float width = 84.2f;
  public static final float halfWidth = width / 2;
  public static final float length = 118.01f;
  public static final float halfLength = length / 2;
  public static final float height = 36.16f;
  public static final float halfHeight = height / 2;
  public static final float frontToRj = RJ_OFFSET.x + halfLength;

  public final Orientation orientation;
  public final Vector3 center;
  public final Vector3 flt;
  public final Vector3 flb;
  public final Vector3 frt;
  public final Vector3 frb;

  public final Vector3 rlt;
  public final Vector3 rlb;
  public final Vector3 rrt;
  public final Vector3 rrb;

  public BoundingBox(Vector3 centerOfRotation, Orientation orientation) {
    this.orientation = orientation;
    Matrix3 hitboxOrientationMatrix = orientation.getOrientationMatrix();

    this.center =  centerOfRotation.plus(orientation.getOrientationMatrix().dot(RJ_OFFSET));

    // TODO: Take into account the orientation of the hitbox.
    this.flt = this.center.plus(hitboxOrientationMatrix.dot(Vector3.of(halfLength, -halfWidth, halfHeight)));
    this.flb = this.center.plus(hitboxOrientationMatrix.dot(Vector3.of(halfLength, -halfWidth, -halfHeight)));
    this.frt = this.center.plus(hitboxOrientationMatrix.dot(Vector3.of(halfLength, halfWidth, halfHeight)));
    this.frb = this.center.plus(hitboxOrientationMatrix.dot(Vector3.of(halfLength, halfWidth, -halfHeight)));

    this.rlt = this.center.plus(hitboxOrientationMatrix.dot(Vector3.of(-halfLength, -halfWidth, halfHeight)));
    this.rlb = this.center.plus(hitboxOrientationMatrix.dot(Vector3.of(-halfLength, -halfWidth, -halfHeight)));
    this.rrt = this.center.plus(hitboxOrientationMatrix.dot(Vector3.of(-halfLength, halfWidth, halfHeight)));
    this.rrb = this.center.plus(hitboxOrientationMatrix.dot(Vector3.of(-halfLength, halfWidth, -halfHeight)));
  }
}
