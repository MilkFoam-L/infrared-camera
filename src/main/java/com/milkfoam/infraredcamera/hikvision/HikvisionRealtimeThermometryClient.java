package com.milkfoam.infraredcamera.hikvision;

import com.milkfoam.infraredcamera.fire.NormalizedPoint;
import com.milkfoam.infraredcamera.fire.ThermalMeasurement;
import com.sun.jna.Pointer;
import java.util.Objects;
import java.util.function.Consumer;

public final class HikvisionRealtimeThermometryClient implements AutoCloseable {

  private final HCNetSdkLibrary sdk;
  private final int userId;
  private HCNetSdkLibrary.FRemoteConfigCallback callback;
  private int handle = -1;

  public HikvisionRealtimeThermometryClient(HCNetSdkLibrary sdk, int userId) {
    this.sdk = Objects.requireNonNull(sdk, "sdk");
    this.userId = userId;
  }

  public synchronized void start(int channelId, int ruleId, Consumer<ThermalMeasurement> consumer) {
    if (handle >= 0) {
      return;
    }
    Objects.requireNonNull(consumer, "consumer");
    HCNetSdkLibrary.NET_DVR_REALTIME_THERMOMETRY_COND cond = condition(channelId, ruleId);
    cond.write();
    callback = (dwType, lpBuffer, dwBufLen, pUserData) -> {
      if (dwType != HCNetSdkLibrary.NET_SDK_CALLBACK_TYPE_DATA || lpBuffer == null) {
        return;
      }
      HCNetSdkLibrary.NET_DVR_THERMOMETRY_UPLOAD upload =
          new HCNetSdkLibrary.NET_DVR_THERMOMETRY_UPLOAD(lpBuffer);
      consumer.accept(toMeasurement(upload));
    };
    handle = sdk.NET_DVR_StartRemoteConfig(
        userId,
        HCNetSdkLibrary.NET_DVR_GET_REALTIME_THERMOMETRY,
        cond.getPointer(),
        cond.size(),
        callback,
        Pointer.NULL);
    if (handle < 0) {
      throw new IllegalStateException("NET_DVR_StartRemoteConfig failed, sdkError=" + sdk.NET_DVR_GetLastError());
    }
  }

  @Override
  public synchronized void close() {
    if (handle >= 0) {
      sdk.NET_DVR_StopRemoteConfig(handle);
      handle = -1;
    }
  }

  static HCNetSdkLibrary.NET_DVR_REALTIME_THERMOMETRY_COND condition(int channelId, int ruleId) {
    if (channelId <= 0) {
      throw new IllegalArgumentException("channelId must be positive");
    }
    if (ruleId < 0 || ruleId > 255) {
      throw new IllegalArgumentException("ruleId must be between 0 and 255");
    }
    HCNetSdkLibrary.NET_DVR_REALTIME_THERMOMETRY_COND cond =
        new HCNetSdkLibrary.NET_DVR_REALTIME_THERMOMETRY_COND();
    cond.dwChan = channelId;
    cond.byRuleID = (byte) ruleId;
    return cond;
  }

  static ThermalMeasurement toMeasurement(HCNetSdkLibrary.NET_DVR_THERMOMETRY_UPLOAD upload) {
    double min = upload.fLowestPointTemperature;
    double max = upload.fHighestPointTemperature;
    double average = upload.struLinePolygonThermCfg.fAverageTemperature;
    if (average == 0.0 && upload.struPointThermCfg.fTemperature != 0.0) {
      average = upload.struPointThermCfg.fTemperature;
      min = upload.struPointThermCfg.fTemperature;
      max = upload.struPointThermCfg.fTemperature;
    }
    return new ThermalMeasurement(
        min,
        max,
        average,
        new NormalizedPoint(upload.struHighestPoint.fX, upload.struHighestPoint.fY));
  }
}
