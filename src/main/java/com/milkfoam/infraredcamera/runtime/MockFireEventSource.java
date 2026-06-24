package com.milkfoam.infraredcamera.runtime;

import com.milkfoam.infraredcamera.fire.FireDetectionEvent;
import com.milkfoam.infraredcamera.fire.NormalizedPoint;
import com.milkfoam.infraredcamera.fire.NormalizedRect;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class MockFireEventSource implements FireEventSource {

  private final String cameraId;
  private final int channelId;
  private final String deviceIp;
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread thread = new Thread(r, "mock-fire-event-source");
    thread.setDaemon(true);
    return thread;
  });
  private final AtomicInteger sequence = new AtomicInteger();

  public MockFireEventSource(String cameraId, int channelId, String deviceIp) {
    this.cameraId = cameraId;
    this.channelId = channelId;
    this.deviceIp = deviceIp;
  }

  @Override
  public void start(Consumer<FireDetectionEvent> eventConsumer) {
    executor.scheduleAtFixedRate(() -> eventConsumer.accept(nextEvent()), 0, 1200, TimeUnit.MILLISECONDS);
  }

  @Override
  public void close() {
    executor.shutdownNow();
  }

  private FireDetectionEvent nextEvent() {
    int index = sequence.incrementAndGet();
    double x = 0.12 + (index % 7) * 0.09;
    double y = 0.18 + (index % 5) * 0.08;
    double width = 0.12;
    double height = 0.16;
    double pointX = x + width * 0.52;
    double pointY = y + height * 0.42;
    double temperature = 72.0 + (index % 12) * 2.7;
    double distance = 8.5 + (index % 6) * 1.2;
    String eventId = String.format(Locale.ROOT, "fire-demo-%04d", index);

    return new FireDetectionEvent(
        eventId,
        cameraId,
        channelId,
        deviceIp,
        "fire_detection",
        OffsetDateTime.now(),
        temperature,
        distance,
        new NormalizedRect(x, y, width, height),
        new NormalizedPoint(pointX, pointY),
        "/api/fire-events/" + eventId + "/snapshot",
        "COMM_FIREDETECTION_ALARM");
  }
}
