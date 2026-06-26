package com.milkfoam.infraredcamera.hikvision;

import com.milkfoam.infraredcamera.fire.LiveFrameStore;
import com.milkfoam.infraredcamera.fire.ThermalImageFireDetector;
import com.sun.jna.Memory;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class HikvisionThermalSnapshotClient implements AutoCloseable {

  private static final int DEFAULT_BUFFER_SIZE = 512 * 1024;
  private static final long DEFAULT_INTERVAL_MILLIS = 1000L;
  private static final long MIN_DETECTION_INTERVAL_MILLIS = 5000L;

  private final HCNetSdkLibrary sdk;
  private final int userId;
  private final int channelId;
  private final LiveFrameStore liveFrameStore;
  private final Consumer<ThermalImageFireDetector.DetectedFire> detectionConsumer;
  private final int bufferSize;
  private final int minFireBrightness;
  private ScheduledExecutorService executor;
  private long lastDetectionAtMillis;

  public HikvisionThermalSnapshotClient(
      HCNetSdkLibrary sdk,
      int userId,
      int channelId,
      LiveFrameStore liveFrameStore) {
    this(sdk, userId, channelId, liveFrameStore, detection -> { }, DEFAULT_BUFFER_SIZE, 170);
  }

  public HikvisionThermalSnapshotClient(
      HCNetSdkLibrary sdk,
      int userId,
      int channelId,
      LiveFrameStore liveFrameStore,
      Consumer<ThermalImageFireDetector.DetectedFire> detectionConsumer) {
    this(sdk, userId, channelId, liveFrameStore, detectionConsumer, DEFAULT_BUFFER_SIZE, 170);
  }

  public HikvisionThermalSnapshotClient(
      HCNetSdkLibrary sdk,
      int userId,
      int channelId,
      LiveFrameStore liveFrameStore,
      Consumer<ThermalImageFireDetector.DetectedFire> detectionConsumer,
      int minFireBrightness) {
    this(sdk, userId, channelId, liveFrameStore, detectionConsumer, DEFAULT_BUFFER_SIZE, minFireBrightness);
  }

  HikvisionThermalSnapshotClient(
      HCNetSdkLibrary sdk,
      int userId,
      int channelId,
      LiveFrameStore liveFrameStore,
      Consumer<ThermalImageFireDetector.DetectedFire> detectionConsumer,
      int bufferSize,
      int minFireBrightness) {
    if (userId < 0) {
      throw new IllegalArgumentException("userId must be logged in");
    }
    if (channelId <= 0) {
      throw new IllegalArgumentException("channelId must be positive");
    }
    if (bufferSize <= 0) {
      throw new IllegalArgumentException("bufferSize must be positive");
    }
    if (minFireBrightness < 0 || minFireBrightness > 255) {
      throw new IllegalArgumentException("minFireBrightness must be between 0 and 255");
    }
    this.sdk = Objects.requireNonNull(sdk, "sdk");
    this.userId = userId;
    this.channelId = channelId;
    this.liveFrameStore = Objects.requireNonNull(liveFrameStore, "liveFrameStore");
    this.detectionConsumer = Objects.requireNonNull(detectionConsumer, "detectionConsumer");
    this.bufferSize = bufferSize;
    this.minFireBrightness = minFireBrightness;
  }

  public synchronized void start() {
    if (executor != null) {
      return;
    }
    executor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread thread = new Thread(r, "hikvision-thermal-snapshot-worker");
      thread.setDaemon(true);
      return thread;
    });
    captureSafely();
    executor.scheduleWithFixedDelay(
        this::captureSafely,
        DEFAULT_INTERVAL_MILLIS,
        DEFAULT_INTERVAL_MILLIS,
        TimeUnit.MILLISECONDS);
  }

  boolean captureOnce() {
    HCNetSdkLibrary.NET_DVR_JPEGPARA jpegPara = new HCNetSdkLibrary.NET_DVR_JPEGPARA();
    jpegPara.wPicSize = 0;
    jpegPara.wPicQuality = 0;
    jpegPara.write();
    int[] returnedSize = new int[] {0};
    try (Memory buffer = new Memory(bufferSize)) {
      boolean ok = sdk.NET_DVR_CaptureJPEGPicture_NEW(
          userId,
          channelId,
          jpegPara,
          buffer,
          bufferSize,
          returnedSize);
      if (!ok || returnedSize[0] <= 0) {
        return false;
      }
      byte[] bytes = buffer.getByteArray(0, Math.min(returnedSize[0], bufferSize));
      byte[] frameBytes = Arrays.copyOf(bytes, bytes.length);
      liveFrameStore.save("image/jpeg", frameBytes);
      detectFire(frameBytes);
      return true;
    }
  }

  private void detectFire(byte[] frameBytes) {
    long now = System.currentTimeMillis();
    if (now - lastDetectionAtMillis < MIN_DETECTION_INTERVAL_MILLIS) {
      return;
    }
    Optional<ThermalImageFireDetector.DetectedFire> detected = ThermalImageFireDetector.detect(frameBytes, minFireBrightness);
    if (detected.isEmpty()) {
      return;
    }
    lastDetectionAtMillis = now;
    detectionConsumer.accept(detected.get());
  }

  @Override
  public synchronized void close() {
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
  }

  private void captureSafely() {
    if (!captureOnce()) {
      System.out.println("热成像抓图失败，sdkError=" + sdk.NET_DVR_GetLastError());
    }
  }
}
