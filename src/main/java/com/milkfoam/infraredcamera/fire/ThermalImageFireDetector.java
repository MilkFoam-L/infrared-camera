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
  private static final double TOP_OSD_IGNORE_HEIGHT_RATIO = 0.14;
  private static final double BOTTOM_OSD_IGNORE_TOP_RATIO = 0.82;
  private static final double BOTTOM_OSD_IGNORE_LEFT_RATIO = 0.66;
  private static final double MIN_DISPLAY_LIKE_WIDTH_RATIO = 0.05;
  private static final double MIN_DISPLAY_LIKE_HEIGHT_RATIO = 0.03;
  private static final double MIN_DISPLAY_LIKE_ASPECT_RATIO = 1.6;
  private static final double MAX_DISPLAY_LIKE_ASPECT_RATIO = 4.0;
  private static final double MIN_DISPLAY_LIKE_FILL_RATIO = 0.78;

  private ThermalImageFireDetector() {
  }

  public static Optional<DetectedFire> detect(byte[] imageBytes) {
    return detect(imageBytes, MIN_BRIGHTNESS);
  }

  public static Optional<DetectedFire> detect(byte[] imageBytes, int minBrightness) {
    BufferedImage image;
    try {
      image = ImageIO.read(new ByteArrayInputStream(imageBytes));
    } catch (IOException ex) {
      return Optional.empty();
    }
    if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
      return Optional.empty();
    }
    return detect(image, minBrightness);
  }

  static Optional<DetectedFire> detect(BufferedImage image) {
    return detect(image, MIN_BRIGHTNESS);
  }

  static Optional<DetectedFire> detect(BufferedImage image, int minBrightness) {
    int width = image.getWidth();
    int height = image.getHeight();
    int[] luminance = new int[width * height];
    long sum = 0;
    int validPixelCount = 0;
    int max = 0;
    int maxX = 0;
    int maxY = 0;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int index = y * width + x;
        if (isIgnoredOsdArea(x, y, width, height)) {
          luminance[index] = 0;
          continue;
        }
        int rgb = image.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int value = (int) Math.round(r * 0.299 + g * 0.587 + b * 0.114);
        luminance[index] = value;
        sum += value;
        validPixelCount++;
        if (value > max) {
          max = value;
          maxX = x;
          maxY = y;
        }
      }
    }

    if (validPixelCount == 0) {
      return Optional.empty();
    }

    double average = sum / (double) validPixelCount;
    int threshold = (int) Math.max(minBrightness, average + (max - average) * THRESHOLD_RATIO);
    if (max < minBrightness) {
      return Optional.empty();
    }

    boolean[] visited = new boolean[width * height];
    Component best = null;
    ArrayDeque<Integer> queue = new ArrayDeque<>();

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int start = y * width + x;
        if (isIgnoredOsdArea(x, y, width, height) || visited[start] || luminance[start] < threshold) {
          continue;
        }

        Component component = floodFill(width, height, luminance, visited, queue, start, threshold);
        if (component.pixelCount >= MIN_COMPONENT_PIXELS && (best == null || component.score() > best.score())) {
          best = component;
        }
      }
    }

    if (best == null || isDisplayLikeRegion(best, width, height)) {
      return Optional.empty();
    }

    NormalizedRect rect = new NormalizedRect(
        best.minX / (double) width,
        best.minY / (double) height,
        Math.max(1, best.maxX - best.minX + 1) / (double) width,
        Math.max(1, best.maxY - best.minY + 1) / (double) height);
    NormalizedPoint point = new NormalizedPoint(best.maxXInComponent / (double) width, best.maxYInComponent / (double) height);
    return Optional.of(new DetectedFire(rect, point, best.maxBrightness, best.pixelCount, threshold));
  }

  private static boolean isDisplayLikeRegion(Component component, int imageWidth, int imageHeight) {
    int componentWidth = component.maxX - component.minX + 1;
    int componentHeight = component.maxY - component.minY + 1;
    double widthRatio = componentWidth / (double) imageWidth;
    double heightRatio = componentHeight / (double) imageHeight;
    double aspectRatio = componentWidth / (double) componentHeight;
    double fillRatio = component.pixelCount / (double) (componentWidth * componentHeight);
    return widthRatio >= MIN_DISPLAY_LIKE_WIDTH_RATIO
        && heightRatio >= MIN_DISPLAY_LIKE_HEIGHT_RATIO
        && aspectRatio >= MIN_DISPLAY_LIKE_ASPECT_RATIO
        && aspectRatio <= MAX_DISPLAY_LIKE_ASPECT_RATIO
        && fillRatio >= MIN_DISPLAY_LIKE_FILL_RATIO;
  }

  private static boolean isIgnoredOsdArea(int x, int y, int width, int height) {
    if (y < height * TOP_OSD_IGNORE_HEIGHT_RATIO) {
      return true;
    }
    return y > height * BOTTOM_OSD_IGNORE_TOP_RATIO && x > width * BOTTOM_OSD_IGNORE_LEFT_RATIO;
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
      int pixelCount,
      int brightnessThreshold) {
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
