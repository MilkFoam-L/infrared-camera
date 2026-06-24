package com.milkfoam.infraredcamera.thermal;

import com.milkfoam.infraredcamera.fire.NormalizedPoint;
import java.util.Arrays;

public final class ThermalFrame {

  private final int width;
  private final int height;
  private final double[] temperatures;
  private final double minTemperature;
  private final double maxTemperature;
  private final double averageTemperature;
  private final NormalizedPoint highestPoint;

  public ThermalFrame(int width, int height, double[] temperatures) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("width and height must be positive");
    }
    if (temperatures.length != width * height) {
      throw new IllegalArgumentException("temperature count does not match frame size");
    }
    this.width = width;
    this.height = height;
    this.temperatures = Arrays.copyOf(temperatures, temperatures.length);

    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    double total = 0.0;
    int maxIndex = 0;
    for (int i = 0; i < temperatures.length; i++) {
      double value = temperatures[i];
      min = Math.min(min, value);
      if (value > max) {
        max = value;
        maxIndex = i;
      }
      total += value;
    }
    this.minTemperature = min;
    this.maxTemperature = max;
    this.averageTemperature = total / temperatures.length;
    int maxX = maxIndex % width;
    int maxY = maxIndex / width;
    this.highestPoint = new NormalizedPoint((maxX + 0.5) / width, (maxY + 0.5) / height);
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public double minTemperature() {
    return minTemperature;
  }

  public double maxTemperature() {
    return maxTemperature;
  }

  public double averageTemperature() {
    return averageTemperature;
  }

  public NormalizedPoint highestPoint() {
    return highestPoint;
  }

  public double temperatureAt(int x, int y) {
    if (x < 0 || x >= width || y < 0 || y >= height) {
      throw new IllegalArgumentException("point is outside frame");
    }
    return temperatures[y * width + x];
  }

  public double[] temperatures() {
    return Arrays.copyOf(temperatures, temperatures.length);
  }
}
