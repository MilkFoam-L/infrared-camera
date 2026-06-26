package com.milkfoam.infraredcamera.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ThermalImageFireDetectorTest {

  @Test
  void detectsBrightFireRegionFromThermalImage() {
    BufferedImage image = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
    fill(image, new Color(35, 35, 35));
    for (int y = 30; y < 45; y++) {
      for (int x = 40; x < 58; x++) {
        image.setRGB(x, y, Color.WHITE.getRGB());
      }
    }

    Optional<ThermalImageFireDetector.DetectedFire> detected = ThermalImageFireDetector.detect(image);

    assertTrue(detected.isPresent());
    ThermalImageFireDetector.DetectedFire fire = detected.orElseThrow();
    assertEquals(0.40, fire.rect().x(), 0.02);
    assertEquals(0.375, fire.rect().y(), 0.02);
    assertEquals(0.18, fire.rect().width(), 0.03);
    assertEquals(0.1875, fire.rect().height(), 0.03);
    assertTrue(fire.pixelCount() >= 24);
    assertTrue(fire.brightnessThreshold() >= 170);
  }

  @Test
  void ignoresWarmHumanLikeRegionBelowConfiguredThreshold() {
    BufferedImage image = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
    fill(image, new Color(35, 35, 35));
    Color warmHumanLike = new Color(220, 220, 220);
    for (int y = 28; y < 52; y++) {
      for (int x = 12; x < 34; x++) {
        image.setRGB(x, y, warmHumanLike.getRGB());
      }
    }

    assertTrue(ThermalImageFireDetector.detect(image, 245).isEmpty());
  }

  @Test
  void ignoresBrightCameraOverlayTextAreas() {
    BufferedImage image = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
    fill(image, new Color(35, 35, 35));
    for (int y = 2; y < 8; y++) {
      for (int x = 5; x < 70; x++) {
        image.setRGB(x, y, Color.WHITE.getRGB());
      }
    }
    for (int y = 68; y < 75; y++) {
      for (int x = 70; x < 96; x++) {
        image.setRGB(x, y, Color.WHITE.getRGB());
      }
    }

    assertTrue(ThermalImageFireDetector.detect(image).isEmpty());
  }

  @Test
  void ignoresDisplayLikeBrightRectangle() {
    BufferedImage image = new BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB);
    fill(image, new Color(35, 35, 35));
    for (int y = 32; y < 57; y++) {
      for (int x = 25; x < 85; x++) {
        image.setRGB(x, y, Color.WHITE.getRGB());
      }
    }

    assertTrue(ThermalImageFireDetector.detect(image, 245).isEmpty());
  }

  @Test
  void ignoresDarkImageWithoutFireRegion() {
    BufferedImage image = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
    fill(image, new Color(35, 35, 35));

    assertTrue(ThermalImageFireDetector.detect(image).isEmpty());
  }

  private static void fill(BufferedImage image, Color color) {
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        image.setRGB(x, y, color.getRGB());
      }
    }
  }
}
