package com.milkfoam.infraredcamera.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.milkfoam.infraredcamera.fire.FireSnapshotStore;
import com.milkfoam.infraredcamera.fire.LiveFrameStore;
import com.milkfoam.infraredcamera.runtime.FireEventBus;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class FireDetectionHttpServerTest {

  @Test
  void runtimeConfigReturnsCustomFireMaskSwitch() throws Exception {
    FireDetectionHttpServer server = new FireDetectionHttpServer(
        new FireEventBus(),
        new FireSnapshotStore(),
        new LiveFrameStore(),
        0,
        false);
    server.start();
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("http://127.0.0.1:" + server.port() + "/api/runtime-config"))
          .GET()
          .build();

      HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

      assertEquals(200, response.statusCode());
      assertTrue(response.body().contains("\"customFireMaskEnabled\":false"));
    } finally {
      server.close();
    }
  }
}
