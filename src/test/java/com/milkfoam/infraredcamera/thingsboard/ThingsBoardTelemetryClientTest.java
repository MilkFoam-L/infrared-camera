package com.milkfoam.infraredcamera.thingsboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.milkfoam.infraredcamera.fire.FireDetectionEvent;
import com.milkfoam.infraredcamera.fire.NormalizedPoint;
import com.milkfoam.infraredcamera.fire.NormalizedRect;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ThingsBoardTelemetryClientTest {

  @Test
  void buildsTelemetryUrlFromHostAndToken() {
    ThingsBoardConfig config = new ThingsBoardConfig("192.168.1.78:8080", "token-123");

    assertTrue(config.enabled());
    assertEquals(URI.create("http://192.168.1.78:8080/api/v1/token-123/telemetry"), config.telemetryUri());
  }

  @Test
  void disabledWhenHostOrTokenIsBlank() {
    assertFalse(new ThingsBoardConfig("", "token").enabled());
    assertFalse(new ThingsBoardConfig("192.168.1.78:8080", " ").enabled());
  }

  @Test
  void telemetryJsonOnlyContainsWarningFlagAndStatus() {
    FireDetectionEvent event = new FireDetectionEvent(
        "fire-001",
        "hm-tcq203-s",
        2,
        "192.168.1.64",
        "fire_detection",
        OffsetDateTime.of(2026, 6, 23, 9, 30, 0, 0, ZoneOffset.ofHours(8)),
        86.5,
        12.25,
        new NormalizedRect(0.1, 0.2, 0.3, 0.4),
        new NormalizedPoint(0.25, 0.35),
        245,
        "/api/fire-events/fire-001/snapshot",
        "COMM_FIREDETECTION_ALARM");

    String json = ThingsBoardTelemetryClient.telemetryJson(event);

    assertEquals("{\"warning_flag\":\"1\",\"warning_status\":\"1\"}", json);
  }
}
