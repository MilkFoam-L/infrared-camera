package com.milkfoam.infraredcamera.hikvision;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class HikvisionPackedTime {

  private HikvisionPackedTime() {
  }

  public static OffsetDateTime decode(int packedTime, ZoneOffset offset) {
    int year = ((packedTime >>> 26) & 0x3f) + 2000;
    int month = (packedTime >>> 22) & 0x0f;
    int day = (packedTime >>> 17) & 0x1f;
    int hour = (packedTime >>> 12) & 0x1f;
    int minute = (packedTime >>> 6) & 0x3f;
    int second = packedTime & 0x3f;
    return OffsetDateTime.of(year, month, day, hour, minute, second, 0, offset);
  }
}
