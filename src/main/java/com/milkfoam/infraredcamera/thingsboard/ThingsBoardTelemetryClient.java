package com.milkfoam.infraredcamera.thingsboard;

import com.milkfoam.infraredcamera.fire.FireDetectionEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ThingsBoardTelemetryClient implements AutoCloseable {

  private final ThingsBoardConfig config;
  private final HttpClient httpClient;
  private final ExecutorService executor;

  public ThingsBoardTelemetryClient(ThingsBoardConfig config) {
    this.config = Objects.requireNonNull(config, "config");
    this.executor = Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r, "thingsboard-telemetry-worker");
      thread.setDaemon(true);
      return thread;
    });
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
    if (config.enabled()) {
      System.out.println("ThingsBoard telemetry enabled: " + config.telemetryUri());
    } else {
      System.out.println("ThingsBoard telemetry disabled: missing --thingsboard-host or --thingsboard-token");
    }
  }

  public void sendFireDetected(FireDetectionEvent event) {
    Objects.requireNonNull(event, "event");
    if (!config.enabled()) {
      System.out.println("ThingsBoard telemetry skipped: telemetry is disabled, eventId=" + event.eventId());
      return;
    }
    System.out.println("ThingsBoard telemetry queued: eventId=" + event.eventId());
    executor.execute(() -> postTelemetry(event));
  }

  @Override
  public void close() {
    executor.shutdownNow();
  }

  static String telemetryJson(FireDetectionEvent event) {
    return "{"
        + json("warning_flag", "1") + ","
        + json("warning_status", "1") + ","
        + json("camera_id", event.cameraId()) + ","
        + number("channel_id", event.channelId()) + ","
        + json("device_ip", event.deviceIp()) + ","
        + json("event_id", event.eventId()) + ","
        + number("max_temperature", event.maxTemperature()) + ","
        + number("target_distance", event.targetDistance()) + ","
        + number("fire_x", event.rect().x()) + ","
        + number("fire_y", event.rect().y()) + ","
        + number("fire_width", event.rect().width()) + ","
        + number("fire_height", event.rect().height()) + ","
        + number("highest_x", event.highestPoint().x()) + ","
        + number("highest_y", event.highestPoint().y()) + ","
        + json("event_time", event.eventTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        + "}";
  }

  private void postTelemetry(FireDetectionEvent event) {
    String body = telemetryJson(event);
    URI uri = config.telemetryUri();
    HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(5))
        .header("Content-Type", "application/json; charset=utf-8")
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        .build();
    System.out.println("ThingsBoard telemetry posting: eventId=" + event.eventId() + ", url=" + uri);
    System.out.println("ThingsBoard telemetry payload: " + body);
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      System.out.println("ThingsBoard telemetry response: eventId=" + event.eventId()
          + ", status=" + response.statusCode()
          + ", body=" + response.body());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        System.out.println("ThingsBoard telemetry failed: non-2xx status, eventId=" + event.eventId());
      }
    } catch (Exception ex) {
      System.out.println("ThingsBoard telemetry exception: eventId=" + event.eventId()
          + ", type=" + ex.getClass().getName()
          + ", message=" + ex.getMessage());
      ex.printStackTrace(System.out);
    }
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
