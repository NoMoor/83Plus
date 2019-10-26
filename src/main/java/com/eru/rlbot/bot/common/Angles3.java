package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

/** Utilities for calculating rotations in 3d space. */
// https://github.com/samuelpmish/RLUtilities/blob/master/src/mechanics/aerial_turn.cc
public class Angles3 {

  // The allowed deviation of Phi
  private static final double EPSILON_PHI = .1f;
  // The allowed deviation of Omega
  private static final double EPSILON_OMEGA = .15f;

  private static final double SCALE = 10.5f;

  // TODO: Some of these values may need to be inverted. Particularly the x vector.
  private static final Vector3 ANGULAR_ACCELERATION = Vector3.of(-400.0f, -130.0f, 95.0f).scaled(1/SCALE);
  private static final Vector3 ANGULAR_DAMPING = Vector3.of(-50.0f, -30.0f, -20.0f).scaled(1/SCALE);

  // How far ahead to look.
  private static final float HORIZON_TIME = .05f;

  /** Returns controls to optimally rotate toward the target orientation. */
  public static boolean setControlsFor(CarData car, Matrix3 target, ControlsOutput controls) {
    // Omega = Velocity
    Vector3 omega = target.transpose().dot(car.angularVelocity);

    // Theta = orientation
    Matrix3 theta = target.transpose().dot(car.orientation.getOrientationMatrix());
    Vector3 omega_local = omega.dot(theta);

    Vector3 phi = rotation_to_axis(theta);

    boolean finished = (phi.norm() < EPSILON_PHI) && (omega.norm() < EPSILON_OMEGA);

    if (finished) {
      return true;
    }

    Matrix3 z0 = z(phi);
//    Vector3 dphi_dt = z0.dot(omega); // This doesn't seem to be used. ???

    float horizon_time = Math.max(0.03f, 4.0f * HORIZON_TIME);

    Vector3 alpha = Vector3.zero();

    // Apply a few Newton iterations to find
    // local angular accelerations that try not to overshoot
    // the guideline trajectories defined by AerialTurn::G().
    //
    // This helps to ensure monotonic convergence to the
    // desired orientation, when possible.
    int n_iter = 5;
    double eps = 0.001f;
    float offset = 0.00001f;
    for (int i = 0; i < n_iter; i++) {
      Vector3 f0 = f(alpha, horizon_time, theta, omega, z0, phi);

      // TODO: Choose an implementation.
//      Vector3 df_j0 = f0
//          .minus(f(alpha.plus(Matrix3.IDENTITY.row(0).scaled(eps)), horizon_time, theta, omega, z0, phi))
//          .scaled(1 / eps);
//      df_j0 = Vector3.of(df_j0.x + offset, df_j0.y, df_j0.z);
//
//      Vector3 df_j1 = f0
//          .minus(f(alpha.plus(Matrix3.IDENTITY.row(1).scaled(eps)), horizon_time, theta, omega, z0, phi))
//          .scaled(1 / eps);
//      df_j1 = Vector3.of(df_j1.x, df_j1.y + offset, df_j1.z);
//
//      Vector3 df_j2 = f0
//          .minus(f(alpha.plus(Matrix3.IDENTITY.row(2).scaled(eps)), horizon_time, theta, omega, z0, phi))
//          .scaled(1 / eps);
//      df_j2 = Vector3.of(df_j2.x, df_j2.y, df_j2.z + offset);
//
//      Matrix3 J = Matrix3.of(df_j0, df_j1, df_j2);

      List<Vector3> vector3List = new ArrayList<>(3);
      for (int j = 0; j < 3; j++) {

        Vector3 epsIdentity = Matrix3.IDENTITY.row(j).scaled(eps);

        Vector3 diff = f(alpha.plus(epsIdentity), horizon_time, theta, omega, z0, phi);

        Vector3 v = f0.minus(diff).scaled(1 / eps);
        v = (i == 0) ? v.addX(offset) : (i == 1) ? v.addY(offset) : v.addZ(offset);
        vector3List.add(v);
      }

      Matrix3 J = Matrix3.of(vector3List.get(0), vector3List.get(1), vector3List.get(2)).transpose();

      Vector3 delta_alpha = J.inverse().dot(f0);

      alpha = alpha.plus(delta_alpha);

      if (delta_alpha.norm() < 1.0f) break;
    }

    Vector3 rpy = find_controls_for(alpha, omega_local);

    controls.withRoll(rpy.x);
    controls.withPitch(rpy.y);
    controls.withYaw(rpy.z);

    return false;
  }

  // This matrix is used to relate the angular
  // velocity to the time rate of the components
  // in the axis-angle representation of the
  // orientation error, phi. It is defined
  // such that we can write dphi_dt in the
  // following way:
  //
  //    Vector3 dphi_dt = dot(Z(phi), omega)
  //
  private static Matrix3 z(Vector3 q) {

    double norm_q = q.norm();

    // for small enough values, use the taylor expansion
    if (norm_q < 0.2f) {

      return Matrix3.of(
          Vector3.of(
              1.0f - (q.y*q.y + q.z*q.z) / 12.0f,
              (q.x*q.y / 12.0f) + q.z / 2.0f,
              (q.x*q.z / 12.0f) - q.y / 2.0f),
          Vector3.of(
              (q.y*q.x / 12.0f) - q.z / 2.0f,
              1.0f - (q.x*q.x + q.z*q.z) / 12.0f,
              (q.y*q.z / 12.0f) + q.x / 2.0f),
          Vector3.of(
              (q.z*q.x / 12.0f) + q.y / 2.0f,
              (q.z*q.y / 12.0f) - q.x / 2.0f,
              1.0f - (q.x*q.x + q.y*q.y) / 12.0f));

      // otherwise, use the real thing
    } else {
      double qq = norm_q * norm_q;
      double c = 0.5f * norm_q * Math.cos(0.5f * norm_q) / Math.sin(0.5f * norm_q);

      return Matrix3.of(
          Vector3.of(
              (q.x*q.x + c * (q.y*q.y + q.z*q.z)) / qq,
              ((1.0f - c) * q.x*q.y / qq) + q.z / 2.0f,
              ((1.0f - c) * q.x*q.z / qq) - q.y / 2.0f),
          Vector3.of(
              ((1.0f - c) * q.y * q.x / qq) - q.z / 2.0f,
              (q.y*q.y + c * (q.x*q.x + q.z*q.z)) / qq,
              ((1.0f - c) * q.y * q.z / qq) + q.x / 2.0f),
          Vector3.of(
              ((1.0f - c) * q.z * q.x / qq) + q.y / 2.0f,
              ((1.0f - c) * q.z * q.y / qq) - q.x / 2.0f,
              (q.z*q.z + c * (q.x*q.x + q.y*q.y)) / qq));
    }

  }

  // This function provides a guideline for when
  // control switching should take place.
  private static Vector3 g(Vector3 q, Vector3 dq_dt) {
    Vector3 T = ANGULAR_ACCELERATION;
    Vector3 D = ANGULAR_DAMPING;

    double gRoll = -Math.signum(dq_dt.x) * (
        (Math.abs(dq_dt.x) / D.x) +
            (T.x / (D.x * D.x)) * Math.log(T.x / (T.x + D.x * Math.abs(dq_dt.x))));

    double gPitch = -Math.signum(dq_dt.y) * dq_dt.y * dq_dt.y / (2.0f * T.y);
    double gYaw = Math.signum(dq_dt.z) * dq_dt.z * dq_dt.z / (2.0f * T.z);

    return Vector3.of(gRoll, gPitch, gYaw);
  }

  // the error between the predicted state and the precomputed return trajectories
  private static Vector3 f(Vector3 alpha_local, float dt, Matrix3 theta, Vector3 omega, Matrix3 z0, Vector3 phi) {
    Vector3 alpha_world = theta.dot(alpha_local);
    Vector3 omega_pred = omega.plus(alpha_world.scaled(dt));
    Vector3 phi_pred = phi.plus(
        z0.dot(
            omega.plus(alpha_world.scaled(0.5f * dt)))
            .scaled(dt));
    Vector3 dphi_dt_pred = z0.dot(omega_pred);
    return phi_pred.scaled(-1.0d).minus(g(phi_pred, dphi_dt_pred));
  }

  // Let g(x) be the continuous piecewise linear function
  // that interpolates the points:
  // (-1.0, values[0]), (0.0, values.y), (1.0, values.z)
  //
  // solve_pwl() determines a value of x in [-1, 1]
  // such that || g(x) - y || is minimized.
  private static float solve_pwl(float y, Vector3 values) {
    float min_value = Math.min(Math.min(values.x, values.y), values.z);
    float max_value = Math.max(Math.max(values.x, values.y), values.z);
    float clipped_y = clip(y, min_value, max_value);

    // if the clipped value can be found in the interval [-1, 0]
    if ((Math.min(values.x, values.y) <= clipped_y) &&
        (clipped_y <= Math.max(values.x, values.y))) {
      if (Math.abs(values.y - values.x) > 0.0001f) {
        return (clipped_y - values.y) / (values.y - values.x);
      } else {
        return -0.5f;
      }

      // if the clipped value can be found in the interval [0, 1]
    } else {
      if (Math.abs(values.z - values.y) > 0.0001f) {
        return (clipped_y - values.y) / (values.z - values.y);
      } else {
        return 0.5f;
      }
    }
  }

  private static Vector3 find_controls_for(Vector3 ideal_alpha, Vector3 omega_local) {
    Vector3 w = omega_local;
    Vector3 T = ANGULAR_ACCELERATION;
    Vector3 D = ANGULAR_DAMPING;

    // Note: these controls are calculated differently,
    // since Rocket League never disables roll damping.
    Vector3 alphaValuesX = Vector3.of(-T.x + (D.x * w.x), (D.x * w.x), T.x + (D.x * w.x));
    double x = solve_pwl(ideal_alpha.x, alphaValuesX);

    Vector3 alphaValuesY = Vector3.of(-T.y, D.y * w.y, T.y);
    double y = solve_pwl(ideal_alpha.y, alphaValuesY);

    Vector3 alphaValuesZ = Vector3.of(-T.z, D.z * w.z, T.z);
    double z = solve_pwl(ideal_alpha.z, alphaValuesZ);

    return Vector3.of(x, y, z);
  }

  private static Vector3 rotation_to_axis(Matrix3 R) {
    double theta = Math.cos(clip(0.5f * (R.trace() - 1.0f), -1.0f, 1.0f));

    double scale;

    // for small angles, prefer series expansion to division by sin(theta) ~ 0
    if (Math.abs(theta) < 0.00001f) {
      scale = 0.5f + theta * theta / 12.0f;
    } else {
      scale = 0.5f * theta / Math.sin(theta);
    }

    return Vector3.of(
        R.row(2).get(1) - R.row(1).get(2),
        R.row(0).get(2) - R.row(2).get(0),
        R.row(1).get(0) - R.row(0).get(1))
        .scaled(scale);
  }

  private static float clip(float value, float min, float max) {
    return Math.min(max, Math.max(min, value));
  }

  private Angles3() {}
}
