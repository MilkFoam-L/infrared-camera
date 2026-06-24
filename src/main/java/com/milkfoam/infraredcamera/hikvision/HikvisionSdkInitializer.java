package com.milkfoam.infraredcamera.hikvision;

import com.sun.jna.Memory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

final class HikvisionSdkInitializer {

  private HikvisionSdkInitializer() {
  }

  static void configureSdkPaths(HCNetSdkLibrary sdk, String sdkLibraryPath) {
    Objects.requireNonNull(sdk, "sdk");
    if (sdkLibraryPath == null || sdkLibraryPath.isBlank()) {
      return;
    }
    Path sdkFile = Path.of(sdkLibraryPath);
    Path sdkDir = sdkFile.getParent();
    if (sdkDir == null) {
      return;
    }

    configureLocalSdkPath(sdk, sdkDir);
    configureCheckModuleCom(sdk);
    configureOpenSslPath(sdk, HCNetSdkLibrary.NET_SDK_INIT_CFG_LIBEAY_PATH, sdkDir.resolve("libcrypto-1_1-x64.dll"));
    configureOpenSslPath(sdk, HCNetSdkLibrary.NET_SDK_INIT_CFG_SSLEAY_PATH, sdkDir.resolve("libssl-1_1-x64.dll"));
  }

  private static void configureLocalSdkPath(HCNetSdkLibrary sdk, Path sdkDir) {
    HCNetSdkLibrary.NET_DVR_LOCAL_SDK_PATH sdkPath = new HCNetSdkLibrary.NET_DVR_LOCAL_SDK_PATH();
    sdkPath.setPath(sdkDir.toAbsolutePath().toString() + "\\");
    if (!sdk.NET_DVR_SetSDKInitCfg(HCNetSdkLibrary.NET_SDK_INIT_CFG_SDK_PATH, sdkPath.getPointer())) {
      System.out.println("设置海康 SDK 路径失败，sdkError=" + sdk.NET_DVR_GetLastError());
    }
  }

  private static void configureCheckModuleCom(HCNetSdkLibrary sdk) {
    HCNetSdkLibrary.NET_DVR_INIT_CHECK_MODULE_COM check = new HCNetSdkLibrary.NET_DVR_INIT_CHECK_MODULE_COM();
    check.byEnable = 1;
    check.write();
    if (!sdk.NET_DVR_SetSDKInitCfg(HCNetSdkLibrary.NET_SDK_INIT_CFG_TYPE_CHECK_MODULE_COM, check.getPointer())) {
      System.out.println("设置海康组件检查失败，sdkError=" + sdk.NET_DVR_GetLastError());
    }
  }

  private static void configureOpenSslPath(HCNetSdkLibrary sdk, int cfgType, Path path) {
    byte[] bytes = (path.toAbsolutePath().toString() + "\0").getBytes(StandardCharsets.US_ASCII);
    try (Memory memory = new Memory(bytes.length)) {
      memory.write(0, bytes, 0, bytes.length);
      if (!sdk.NET_DVR_SetSDKInitCfg(cfgType, memory)) {
        System.out.println("设置海康 OpenSSL 路径失败，cfgType=" + cfgType + ", sdkError=" + sdk.NET_DVR_GetLastError());
      }
    }
  }
}
