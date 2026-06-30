package com.milkfoam.infraredcamera.hikvision;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.milkfoam.infraredcamera.fire.LiveFrameStore;
import com.sun.jna.Pointer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class HikvisionThermalSnapshotClientTest {

  @Test
  void capturesJpegFrameIntoLiveFrameStore() {
    FakeSdk sdk = new FakeSdk();
    LiveFrameStore store = new LiveFrameStore();
    HikvisionThermalSnapshotClient client = new HikvisionThermalSnapshotClient(sdk, 12, 2, store);

    assertTrue(client.captureOnce());

    assertEquals(12, sdk.userId);
    assertEquals(2, sdk.channelId);
    assertEquals(0, sdk.pictureSize);
    assertEquals(0, sdk.pictureQuality);
    assertTrue(sdk.bufferSize >= 4);
    assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xD8, 1, (byte) 0xD9}, store.latest().orElseThrow().bytes());
  }

  @Test
  void captureDoesNotRunLocalBrightnessDetection() throws IOException {
    FakeSdk sdk = new FakeSdk();
    sdk.jpeg = thermalImageWithBrightRegion();
    LiveFrameStore store = new LiveFrameStore();
    AtomicInteger detections = new AtomicInteger();
    HikvisionThermalSnapshotClient client = new HikvisionThermalSnapshotClient(
        sdk,
        12,
        2,
        store,
        detection -> detections.incrementAndGet(),
        512 * 1024,
        170);

    assertTrue(client.captureOnce());

    assertEquals(0, detections.get());
    assertArrayEquals(sdk.jpeg, store.latest().orElseThrow().bytes());
  }

  @Test
  void doesNotUpdateStoreWhenCaptureFails() {
    FakeSdk sdk = new FakeSdk();
    sdk.captureResult = false;
    LiveFrameStore store = new LiveFrameStore();
    HikvisionThermalSnapshotClient client = new HikvisionThermalSnapshotClient(sdk, 12, 2, store);

    assertFalse(client.captureOnce());

    assertTrue(store.latest().isEmpty());
  }

  private static byte[] thermalImageWithBrightRegion() throws IOException {
    BufferedImage image = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        image.setRGB(x, y, new Color(35, 35, 35).getRGB());
      }
    }
    for (int y = 30; y < 45; y++) {
      for (int x = 40; x < 58; x++) {
        image.setRGB(x, y, Color.WHITE.getRGB());
      }
    }
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }

  private static final class FakeSdk implements HCNetSdkLibrary {
    private boolean captureResult = true;
    private byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, 1, (byte) 0xD9};
    private int userId;
    private int channelId;
    private int pictureSize;
    private int pictureQuality;
    private int bufferSize;

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
      return 23;
    }

    @Override
    public boolean NET_DVR_Cleanup() {
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
      return 77;
    }

    @Override
    public boolean NET_DVR_StopRemoteConfig(int lHandle) {
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
      userId = lUserID;
      channelId = lChannel;
      pictureSize = Short.toUnsignedInt(lpJpegPara.wPicSize);
      pictureQuality = Short.toUnsignedInt(lpJpegPara.wPicQuality);
      sJpegPicBuffer.write(0, jpeg, 0, jpeg.length);
      lpSizeReturned[0] = jpeg.length;
      bufferSize = dwPicSize;
      return captureResult;
    }
  }
}
