package com.eru.rlbot.bot.prediction;

import com.eru.rlbot.bot.common.Accels;
import com.eru.rlbot.bot.common.BotRenderer;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.boost.BoostTracker;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.jump.JumpManager;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NextFramePredictor {

  private static final Logger logger = LogManager.getLogger("FramePrediction");

  private static CarData lastFramePrediction;
  private static ControlsOutput lastFrameControls;

  public static void nextFrame(DataPacket input, ControlsOutput output) {
    if (lastFramePrediction != null) {
      compareResult(lastFramePrediction, input.car);
    }

    lastFramePrediction =
        makePrediction(input.car, output, BoostTracker.forCar(input.car), JumpManager.forCar(input.car));
    lastFrameControls = output;
  }

  public static CarData makePrediction(CarData car, ImmutableList<ControlsOutput> outputs) {
    BoostTracker boostTracker = BoostTracker.copyForCar(car);
    JumpManager jumpManager = JumpManager.copyForCar(car);

    for (ControlsOutput controls : outputs) {
      jumpManager.loadCar(car);
      boostTracker.update(car, controls);
      jumpManager.processOutput(car, controls);

      car = makePrediction(car, controls, boostTracker, jumpManager);
    }
    return car;
  }

  public static CarData makePrediction(
      CarData car, ControlsOutput output, BoostTracker boostTracker, JumpManager jumpManager) {

    CarData.Builder carDataBuilder = car.toBuilder();

    Vector3 newVelocity;
    Vector3 newAngularAcceleration = Vector3.zero();
    if (car.hasWheelContact) { // TODO: Maybe just check if the car is on the ground?
      boostTracker.update(car, output);

      // TODO: Add turning inputs
      newAngularAcceleration = Vector3.zero();

      double acceleration = throttleAcceleration(car, output, boostTracker);
      if (boostTracker.isBoosting()) {
        carDataBuilder.setBoost(car.boost - (Constants.BOOST_RATE * Constants.STEP_SIZE));
      }

      Vector3 accelerationVector = car.orientation.getNoseVector().flatten().asVector3()
          .toMagnitude(acceleration * Constants.STEP_SIZE);
      newVelocity = car.velocity.plus(accelerationVector);
      if (output.holdJump()) {
        newVelocity = newVelocity.plus(car.orientation.getRoofVector()
            .toMagnitude(Constants.JUMP_VELOCITY_INSTANT));
        carDataBuilder.setJumped(true);
      }

      if (isOnCeiling(car)) {
        newVelocity = newVelocity
            .addZ(Constants.NEG_GRAVITY * Constants.STEP_SIZE / 2);

        // Apply sticky force
        if (newVelocity.z < 63) {
          newVelocity = newVelocity.plus(Vector3.of(0, 0, Constants.STICKY_FORCE_ACCEL * Constants.STEP_SIZE));
        }
      }
    } else {
      // Apply gravity
      newVelocity = car.velocity
          .addZ(Constants.NEG_GRAVITY * Constants.STEP_SIZE / 2);

      // Apply throttle acceleration
      double acceleration;
      if (jumpManager.getElapsedJumpTime() < JumpManager.GROUND_CONTACT_TIME) {
        // Continue forward throttle
        acceleration = throttleAcceleration(car, output, boostTracker);
      } else {
        acceleration = throttleAirAcceleration(car, output, boostTracker);
      }
      if (boostTracker.isBoosting()) {
        carDataBuilder.setBoost(car.boost - (Constants.BOOST_RATE * Constants.STEP_SIZE));
      }
      Vector3 accelerationVector = car.orientation.getNoseVector().flatten().asVector3()
          .toMagnitude(acceleration * Constants.STEP_SIZE);
      newVelocity = newVelocity.plus(accelerationVector);

      // Handle flip rotation
      if (jumpManager.isFlipping()) {
        float flipEllapsedTime = car.elapsedSeconds - jumpManager.getFlipTime();
        if (flipEllapsedTime < JumpManager.FLIP_I_TIME) {
          // Do nothing. No inputs can change values here.
        } else if (flipEllapsedTime < JumpManager.FLIP_I_TIME + JumpManager.FLIP_UPWARD_Z_DULL_TIME
            || newVelocity.z < 0) {
          newVelocity = Vector3.of(newVelocity.x, newVelocity.y, newVelocity.z * JumpManager.FLIP_Z_DULL_FACTOR);
        }

        // Accelerate the rotation.
        if (flipEllapsedTime < JumpManager.FLIP_ACCELERATION_TIME) {
          // This is mis-named.
          newAngularAcceleration = Accels.flipAngularAcceleration(
              car.orientation, jumpManager.getFlipPitch(), jumpManager.getFlipYaw(), jumpManager.getFlipRoll());
        } else if (flipEllapsedTime < JumpManager.FLIP_NON_CANCELLABLE_TIME && output.getPitch() != 0) {
          // TODO: Handle flip cancels.
        }
        // Initiate flip.
      } else if (jumpManager.canFlip() && output.holdJump()) {
        carDataBuilder.setDoubleJumped(true);
        newVelocity = car.velocity.plus(
            Accels.flipImpulse(car.orientation, car.velocity, output.getPitch(), output.getYaw(), output.getRoll()));
        newAngularAcceleration = Accels.flipAngularAcceleration(
            car.orientation, output.getPitch(), output.getYaw(), output.getRoll());
        // Hold Jump accelerate upward.
      } else if (!jumpManager.hasMaxJumpHeight() && output.holdJump()) {
        int jumpTick = (int) (jumpManager.getElapsedJumpTime() / Constants.STEP_SIZE);
        Vector3 jumpAcceleration = car.orientation.getRoofVector()
            .toMagnitude(Constants.JUMP_ACCELERATION_FAST_HELD);

        if (jumpTick <= Constants.JUMP_ACCELERATION_HOLD_SLOW_TICK_COUNT) {
          jumpAcceleration = jumpAcceleration.plus(car.orientation.getRoofVector()
              .toMagnitude(-Constants.STICKY_FORCE_ACCEL));
        }
        newVelocity = newVelocity.plus(jumpAcceleration.multiply(Constants.STEP_SIZE));
      }
    }

    Vector3 newAngularVelocity = car.angularVelocity.plus(newAngularAcceleration.multiply(Constants.STEP_SIZE));
    if (car.hasWheelContact) {
      newAngularVelocity = Vector3.zero();
    }
    if (newAngularVelocity.magnitude() > Constants.MAX_ANGULAR_VELOCITY) {
      newAngularVelocity = newAngularVelocity.toMagnitude(Constants.MAX_ANGULAR_VELOCITY);
    }

    Orientation newOrientation = Orientation.fromOrientationMatrix(
        CarBallCollision.antisym(newAngularVelocity)
            .dot(car.orientation.getOrientationMatrix())
            .multiply(Constants.STEP_SIZE)
            .plus(car.orientation.getOrientationMatrix()));

    Vector3 newPosition = car.position.plus(newVelocity.multiply(Constants.STEP_SIZE));
    if (newPosition.z < Constants.CAR_AT_REST) {
      newPosition = newPosition.setZ(Constants.CAR_AT_REST);
      newVelocity = newVelocity.setZ(0);
    }

    return carDataBuilder
        .setTime(car.elapsedSeconds + Constants.STEP_SIZE)
        .setPosition(newPosition)
        .setVelocity(newVelocity)
        .setAngularVelocity(newAngularVelocity)
        .setOrientation(newOrientation)
        .setHasWheelContact(newPosition.z < 20)
        .build();
  }

  private static boolean isOnCeiling(CarData car) {
    return car.position.z > 2000;
  }

  private static double throttleAcceleration(CarData car, ControlsOutput output, BoostTracker boostTracker) {
    if (boostTracker.isBoosting()) {
      return Accels.acceleration(car.velocity.magnitude()) + Constants.BOOSTED_ACCELERATION;
    } else if (output.getThrottle() > 0) {
      return Accels.acceleration(car.velocity.magnitude()) * output.getThrottle();
    } else if (output.getThrottle() == 0) {
      return -Constants.COASTING_DECELERATION;
    } else {
      return -Constants.BREAKING_DECELERATION;
    }
  }

  private static double throttleAirAcceleration(CarData car, ControlsOutput output, BoostTracker boostTracker) {
    if (boostTracker.isBoosting()) {
      return Accels.acceleration(car.velocity.magnitude()) + Constants.BOOSTED_ACCELERATION;
    } else if (output.getThrottle() != 0) {
      return 66 * output.getThrottle();
    }
    return 0;
  }

  private static void compareResult(CarData expected, CarData actual) {
    BotRenderer botRenderer = BotRenderer.forIndex(actual.playerIndex);
//    botRenderer.renderHitBox(Color.MAGENTA, expected);
    botRenderer.renderHitBox(actual);

//    logger.log(Level.INFO, "avX {} {} {} {} {} {}", expected.angularVelocity.x - actual.angularVelocity.x, expected.angularVelocity.x, actual.angularVelocity.x, lastFrameControls.holdJump(), lastFramePrediction.hasWheelContact, actual.hasWheelContact);
//    logger.log(Level.INFO, "vY {} {} {} {} {} {}", expected.velocity.y - actual.velocity.y, expected.velocity.y, actual.velocity.y, lastFrameControls.holdJump(), lastFramePrediction.hasWheelContact, actual.hasWheelContact);
//    logger.log(Level.INFO, "vZ {} {} {} {} {} {}", expected.velocity.z - actual.velocity.z, expected.velocity.z, actual.velocity.z, lastFrameControls.holdJump(), lastFramePrediction.hasWheelContact, actual.hasWheelContact);
//    logger.log(Level.INFO, "pZ {} {} {} {} {} {}", expected.position.z - actual.position.z, expected.position.z, actual.position.z, lastFrameControls.holdJump(), lastFramePrediction.hasWheelContact, actual.hasWheelContact);
//    logger.log(Level.INFO, "Speed diff: {} {} {}", expected.velocity.magnitude() - actual.velocity.magnitude(), actual.velocity.magnitude(), lastFrameControls.holdJump());
//    logger.log(Level.INFO, "velocity {} {} {} {}", expected.velocity.y - actual.velocity.y, expected.velocity.y, actual.velocity.y, lastFrameControls.holdJump());
//    logger.log(Level.INFO, "Distance diff: {}", expected.position.distance(actual.position));
//    logger.log(Level.INFO, "Velocity diff: {}", expected.velocity.magnitude()-actual.velocity.magnitude());
//    logger.log(Level.INFO, "Nose diff: {}", expected.orientation.getNoseVector().angle(actual.orientation.getNoseVector()));
  }
}
