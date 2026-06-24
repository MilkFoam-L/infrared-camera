package com.milkfoam.infraredcamera.fire;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LiveFrameStoreTest {

  @Test
  void storesLatestLiveFrameWithDefensiveCopies() {
    LiveFrameStore store = new LiveFrameStore();
    byte[] bytes = new byte[] {1, 2, 3, 4};

    store.save("image/jpeg", bytes);
    bytes[0] = 9;

    var frame = store.latest().orElseThrow();
    assertEquals("image/jpeg", frame.contentType());
    assertArrayEquals(new byte[] {1, 2, 3, 4}, frame.bytes());
    assertTrue(frame.updatedAt().toEpochMilli() > 0);

    byte[] read = frame.bytes();
    read[1] = 9;
    assertArrayEquals(new byte[] {1, 2, 3, 4}, store.latest().orElseThrow().bytes());
  }
}
