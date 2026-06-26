package com.milkfoam.infraredcamera.hikvision;

import com.milkfoam.infraredcamera.fire.FireDetectionEvent;
import com.milkfoam.infraredcamera.fire.NormalizedPoint;
import com.milkfoam.infraredcamera.fire.NormalizedRect;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class HikvisionFireAlarmMapper {

  private HikvisionFireAlarmMapper() {
  }

  public static FireDetectionEvent toEvent(String cameraId, RawFireAlarm alarm, ZoneOffset offset) {
    Objects.requireNonNull(cameraId, "cameraId");
    Objects.requireNonNull(alarm, "alarm");
    var eventTime = HikvisionPackedTime.decode(alarm.packedAbsTime(), offset);
    String eventId = "fire-" + cameraId + "-" + eventTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    return new FireDetectionEvent(
        eventId,
        cameraId,
        alarm.channelId() > 0 ? alarm.channelId() : alarm.ivmsChannelId(),
        alarm.deviceIp(),
        "fire_detection",
        eventTime,
        alarm.maxTemperature(),
        alarm.targetDistance(),
        new NormalizedRect(alarm.rectX(), alarm.rectY(), alarm.rectWidth(), alarm.rectHeight()),
        new NormalizedPoint(alarm.pointX(), alarm.pointY()),
        0,
        "/api/fire-events/" + eventId + "/snapshot",
        alarm.rawCommand());
  }

  public record RawFireAlarm(
      String deviceIp,
      int port,
      int channelId,
      int ivmsChannelId,
      int packedAbsTime,
      double maxTemperature,
      double targetDistance,
      double rectX,
      double rectY,
      double rectWidth,
      double rectHeight,
      double pointX,
      double pointY,
      String snapshotName,
      String rawCommand) {
  }
}
