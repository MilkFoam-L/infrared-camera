package com.milkfoam.infraredcamera.thingsboard;

import java.net.URI;

public record ThingsBoardConfig(String host, String deviceToken) {

  public boolean enabled() {
    return host != null && !host.isBlank() && deviceToken != null && !deviceToken.isBlank();
  }

  public URI telemetryUri() {
    if (!enabled()) {
      throw new IllegalStateException("ThingsBoard telemetry is disabled");
    }
    String normalizedHost = host.trim();
    if (!normalizedHost.startsWith("http://") && !normalizedHost.startsWith("https://")) {
      normalizedHost = "http://" + normalizedHost;
    }
    String normalizedToken = deviceToken.trim();
    return URI.create(normalizedHost + "/api/v1/" + normalizedToken + "/telemetry");
  }
}
