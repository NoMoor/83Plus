package com.eru.rlbot.common.boost;

import com.eru.rlbot.bot.common.Angles;
import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.common.Pair;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.FieldInfo;
import rlbot.flat.GameTickPacket;

/**
 * Information about where boost pads are located on the field and what status they have.
 *
 * This class is here for your convenience, it is NOT part of the framework. You can change it as much
 * as you want, or delete it.
 */
public class BoostManager {

    private static final String lock = "boost-sync-lock";

    private static ImmutableList<BoostPad> orderedBoosts = ImmutableList.of();
    private static ImmutableList<BoostPad> largeBoosts = ImmutableList.of();
    private static ImmutableList<BoostPad> smallBoosts = ImmutableList.of();

    public static ImmutableList<BoostPad> getLargeBoosts() {
        return largeBoosts;
    }
    public static ImmutableList<BoostPad> getSmallBoosts() {
        return smallBoosts;
    }
    public static ImmutableList<BoostPad> allBoosts() {
        return orderedBoosts;
    }

    private static void loadFieldInfo(FieldInfo fieldInfo) {

        synchronized (lock) {
            ImmutableList.Builder<BoostPad> orderedBuilder = ImmutableList.builder();
            ImmutableList.Builder<BoostPad> largeBuilder = ImmutableList.builder();
            ImmutableList.Builder<BoostPad> smallBuilder = ImmutableList.builder();

            for (int i = 0; i < fieldInfo.boostPadsLength(); i++) {
                rlbot.flat.BoostPad flatPad = fieldInfo.boostPads(i);
                BoostPad pad = new BoostPad(Vector3.of(flatPad.location()), flatPad.isFullBoost());
                orderedBuilder.add(pad);
                if (pad.isLargeBoost()) {
                    largeBuilder.add(pad);
                } else {
                    smallBuilder.add(pad);
                }
            }

            orderedBoosts = orderedBuilder.build();
            largeBoosts = largeBuilder.build();
            smallBoosts = smallBuilder.build();
        }
    }

    public static void loadGameTickPacket(GameTickPacket packet) {
        // Create the boost pad objects.
        if (packet.boostPadStatesLength() > orderedBoosts.size()) {
            try {
                loadFieldInfo(RLBotDll.getFieldInfo());
            } catch (IOException e) {
              e.printStackTrace();
              return;
            }
        }

      for (int i = 0; i < packet.boostPadStatesLength(); i++) {
        orderedBoosts.get(i).setActive(packet.boostPadStates(i).isActive());
      }
    }

  public static Optional<BoostPad> nearestBoostPad(CarData car) {
    return allBoosts().stream()
        .filter(BoostPad::isActive)
        .map(pad -> Pair.of(effectiveDistance(car).apply(pad), pad))
        .sorted(Comparator.comparing(Pair::getFirst))
        .filter(pair -> pair.getFirst() > 1000 || Angles.flatCorrectionAngle(car, pair.getSecond().getLocation()) < .3)
        .findFirst()
        .map(Pair::getSecond);
  }

  public static Optional<BoostPad> boostOnTheWay(CarData car, Vector3 target) {
    double distanceToTarget = car.position.distance(target);

    return allBoosts().stream()
        .filter(BoostPad::isActive)
        .map(pad -> Pair.of(effectiveDistance(car).apply(pad), pad))
        .sorted(Comparator.comparing(Pair::getFirst))
        .filter(pair -> pair.getFirst() < distanceToTarget * .5) // Is nearer me than the target
        .filter(pair -> Angles.flatCorrectionAngle(car, pair.getSecond().getLocation()) < .1) // Is shallow angle pickup
        .findFirst()
        .map(Pair::getSecond);
  }

  private static Function<BoostPad, Double> effectiveDistance(CarData car) {
    return boostPad -> {
      double actualDistance = boostPad.getLocation().distance(car.position);
      double angleChange = Math.abs(Angles.flatCorrectionAngle(car, boostPad.getLocation()));

      // Reduce the cost of boosts that are approximately in front of us.
      double dulledAngleChange = Math.min(0, angleChange - .2);
      double angleChangeDistance = dulledAngleChange * Constants.radius(car.groundSpeed);
      return actualDistance + (angleChangeDistance * 1.2);
    };
  }
}
