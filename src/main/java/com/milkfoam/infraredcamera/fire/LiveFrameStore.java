package com.milkfoam.infraredcamera.fire;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class LiveFrameStore {

  private final AtomicReference<LiveFrame> latest = new AtomicReference<>();

  public void save(String contentType, byte[] bytes) {
    latest.set(new LiveFrame(contentType, bytes, Instant.now()));
  }

  public Optional<LiveFrame> latest() {
    return Optional.ofNullable(latest.get());
  }
}
