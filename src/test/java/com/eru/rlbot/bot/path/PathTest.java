package com.eru.rlbot.bot.path;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.truth.Truth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PathTest {

  @Test
  public void straightLine_maxBoost() {
    double targetTime = 2;
    CarData startingCar = CarData.builder()
        .setPosition(Vector3.zero())
        .setVelocity(Vector3.zero())
        .setTime(0)
        .setTeam(0)
        .setOrientation(Orientation.fromFlatVelocity(Vector3.of(0, 1, 0)))
        .setBoost(52)
        .build();

    CarData targetCar = startingCar.toBuilder()
        .setPosition(Vector3.of(0, 2002, 0))
        .setVelocity(Vector3.of(0, 2168, 0))
        .setTime(targetTime)
        .build();

    Path straightPath = Path.builder()
        .setStartingCar(startingCar)
        .setTargetCar(targetCar)
        .addEarlierSegment(Segment.straight(startingCar.position, targetCar.position))
        .build();

    Plan plan = straightPath.makeSpeedPlan(startingCar.boost, targetTime);

    Truth.assertThat(plan.traverseTime).isWithin(.016).of(targetTime);
  }

  @Test
  public void straightLine_noBoost() {
    double targetTime = 2.3;
    CarData startingCar = CarData.builder()
        .setPosition(Vector3.zero())
        .setVelocity(Vector3.zero())
        .setTime(0)
        .setTeam(0)
        .setOrientation(Orientation.fromFlatVelocity(Vector3.of(0, 1, 0)))
        .setBoost(52)
        .build();

    CarData targetCar = startingCar.toBuilder()
        .setPosition(Vector3.of(0, 2002, 0))
        .setVelocity(Vector3.of(0, 1386, 0))
        .setTime(targetTime)
        .build();

    Path straightPath = Path.builder()
        .setStartingCar(startingCar)
        .setTargetCar(targetCar)
        .addEarlierSegment(Segment.straight(startingCar.position, targetCar.position))
        .build();

    Plan plan = straightPath.makeSpeedPlan(startingCar.boost, targetTime);

    Truth.assertThat(plan.traverseTime).isWithin(.016).of(targetTime);
  }

  @Test
  public void straightLine_slowDown() {
    double targetTime = 2.3;
    CarData startingCar = CarData.builder()
        .setPosition(Vector3.zero())
        .setVelocity(Vector3.of(0, 1000, 0))
        .setTime(0)
        .setTeam(0)
        .setOrientation(Orientation.fromFlatVelocity(Vector3.of(0, 1, 0)))
        .setBoost(52)
        .build();

    CarData targetCar = startingCar.toBuilder()
        .setPosition(Vector3.of(0, 2002, 0))
        .setVelocity(Vector3.of(0, 1386, 0))
        .setTime(targetTime)
        .build();

    Path straightPath = Path.builder()
        .setStartingCar(startingCar)
        .setTargetCar(targetCar)
        .addEarlierSegment(Segment.straight(startingCar.position, targetCar.position))
        .build();

    Plan plan = straightPath.makeSpeedPlan(startingCar.boost, targetTime);

    Truth.assertThat(plan.traverseTime).isWithin(.016).of(targetTime);
  }

  @Test
  public void straightLine_latencyTest() {
    double targetTime = 2.3;
    CarData startingCar = CarData.builder()
        .setPosition(Vector3.zero())
        .setVelocity(Vector3.of(0, 1000, 0))
        .setTime(0)
        .setTeam(0)
        .setOrientation(Orientation.fromFlatVelocity(Vector3.of(0, 1, 0)))
        .setBoost(52)
        .build();

    CarData targetCar = startingCar.toBuilder()
        .setPosition(Vector3.of(0, 2002, 0))
        .setVelocity(Vector3.of(0, 1386, 0))
        .setTime(targetTime)
        .build();

    Path straightPath = Path.builder()
        .setStartingCar(startingCar)
        .setTargetCar(targetCar)
        .addEarlierSegment(Segment.straight(startingCar.position, targetCar.position))
        .build();

    straightPath.lockAndSegment(true);
  }

  @Test
  public void curveTest() {

  }
}
