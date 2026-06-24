package com.milkfoam.infraredcamera.hikvision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.jna.win32.StdCallLibrary;
import org.junit.jupiter.api.Test;

class HCNetSdkLibraryTest {

  @Test
  void fireAlarmCallbackUsesStdCallConventionForWindowsSdk() {
    assertTrue(StdCallLibrary.StdCallCallback.class.isAssignableFrom(HCNetSdkLibrary.FMSGCallBack_V50.class));
  }

  @Test
  void remoteConfigCallbackUsesStdCallConventionForThermometry() {
    assertTrue(StdCallLibrary.StdCallCallback.class.isAssignableFrom(HCNetSdkLibrary.FRemoteConfigCallback.class));
  }

  @Test
  void exposesRealtimeThermometryCommandFromSdkDocument() {
    assertEquals(3629, HCNetSdkLibrary.NET_DVR_GET_REALTIME_THERMOMETRY);
    assertEquals(2, HCNetSdkLibrary.NET_SDK_CALLBACK_TYPE_DATA);
  }

  @Test
  void realtimeThermometryConditionInitializesStructureSize() {
    HCNetSdkLibrary.NET_DVR_REALTIME_THERMOMETRY_COND cond =
        new HCNetSdkLibrary.NET_DVR_REALTIME_THERMOMETRY_COND();

    assertTrue(cond.dwSize > 0);
  }

  @Test
  void thermometryUploadStructureContainsHighestPointFields() {
    HCNetSdkLibrary.NET_DVR_THERMOMETRY_UPLOAD upload =
        new HCNetSdkLibrary.NET_DVR_THERMOMETRY_UPLOAD();

    assertEquals(0.0f, upload.fHighestPointTemperature);
    assertEquals(0.0f, upload.struHighestPoint.fX);
    assertEquals(0.0f, upload.struHighestPoint.fY);
  }
}
