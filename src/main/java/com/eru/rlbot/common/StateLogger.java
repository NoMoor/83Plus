package com.eru.rlbot.common;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import rlbot.gamestate.DesiredRotation;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;

public class StateLogger {

  private static final String FILE_NAME = "captured_event";

  private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer()
      .includingDefaultValueFields()
      .preservingProtoFieldNames()
      .omittingInsignificantWhitespace();

  private static final long BUFFER_LENGTH = 4 * Constants.STEP_SIZE_COUNT;
  private static final Queue<DataPacket> dataPacketBuffer = new LinkedList<>();

  public static void log(DataPacket input) {
    dataPacketBuffer.add(input);
    while (dataPacketBuffer.size() > BUFFER_LENGTH) {
      dataPacketBuffer.poll();
    }
  }

  public static void capture() {
    if (dataPacketBuffer.isEmpty()) {
      return;
    }

    String fileName = FILE_NAME + System.currentTimeMillis() + ".dat";
    try (PrintWriter printWriter = new PrintWriter(new FileWriter(fileName))) {
      dataPacketBuffer.stream()
          .map(StateLogger::formatEntry)
          .forEach(printWriter::println);
    } catch (IOException e) {
      // Swallow it.
    }
  }

  private static String formatEntry(DataPacket packet) {
    GameStateProtos.GameState gameState = GameStateProtos.GameState.newBuilder()
        .setFrameId(convert(packet.car.elapsedSeconds))
        .addAllCar(packet.allCars.stream()
            .map(StateLogger::convert)
            .collect(toImmutableList()))
        .setBall(convert(packet.ball))
        .build();

    try {
      return JSON_PRINTER.print(gameState);
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
      return "Error: " + e.getMessage();
    }
  }

  private static long convert(float elapsedSeconds) {
    return Math.round(elapsedSeconds * 120);
  }

  private static GameStateProtos.GameState.CarState convert(CarData car) {
    return GameStateProtos.GameState.CarState.newBuilder()
        .setId(car.playerIndex)
        .setTeam(car.team)
        .addAllPos(convert(car.position))
        .addAllVel(convert(car.velocity))
        .addAllSpin(convert(car.angularVelocity))
        .addAllOrientation(convert(car.orientation))
        .build();
  }

  private static ImmutableList<Float> convert(Orientation orientation) {
    DesiredRotation eulerVector = orientation.toEuclidianVector();
    return ImmutableList.of(eulerVector.pitch, eulerVector.yaw, eulerVector.roll);
  }

  private static GameStateProtos.GameState.BallState convert(BallData ball) {
    return GameStateProtos.GameState.BallState.newBuilder()
        .addAllPos(convert(ball.position))
        .addAllVel(convert(ball.velocity))
        .addAllSpin(convert(ball.spin))
        .build();
  }

  private static ImmutableList<Float> convert(Vector3 vec) {
    return ImmutableList.of(vec.x, vec.y, vec.z);
  }
}
