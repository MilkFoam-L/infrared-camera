package com.milkfoam.infraredcamera.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class ThermalFrameParserTest {

  @Test
  void parsesFloatTemperatureFrame() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 4 * 4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0);
    buffer.putFloat(10.0f);
    buffer.putFloat(25.5f);
    buffer.putFloat(18.0f);
    buffer.putFloat(41.25f);

    ThermalFrame frame = ThermalFrameParser.parseFloatFrame(2, 2, buffer.array(), 4);

    assertEquals(2, frame.width());
    assertEquals(2, frame.height());
    assertEquals(10.0, frame.minTemperature(), 0.0001);
    assertEquals(41.25, frame.maxTemperature(), 0.0001);
    assertEquals(23.6875, frame.averageTemperature(), 0.0001);
    assertEquals(0.75, frame.highestPoint().x(), 0.0001);
    assertEquals(0.75, frame.highestPoint().y(), 0.0001);
  }

  @Test
  void parsesUnsignedShortTemperatureFrame() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 4 * 2).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0);
    buffer.putShort((short) 29315);
    buffer.putShort((short) 30015);
    buffer.putShort((short) 31015);
    buffer.putShort((short) 32015);

    ThermalFrame frame = ThermalFrameParser.parseUInt16Frame(2, 2, buffer.array(), 4, 100, 0);

    assertEquals(20.0, frame.temperatureAt(0, 0), 0.0001);
    assertEquals(47.0, frame.maxTemperature(), 0.0001);
    assertEquals(0.75, frame.highestPoint().x(), 0.0001);
    assertEquals(0.75, frame.highestPoint().y(), 0.0001);
  }

  @Test
  void rejectsShortFrameData() {
    assertThrows(IllegalArgumentException.class, () -> ThermalFrameParser.parseFloatFrame(2, 2, new byte[8], 4));
  }
}
