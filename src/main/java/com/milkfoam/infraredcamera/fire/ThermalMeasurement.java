package com.milkfoam.infraredcamera.fire;

public record ThermalMeasurement(
    double minTemperature,
    double maxTemperature,
    double averageTemperature,
    NormalizedPoint highestPoint) {
}
