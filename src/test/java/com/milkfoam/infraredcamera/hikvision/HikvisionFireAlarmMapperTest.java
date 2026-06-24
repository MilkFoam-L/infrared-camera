package com.milkfoam.infraredcamera.hikvision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class HikvisionFireAlarmMapperTest {

  @Test
  void mapsSdkAlarmToBusinessEvent() {
    HikvisionFireAlarmMapper.RawFireAlarm alarm = new HikvisionFireAlarmMapper.RawFireAlarm(
        "192.168.1.64",
        8000,
        2,
        2,
        pack(2026, 6, 22, 10, 30, 12),
        86.0,
        12.0,
        0.412,
        0.228,
        0.103,
        0.156,
        0.463,
        0.279,
        "snapshot.jpg",
        "COMM_FIREDETECTION_ALARM");

    var event = HikvisionFireAlarmMapper.toEvent("cam-001", alarm, ZoneOffset.ofHours(8));

    assertTrue(event.eventId().startsWith("fire-cam-001-"));
    assertEquals("cam-001", event.cameraId());
    assertEquals(2, event.channelId());
    assertEquals("192.168.1.64", event.deviceIp());
    assertEquals(86.0, event.maxTemperature());
    assertEquals(12.0, event.targetDistance());
    assertEquals(0.412, event.rect().x());
    assertEquals(0.463, event.highestPoint().x());
    assertEquals("/api/fire-events/" + event.eventId() + "/snapshot", event.snapshotUrl());
  }

  private static int pack(int year, int month, int day, int hour, int minute, int second) {
    return ((year - 2000) << 26)
        | (month << 22)
        | (day << 17)
        | (hour << 12)
        | (minute << 6)
        | second;
  }
}
