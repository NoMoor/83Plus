package com.eru.rlbot.bot.prediction;

import com.eru.rlbot.bot.common.CarBall;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.common.Matrix3;
import com.eru.rlbot.common.Numbers;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import java.awt.Color;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class for calculating collision interactions between car and ball.
 *
 * The major contents of this class are credited to https://samuelpmish.github.io/notes/RocketLeague/car_ball_interaction/
 */
public class CarBallCollision {

  private static final Logger logger = LogManager.getLogger("CollisionTimer");

  private static final double MU = 2.0f;

  /** Calculates the expected next BallData based on the given ball and car. */
  public static BallData calculateCollision(BallData ball, CarData car) {
    return calculateCollision(ball, car, false);
  }

  public static BallData calculateCollision(BallData ball, CarData car, boolean renderComponents) {
    long nanoStartTime = System.nanoTime();

    Vector3 touchPoint = CarBall.nearestPointOnHitBox(ball.position, car);

    double distanceToHitbox = touchPoint.minus(ball.position).magnitude();
    if (distanceToHitbox > Constants.BALL_COLLISION_RADIUS) {
      return ball;
    }

    Vector3 normalizedBallCarTouch = touchPoint.minus(ball.position).normalize();

    // Levers of rotation for ball and car.
    Matrix3 ballLever = antisym(touchPoint.minus(ball.position));
    Matrix3 carLever = antisym(touchPoint.minus(car.position));

    // Transpose the inverse car moment of inertia to world coordinates. (???)
    Matrix3 inverseCarMomentOfInertia = car.orientation.getOrientationMatrix()
        .dot(Constants.CAR_INVERSE_MOMENT_OF_INERTIA.dot(car.orientation.getOrientationMatrix().transpose())); // Perhaps this is wrong?

    // A simplification of car-ball mass moments of inertia.
    Matrix3 reducedMassMatrix = Matrix3.IDENTITY.multiply((1 / Constants.BALL_MASS) + (1 / Constants.CAR_MASS))
        .minus(ballLever.dot(ballLever).divide(Constants.BALL_MOMENT_OF_INERTIA))
        .minus(carLever.dot(inverseCarMomentOfInertia.dot(carLever)))
        .inverse();

    Vector3 deltaV = car.velocity
        .minus(carLever.dot(car.angularVelocity))
        .minus(ball.velocity.minus(ballLever.dot(ball.spin)));

    // Compute the impulse that is consistent with an inelastic collision
    Vector3 physicsImpulse = reducedMassMatrix.dot(deltaV);

    // Scale the physics impulse to satisfy Coulomb friction model
    Vector3 physicsImpulsePerpendicular = normalizedBallCarTouch
        .multiply(Math.min(physicsImpulse.dot(normalizedBallCarTouch), -1));
    Vector3 physicsImpulseParallel = physicsImpulse.minus(physicsImpulsePerpendicular);

    double ratio = physicsImpulsePerpendicular.magnitude() / Math.max(physicsImpulseParallel.magnitude(), 0.001f);
    Vector3 scaledPhysicsImpulse =
        physicsImpulsePerpendicular.plus(physicsImpulseParallel.multiply(Math.min(1.0f, MU * ratio)));

    Vector3 physicsVelocity = scaledPhysicsImpulse.divide(Constants.BALL_MASS);
    Vector3 psyonixVelocity = getPsyonixVelocity(car, ball);

    if (renderComponents) {
      BotRenderer.forIndex(car.serialNumber).renderProjection(ball.position, ball.position.plus(physicsVelocity), Color.RED, 0, "Physics");
      BotRenderer.forIndex(car.serialNumber).renderProjection(ball.position, ball.position.plus(psyonixVelocity), Color.MAGENTA, 0, "Psyonix");
    }

    Vector3 deltaSpin = ballLever.dot(scaledPhysicsImpulse).divide(Constants.BALL_MOMENT_OF_INERTIA);
    Vector3 deltaVelocity = physicsVelocity.plus(psyonixVelocity);

    logger.log(Level.DEBUG, "Time nanos: " + (System.nanoTime() - nanoStartTime));

    return BallData.builder()
        .setPosition(ball.position) // TODO: Determine where the ball will be.
        .setSpin(ball.spin.plus(deltaSpin))
        .setVelocity(ball.velocity.plus(deltaVelocity))
        .setTime(ball.time + Constants.STEP_SIZE)
        .build();
  }

  // This seems to be contributing too much x-velocity
  public static Vector3 getPsyonixVelocity(CarData car, BallData ball) {
    // Calculate Psyonix Impulse
    Vector3 carNose = car.orientation.getNoseVector();
    Vector3 carBall = ball.position.minus(car.position);
    carBall = Vector3.of(carBall.x, carBall.y, carBall.z * .35f); // Damp the z for easier dribbling

    double carBallNose = carBall.dot(carNose);

    if (carBallNose == 0) {
      return Vector3.zero();
    }

    Vector3 impulseDirection = carBall
        .minus(carNose
            .multiply(carBallNose)
            .multiply(.35f))
        .normalize();

    double velocityDiff = Math.min(ball.velocity.minus(car.velocity).magnitude(), 4600);
    double psyonixScalingFactor = getPsyonixImplusScalingFactor(velocityDiff);

    return impulseDirection.multiply(velocityDiff * psyonixScalingFactor);
  }

  private static double getPsyonixImplusScalingFactor(double value) {
    if (false)
      return psyonixImpulseScale(value);

    if (value < 500) {
      return .65;
    } else if (value < 2300) {
      return (.1 * (1 - ((value - 500) / 1800))) + .55; // 500 = .65; 2299 = .55
    } else if (value < 4600) {
      return (.25 * (1 - ((value - 2300) / 2300))) + .30; // 2300 = .55; 4599 = .30
    } else {
      return -1;
    }
  }

  private static double psyonixImpulseScale(double dv) {

    double[][] values = {
        {0.0d, 0.65d},
        {500.0d, 0.65d},
        {2300.0d, 0.55d},
        {4600.0d, 0.30d}
    };

    double input = Numbers.clamp(dv, 0, 4600);

    for (int i = 0; i < values.length; i++) {
      if (values[i][0] <= input && input < values[i + 1][0]) {
        double u = (input - values[i][0]) / (values[i + 1][0] - values[i][0]);
        return Numbers.lerp(values[i][1], values[i + 1][1], u);
      }
    }

    return -1;
  }

  // https://github.com/samuelpmish/RLUtilities/blob/879b5e335db2313c46db4a0cb2e89c244153492b/inc/linear_algebra/math.h#L163
  public static Matrix3 antisym(Vector3 vector) {
    return Matrix3.of(
        Vector3.of(0, -vector.z, vector.y),
        Vector3.of(vector.z, 0, -vector.x),
        Vector3.of(-vector.y, vector.x, 0));
  }

  private CarBallCollision() {}
}
