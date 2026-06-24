package com.milkfoam.infraredcamera.fire;

public record FireDetectionStatus(
    boolean fireDetected,
    String cameraId,
    int channelId,
    double maxTemperature,
    NormalizedRect rect,
    NormalizedPoint highestPoint) {

  public static FireDetectionStatus from(FireDetectionEvent event) {
    return new FireDetectionStatus(
        true,
        event.cameraId(),
        event.channelId(),
        event.maxTemperature(),
        event.rect(),
        event.highestPoint());
  }

  public static FireDetectionStatus noFire(String cameraId, int channelId) {
    return new FireDetectionStatus(false, cameraId, channelId, 0.0, null, null);
  }
}
