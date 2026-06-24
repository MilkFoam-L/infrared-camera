package com.milkfoam.infraredcamera.fire;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FireSnapshotStoreTest {

  @Test
  void storesAndReadsSnapshotByEventId() {
    FireSnapshotStore store = new FireSnapshotStore();
    byte[] bytes = new byte[] {1, 2, 3, 4};

    store.save("fire-1", "image/jpeg", bytes);

    var snapshot = store.find("fire-1");
    assertTrue(snapshot.isPresent());
    assertEquals("image/jpeg", snapshot.get().contentType());
    assertArrayEquals(bytes, snapshot.get().bytes());
  }

  @Test
  void snapshotBytesAreDefensivelyCopied() {
    FireSnapshotStore store = new FireSnapshotStore();
    byte[] bytes = new byte[] {1, 2, 3, 4};

    store.save("fire-1", "image/jpeg", bytes);
    bytes[0] = 9;

    var snapshot = store.find("fire-1").orElseThrow();
    assertArrayEquals(new byte[] {1, 2, 3, 4}, snapshot.bytes());

    byte[] read = snapshot.bytes();
    read[1] = 9;
    assertArrayEquals(new byte[] {1, 2, 3, 4}, store.find("fire-1").orElseThrow().bytes());
  }
}
