package com.eru.rlbot.common.util;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * A class which extracts build info from the manifest file.
 */
public class BuildInfo {

  private static final BuildInfo UNKNOWN = new BuildInfo("Unknown", "-", "-", "-");
  private static final String BUILD_DATE_TIME = "build-date-time";
  private static final String BUILD_USERNAME = "build-user";
  private static final String BUILD_VERSION = "version";
  private static final String BUILD_TITLE = "title";

  private static volatile BuildInfo INSTANCE;

  public static BuildInfo getInfo() {
    if (INSTANCE == null) {
      try {
        INSTANCE = readFromManifest();
      } catch (IOException e) {
        INSTANCE = BuildInfo.UNKNOWN;
      }
    }

    return INSTANCE;
  }

  private static BuildInfo readFromManifest() throws IOException {
    Manifest manifest =
        new Manifest(BuildInfo.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"));
    Attributes attributes = manifest.getMainAttributes();
    return new BuildInfo(
        attributes.getValue(BUILD_USERNAME),
        attributes.getValue(BUILD_DATE_TIME),
        attributes.getValue(BUILD_VERSION),
        attributes.getValue(BUILD_TITLE));
  }

  public final String builtBy;
  public final String buildTime;
  public final String buildVersion;
  public final String title;

  private BuildInfo(String builtBy, String buildTime, String buildVersion, String title) {
    this.builtBy = builtBy;
    this.buildTime = buildTime;
    this.buildVersion = buildVersion;
    this.title = title;
  }

  private static final String NEW_LINE = "\n";

  public String displayFormat() {
    return "Bot: " + title + NEW_LINE +
        "Version: " + buildVersion + NEW_LINE +
        "Build Time: " + buildTime + NEW_LINE +
        "Built By: " + builtBy;
  }
}
