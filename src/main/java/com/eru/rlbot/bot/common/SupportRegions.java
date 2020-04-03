package com.eru.rlbot.bot.common;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Breaks the field up into various regions to make 'support' logic more stable.
 */
public final class SupportRegions {

  private static final double REGION_RADIUS = 1200;
  private static final ImmutableList<Circle> ALL_REGIONS = ImmutableList.<Circle>builder()
      .add(Circle.forPath(Vector3.fieldLevel(0, 0), REGION_RADIUS * .8))
      .add(Circle.forPath(Vector3.fieldLevel(1600, 0), REGION_RADIUS * .8))
      .add(Circle.forPath(Vector3.fieldLevel(3200, 0), REGION_RADIUS * .8))
      .add(Circle.forPath(Vector3.fieldLevel(-1600, 0), REGION_RADIUS * .8))
      .add(Circle.forPath(Vector3.fieldLevel(-3200, 0), REGION_RADIUS * .8))
      // Orange Half
      .add(Circle.forPath(Vector3.fieldLevel(900, 1500), REGION_RADIUS * .9))
      .add(Circle.forPath(Vector3.fieldLevel(-900, 1500), REGION_RADIUS * .9))
      .add(Circle.forPath(Vector3.fieldLevel(2800, 1700), REGION_RADIUS))
      .add(Circle.forPath(Vector3.fieldLevel(-2800, 1700), REGION_RADIUS))
      .add(Circle.forPath(Vector3.fieldLevel(2800, 4000), REGION_RADIUS * 1.1))
      .add(Circle.forPath(Vector3.fieldLevel(0, 5000), REGION_RADIUS * 2))
      .add(Circle.forPath(Vector3.fieldLevel(-2800, 4000), REGION_RADIUS * 1.1))
      // Blue Half
      .add(Circle.forPath(Vector3.fieldLevel(900, -1500), REGION_RADIUS * .9))
      .add(Circle.forPath(Vector3.fieldLevel(-900, -1500), REGION_RADIUS * .9))
      .add(Circle.forPath(Vector3.fieldLevel(2800, -1700), REGION_RADIUS))
      .add(Circle.forPath(Vector3.fieldLevel(-2800, -1700), REGION_RADIUS))
      .add(Circle.forPath(Vector3.fieldLevel(2800, -4000), REGION_RADIUS * 1.1))
      .add(Circle.forPath(Vector3.fieldLevel(0, -4300), REGION_RADIUS * 1.3))
      .add(Circle.forPath(Vector3.fieldLevel(-2800, -4000), REGION_RADIUS * 1.1))
      .build();

  /**
   * Returns the center location of the given circles weighted slightly toward the rear post.
   */
  public Vector3 averageLocation() {
    Vector3 farPost = Goal.ownGoal(team).getFarPost(supportLocation);

    List<Vector3> locations = regions.stream()
        .map(region -> region.center)
        .collect(Collectors.toList());
    locations.add(farPost);

    double avgX = locations.stream()
        .mapToDouble(location -> location.x)
        .average()
        .orElse(farPost.x);
    double avgY = locations.stream()
        .mapToDouble(location -> location.y)
        .average()
        .orElse(farPost.y);

    return Vector3.fieldLevel(avgX, avgY);
  }

  /**
   * Returns a list of support regions to support the given location.
   */
  public static SupportRegions getSupportRegions(Vector3 supportingLocation, int team) {
    Vector3 defendingLocation = Goal.ownGoal(team).center;
    SupportRegions tooClose = getSupportRegions(supportingLocation, team, 1);
    Optional<Circle> closest = tooClose.regions.stream().min(closestTo(supportingLocation));

    if (!closest.isPresent()) {
      // If we are very close to our own goal, use the goal arc itself as the support zone.
      ImmutableList<Circle> lastLine = ALL_REGIONS.stream()
          .min(closestTo(supportingLocation)).map(ImmutableList::of).get();
      return new SupportRegions(lastLine, team, supportingLocation);
    }

    SupportRegions secondLine = getSupportRegions(closest.get().center, team, 3);

    Optional<Circle> closestSecondLIne = secondLine.regions.stream().min(closestTo(supportingLocation));

    ImmutableList<Circle> localRegions;
    if (!closestSecondLIne.isPresent()) {
      // If we are very close to our own goal, use the goal arc itself as the support zone.
      localRegions = ALL_REGIONS.stream().min(closestTo(defendingLocation)).map(ImmutableList::of).get();
    } else {
      SupportRegions thirdLine = getSupportRegions(closestSecondLIne.get().center, team, 4);
      localRegions = ImmutableList.<Circle>builder()
          .addAll(tooClose.regions)
          .addAll(secondLine.regions)
//          .addAll(thirdLine.regions)
          .build().stream()
          .distinct()
          .limit(4)
          .collect(toImmutableList());
    }
    return new SupportRegions(localRegions, team, supportingLocation);
  }

  public static SupportRegions getSupportRegions(Vector3 supportingLocation, int team, int count) {
    Vector3 defendingLocation = Goal.ownGoal(team).center;

    Circle supportingRegion =
        ALL_REGIONS.stream()
            .min(Comparator.comparing(region -> region.center.distance(supportingLocation)))
            .orElseThrow(IllegalStateException::new);

    double supportRegionToGoal = supportingRegion.center.distance(defendingLocation) - supportingRegion.radius;

    ImmutableList<Circle> regions = ALL_REGIONS.stream()
        .filter(region -> region.center.distance(defendingLocation) < supportRegionToGoal)
        .sorted(Comparator.comparing(region -> region.center.distance(supportingRegion.center)))
        .limit(count)
        .collect(toImmutableList());

    return new SupportRegions(regions, team, supportingLocation);
  }

  private static Comparator<Circle> closestTo(Vector3 location) {
    return Comparator.comparing(circle -> circle.center.distance(location));
  }

  public final ImmutableList<Circle> regions;
  public final int team;
  public final Vector3 supportLocation;

  private SupportRegions(ImmutableList<Circle> regions, int team, Vector3 supportLocation) {
    this.regions = regions;
    this.team = team;
    this.supportLocation = supportLocation;
  }
}
