package com.eru.rlbot.bot.maneuver;

import com.eru.rlbot.bot.common.Angles3;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.renderer.BotRenderer;
import com.eru.rlbot.bot.tactics.Tactic;
import com.eru.rlbot.common.Numbers;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.output.Controls;
import com.eru.rlbot.common.vector.Vector3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AirDribble extends Maneuver {

  private static final Logger logger = LogManager.getLogger("AirDribble");

  boolean done;

  @Override
  public void execute(DataPacket input, Controls output, Tactic tactic) {
    if (checkDone(input)) {
      return;
    }

    BotRenderer botRenderer = BotRenderer.forCar(input.car);

    double p = .3;
    double correctionMagnitude =
        Numbers.clamp(p * input.ball.velocity.flatten().magnitude(), 0, Constants.BALL_COLLISION_RADIUS / 2);
    Vector3 velocityCorrection = input.ball.velocity.flat().toMagnitudeUnchecked(correctionMagnitude);
    double verticalCorrection = Math.sin(Math.acos(correctionMagnitude / Constants.BALL_RADIUS)) * Constants.BALL_RADIUS;
    verticalCorrection = Constants.BALL_COLLISION_RADIUS;
    Vector3 targetPoint = input.ball.position.plus(velocityCorrection.setZ(-verticalCorrection));

    Vector3 carToTargetPoint = targetPoint.minus(input.car.position);

    Orientation slowBallOrientation = Orientation.noseWithRoofBias(carToTargetPoint, input.car.velocity);

    // TODO: This points the car toward the right spot but what we really want is for the car itself to be under that spot.
    Angles3.setControlsFor(input.car, slowBallOrientation, output);

    double velocityDiff = input.ball.velocity.z - input.car.velocity.z;
    boolean needsBoost = input.ball.velocity.z < 0 || input.car.velocity.z < 0 || input.ball.velocity.z - input.car.velocity.z > 0;
    double angle = input.car.orientation.getNoseVector().angle(carToTargetPoint);
    boolean pointedTheRightWay = angle < .2;
    // or the relative car ball velocity is < some amount.
    if (needsBoost && pointedTheRightWay) {
      output.withBoost();
    } else if (!pointedTheRightWay && needsBoost) {
      logger.info("Angle too big");
    }

    botRenderer.renderAirDribble(input.car, input.ball);
    botRenderer.renderTarget(targetPoint);
  }

  private boolean checkDone(DataPacket input) {
    return input.car.hasWheelContact || input.car.position.z > input.car.position.z + Constants.BALL_RADIUS;
  }

  @Override
  public boolean isComplete() {
    return done;
  }
}
