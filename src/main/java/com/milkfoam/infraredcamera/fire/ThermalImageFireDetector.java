package com.milkfoam.infraredcamera.fire;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Optional;
import javax.imageio.ImageIO;

public final class ThermalImageFireDetector {

  private static final int MIN_COMPONENT_PIXELS = 24;
  private static final int MIN_BRIGHTNESS = 170;
  private static final double THRESHOLD_RATIO = 0.72;

  private ThermalImageFireDetector() {
  }

  public static Optional<DetectedFire> detect(byte[] imageBytes) {
    BufferedImage image;
    try {
      image = ImageIO.read(new ByteArrayInputStream(imageBytes));
    } catch (IOException ex) {
      return Optional.empty();
    }
    if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
      return Optional.empty();
    }
    return detect(image);
  }

  static Optional<DetectedFire> detect(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    int[] luminance = new int[width * height];
    long sum = 0;
    int max = 0;
    int maxX = 0;
    int maxY = 0;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int rgb = image.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int value = (int) Math.round(r * 0.299 + g * 0.587 + b * 0.114);
        int index = y * width + x;
        luminance[index] = value;
        sum += value;
        if (value > max) {
          max = value;
          maxX = x;
          maxY = y;
        }
      }
    }

    double average = sum / (double) luminance.length;
    int threshold = (int) Math.max(MIN_BRIGHTNESS, average + (max - average) * THRESHOLD_RATIO);
    if (max < MIN_BRIGHTNESS) {
      return Optional.empty();
    }

    boolean[] visited = new boolean[width * height];
    Component best = null;
    ArrayDeque<Integer> queue = new ArrayDeque<>();

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int start = y * width + x;
        if (visited[start] || luminance[start] < threshold) {
          continue;
        }

        Component component = floodFill(width, height, luminance, visited, queue, start, threshold);
        if (component.pixelCount >= MIN_COMPONENT_PIXELS && (best == null || component.score() > best.score())) {
          best = component;
        }
      }
    }

    if (best == null) {
      return Optional.empty();
    }

    NormalizedRect rect = new NormalizedRect(
        best.minX / (double) width,
        best.minY / (double) height,
        Math.max(1, best.maxX - best.minX + 1) / (double) width,
        Math.max(1, best.maxY - best.minY + 1) / (double) height);
    NormalizedPoint point = new NormalizedPoint(best.maxXInComponent / (double) width, best.maxYInComponent / (double) height);
    return Optional.of(new DetectedFire(rect, point, best.maxBrightness, best.pixelCount));
  }

  private static Component floodFill(
      int width,
      int height,
      int[] luminance,
      boolean[] visited,
      ArrayDeque<Integer> queue,
      int start,
      int threshold) {
    Component component = new Component(start % width, start / width, luminance[start]);
    visited[start] = true;
    queue.clear();
    queue.add(start);

    while (!queue.isEmpty()) {
      int index = queue.removeFirst();
      int x = index % width;
      int y = index / width;
      component.add(x, y, luminance[index]);
      addNeighbor(width, height, luminance, visited, queue, x + 1, y, threshold);
      addNeighbor(width, height, luminance, visited, queue, x - 1, y, threshold);
      addNeighbor(width, height, luminance, visited, queue, x, y + 1, threshold);
      addNeighbor(width, height, luminance, visited, queue, x, y - 1, threshold);
    }

    return component;
  }

  private static void addNeighbor(
      int width,
      int height,
      int[] luminance,
      boolean[] visited,
      ArrayDeque<Integer> queue,
      int x,
      int y,
      int threshold) {
    if (x < 0 || y < 0 || x >= width || y >= height) {
      return;
    }
    int index = y * width + x;
    if (visited[index] || luminance[index] < threshold) {
      return;
    }
    visited[index] = true;
    queue.add(index);
  }

  public record DetectedFire(
      NormalizedRect rect,
      NormalizedPoint highestPoint,
      double brightness,
      int pixelCount) {
  }

  private static final class Component {
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;
    private int maxXInComponent;
    private int maxYInComponent;
    private int maxBrightness;
    private int pixelCount;
    private long brightnessSum;

    private Component(int x, int y, int brightness) {
      this.minX = x;
      this.minY = y;
      this.maxX = x;
      this.maxY = y;
      this.maxXInComponent = x;
      this.maxYInComponent = y;
      this.maxBrightness = brightness;
    }

    private void add(int x, int y, int brightness) {
      minX = Math.min(minX, x);
      minY = Math.min(minY, y);
      maxX = Math.max(maxX, x);
      maxY = Math.max(maxY, y);
      if (brightness > maxBrightness) {
        maxBrightness = brightness;
        maxXInComponent = x;
        maxYInComponent = y;
      }
      pixelCount++;
      brightnessSum += brightness;
    }

    private double score() {
      return brightnessSum / (double) pixelCount + Math.log(pixelCount + 1) * 8.0;
    }
  }
}
