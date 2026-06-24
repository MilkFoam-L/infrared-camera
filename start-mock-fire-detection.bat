@echo off
setlocal EnableExtensions
chcp 65001 >nul
title 热成像火点检测 - 本地模拟模式

set "APP_DIR=%~dp0"
set "JAR=%APP_DIR%target\infrared-camera-1.0.0.jar"
set "HTTP_PORT=8765"

echo.
echo ========================================
echo   热成像火点检测 - 本地模拟模式启动器
echo ========================================
echo.

if not exist "%JAR%" (
  echo [错误] 未找到程序包：%JAR%
  echo 请先在项目目录执行：mvn package
  echo.
  pause
  exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
  echo [错误] 未找到 java 命令，请先安装 JDK 17 或把 java 加入 PATH。
  echo.
  pause
  exit /b 1
)

echo 本地页面：http://127.0.0.1:%HTTP_PORT%/
echo 正在启动，启动后请不要关闭此窗口。
echo.

java -jar "%JAR%" --mode=mock --http-port=%HTTP_PORT% --camera-id=cam-001 --channel=2 --device-ip=192.168.1.64

echo.
echo 程序已退出，退出码：%ERRORLEVEL%
pause
