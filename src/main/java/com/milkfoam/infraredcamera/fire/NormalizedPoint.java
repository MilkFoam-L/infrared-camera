package com.milkfoam.infraredcamera.fire;

public record NormalizedPoint(double x, double y) {

  public NormalizedPoint {
    requireInRange("x", x);
    requireInRange("y", y);
  }

  private static void requireInRange(String name, double value) {
    if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException(name + " must be between 0.0 and 1.0");
    }
  }
}
