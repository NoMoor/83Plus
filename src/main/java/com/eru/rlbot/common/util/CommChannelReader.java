package com.eru.rlbot.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Utility for reading a network port out of a config file. Expects a file that has only one line
 * that's just a number.
 */
public final class CommChannelReader {

  /**
   * Obtains the port from the given file.
   */
  public static int readPortFromFile(String portFile) {
    Path path = Paths.get(portFile);

    try (Stream<String> lines = Files.lines(path)) {
      return lines.findFirst()
          .map(Integer::parseInt)
          .orElseThrow(() -> new RuntimeException("Config file was empty!"));
    } catch (final IOException e) {
      throw new RuntimeException("Failed to read port file! Tried to find it at "
          + path.toAbsolutePath().toString());
    }
  }

  private CommChannelReader() {
  }
}
