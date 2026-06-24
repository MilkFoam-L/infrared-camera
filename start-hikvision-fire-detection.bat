@echo off
setlocal EnableExtensions DisableDelayedExpansion
title infrared-camera hikvision launcher

set "APP_DIR=%~dp0"
set "JAR=%APP_DIR%target\infrared-camera-1.0.0.jar"
set "SDK_DIR=%APP_DIR%EN-HCNetSDKV6.1.9.4_build20220412_win64\lib"
set "SDK_DLL=%SDK_DIR%\HCNetSDK.dll"
set "LOG_FILE=%APP_DIR%start-hikvision-fire-detection.log"

set "CAMERA_ID=hm-tcq203-s"
set "CAMERA_HOST=192.168.1.64"
set "CAMERA_PORT=8000"
set "CAMERA_USER=admin"
set "THERMAL_CHANNEL=2"
set "HTTP_PORT=8765"
set "THINGSBOARD_HOST=192.168.1.78:8080"
set "THINGSBOARD_TOKEN=%THINGSBOARD_TOKEN%"

echo.
echo ========================================
echo   infrared-camera hikvision launcher
echo ========================================
echo.

if not exist "%JAR%" (
  echo [ERROR] Jar not found: %JAR%
  echo Please run: mvn package
  goto END
)

if not exist "%SDK_DLL%" (
  echo [ERROR] HCNetSDK.dll not found: %SDK_DLL%
  echo Please check SDK_DIR: %SDK_DIR%
  goto END
)

where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] java command not found. Please install JDK 17 and add java to PATH.
  goto END
)

set "PATH=%SDK_DIR%;%SDK_DIR%\HCNetSDKCom;%PATH%"

if "%CAMERA_PASSWORD%"=="" (
  set /p "CAMERA_PASSWORD=Input camera password: "
)

if "%CAMERA_PASSWORD%"=="" (
  echo [ERROR] Camera password is empty.
  goto END
)

if "%THINGSBOARD_TOKEN%"=="" (
  set /p "THINGSBOARD_TOKEN=Input ThingsBoard device token: "
)

if "%THINGSBOARD_TOKEN%"=="" (
  echo [WARN] ThingsBoard token is empty. Telemetry upload will be disabled.
)

echo Camera host: %CAMERA_HOST%
echo SDK port: %CAMERA_PORT%
echo Thermal channel: %THERMAL_CHANNEL%
echo Local page: http://127.0.0.1:%HTTP_PORT%/
echo SDK DLL: %SDK_DLL%
echo ThingsBoard: http://%THINGSBOARD_HOST%
echo Log file: %LOG_FILE%
echo.
echo Starting service. Do not close this window if it starts successfully.
echo Open browser: http://127.0.0.1:%HTTP_PORT%/
echo.

echo ===== %DATE% %TIME% start ===== > "%LOG_FILE%"
echo APP_DIR=%APP_DIR% >> "%LOG_FILE%"
echo JAR=%JAR% >> "%LOG_FILE%"
echo SDK_DIR=%SDK_DIR% >> "%LOG_FILE%"
echo SDK_DLL=%SDK_DLL% >> "%LOG_FILE%"
echo CAMERA_HOST=%CAMERA_HOST% >> "%LOG_FILE%"
echo THERMAL_CHANNEL=%THERMAL_CHANNEL% >> "%LOG_FILE%"
echo THINGSBOARD_HOST=%THINGSBOARD_HOST% >> "%LOG_FILE%"
java -version >> "%LOG_FILE%" 2>&1

call java -jar "%JAR%" --mode=hikvision --http-port=%HTTP_PORT% --camera-id=%CAMERA_ID% --host=%CAMERA_HOST% --port=%CAMERA_PORT% --username=%CAMERA_USER% --password="%CAMERA_PASSWORD%" --channel=%THERMAL_CHANNEL% --sdk-lib="%SDK_DLL%" --thingsboard-host=%THINGSBOARD_HOST% --thingsboard-token=%THINGSBOARD_TOKEN% >> "%LOG_FILE%" 2>&1
set "APP_EXIT_CODE=%ERRORLEVEL%"

echo.
echo Java process exited. Exit code: %APP_EXIT_CODE%
echo Log file: %LOG_FILE%

:SHOW_LOG
echo.
echo ================ LOG BEGIN ================
if exist "%LOG_FILE%" (
  type "%LOG_FILE%"
) else (
  echo Log file does not exist.
)
echo ================ LOG END ==================
echo.
echo Copy the log text above and send it to the developer.
goto HOLD

:END
echo.
echo Startup failed before Java process.
echo Log file: %LOG_FILE%
goto SHOW_LOG

:HOLD
echo.
echo This window will stay open.
echo Press any key to open an interactive cmd prompt, or close this window manually.
pause >nul
cmd /k
