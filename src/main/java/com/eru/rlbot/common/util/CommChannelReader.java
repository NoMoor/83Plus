package com.eru.rlbot.common.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    InputStream port = ClassLoader.getSystemClassLoader().getResourceAsStream(portFile);

    try (Stream<String> lines = new BufferedReader(new InputStreamReader(port)).lines()) {
      return lines.findFirst()
          .map(Integer::parseInt)
          .orElseThrow(() -> new RuntimeException("Config file was empty!"));
    } catch (final Exception e) {
      throw new RuntimeException("Failed to read port file! Tried to find it at "
          + portFile);
    }
  }

  private CommChannelReader() {}
}
