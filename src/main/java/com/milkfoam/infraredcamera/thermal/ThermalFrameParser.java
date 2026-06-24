package com.milkfoam.infraredcamera.thermal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ThermalFrameParser {

  private ThermalFrameParser() {
  }

  public static ThermalFrame parseFloatFrame(int width, int height, byte[] data, int offset) {
    int pointCount = checkedPointCount(width, height);
    int required = offset + pointCount * Float.BYTES;
    if (offset < 0 || data.length < required) {
      throw new IllegalArgumentException("not enough float temperature data");
    }

    ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    double[] temperatures = new double[pointCount];
    int index = offset;
    for (int i = 0; i < pointCount; i++) {
      temperatures[i] = clampTemperature(buffer.getFloat(index));
      index += Float.BYTES;
    }
    return new ThermalFrame(width, height, temperatures);
  }

  public static ThermalFrame parseUInt16Frame(
      int width,
      int height,
      byte[] data,
      int offset,
      int scale,
      int temperatureOffset) {
    int pointCount = checkedPointCount(width, height);
    int required = offset + pointCount * Short.BYTES;
    if (offset < 0 || data.length < required) {
      throw new IllegalArgumentException("not enough uint16 temperature data");
    }
    if (scale <= 0) {
      throw new IllegalArgumentException("scale must be positive");
    }

    ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    double[] temperatures = new double[pointCount];
    int index = offset;
    for (int i = 0; i < pointCount; i++) {
      int raw = Short.toUnsignedInt(buffer.getShort(index));
      temperatures[i] = raw / (double) scale + temperatureOffset - 273.15;
      index += Short.BYTES;
    }
    return new ThermalFrame(width, height, temperatures);
  }

  private static int checkedPointCount(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("width and height must be positive");
    }
    return Math.multiplyExact(width, height);
  }

  private static double clampTemperature(double value) {
    if (value > 9999.0) {
      return 9999.0;
    }
    if (value < -9999.0) {
      return -9999.0;
    }
    return value;
  }
}
