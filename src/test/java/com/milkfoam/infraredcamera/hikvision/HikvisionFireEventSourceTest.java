package com.milkfoam.infraredcamera.hikvision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.milkfoam.infraredcamera.fire.FireSnapshotStore;
import com.milkfoam.infraredcamera.fire.LiveFrameStore;
import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;

class HikvisionFireEventSourceTest {

  @Test
  void startsRealtimeThermometryWhenFireEventSourceStarts() {
    FakeSdk sdk = new FakeSdk();
    LiveFrameStore liveFrameStore = new LiveFrameStore();
    HikvisionFireEventSource source = new HikvisionFireEventSource(config(), new FireSnapshotStore(), liveFrameStore, sdk);

    source.start(event -> { });

    assertTrue(sdk.remoteConfigStarted);
    assertEquals(12, sdk.remoteConfigUserId);
    assertEquals(HCNetSdkLibrary.NET_DVR_GET_REALTIME_THERMOMETRY, sdk.remoteConfigCommand);
    assertEquals(2, sdk.remoteConfigChannel);
    assertEquals(0, sdk.remoteConfigRuleId);
    assertNotNull(sdk.remoteConfigCallback);
    assertTrue(liveFrameStore.latest().isPresent());
  }

  @Test
  void stopsRealtimeThermometryBeforeCleaningUpSdk() {
    FakeSdk sdk = new FakeSdk();
    LiveFrameStore liveFrameStore = new LiveFrameStore();
    HikvisionFireEventSource source = new HikvisionFireEventSource(config(), new FireSnapshotStore(), liveFrameStore, sdk);
    source.start(event -> { });

    source.close();

    assertEquals(77, sdk.stoppedRemoteConfigHandle);
    assertTrue(sdk.cleanedUp);
  }

  private static HikvisionClientConfig config() {
    return new HikvisionClientConfig(
        "cam-001",
        "192.168.1.64",
        8000,
        "admin",
        "password",
        2,
        null);
  }

  private static final class FakeSdk implements HCNetSdkLibrary {
    private boolean remoteConfigStarted;
    private int remoteConfigUserId;
    private int remoteConfigCommand;
    private int remoteConfigChannel;
    private int remoteConfigRuleId;
    private FRemoteConfigCallback remoteConfigCallback;
    private int stoppedRemoteConfigHandle = -1;
    private boolean cleanedUp;

    @Override
    public boolean NET_DVR_SetSDKInitCfg(int enumType, Pointer lpInBuff) {
      return true;
    }

    @Override
    public boolean NET_DVR_Init() {
      return true;
    }

    @Override
    public boolean NET_DVR_SetConnectTime(int dwWaitTime, int dwTryTimes) {
      return true;
    }

    @Override
    public boolean NET_DVR_SetReconnect(int dwInterval, boolean bEnableRecon) {
      return true;
    }

    @Override
    public boolean NET_DVR_SetLogToFile(int nLogLevel, String strLogDir, boolean bAutoDel) {
      return true;
    }

    @Override
    public int NET_DVR_GetLastError() {
      return 0;
    }

    @Override
    public boolean NET_DVR_Cleanup() {
      cleanedUp = true;
      return true;
    }

    @Override
    public int NET_DVR_Login_V40(NET_DVR_USER_LOGIN_INFO pLoginInfo, NET_DVR_DEVICEINFO_V40 lpDeviceInfo) {
      return 12;
    }

    @Override
    public boolean NET_DVR_Logout(int lUserID) {
      return true;
    }

    @Override
    public boolean NET_DVR_STDXMLConfig(
        int lUserID,
        NET_DVR_XML_CONFIG_INPUT lpInputParam,
        NET_DVR_XML_CONFIG_OUTPUT lpOutputParam) {
      return true;
    }

    @Override
    public boolean NET_DVR_SetDVRMessageCallBack_V50(
        int iIndex,
        FMSGCallBack_V50 fMessageCallBack,
        Pointer pUser) {
      return true;
    }

    @Override
    public int NET_DVR_SetupAlarmChan_V41(int lUserID, NET_DVR_SETUPALARM_PARAM lpSetupParam) {
      return 34;
    }

    @Override
    public boolean NET_DVR_CloseAlarmChan_V30(int lAlarmHandle) {
      return true;
    }

    @Override
    public int NET_DVR_StartRemoteConfig(
        int lUserID,
        int dwCommand,
        Pointer lpInBuffer,
        int dwInBufferLen,
        FRemoteConfigCallback cbStateCallback,
        Pointer pUserData) {
      remoteConfigStarted = true;
      remoteConfigUserId = lUserID;
      remoteConfigCommand = dwCommand;
      remoteConfigChannel = lpInBuffer.getInt(4);
      remoteConfigRuleId = Byte.toUnsignedInt(lpInBuffer.getByte(8));
      remoteConfigCallback = cbStateCallback;
      return 77;
    }

    @Override
    public boolean NET_DVR_StopRemoteConfig(int lHandle) {
      stoppedRemoteConfigHandle = lHandle;
      return true;
    }

    @Override
    public boolean NET_DVR_CaptureJPEGPicture_NEW(
        int lUserID,
        int lChannel,
        NET_DVR_JPEGPARA lpJpegPara,
        Pointer sJpegPicBuffer,
        int dwPicSize,
        int[] lpSizeReturned) {
      byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, 1, (byte) 0xD9};
      sJpegPicBuffer.write(0, jpeg, 0, jpeg.length);
      lpSizeReturned[0] = jpeg.length;
      return true;
    }
  }
}
