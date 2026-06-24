package com.milkfoam.infraredcamera.hikvision;

public record HikvisionClientConfig(
    String cameraId,
    String host,
    int port,
    String username,
    String password,
    int thermalChannel,
    String sdkLibraryPath) {

  public HikvisionClientConfig {
    if (cameraId == null || cameraId.isBlank()) {
      cameraId = "cam-001";
    }
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("host is required in hikvision mode");
    }
    if (port <= 0) {
      throw new IllegalArgumentException("port must be positive");
    }
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("username is required in hikvision mode");
    }
    if (password == null || password.isBlank()) {
      throw new IllegalArgumentException("password is required in hikvision mode");
    }
    if (thermalChannel <= 0) {
      throw new IllegalArgumentException("thermalChannel must be positive");
    }
  }
}
