package com.milkfoam.infraredcamera.fire;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class FireSnapshotStore {

  private final ConcurrentMap<String, FireSnapshot> snapshots = new ConcurrentHashMap<>();

  public void save(String eventId, String contentType, byte[] bytes) {
    snapshots.put(eventId, new FireSnapshot(eventId, contentType, bytes));
  }

  public Optional<FireSnapshot> find(String eventId) {
    return Optional.ofNullable(snapshots.get(eventId));
  }
}
