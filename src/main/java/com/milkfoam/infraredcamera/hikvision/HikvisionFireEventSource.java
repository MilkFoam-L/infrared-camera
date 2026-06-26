package com.milkfoam.infraredcamera.hikvision;

import com.milkfoam.infraredcamera.fire.FireDetectionEvent;
import com.milkfoam.infraredcamera.fire.FireSnapshotStore;
import com.milkfoam.infraredcamera.fire.LiveFrameStore;
import com.milkfoam.infraredcamera.fire.ThermalImageFireDetector;
import com.milkfoam.infraredcamera.runtime.FireEventSource;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class HikvisionFireEventSource implements FireEventSource {

  private final HikvisionClientConfig config;
  private final FireSnapshotStore snapshotStore;
  private final LiveFrameStore liveFrameStore;
  private final HCNetSdkLibrary sdk;
  private final AtomicInteger localDetectionSequence = new AtomicInteger();
  private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor(r -> {
    Thread thread = new Thread(r, "hikvision-fire-callback-worker");
    thread.setDaemon(true);
    return thread;
  });
  private HCNetSdkLibrary.FMSGCallBack_V50 callback;
  private HikvisionThermalSnapshotClient snapshotClient;
  private HikvisionRealtimeThermometryClient thermometryClient;
  private int userId = -1;
  private int alarmHandle = -1;
  private volatile boolean started;

  public HikvisionFireEventSource(
      HikvisionClientConfig config,
      FireSnapshotStore snapshotStore,
      LiveFrameStore liveFrameStore) {
    this(config, snapshotStore, liveFrameStore, HCNetSdkLibrary.load(config.sdkLibraryPath()));
  }

  HikvisionFireEventSource(
      HikvisionClientConfig config,
      FireSnapshotStore snapshotStore,
      LiveFrameStore liveFrameStore,
      HCNetSdkLibrary sdk) {
    this.config = Objects.requireNonNull(config, "config");
    this.snapshotStore = Objects.requireNonNull(snapshotStore, "snapshotStore");
    this.liveFrameStore = Objects.requireNonNull(liveFrameStore, "liveFrameStore");
    this.sdk = Objects.requireNonNull(sdk, "sdk");
  }

  @Override
  public synchronized void start(Consumer<FireDetectionEvent> eventConsumer) {
    if (started) {
      return;
    }
    HikvisionSdkInitializer.configureSdkPaths(sdk, config.sdkLibraryPath());
    if (!sdk.NET_DVR_Init()) {
      throw sdkException("NET_DVR_Init failed");
    }
    sdk.NET_DVR_SetConnectTime(2000, 1);
    sdk.NET_DVR_SetReconnect(10000, true);

    HCNetSdkLibrary.NET_DVR_USER_LOGIN_INFO loginInfo = new HCNetSdkLibrary.NET_DVR_USER_LOGIN_INFO();
    loginInfo.setCredentials(config.host(), config.port(), config.username(), config.password());
    HCNetSdkLibrary.NET_DVR_DEVICEINFO_V40 deviceInfo = new HCNetSdkLibrary.NET_DVR_DEVICEINFO_V40();
    userId = sdk.NET_DVR_Login_V40(loginInfo, deviceInfo);
    if (userId < 0) {
      sdk.NET_DVR_Cleanup();
      throw sdkException("NET_DVR_Login_V40 failed");
    }

    queryThermalCapabilities();
    callback = (lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser) -> {
      if (lCommand == HCNetSdkLibrary.COMM_FIREDETECTION_ALARM) {
        System.out.println("SDK_FIRE_ALARM_IGNORED command=COMM_FIREDETECTION_ALARM, reason=using local thermal image detection");
      }
    };

    if (!sdk.NET_DVR_SetDVRMessageCallBack_V50(0, callback, Pointer.NULL)) {
      close();
      throw sdkException("NET_DVR_SetDVRMessageCallBack_V50 failed");
    }

    HCNetSdkLibrary.NET_DVR_SETUPALARM_PARAM alarmParam = new HCNetSdkLibrary.NET_DVR_SETUPALARM_PARAM();
    alarmParam.write();
    alarmHandle = sdk.NET_DVR_SetupAlarmChan_V41(userId, alarmParam);
    if (alarmHandle < 0) {
      close();
      throw sdkException("NET_DVR_SetupAlarmChan_V41 failed");
    }

    snapshotClient = new HikvisionThermalSnapshotClient(
        sdk,
        userId,
        config.thermalChannel(),
        liveFrameStore,
        detection -> callbackExecutor.execute(() -> eventConsumer.accept(toLocalDetectionEvent(detection))));
    snapshotClient.start();
    thermometryClient = new HikvisionRealtimeThermometryClient(sdk, userId);
    thermometryClient.start(config.thermalChannel(), 0, measurement -> {
    });
    started = true;
  }

  @Override
  public synchronized void close() {
    if (snapshotClient != null) {
      snapshotClient.close();
      snapshotClient = null;
    }
    if (thermometryClient != null) {
      thermometryClient.close();
      thermometryClient = null;
    }
    if (alarmHandle >= 0) {
      sdk.NET_DVR_CloseAlarmChan_V30(alarmHandle);
      alarmHandle = -1;
    }
    if (userId >= 0) {
      sdk.NET_DVR_Logout(userId);
      userId = -1;
    }
    sdk.NET_DVR_Cleanup();
    callbackExecutor.shutdownNow();
    started = false;
  }

  private void queryThermalCapabilities() {
    stdXmlGet("GET /ISAPI/Thermal/capabilities\r\n");
    stdXmlGet("GET /ISAPI/Thermal/channels/" + config.thermalChannel() + "/fireDetection/capabilities\r\n");
  }

  private String stdXmlGet(String requestUrl) {
    byte[] requestBytes = requestUrl.getBytes(StandardCharsets.UTF_8);
    try (Memory request = new Memory(requestBytes.length + 1);
        Memory output = new Memory(64 * 1024);
        Memory status = new Memory(4 * 1024)) {
      request.write(0, requestBytes, 0, requestBytes.length);
      HCNetSdkLibrary.NET_DVR_XML_CONFIG_INPUT input = new HCNetSdkLibrary.NET_DVR_XML_CONFIG_INPUT();
      input.lpRequestUrl = request;
      input.dwRequestUrlLen = requestBytes.length;
      input.lpInBuffer = Pointer.NULL;
      input.dwInBufferSize = 0;
      input.write();

      HCNetSdkLibrary.NET_DVR_XML_CONFIG_OUTPUT out = new HCNetSdkLibrary.NET_DVR_XML_CONFIG_OUTPUT();
      out.lpOutBuffer = output;
      out.dwOutBufferSize = (int) output.size();
      out.lpStatusBuffer = status;
      out.dwStatusSize = (int) status.size();
      out.write();

      if (!sdk.NET_DVR_STDXMLConfig(userId, input, out)) {
        throw sdkException("NET_DVR_STDXMLConfig failed for " + requestUrl.trim());
      }
      return output.getString(0, StandardCharsets.UTF_8.name());
    }
  }

  private FireDetectionEvent toLocalDetectionEvent(ThermalImageFireDetector.DetectedFire detection) {
    String eventId = String.format(Locale.ROOT, "local-frame-fire-%06d", localDetectionSequence.incrementAndGet());
    System.out.println("LOCAL_THERMAL_FIRE_DETECTED eventId=" + eventId
        + ", brightness=" + String.format(Locale.ROOT, "%.1f", detection.brightness())
        + ", pixels=" + detection.pixelCount()
        + ", rect=x=" + String.format(Locale.ROOT, "%.4f", detection.rect().x())
        + ",y=" + String.format(Locale.ROOT, "%.4f", detection.rect().y())
        + ",width=" + String.format(Locale.ROOT, "%.4f", detection.rect().width())
        + ",height=" + String.format(Locale.ROOT, "%.4f", detection.rect().height()));
    return new FireDetectionEvent(
        eventId,
        config.cameraId(),
        config.thermalChannel(),
        config.host(),
        "local_thermal_frame_detection",
        OffsetDateTime.now(),
        detection.brightness(),
        0.0,
        detection.rect(),
        detection.highestPoint(),
        "",
        "LOCAL_THERMAL_FRAME_DETECTION");
  }

  private FireDetectionEvent mapAlarm(Pointer pAlarmInfo) {
    HCNetSdkLibrary.NET_DVR_FIREDETECTION_ALARM alarm =
        new HCNetSdkLibrary.NET_DVR_FIREDETECTION_ALARM(pAlarmInfo);
    int channel = Byte.toUnsignedInt(alarm.struDevInfo.byChannel);
    int ivmsChannel = Byte.toUnsignedInt(alarm.struDevInfo.byIvmsChannel);
    int channelEx = Short.toUnsignedInt(alarm.wDevInfoIvmsChannelEx);
    if (channelEx > 0) {
      ivmsChannel = channelEx;
    }

    HikvisionFireAlarmMapper.RawFireAlarm raw = new HikvisionFireAlarmMapper.RawFireAlarm(
        alarm.struDevInfo.struDevIP.ipv4().isBlank() ? config.host() : alarm.struDevInfo.struDevIP.ipv4(),
        Short.toUnsignedInt(alarm.struDevInfo.wPort),
        channel,
        ivmsChannel,
        alarm.dwAbsTime,
        Short.toUnsignedInt(alarm.wFireMaxTemperature) / 10.0,
        Short.toUnsignedInt(alarm.wTargetDistance),
        alarm.struRect.fX,
        alarm.struRect.fY,
        alarm.struRect.fWidth,
        alarm.struRect.fHeight,
        alarm.struPoint.fX,
        alarm.struPoint.fY,
        "",
        "COMM_FIREDETECTION_ALARM");
    FireDetectionEvent event = HikvisionFireAlarmMapper.toEvent(
        config.cameraId(), raw, ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now()));
    saveThermalSnapshot(event.eventId(), alarm);
    return event;
  }

  private void saveThermalSnapshot(String eventId, HCNetSdkLibrary.NET_DVR_FIREDETECTION_ALARM alarm) {
    if (alarm.dwPicDataLen <= 0 || alarm.pBuffer == null) {
      return;
    }
    byte[] bytes = alarm.pBuffer.getByteArray(0, alarm.dwPicDataLen);
    String contentType = alarm.byPicTransType == 1 ? "text/plain; charset=utf-8" : "image/jpeg";
    snapshotStore.save(eventId, contentType, bytes);
  }

  private IllegalStateException sdkException(String message) {
    return new IllegalStateException(message + ", sdkError=" + sdk.NET_DVR_GetLastError());
  }
}
