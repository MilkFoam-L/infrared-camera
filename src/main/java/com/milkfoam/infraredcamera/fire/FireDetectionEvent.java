package com.milkfoam.infraredcamera.fire;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public record FireDetectionEvent(
    String eventId,
    String cameraId,
    int channelId,
    String deviceIp,
    String eventType,
    OffsetDateTime eventTime,
    double maxTemperature,
    double targetDistance,
    NormalizedRect rect,
    NormalizedPoint highestPoint,
    int fireBrightnessThreshold,
    String snapshotUrl,
    String rawCommand) {

  public FireDetectionEvent {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(cameraId, "cameraId");
    Objects.requireNonNull(deviceIp, "deviceIp");
    Objects.requireNonNull(eventType, "eventType");
    Objects.requireNonNull(eventTime, "eventTime");
    Objects.requireNonNull(rect, "rect");
    Objects.requireNonNull(highestPoint, "highestPoint");
    Objects.requireNonNull(snapshotUrl, "snapshotUrl");
    Objects.requireNonNull(rawCommand, "rawCommand");
    if (channelId <= 0) {
      throw new IllegalArgumentException("channelId must be positive");
    }
    if (fireBrightnessThreshold < 0 || fireBrightnessThreshold > 255) {
      throw new IllegalArgumentException("fireBrightnessThreshold must be between 0 and 255");
    }
  }

  public String toJson() {
    return "{"
        + json("eventId", eventId) + ","
        + json("cameraId", cameraId) + ","
        + number("channelId", channelId) + ","
        + json("deviceIp", deviceIp) + ","
        + json("eventType", eventType) + ","
        + json("eventTime", eventTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)) + ","
        + number("maxTemperature", maxTemperature) + ","
        + number("targetDistance", targetDistance) + ","
        + "\"rect\":{"
        + number("x", rect.x()) + ","
        + number("y", rect.y()) + ","
        + number("width", rect.width()) + ","
        + number("height", rect.height()) + "},"
        + "\"highestPoint\":{"
        + number("x", highestPoint.x()) + ","
        + number("y", highestPoint.y()) + "},"
        + number("fireBrightnessThreshold", fireBrightnessThreshold) + ","
        + json("snapshotUrl", snapshotUrl) + ","
        + json("rawCommand", rawCommand)
        + "}";
  }

  private static String json(String name, String value) {
    return "\"" + name + "\":\"" + escape(value) + "\"";
  }

  private static String number(String name, int value) {
    return "\"" + name + "\":" + value;
  }

  private static String number(String name, double value) {
    return "\"" + name + "\":" + String.format(Locale.ROOT, "%.6f", value);
  }

  private static String escape(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
  }
}
