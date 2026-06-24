package com.milkfoam.infraredcamera.fire;

import java.util.Arrays;
import java.util.Objects;

public record FireSnapshot(String eventId, String contentType, byte[] bytes) {

  public FireSnapshot {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(contentType, "contentType");
    Objects.requireNonNull(bytes, "bytes");
    bytes = Arrays.copyOf(bytes, bytes.length);
  }

  @Override
  public byte[] bytes() {
    return Arrays.copyOf(bytes, bytes.length);
  }
}
