package com.eru.rlbot.bot.maneuver;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Numbers;
import com.eru.rlbot.common.Pair;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector2;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.awt.Color;
import java.util.Comparator;
import java.util.stream.Stream;

public class Recover extends Maneuver {

  private final Vector3 target;

  private boolean complete;
  private double timeToFloor = -1;

  private boolean aggressive = true;

  public Recover(Vector3 target) {
    this.target = target;
  }

  @Override
  public void execute(DataPacket input, Controls output, Tactic tactic) {
    if (timeToFloor < -0.5) {
      timeToFloor = timeToFloor(input.car.velocity, input.car.position);
    } else if (!input.car.hasWheelContact) {
      timeToFloor -= Constants.STEP_SIZE;
    }

    BotRenderer botRenderer = BotRenderer.forCar(input.car);

    botRenderer.renderAboveCar(input.car, "Recover");

    Vector3 localVelocity = input.car.orientation.localCoordinates(input.car.velocity);
    double sheerVelocity = localVelocity.y;
    if (input.car.hasWheelContact) {
      if (sheerVelocity < 300) {
        complete = true;
      }
      output.withThrottle(1.0);
      return;
    }

    if (sheerVelocity > 100 || localVelocity.x < -100) {
      output.withSteer(Angles.flatCorrectionAngle(input.car, target));
    }

    landOnSurface(input, output);
  }

  private void landOnSurface(DataPacket input, Controls output) {
    RecoveryData landing = calculateLandingLocation(input.car);
    double surfaceVelocity = landing.surface.roofNormal.dot(input.car.velocity);
    Vector3 landingImpact = landing.surface.roofNormal.toMagnitude(surfaceVelocity);

    // Take the current velocity and subtract out the surface normal
    Vector3 velocityAfterLanding = input.car.velocity.minus(landingImpact);

    BotRenderer botRenderer = BotRenderer.forCar(input.car);
    if (landing.timeToLanding > .75 && aggressive) {

      RecoveryData alternativeLanding = findAlternateLanding(input, landing);

      Vector3 boostTarget;
      Vector3 carToBoostTarget;
      Orientation orientation;
      if (alternativeLanding != landing) {
        // Need to use boost to get to target another surface.
        carToBoostTarget = alternativeLanding.surface.roofNormal.toMagnitude(-1);
        boostTarget = input.car.position.plus(carToBoostTarget.toMagnitude(1000));

        orientation = Orientation.noseWithRoofBias(carToBoostTarget, input.car.velocity);
      } else {
        // Boost to make a better landing.
        Vector3 landingToTarget = target.minus(landing.position);
        Vector3 projectionOnSurface = landingToTarget.minus(
            landing.surface.roofNormal.toMagnitude(landingToTarget.dot(landing.surface.roofNormal)));

        if (velocityAfterLanding.magnitude() > 800) {
          // Boost straight at the wall.
          carToBoostTarget = landing.surface.roofNormal.toMagnitude(-1);
          boostTarget = input.car.position.plus(carToBoostTarget.toMagnitude(1000));
          orientation = Orientation.noseWithRoofBias(carToBoostTarget, input.car.velocity);
        } else {
          // Boost toward the target for a better exit.
          double boostTargetScaling = (-velocityAfterLanding.magnitude() + 800);

          boostTarget = landing.position.plus(projectionOnSurface.toMagnitude(boostTargetScaling));
          carToBoostTarget = boostTarget.minus(input.car.position);

          orientation = Orientation.noseWithRoofBias(carToBoostTarget, landing.surface.roofNormal);
        }
      }

      Angles3.setControlsFor(input.car, orientation, output);

      double noseVelocity = input.car.orientation.getNoseVector().dot(input.car.velocity);
      double carToBoostTargetAngle = input.car.orientation.getNoseVector().angle(carToBoostTarget);

      if (carToBoostTargetAngle < .5 && noseVelocity < 2200) {
        output.withBoost();
      }

      botRenderer.renderTarget(Color.MAGENTA, boostTarget);
      botRenderer.renderTarget(Color.CYAN, landing.position);
    } else {
      // Set wheels for landing
      Vector3 noseBias = velocityAfterLanding.setZ(-600);
      Orientation orientation = Orientation.roofWithNoseBias(landing.surface.roofNormal, noseBias);

      botRenderer.renderTarget(Color.MAGENTA, landing.position);
      botRenderer.renderProjection(landing.position, landing.position.plus(landing.surface.roofNormal.toMagnitude(300)), Color.RED);
      botRenderer.renderHitBox(CarData.builder()
          .setOrientation(orientation)
          .setPosition(landing.position)
          .build());

      Angles3.setControlsFor(input.car, orientation, output);
    }
  }

  private RecoveryData findAlternateLanding(DataPacket input, RecoveryData landing) {
    // Create alternative landing positions by altering the velocity and seeing if we can get anywhere else faster.
    // Exclude landing on the same surface.
    return generateVelocityAlternatives(input.car, landing.timeToLanding).stream()
        .map(this::calculateLandingLocation)
        .min(Comparator.comparing(data -> data.timeToLanding))
        .orElse(landing);
  }

  private static final ImmutableList<Vector3> CARDINAL_DIRECTIONS = ImmutableList.of(
      Vector3.of(0, 0, 1),
      Vector3.of(0, 0, -1),
      Vector3.of(0, 1, 0),
      Vector3.of(0, -1, 0),
      Vector3.of(1, 0, 0),
      Vector3.of(-1, 0, 0));

  private ImmutableList<CarData> generateVelocityAlternatives(CarData car, double timeToLanding) {
    return CARDINAL_DIRECTIONS.stream()
        .map(boostDirection -> {
          // Assume 3 radians in .75 seconds = 1 radian / .25 seconds
          double rotationTime = car.orientation.getNoseVector().angle(boostDirection) * .25;

          double boostTime = timeToLanding - .75 - rotationTime;

          if (boostTime < 0) {
            return car;
          }

          double addedVelocity = boostTime * Constants.BOOSTED_ACCELERATION / 3;

          return car.toBuilder()
              .setVelocity(car.velocity.plus(boostDirection.toMagnitude(addedVelocity)))
              .setPosition(car.position.plus(car.velocity.multiply(rotationTime)))
              .setTime(car.elapsedSeconds + rotationTime)
              .build();
        })
        .collect(toImmutableList());
  }

  private RecoveryData calculateLandingLocation(CarData car) {
    Vector3 position = car.position;
    Vector3 velocity = car.velocity;

    double timeToApex = velocity.z < 0 ? 0 : velocity.z / Constants.GRAVITY;
    double apexHeight = heightAtTime(position, velocity, timeToApex);

    double timeToFloor = timeToFloor(velocity, position);
    double timeToLeftWall = timeToX(velocity, position, Constants.HALF_WIDTH);
    double timeToRightWall = timeToX(velocity, position, -Constants.HALF_WIDTH);
    double timeToFrontWall = timeToY(velocity, position, Constants.HALF_LENGTH);
    double timeToBackWall = timeToY(velocity, position, -Constants.HALF_LENGTH);

    double timeToBackLeftCorner = timeToCorner(velocity, position, Surface.X_INTERCEPT, -Surface.Y_INTERCEPT);
    double timeToBackRightCorner = timeToCorner(velocity, position, -Surface.X_INTERCEPT, -Surface.Y_INTERCEPT);
    double timeToFrontLeftCorner = timeToCorner(velocity, position, Surface.X_INTERCEPT, Surface.Y_INTERCEPT);
    double timeToFrontRightCorner = timeToCorner(velocity, position, -Surface.X_INTERCEPT, Surface.Y_INTERCEPT);
    double timeToCeiling = timeToCeiling(velocity, position);

    Pair<Surface, Double> landing = Stream.of(
        Pair.of(Surface.LEFT_WALL, timeToLeftWall),
        Pair.of(Surface.RIGHT_WALL, timeToRightWall),
        Pair.of(Surface.FRONT_WALL, timeToFrontWall),
        Pair.of(Surface.BACK_WALL, timeToBackWall),
        Pair.of(Surface.BACK_LEFT_CORNER, timeToBackLeftCorner),
        Pair.of(Surface.BACK_RIGHT_CORNER, timeToBackRightCorner),
        Pair.of(Surface.FRONT_LEFT_CORNER, timeToFrontLeftCorner),
        Pair.of(Surface.FRONT_RIGHT_CORNER, timeToFrontRightCorner),
        Pair.of(Surface.FLOOR, timeToFloor),
        Pair.of(Surface.CEILING, timeToCeiling))
        .min(Comparator.comparing(Pair::getSecond))
        .get();

    Vector3 landingPosition = positionAtTime(position, velocity, landing.getSecond());

    return RecoveryData.newBuilder()
        .onSurface(landing.getFirst())
        .withLandingPosition(landingPosition)
        .withLandingTime(landing.getSecond())
        .withApexHeight(apexHeight)
        .withApextime(timeToApex)
        .build();
  }

  private double timeToCeiling(Vector3 velocity, Vector3 position) {
    return timeToHeight(velocity, position, Constants.FIELD_HEIGHT - Constants.CAR_AT_REST);
  }

  private double timeToCorner(Vector3 velocity, Vector3 position, float xIntercept, float yIntercept) {
    // y = (-yIntercept/xIntercept) x + yIntercept
    // y = velocity.y/velocity.x x + ((-velocity.y/velocity.x) * position.x)
    // (-yIntercept/xIntercept) x + yIntercept = velocity.y/velocity.x x + ((-velocity.y/velocity.x) * position.x)

    // (-yIntercept/xIntercept) x - velocity.y/velocity.x x = + ((-velocity.y/velocity.x) * position.x) - yIntercept
    // x = (((-velocity.y/velocity.x) * position.x) - yIntercept) / ((-yIntercept/xIntercept) - velocity.y/velocity.x)

    float carSlope = velocity.y / velocity.x;
    float carYIntercept = position.y - (carSlope * position.x);

    float x = (carYIntercept - yIntercept) / ((-yIntercept / xIntercept) - carSlope);
    float y = (carSlope * x) + carYIntercept;

    double flatVelocity = velocity.flat().magnitude();

    Vector2 insersectionPoint = Vector2.of(x, y);
    double distanceToIntersect = position.flatten().distance(insersectionPoint);

    Vector2 carToIntersection = insersectionPoint.minus(position.flatten());

    if (carToIntersection.dotProduct(velocity.flatten()) >= 0) {
      return distanceToIntersect / flatVelocity;
    }

    return Double.MAX_VALUE;
  }

  private double timeToY(Vector3 velocity, Vector3 position, int halfLength) {
    return Numbers.positiveOrMax((halfLength - position.y) / velocity.y);
  }

  private double timeToX(Vector3 velocity, Vector3 position, int halfWidth) {
    return Numbers.positiveOrMax((halfWidth - position.x) / velocity.x);
  }

  private double timeToFloor(Vector3 velocity, Vector3 position) {
    return timeToHeight(velocity, position, Constants.CAR_AT_REST);
  }

  private double timeToHeight(Vector3 velocity, Vector3 position, double desiredHeight) {
    double h0 = position.z;

    if (h0 < 0) {
      return 0;
    }

    double vz0 = velocity.z;

    double a = (Constants.NEG_GRAVITY / 2);
    double b = vz0;
    double c = h0 - desiredHeight;

    // discriminant = b^2 - 4ac
    double discriminant = (b * b) - (4 * a * c);

    if (discriminant < 0) {
      return Double.MAX_VALUE;
    }

    double denominator = (2 * a);
    double discriminantSqrt = Math.sqrt(discriminant);
    if (discriminant == 0) { // Max height 0 desired height.
      return (-vz0 + discriminantSqrt) / denominator;
    }

    double solution1 = (-vz0 + discriminantSqrt) / denominator;
    double solution2 = (-vz0 - discriminantSqrt) / denominator;

    return solution1 > 0 && solution2 > 0 ? Math.min(solution1, solution2) : solution1 > 0 ? solution1 : solution2;
  }

  private double heightAtTime(Vector3 position, Vector3 velocity, double time) {
    return ((Constants.NEG_GRAVITY / 2) * time * time)
        + (velocity.z * time)
        + position.z;
  }

  private Vector3 positionAtTime(Vector3 position, Vector3 velocity, double time) {
    double height = heightAtTime(position, velocity, time);
    Vector3 horizontalLocation = velocity.flatten().multiplied(time).asVector3(Constants.CAR_AT_REST)
        .plus(position.flat());

    return Vector3.of(horizontalLocation.x, horizontalLocation.y, height);
  }

  @Override
  public boolean isComplete() {
    return complete;
  }

  public static class RecoveryData {
    private final Surface surface;
    private final Vector3 position;
    private final double timeToLanding;
    private final double maxHeight;
    private final double timeToMaxHeight;

    RecoveryData(Builder builder) {
      this.surface = builder.surface;
      this.position = builder.landingLocation;
      this.timeToLanding = builder.timeToLanding;
      this.maxHeight = builder.maxHeight;
      this.timeToMaxHeight = builder.timeToMaxHeight;
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    public static class Builder {
      Surface surface;
      Vector3 landingLocation;
      double timeToLanding;
      double maxHeight;
      double timeToMaxHeight;

      public Builder onSurface(Surface surface) {
        this.surface = surface;
        return this;
      }

      public Builder withLandingPosition(Vector3 landingLocation) {
        this.landingLocation = landingLocation;
        return this;
      }

      public Builder withLandingTime(double timeToLanding) {
        this.timeToLanding = timeToLanding;
        return this;
      }

      public Builder withApexHeight(double maxHeight) {
        this.maxHeight = maxHeight;
        return this;
      }

      public Builder withApextime(double timeToMaxHeight) {
        this.timeToMaxHeight = timeToMaxHeight;
        return this;
      }

      public RecoveryData build() {
        return new RecoveryData(this);
      }

      Builder() {
      }
    }
  }

  public enum Surface {
    FLOOR(Vector3.of(0, 0, 1)),
    LEFT_WALL(Vector3.of(-1, 0, 0)),
    RIGHT_WALL(Vector3.of(1, 0, 0)),
    FRONT_WALL(Vector3.of(0, -1, 0)),
    BACK_WALL(Vector3.of(0, 1, 0)),
    BACK_LEFT_CORNER(Vector3.of(-Math.sin(Math.PI / 4), Math.cos(Math.PI / 4), 0)),
    BACK_RIGHT_CORNER(Vector3.of(Math.sin(Math.PI / 4), Math.cos(Math.PI / 4), 0)),
    FRONT_LEFT_CORNER(Vector3.of(-Math.sin(Math.PI / 4), -Math.cos(Math.PI / 4), 0)),
    FRONT_RIGHT_CORNER(Vector3.of(Math.sin(Math.PI / 4), -Math.cos(Math.PI / 4), 0)),
    CEILING(Vector3.of(0, 0, -1));

    public static final float X_INTERCEPT = 4000 + Constants.HALF_WIDTH;
    public static final float Y_INTERCEPT = 3000 + Constants.HALF_LENGTH;

    private final Vector3 roofNormal;

    Surface(Vector3 roofNormal) {
      this.roofNormal = roofNormal;
    }

    public boolean isHorizontal() {
      return this == FLOOR || this == CEILING;
    }
  }
}
