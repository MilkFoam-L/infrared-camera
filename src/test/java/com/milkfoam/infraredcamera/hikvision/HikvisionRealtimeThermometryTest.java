package com.milkfoam.infraredcamera.hikvision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.milkfoam.infraredcamera.fire.NormalizedPoint;
import com.milkfoam.infraredcamera.fire.ThermalMeasurement;
import org.junit.jupiter.api.Test;

class HikvisionRealtimeThermometryTest {

  @Test
  void mapsThermometryUploadToMeasurement() {
    HCNetSdkLibrary.NET_DVR_THERMOMETRY_UPLOAD upload = new HCNetSdkLibrary.NET_DVR_THERMOMETRY_UPLOAD();
    upload.fHighestPointTemperature = 88.5f;
    upload.fLowestPointTemperature = 21.5f;
    upload.struLinePolygonThermCfg.fAverageTemperature = 45.25f;
    upload.struHighestPoint.fX = 0.42f;
    upload.struHighestPoint.fY = 0.31f;

    ThermalMeasurement measurement = HikvisionRealtimeThermometryClient.toMeasurement(upload);

    assertEquals(21.5, measurement.minTemperature(), 0.0001);
    assertEquals(88.5, measurement.maxTemperature(), 0.0001);
    assertEquals(45.25, measurement.averageTemperature(), 0.0001);
    assertEquals(0.42, measurement.highestPoint().x(), 0.0001);
    assertEquals(0.31, measurement.highestPoint().y(), 0.0001);
  }

  @Test
  void buildsRealtimeThermometryCondition() {
    HCNetSdkLibrary.NET_DVR_REALTIME_THERMOMETRY_COND cond =
        HikvisionRealtimeThermometryClient.condition(2, 1);

    assertTrue(cond.dwSize > 0);
    assertEquals(2, cond.dwChan);
    assertEquals(1, cond.byRuleID);
  }
}
