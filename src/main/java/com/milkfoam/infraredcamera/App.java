package com.milkfoam.infraredcamera;

import com.milkfoam.infraredcamera.fire.FireDetectionEvent;
import com.milkfoam.infraredcamera.fire.FireSnapshotStore;
import com.milkfoam.infraredcamera.fire.LiveFrameStore;
import com.milkfoam.infraredcamera.fire.NormalizedPoint;
import com.milkfoam.infraredcamera.fire.NormalizedRect;
import com.milkfoam.infraredcamera.hikvision.HikvisionClientConfig;
import com.milkfoam.infraredcamera.hikvision.HikvisionFireEventSource;
import com.milkfoam.infraredcamera.runtime.FireEventBus;
import com.milkfoam.infraredcamera.runtime.FireEventSource;
import com.milkfoam.infraredcamera.runtime.MockFireEventSource;
import com.milkfoam.infraredcamera.thingsboard.ThingsBoardConfig;
import com.milkfoam.infraredcamera.thingsboard.ThingsBoardTelemetryClient;
import com.milkfoam.infraredcamera.web.FireDetectionHttpServer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class App {

  private App() {
  }

  public static void main(String[] args) throws Exception {
    Map<String, String> options = parseArgs(args);
    String mode = options.getOrDefault("mode", "mock");
    int httpPort = Integer.parseInt(options.getOrDefault("http-port", "8765"));
    String cameraId = options.getOrDefault("camera-id", "cam-001");
    int channel = Integer.parseInt(options.getOrDefault("channel", "2"));

    FireEventBus eventBus = new FireEventBus();
    FireSnapshotStore snapshotStore = new FireSnapshotStore();
    LiveFrameStore liveFrameStore = new LiveFrameStore();
    FireEventSource source = createSource(mode, options, cameraId, channel, snapshotStore, liveFrameStore);
    FireDetectionHttpServer httpServer = new FireDetectionHttpServer(eventBus, snapshotStore, liveFrameStore, httpPort);
    ThingsBoardTelemetryClient thingsBoardClient = new ThingsBoardTelemetryClient(new ThingsBoardConfig(
        options.get("thingsboard-host"),
        options.get("thingsboard-token")));
    AtomicReference<OffsetDateTime> latestFireTime = new AtomicReference<>();
    ScheduledExecutorService detectionLogger = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread thread = new Thread(r, "fire-detection-status-logger");
      thread.setDaemon(true);
      return thread;
    });

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      detectionLogger.shutdownNow();
      source.close();
      thingsBoardClient.close();
      httpServer.close();
    }, "infrared-camera-shutdown"));

    httpServer.start();
    source.start(event -> {
      latestFireTime.set(OffsetDateTime.now());
      System.out.println("检测到火点：" + eventSummary(event));
      eventBus.publish(event);
      thingsBoardClient.sendFireDetected(event);
    });
    detectionLogger.scheduleAtFixedRate(() -> logDetectionStatus(latestFireTime), 0, 5, TimeUnit.SECONDS);
    System.out.println("热成像火点检测服务已启动");
    System.out.println("模式: " + mode);
    System.out.println("访问: http://127.0.0.1:" + httpServer.port() + "/");
    new CountDownLatch(1).await();
  }

  private static void logDetectionStatus(AtomicReference<OffsetDateTime> latestFireTime) {
    OffsetDateTime now = OffsetDateTime.now();
    Optional<OffsetDateTime> fireTime = Optional.ofNullable(latestFireTime.get());
    if (fireTime.isEmpty()) {
      System.out.println("火点检测状态：未检测到火点，最近火点=无，检测时间="
          + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
      return;
    }

    long secondsSinceLastFire = Duration.between(fireTime.get(), now).toSeconds();
    String status = secondsSinceLastFire <= 10 ? "最近检测到火点" : "未检测到新火点";
    System.out.println("火点检测状态：" + status
        + "，最近火点=" + fireTime.get().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        + "，距离上次火点秒数=" + secondsSinceLastFire
        + "，检测时间=" + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
  }

  private static String eventSummary(FireDetectionEvent event) {
    return "事件ID=" + event.eventId()
        + "，摄像头=" + event.cameraId()
        + "，通道=" + event.channelId()
        + "，最高亮度/温度=" + String.format(Locale.ROOT, "%.1f", event.maxTemperature())
        + "，距离=" + String.format(Locale.ROOT, "%.2f", event.targetDistance())
        + "，火点范围=" + rectSummary(event.rect())
        + "，最高点=" + pointSummary(event.highestPoint());
  }

  private static String rectSummary(NormalizedRect rect) {
    return String.format(Locale.ROOT, "x=%.4f，y=%.4f，宽=%.4f，高=%.4f",
        rect.x(), rect.y(), rect.width(), rect.height());
  }

  private static String pointSummary(NormalizedPoint point) {
    return String.format(Locale.ROOT, "x=%.4f，y=%.4f", point.x(), point.y());
  }

  private static FireEventSource createSource(
      String mode,
      Map<String, String> options,
      String cameraId,
      int channel,
      FireSnapshotStore snapshotStore,
      LiveFrameStore liveFrameStore) {
    if ("hikvision".equalsIgnoreCase(mode)) {
      HikvisionClientConfig config = new HikvisionClientConfig(
          cameraId,
          required(options, "host"),
          Integer.parseInt(options.getOrDefault("port", "8000")),
          required(options, "username"),
          required(options, "password"),
          channel,
          options.get("sdk-lib"));
      return new HikvisionFireEventSource(config, snapshotStore, liveFrameStore);
    }
    if (!"mock".equalsIgnoreCase(mode)) {
      throw new IllegalArgumentException("unsupported mode: " + mode + ", expected mock or hikvision");
    }
    String deviceIp = options.getOrDefault("device-ip", "192.168.1.64");
    return new MockFireEventSource(cameraId, channel, deviceIp);
  }

  private static String required(Map<String, String> options, String name) {
    String value = options.get(name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("--" + name + " is required");
    }
    return value;
  }

  private static Map<String, String> parseArgs(String[] args) {
    Map<String, String> options = new HashMap<>();
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (!arg.startsWith("--")) {
        throw new IllegalArgumentException("invalid argument: " + arg);
      }
      String key;
      String value;
      int equalsIndex = arg.indexOf('=');
      if (equalsIndex > 0) {
        key = arg.substring(2, equalsIndex);
        value = arg.substring(equalsIndex + 1);
      } else {
        key = arg.substring(2);
        if (i + 1 >= args.length) {
          throw new IllegalArgumentException("missing value for " + arg);
        }
        value = args[++i];
      }
      options.put(key, value);
    }
    return options;
  }
}
