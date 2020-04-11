package com.eru.rlbot.common;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.bot.flags.GlobalDebugOptions;
import com.eru.rlbot.common.input.BallData;
import com.eru.rlbot.common.input.CarData;
import com.eru.rlbot.common.input.DataPacket;
import com.eru.rlbot.common.input.Orientation;
import com.eru.rlbot.common.vector.Vector3;
import com.eru.rlbot.testing.TrainingId;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rlbot.gamestate.DesiredRotation;

/**
 * Utility for logging game states to proto json format.
 */
public class StateLogger {

  private static final Logger logger = LogManager.getLogger("StateLogger");
  private static final Logger log2Console = LogManager.getLogger("StateLoggerConsole");

  private static final String FILE_NAME = "captured_event";

  private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer()
      .includingDefaultValueFields()
      .preservingProtoFieldNames()
      .omittingInsignificantWhitespace();

  private static final long BUFFER_LENGTH = 4 * Constants.STEP_SIZE_COUNT;
  private static final ConcurrentLinkedDeque<DataPacket> dataPacketBuffer = new ConcurrentLinkedDeque<>();

  private static volatile int controllingPlayer = -1;

  /** Tracks the input in the buffer. */
  public static void track(DataPacket input) {
    if (controllingPlayer == -1) {
      controllingPlayer = input.car.serialNumber;
    } else if (controllingPlayer != input.car.serialNumber) {
      return;
    }

    synchronized (dataPacketBuffer) {
      dataPacketBuffer.add(input);
      while (dataPacketBuffer.size() > BUFFER_LENGTH) {
        dataPacketBuffer.poll();
      }
    }
  }

  /**
   * Captures the data buffer into a file.
   */
  public static void capture() {
    ImmutableList<DataPacket> eventCapture;
    synchronized (dataPacketBuffer) {
      eventCapture = ImmutableList.copyOf(dataPacketBuffer);
      dataPacketBuffer.clear();
    }

    if (eventCapture.isEmpty()) {
      return;
    }

    String folderName = "logs/captures/";
    ensureFolderExists(folderName);
    String fileName = folderName + FILE_NAME + System.currentTimeMillis() + ".dat";

    try (PrintWriter printWriter = new PrintWriter(new FileWriter(fileName))) {
      eventCapture.stream()
          .map(StateLogger::format)
          .forEach(printWriter::println);
    } catch (IOException e) {
      log2Console.error("Cannot write state", e);
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static void ensureFolderExists(String folderName) {
    new File(folderName).mkdirs();
  }

  private static String format(DataPacket input) {
    return format(input, "");
  }

  private static String format(DataPacket input, String label) {
    try {
      return JSON_PRINTER.print(convert(input, label));
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
      return "";
    }
  }

  public static String format(BallData ball) {
    try {
      return JSON_PRINTER.print(convert(ball));
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
      return "";
    }
  }

  private static GameStateProtos.GameState convert(DataPacket input, String label) {
    return GameStateProtos.GameState.newBuilder()
        .setFrameId(toFrameId(input.car.elapsedSeconds))
        .addAllCar(input.allCars.stream()
            .map(StateLogger::convert)
            .collect(toImmutableList()))
        .setBall(convert(input.ball))
        .setTrainingId(TrainingId.getId())
        .setLabel(label)
        .build();
  }

  /**
   * Logs the packet in json format.
   *
   * @see #log(DataPacket, String)
   */
  public static void log(DataPacket packet) {
    log(packet, "");
  }

  /**
   * Logs the packet and label in json format.
   */
  public static void log(DataPacket packet, String label) {
    if (!GlobalDebugOptions.isStateLoggerEnabled()) {
      return;
    }

    String formattedEntry = format(packet, label);
    if (!formattedEntry.isEmpty()) {
      logger.info(formattedEntry);
    }
  }

  /**
   * Returns the data packet as a proto in json string format.
   */
  public static String toJson(DataPacket packet) {
    return format(packet);
  }

  /**
   * Converst the elapsed seconds to a frame id.
   */
  private static long toFrameId(float elapsedSeconds) {
    return Math.round(elapsedSeconds * 120);
  }

  private static GameStateProtos.GameState.CarState convert(CarData car) {
    return GameStateProtos.GameState.CarState.newBuilder()
        .setId(car.serialNumber)
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
