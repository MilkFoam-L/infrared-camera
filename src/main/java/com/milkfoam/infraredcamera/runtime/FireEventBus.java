package com.milkfoam.infraredcamera.runtime;

import com.milkfoam.infraredcamera.fire.FireDetectionEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class FireEventBus {

  private final AtomicReference<FireDetectionEvent> latestEvent = new AtomicReference<>();
  private final Set<OutputStream> subscribers = ConcurrentHashMap.newKeySet();

  public void publish(FireDetectionEvent event) {
    latestEvent.set(event);
    byte[] payload = ("event: fire\n" + "data: " + event.toJson() + "\n\n").getBytes(StandardCharsets.UTF_8);
    for (OutputStream subscriber : subscribers) {
      try {
        subscriber.write(payload);
        subscriber.flush();
      } catch (IOException ex) {
        subscribers.remove(subscriber);
        closeQuietly(subscriber);
      }
    }
  }

  public Optional<FireDetectionEvent> latestEvent() {
    return Optional.ofNullable(latestEvent.get());
  }

  public void subscribe(OutputStream outputStream) throws IOException {
    subscribers.add(outputStream);
    outputStream.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
    FireDetectionEvent latest = latestEvent.get();
    if (latest != null) {
      outputStream.write(("event: fire\n" + "data: " + latest.toJson() + "\n\n").getBytes(StandardCharsets.UTF_8));
    }
    outputStream.flush();
  }

  public void unsubscribe(OutputStream outputStream) {
    subscribers.remove(outputStream);
    closeQuietly(outputStream);
  }

  public int subscriberCount() {
    return subscribers.size();
  }

  private static void closeQuietly(OutputStream outputStream) {
    try {
      outputStream.close();
    } catch (IOException ignored) {
      // ignored intentionally
    }
  }
}
