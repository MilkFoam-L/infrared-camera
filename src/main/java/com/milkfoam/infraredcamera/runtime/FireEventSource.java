package com.milkfoam.infraredcamera.runtime;

import com.milkfoam.infraredcamera.fire.FireDetectionEvent;
import java.util.function.Consumer;

public interface FireEventSource extends AutoCloseable {

  void start(Consumer<FireDetectionEvent> eventConsumer);

  @Override
  void close();
}
