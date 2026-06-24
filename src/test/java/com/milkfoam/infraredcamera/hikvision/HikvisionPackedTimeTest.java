package com.milkfoam.infraredcamera.hikvision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class HikvisionPackedTimeTest {

  @Test
  void decodesPackedSdkTime() {
    int packed = pack(2026, 6, 22, 10, 30, 12);

    OffsetDateTime decoded = HikvisionPackedTime.decode(packed, ZoneOffset.ofHours(8));

    assertEquals(2026, decoded.getYear());
    assertEquals(6, decoded.getMonthValue());
    assertEquals(22, decoded.getDayOfMonth());
    assertEquals(10, decoded.getHour());
    assertEquals(30, decoded.getMinute());
    assertEquals(12, decoded.getSecond());
    assertEquals(ZoneOffset.ofHours(8), decoded.getOffset());
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
