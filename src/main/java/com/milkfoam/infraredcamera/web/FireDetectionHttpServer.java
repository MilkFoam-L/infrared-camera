package com.milkfoam.infraredcamera.web;

import com.milkfoam.infraredcamera.fire.FireSnapshotStore;
import com.milkfoam.infraredcamera.fire.LiveFrameStore;
import com.milkfoam.infraredcamera.runtime.FireEventBus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public final class FireDetectionHttpServer implements AutoCloseable {

  private static final Map<String, String> RESOURCE_ROUTES = Map.of(
      "/", "web/index.html",
      "/app.js", "web/app.js",
      "/style.css", "web/style.css");

  private final FireEventBus eventBus;
  private final FireSnapshotStore snapshotStore;
  private final LiveFrameStore liveFrameStore;
  private final HttpServer server;

  public FireDetectionHttpServer(
      FireEventBus eventBus,
      FireSnapshotStore snapshotStore,
      LiveFrameStore liveFrameStore,
      int port) throws IOException {
    this.eventBus = eventBus;
    this.snapshotStore = snapshotStore;
    this.liveFrameStore = liveFrameStore;
    this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    this.server.setExecutor(Executors.newCachedThreadPool());
    this.server.createContext("/", this::handleStatic);
    this.server.createContext("/api/live-frame", this::handleLiveFrame);
    this.server.createContext("/api/fire-events/latest", this::handleLatest);
    this.server.createContext("/api/fire-events/stream", this::handleStream);
    this.server.createContext("/api/fire-events", this::handleSnapshot);
  }

  public void start() {
    server.start();
  }

  public int port() {
    return server.getAddress().getPort();
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private void handleStatic(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    String resource = RESOURCE_ROUTES.get(path);
    if (resource == null) {
      sendText(exchange, 404, "text/plain; charset=utf-8", "Not found");
      return;
    }
    byte[] bytes = readResource(resource);
    String contentType = switch (resource.substring(resource.lastIndexOf('.') + 1)) {
      case "html" -> "text/html; charset=utf-8";
      case "js" -> "text/javascript; charset=utf-8";
      case "css" -> "text/css; charset=utf-8";
      default -> "application/octet-stream";
    };
    sendBytes(exchange, 200, contentType, bytes);
  }

  private void handleLatest(HttpExchange exchange) throws IOException {
    String json = eventBus.latestEvent()
        .map(event -> "{\"fireDetected\":true,\"event\":" + event.toJson() + "}")
        .orElse("{\"fireDetected\":false}");
    sendText(exchange, 200, "application/json; charset=utf-8", json);
  }

  private void handleLiveFrame(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    var frame = liveFrameStore.latest();
    if (frame.isPresent()) {
      sendBytes(exchange, 200, frame.get().contentType(), frame.get().bytes());
      return;
    }
    String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1280\" height=\"720\" viewBox=\"0 0 1280 720\">"
        + "<rect width=\"1280\" height=\"720\" fill=\"#101820\"/>"
        + "<text x=\"640\" y=\"360\" text-anchor=\"middle\" fill=\"#9fb3c8\" font-family=\"Arial\" font-size=\"34\">等待热成像抓图</text>"
        + "</svg>";
    sendText(exchange, 200, "image/svg+xml; charset=utf-8", svg);
  }

  private void handleStream(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
    exchange.getResponseHeaders().set("Connection", "keep-alive");
    exchange.sendResponseHeaders(200, 0);
    OutputStream body = exchange.getResponseBody();
    try {
      eventBus.subscribe(body);
      new CountDownLatch(1).await();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } finally {
      eventBus.unsubscribe(body);
      exchange.close();
    }
  }

  private void handleSnapshot(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    if (!path.endsWith("/snapshot")) {
      sendText(exchange, 404, "text/plain; charset=utf-8", "Not found");
      return;
    }
    String eventId = eventIdFromSnapshotPath(path);
    var snapshot = snapshotStore.find(eventId);
    if (snapshot.isPresent()) {
      sendBytes(exchange, 200, snapshot.get().contentType(), snapshot.get().bytes());
      return;
    }
    String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"640\" height=\"360\" viewBox=\"0 0 640 360\">"
        + "<defs><linearGradient id=\"g\" x1=\"0\" x2=\"1\"><stop stop-color=\"#211\"/><stop offset=\"1\" stop-color=\"#431\"/></linearGradient></defs>"
        + "<rect width=\"640\" height=\"360\" fill=\"url(#g)\"/>"
        + "<circle cx=\"330\" cy=\"170\" r=\"46\" fill=\"#ff5a1f\" opacity=\"0.85\"/>"
        + "<circle cx=\"330\" cy=\"170\" r=\"18\" fill=\"#ffe066\"/>"
        + "<text x=\"24\" y=\"332\" fill=\"#fff\" font-family=\"Arial\" font-size=\"20\">fire snapshot placeholder</text>"
        + "</svg>";
    sendText(exchange, 200, "image/svg+xml; charset=utf-8", svg);
  }

  private static String eventIdFromSnapshotPath(String path) {
    String prefix = "/api/fire-events/";
    String suffix = "/snapshot";
    if (!path.startsWith(prefix) || !path.endsWith(suffix) || path.length() <= prefix.length() + suffix.length()) {
      return "";
    }
    return path.substring(prefix.length(), path.length() - suffix.length());
  }

  private static byte[] readResource(String resource) throws IOException {
    try (var inputStream = FireDetectionHttpServer.class.getClassLoader().getResourceAsStream(resource)) {
      if (inputStream == null) {
        throw new IOException("resource not found: " + resource);
      }
      return inputStream.readAllBytes();
    }
  }

  private static void sendText(HttpExchange exchange, int status, String contentType, String text) throws IOException {
    sendBytes(exchange, status, contentType, text.getBytes(StandardCharsets.UTF_8));
  }

  private static void sendBytes(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream body = exchange.getResponseBody()) {
      body.write(bytes);
    }
  }
}
