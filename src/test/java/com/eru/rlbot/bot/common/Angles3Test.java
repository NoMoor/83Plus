package com.eru.rlbot.bot.common;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.CarOrientation;
import com.eru.rlbot.common.output.ControlsOutput;
import com.eru.rlbot.common.vector.Vector3;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Angles3}. */
@RunWith(JUnit4.class)
public class Angles3Test {

  @Test
  public void test_xAxis() {
    CarData.Builder cBuidler = new CarData.Builder();

    cBuidler.time = 0.0f;
    cBuidler.position = Vector3.of(0, 0, 0);
    cBuidler.velocity = Vector3.of(0, 0, 0);
    cBuidler.angularVelocity = Vector3.of(0, 0, 0);
    cBuidler.orientation = CarOrientation.convert(1, 0, 0);
    CarData car = cBuidler.build();

    Matrix3 target = Matrix3.of(
        Vector3.of(1, 0, 0),
        Vector3.of(0, 1, 0),
        Vector3.of(0, 0, 1));

    ControlsOutput output = new ControlsOutput();

    Angles3.setControlsFor(car, target, output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(-1.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(0.0, .01)));
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(0.0, .01)));
  }

  @Test
  public void test_yAxis() {
    CarData.Builder cbuidler = new CarData.Builder();
    cbuidler.time = 0.0f;
    cbuidler.position = Vector3.of(0, 0, 0);
    cbuidler.velocity = Vector3.of(0, 0, 0);
    cbuidler.angularVelocity = Vector3.of(0, 0, 0);
    cbuidler.orientation = CarOrientation.convert(0, 1, 0);
    CarData car = cbuidler.build();

    Matrix3 target = Matrix3.of(
        Vector3.of(1, 0, 0),
        Vector3.of(0, 1, 0),
        Vector3.of(0, 0, 1));

    ControlsOutput output = new ControlsOutput();

    Angles3.setControlsFor(car, target, output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(0.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(0.0, .01)));
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(-1.0, .01)));
  }

  @Test
  public void test_zAxis() {
    CarData.Builder cbuidler = new CarData.Builder();
    cbuidler.time = 0.0f;
    cbuidler.position = Vector3.of(0, 0, 500);
    cbuidler.velocity = Vector3.of(0, 0, 0);
    cbuidler.angularVelocity = Vector3.of(0, 0, 0);
    cbuidler.orientation = CarOrientation.convert(0, 0, 1.65); // ???
    CarData car = cbuidler.build();

    Matrix3 target = Matrix3.of(
        Vector3.of(1, 0, 0),
        Vector3.of(0, 1, 0),
        Vector3.of(0, 0, 1));

    ControlsOutput output = new ControlsOutput();

    Angles3.setControlsFor(car, target, output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(0.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(-.96, .01))); //???
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(0.0, .01)));
  }

  @Test
  public void test_xAngularVelocity() {
    CarData.Builder cbuidler = new CarData.Builder();
    cbuidler.time = 0.0f;
    cbuidler.position = Vector3.of(0, 0, 0);
    cbuidler.velocity = Vector3.of(0, 0, 0);
    cbuidler.angularVelocity = Vector3.of(15, 0, 0);
    cbuidler.orientation = CarOrientation.convert(0, 0, 0);
    CarData car = cbuidler.build();

    Matrix3 target = Matrix3.of(
        Vector3.of(1, 0, 0),
        Vector3.of(0, -1, 0),
        Vector3.of(0, 0, 1));

    ControlsOutput output = new ControlsOutput();

    Angles3.setControlsFor(car, target, output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(0.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(1, .01)));
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(0.0, .01)));
  }

  @Test
  public void test_targetPosition() {
    CarData.Builder cBuidler = new CarData.Builder();

    cBuidler.time = 0.0f;
    cBuidler.position = Vector3.of(0, 0, 0);
    cBuidler.velocity = Vector3.of(0, 0, 0);
    cBuidler.angularVelocity = Vector3.of(0, 0, 0);
    cBuidler.orientation = CarOrientation.convert(0, 0, 0);
    CarData car = cBuidler.build();

    Matrix3 target = CarOrientation.convert(0, 1, 0).getOrientationMatrix();

    ControlsOutput output = new ControlsOutput();

    Angles3.setControlsFor(car, target, output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(0.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(0.0, .01)));
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(1.0, .01)));
  }

  @Test
  public void test_targetAllPositions() {
    CarData.Builder cBuidler = new CarData.Builder();

    cBuidler.time = 0.0f;
    cBuidler.position = Vector3.of(0, 0, 0);
    cBuidler.velocity = Vector3.of(0, 0, 0);
    cBuidler.angularVelocity = Vector3.of(0, 0, 0);
    cBuidler.orientation = CarOrientation.convert(0, 0, 0);
    CarData car = cBuidler.build();

    Matrix3 target = CarOrientation.convert(1, 1, 1).getOrientationMatrix();

    ControlsOutput output = new ControlsOutput();

    Angles3.setControlsFor(car, target, output);

    assertThat("Pitch", (double) output.getPitch(), is(closeTo(1.0, .01)));
    assertThat("Roll", (double) output.getRoll(), is(closeTo(.3, .01)));
    assertThat("Yaw", (double) output.getYaw(), is(closeTo(.42, .01)));
  }

  public static void print(String s, Object... args) {
    System.out.println(String.format(s, args));
  }
}