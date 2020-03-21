package com.eru.rlbot.bot.tactics.kickoff;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.vector.Vector2;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

/**
 * Kickoff specific location information.
 */
public class KickoffLocations {

  private static final double CENTER_X = 0;
  private static final double CENTER_Y = 4608;
  private static final double CENTER_MID_X = 256;
  private static final double CENTER_MID_Y = 3840;
  private static final double FAR_X = 2048;
  private static final double FAR_Y = 2560;

  public static Optional<KickoffLocation> getKickoffLocation(CarData car) {
    if (car.velocity.magnitude() > 40) {
      return Optional.empty();
    }

    return Arrays.stream(KickoffLocation.values())
        .sorted(Comparator.comparing(location -> car.position.distance(location.location.asVector3())))
        .filter(kickoffLocation -> kickoffLocation.location.distance(car.position.flatten()) < 50)
        .findFirst();
  }

  private static final double POS = 1.0;
  private static final double NEG = -1.0;
  private static final double SIT = 0;

  public static KickoffLocation defaultLocation(int team) {
    return KickoffLocation.defaultLocation(team);
  }

  public enum KickoffLocation {
    CENTER_BLUE(Vector2.of(CENTER_X, -CENTER_Y), KickoffStation.CENTER, SIT, 5),
    LEFT_CENTER_BLUE(Vector2.of(CENTER_MID_X, -CENTER_MID_Y), KickoffStation.LEFT_CENTER, POS, 3),
    RIGHT_CENTER_BLUE(Vector2.of(-CENTER_MID_X, -CENTER_MID_Y), KickoffStation.RIGHT_CENTER, NEG, 4),
    LEFT_BLUE(Vector2.of(FAR_X, -FAR_Y), KickoffStation.LEFT, POS, 1),
    RIGHT_BLUE(Vector2.of(-FAR_X, -FAR_Y), KickoffStation.RIGHT, NEG, 2),

    CENTER_ORANGE(Vector2.of(CENTER_X, CENTER_Y), KickoffStation.CENTER, SIT, 5),
    LEFT_CENTER_ORANGE(Vector2.of(-CENTER_MID_X, CENTER_MID_Y), KickoffStation.LEFT_CENTER, NEG, 3),
    RIGHT_CENTER_ORANGE(Vector2.of(CENTER_MID_X, CENTER_MID_Y), KickoffStation.RIGHT_CENTER, POS, 4),
    LEFT_ORANGE(Vector2.of(-FAR_X, FAR_Y), KickoffStation.LEFT, NEG, 1),
    RIGHT_ORANGE(Vector2.of(FAR_X, FAR_Y), KickoffStation.RIGHT, POS, 2);

    public final Vector2 location;
    public final KickoffStation station;
    public final double pushModifier;
    public final double turnModifier;
    public final int priority;

    KickoffLocation(Vector2 location, KickoffStation station, double pushModifier, int priority) {
      this.location = location;
      this.station = station;
      this.turnModifier = station.turnModifier;
      this.pushModifier = pushModifier;
      this.priority = priority;
    }

    public static KickoffLocation defaultLocation(int team) {
      return team == 0 ? CENTER_BLUE : CENTER_ORANGE;
    }

    public int getPriority() {
      return priority;
    }
  }

  public enum KickoffStation {
    LEFT(1), LEFT_CENTER(1), CENTER(0), RIGHT_CENTER(-1), RIGHT(-1);

    private final double turnModifier;

    KickoffStation(double turnModifier) {
      this.turnModifier = turnModifier;
    }
  }
}
