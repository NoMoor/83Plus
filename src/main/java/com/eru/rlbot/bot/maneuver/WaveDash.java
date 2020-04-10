package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Matrix3;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import java.awt.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Performs a wave-dash.
 */
public class WaveDash extends Maneuver {

  private static final Logger logger = LogManager.getLogger("WaveDash");

  public static final double MIN_DASH_TIME = 1;

  private static final Matrix3 WAVE_DASH_TILT = Orientation.convert(.1, 0, 0).getOrientationMatrix();

  private boolean wasInAir;
  private boolean complete;

  private final Vector3 target;
  private final float boostAllowed;

  public WaveDash(Builder builder) {
    target = builder.target;
    boostAllowed = builder.boostAllowed;
  }

  @Override
  public void execute(DataPacket input, Controls output, Tactic tactic) {
    Vector3 relativeTargetDirection = target.minus(input.car.position);
    WaveDashProfile profile = WaveDashProfile.create(input.car, relativeTargetDirection);
    JumpManager jumpManager = JumpManager.forCar(input.car);
    BotRenderer botRenderer = BotRenderer.forCar(input.car);

    output.withThrottle(1.0); // Don't get stuck not moving.

    if (wasInAir && input.car.hasWheelContact) {
      output.withSlide();
      if (input.car.angularVelocity.flatten().magnitude() < .5) {
        complete = true;
      }
    } else if (!input.car.hasWheelContact) {
      botRenderer.setBranchInfo("Setup for dash: " + (profile.isLargeTurn ? "large" : "small"));
      wasInAir = true;
      output
          .withThrottle(1.0)
          .withSlide();

      double noseVelocityCorrection =
          Angles.flatCorrectionAngle(Vector3.zero(), input.car.orientation.getNoseVector(), input.car.velocity);

      if (input.car.velocity.z > 20
          && boostAllowed > 0
          && Math.abs(noseVelocityCorrection) < .1) {
        output
            .withPitch(input.car.orientation.getNoseVector().z > -.09 ? -1 : 1)
            .withBoost(input.car.orientation.getNoseVector().z < -.05 && input.car.groundSpeed < 1780)
            .withSlide();
      } else {
        output.withJump(jumpManager.canJumpAccelerate());

        Matrix3 targetOrientation = targetOrientation(profile);

        CarData desiredCar = input.car.toBuilder()
            .setOrientation(Orientation.fromOrientationMatrix(targetOrientation))
            .build();
        botRenderer.renderHitBox(Color.MAGENTA, desiredCar);

        Angles3.setControlsFor(input.car, targetOrientation, output);

        rlbot.vector.Vector3 lowestCorner = input.car.boundingBox.getLowestCorner();
        if (input.car.velocity.z < 0 && lowestCorner.z < 33 && jumpManager.canFlip()) {
          Pair<Float, Float> pitchYaw = findPitchYaw(profile);
          output
              .withJump()
              .withPitch(pitchYaw.getFirst())
              .withYaw(pitchYaw.getSecond())
              .withSlide();
        }
      }
    } else {
      // Make sure we don't drop a frame when we should be jumping.
      output.withJump(output.holdJump() || (profile.isLargeTurn && jumpManager.canJumpAccelerate()));
    }
  }

  public static Matrix3 targetOrientation(WaveDashProfile profile) {
    Orientation waveDashTiltRotator =
        Orientation.convert(0, profile.targetDashYaw - profile.targetCarYaw, 0);
    Orientation correctionDiffRotator =
        Orientation.convert(0, profile.targetDashYaw, 0);

    Matrix3 tiltedCar = WAVE_DASH_TILT.rotateOrientation(waveDashTiltRotator.getOrientationMatrix().transpose());
    return correctionDiffRotator.getOrientationMatrix().rotateOrientation(tiltedCar);
  }

  private Pair<Float, Float> findPitchYaw(WaveDashProfile profile) {
    return Pair.of((float) -profile.flatLocalDashDirection.x, (float) profile.flatLocalDashDirection.y);
  }

  @Override
  public boolean isComplete() {
    return complete;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Vector3 target;
    private float boostAllowed;

    public Builder withTarget(Vector3 target) {
      this.target = target;
      return this;
    }

    public Builder withBoost(float boostAllowed) {
      this.boostAllowed = boostAllowed;
      return this;
    }

    public WaveDash build() {
      return new WaveDash(this);
    }
  }

  /**
   * Storage class for keeping consistent values for targets across rotations and dodging.
   */
  private static class WaveDashProfile {
    private final double targetCarYaw;
    private final double targetDashYaw;
    private final Vector2 flatLocalDashDirection;
    private boolean isLargeTurn;

    private WaveDashProfile(CarData car, Vector3 targetVector) {
      double currentCarYaw = car.orientation.toEuclidianVector().yaw;
      double targetYaw = Orientation.fromFlatVelocity(targetVector).toEuclidianVector().yaw;
      double carTargetYawDiff = car.orientation.getNoseVector().flatten().correctionAngle(targetVector.flatten());
      double velocityTargetYawDiff = Angles.flatCorrectionAngle(Vector3.zero(), car.velocity, targetVector);

      if (Math.abs(carTargetYawDiff) > 2.5 && (car.velocity.z > 0 || JumpManager.forCar(car).canJumpAccelerate())) {
        targetCarYaw = currentCarYaw + carTargetYawDiff;
        isLargeTurn = true;
      } else {
        targetCarYaw = currentCarYaw + (carTargetYawDiff * .3);
      }
      targetDashYaw = targetYaw + velocityTargetYawDiff;
      Vector3 dashDirection = Orientation.convert(0, targetDashYaw, 0).getNoseVector();

      Vector3 localDashDirection = car.orientation.getOrientationMatrix().transpose().dot(dashDirection);
      flatLocalDashDirection = localDashDirection.flatten().normalized();
    }

    public static WaveDashProfile create(CarData car, Vector3 relativeTargetDirection) {
      return new WaveDashProfile(car, relativeTargetDirection);
    }
  }
}
