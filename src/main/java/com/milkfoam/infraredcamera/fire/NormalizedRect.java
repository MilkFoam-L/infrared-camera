package com.milkfoam.infraredcamera.fire;

public record NormalizedRect(double x, double y, double width, double height) {

  public NormalizedRect {
    requireInRange("x", x);
    requireInRange("y", y);
    requirePositive("width", width);
    requirePositive("height", height);
    if (x + width > 1.0) {
      throw new IllegalArgumentException("rect x + width must be <= 1.0");
    }
    if (y + height > 1.0) {
      throw new IllegalArgumentException("rect y + height must be <= 1.0");
    }
  }

  public double centerX() {
    return x + width / 2.0;
  }

  public double centerY() {
    return y + height / 2.0;
  }

  private static void requireInRange(String name, double value) {
    if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must be between 0.0 and 1.0");
    }
  }

  private static void requirePositive(String name, double value) {
    if (Double.isNaN(value) || value <= 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must be greater than 0.0 and <= 1.0");
    }
  }
}
