package com.milkfoam.infraredcamera.fire;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

public final class LiveFrame {

  private final String contentType;
  private final byte[] bytes;
  private final Instant updatedAt;

  public LiveFrame(String contentType, byte[] bytes, Instant updatedAt) {
    this.contentType = Objects.requireNonNull(contentType, "contentType");
    this.bytes = Arrays.copyOf(Objects.requireNonNull(bytes, "bytes"), bytes.length);
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
  }

  public String contentType() {
    return contentType;
  }

  public byte[] bytes() {
    return Arrays.copyOf(bytes, bytes.length);
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
