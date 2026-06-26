package com.milkfoam.infraredcamera.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class FireModelTest {

  @Test
  void acceptsNormalizedRectInsideImage() {
    NormalizedRect rect = new NormalizedRect(0.2, 0.3, 0.4, 0.5);

    assertEquals(0.2, rect.x());
    assertEquals(0.3, rect.y());
    assertEquals(0.4, rect.width());
    assertEquals(0.5, rect.height());
    assertEquals(0.4, rect.centerX());
    assertEquals(0.55, rect.centerY());
  }

  @Test
  void rejectsRectOutsideImage() {
    assertThrows(IllegalArgumentException.class, () -> new NormalizedRect(0.8, 0.3, 0.3, 0.2));
    assertThrows(IllegalArgumentException.class, () -> new NormalizedRect(-0.1, 0.3, 0.2, 0.2));
    assertThrows(IllegalArgumentException.class, () -> new NormalizedRect(0.1, 0.3, 0.0, 0.2));
  }

  @Test
  void rejectsPointOutsideImage() {
    assertThrows(IllegalArgumentException.class, () -> new NormalizedPoint(1.1, 0.2));
    assertThrows(IllegalArgumentException.class, () -> new NormalizedPoint(0.2, -0.1));
  }

  @Test
  void reportsFirePresenceFromEvent() {
    FireDetectionEvent event = new FireDetectionEvent(
        "fire-1",
        "cam-001",
        2,
        "192.168.1.64",
        "fire_detection",
        OffsetDateTime.parse("2026-06-22T10:30:12+08:00"),
        86.5,
        12.0,
        new NormalizedRect(0.4, 0.2, 0.1, 0.2),
        new NormalizedPoint(0.45, 0.3),
        245,
        "/api/fire-events/fire-1/snapshot",
        "COMM_FIREDETECTION_ALARM");

    FireDetectionStatus status = FireDetectionStatus.from(event);

    assertTrue(status.fireDetected());
    assertEquals("cam-001", status.cameraId());
    assertEquals(2, status.channelId());
    assertEquals(86.5, status.maxTemperature());
    assertEquals(0.4, status.rect().x());
    assertTrue(event.toJson().contains("\"fireBrightnessThreshold\":245"));
  }

  @Test
  void emptyStatusMeansNoFire() {
    FireDetectionStatus status = FireDetectionStatus.noFire("cam-001", 2);

    assertFalse(status.fireDetected());
    assertEquals("cam-001", status.cameraId());
    assertEquals(2, status.channelId());
  }
}
