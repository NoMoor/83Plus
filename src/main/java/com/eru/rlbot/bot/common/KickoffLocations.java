package com.eru.rlbot.bot.common;

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

  public enum KickoffLocation {
    CENTER_BLUE(Vector2.of(CENTER_X, -CENTER_Y), KickoffStation.CENTER, SIT),
    LEFT_CENTER_BLUE(Vector2.of(CENTER_MID_X, -CENTER_MID_Y), KickoffStation.LEFT_CENTER, POS),
    RIGHT_CENTER_BLUE(Vector2.of(-CENTER_MID_X, -CENTER_MID_Y), KickoffStation.RIGHT_CENTER, NEG),
    LEFT_BLUE(Vector2.of(FAR_X, -FAR_Y), KickoffStation.LEFT, POS),
    RIGHT_BLUE(Vector2.of(-FAR_X, -FAR_Y), KickoffStation.RIGHT, NEG),

    CENTER_ORANGE(Vector2.of(CENTER_X, CENTER_Y), KickoffStation.CENTER, SIT),
    LEFT_CENTER_ORANGE(Vector2.of(-CENTER_MID_X, CENTER_MID_Y), KickoffStation.LEFT_CENTER, NEG),
    RIGHT_CENTER_ORANGE(Vector2.of(CENTER_MID_X, CENTER_MID_Y), KickoffStation.RIGHT_CENTER, POS),
    LEFT_ORANGE(Vector2.of(-FAR_X, FAR_Y), KickoffStation.LEFT, NEG),
    RIGHT_ORANGE(Vector2.of(FAR_X, FAR_Y), KickoffStation.RIGHT, POS);

    public final Vector2 location;
    public final KickoffStation station;
    public final double pushModifier;
    public final double turnModifier;

    KickoffLocation(Vector2 location, KickoffStation station, double pushModifier) {
      this.location = location;
      this.station = station;
      this.turnModifier = station.turnModifier;
      this.pushModifier = pushModifier;
    }

    public static KickoffLocation defaultLocation(int team) {
      return team == 0 ? CENTER_BLUE : CENTER_ORANGE;
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
