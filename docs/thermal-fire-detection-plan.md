# 热成像火点检测与实时标注 Java 实施文档

## 业务目标

基于海康设备网络 SDK 的热成像火点检测、测温与报警能力，实现以下闭环：

1. 判断摄像头画面中是否存在火焰/火点。
2. 获取火点在画面中的归一化位置、最高温点、最高温度和目标距离。
3. 将设备报警统一转换为后端 JSON 事件。
4. 通过本地 HTTP/SSE 实时推送给页面。
5. 在页面上实时标注火焰位置框和最高温点。

当前仓库未提供真实海康 SDK 动态库、JNA jar、摄像头地址和账号密码，因此本轮落地为：

- 一个可运行的 Java 本地模拟原型，用于验证后端事件模型、SSE 实时推送和前端标注能力。
- 一个按海康 Java 开发指南和 CHM 文档建立的 SDK/JNA 接入骨架，用于后续真实设备联调。

真实设备联调时，把模拟事件源切换为 `hikvision` 模式，并提供 `HCNetSDK.dll/.so`、摄像头地址、账号、密码和热成像通道号。

---

## 一、文档依据

本实施方案参考以下本地文档：

| 文档 | 用途 |
|---|---|
| `chm_extract/00新手指南/Java开发指南.html` | Java 通过 JNA 加载海康 SDK 动态库、结构体字段顺序和接口声明方式 |
| `chm_extract/07微影传感/火点检测事件.html` | 火点检测流程、布防流程、报警回调、`COMM_FIREDETECTION_ALARM` |
| `chm_extract/07微影传感/热成像实时测温.html` | 实时测温接口 `NET_DVR_GET_REALTIME_THERMOMETRY` 与测温回调思路 |
| `chm_extract/07微影传感/热成像全屏测温.html` | 全屏测温像素温度数据、4 字节 float 与 2 字节温度数据解析方式 |
| `chm_extract/结构体/NET_DVR_FIREDETECTION_ALARM.html` | 火点报警结构体字段：火点框、最高温点、最高温、距离、图片等 |
| `chm_extract/接口定义/NET_DVR_SetDVRMessageCallBack_V50.html` | `COMM_FIREDETECTION_ALARM = 0x4991` |

---

## 二、执行步骤

### 第一步：确认设备热成像能力

真实设备模式启动后需要执行：

1. 初始化 SDK：`NET_DVR_Init`。
2. 设置连接与重连：`NET_DVR_SetConnectTime`、`NET_DVR_SetReconnect`。
3. 登录设备：`NET_DVR_Login_V40`。
4. 通过 `NET_DVR_STDXMLConfig` 查询：

```text
GET /ISAPI/Thermal/capabilities
GET /ISAPI/Thermal/channels/<channelID>/fireDetection/capabilities
```

必须重点确认：

```text
isSupportFireDetection = true
isSupportRealtimeThermometry = true
isSupportThermometry = true
isSupportFireDetectionSchedule = true
```

注意：热成像相关能力只支持热成像通道。双光设备通常一个通道为可见光、一个通道为热成像，需要现场确认真实通道号。

### 第二步：配置火点检测和报警上传

真实设备需要在 Web 页面或 SDK 透传接口中完成配置：

```text
GET /ISAPI/Thermal/channels/<channelID>/fireDetection
PUT /ISAPI/Thermal/channels/<channelID>/fireDetection

GET /ISAPI/Event/triggers/fireDetection-<channelID>
PUT /ISAPI/Event/triggers/fireDetection-<channelID>

GET /ISAPI/Event/schedules/fireDetections/fireDetection-<channelID>
PUT /ISAPI/Event/schedules/fireDetections/fireDetection-<channelID>
```

触发方式必须包含“上传报警信息”，否则 SDK 布防后不会收到火点事件。

如需降低误报，可配置屏蔽区：

```text
GET /ISAPI/Thermal/channels/<channelID>/fireShieldMask
PUT /ISAPI/Thermal/channels/<channelID>/fireShieldMask
GET /ISAPI/Thermal/channels/<channelID>/SmokeShieldMask
PUT /ISAPI/Thermal/channels/<channelID>/SmokeShieldMask
```

### 第三步：接收火点报警

Java 接入路径：

1. 通过 JNA 加载 `HCNetSDK.dll` 或 `libhcnetsdk.so`。
2. 设置报警回调：`NET_DVR_SetDVRMessageCallBack_V50`。
3. 对设备布防：`NET_DVR_SetupAlarmChan_V41`。
4. 在回调中判断：`lCommand == COMM_FIREDETECTION_ALARM`。
5. 将 `pAlarmInfo` 解析为 `NET_DVR_FIREDETECTION_ALARM`。
6. 提取字段并转换为统一事件：

```text
设备 IP、通道号、报警时间、火点矩形框、最高温点、最高温、目标距离、抓拍图、原始命令
```

回调线程只做轻量解析和入队，保存图片、写库、推送前端放到异步流程。

### 第四步：统一后端事件模型

统一 JSON 示例：

```json
{
  "eventId": "fire-demo-0001",
  "cameraId": "cam-001",
  "channelId": 2,
  "deviceIp": "192.168.1.64",
  "eventType": "fire_detection",
  "eventTime": "2026-06-22T22:59:06.5438563+08:00",
  "maxTemperature": 80.1,
  "targetDistance": 12.1,
  "rect": { "x": 0.300, "y": 0.260, "width": 0.120, "height": 0.160 },
  "highestPoint": { "x": 0.3624, "y": 0.3272 },
  "snapshotUrl": "/api/fire-events/fire-demo-0001/snapshot",
  "rawCommand": "COMM_FIREDETECTION_ALARM"
}
```

坐标均使用 0 到 1 的归一化值，前端根据播放器实际宽高进行映射。

### 第五步：真实热成像抓图与实时前端标注

页面能力：

1. 后端通过 `NET_DVR_CaptureJPEGPicture_NEW` 从热成像通道抓取 JPEG，并通过 `/api/live-frame` 提供最新画面。
2. 前端每秒刷新 `/api/live-frame`，页面主体只展示摄像头热成像画面。
3. 通过 `EventSource` 连接 `/api/fire-events/stream`。
4. 接收火点事件后以前端画布读取真实画面像素，只在报警区域内按亮度阈值把火源轮廓像素标为红色。
5. `rect.x/y/width/height` 只作为火源像素搜索范围，不再直接绘制整块矩形框。
6. 新报警刷新红色像素标注，旧标注短时间后变淡并清除；页面不再显示右侧报警信息面板。

### 第六步：启动实时测温长连接

真实设备模式登录和布防后会启动实时测温：

1. 构造 `NET_DVR_REALTIME_THERMOMETRY_COND`，通道号使用热成像通道，规则号默认 `0`。
2. 通过 `NET_DVR_StartRemoteConfig` 执行 `NET_DVR_GET_REALTIME_THERMOMETRY`。
3. 在远程配置回调中解析 `NET_DVR_THERMOMETRY_UPLOAD`，得到最高温、最低温、平均温和最高温点。
4. 当前实时标注仍以火点报警事件为前端主事件，实时测温数据作为真实设备联调时的温度辅助链路。

### 第七步：退出与资源释放

真实设备模式退出时需要执行：

1. 停止实时测温：`NET_DVR_StopRemoteConfig`。
2. 撤防：`NET_DVR_CloseAlarmChan_V30`。
3. 登出：`NET_DVR_Logout`。
4. 释放 SDK：`NET_DVR_Cleanup`。

---

## 三、当前 Java 工程结构

| 文件/目录 | 作用 |
|---|---|
| `pom.xml` | Maven 构建文件，Java 17，集成 JNA、JUnit 5 和可执行 jar 打包 |
| `src/main/java/com/milkfoam/infraredcamera/App.java` | 程序入口，支持 `mock` 和 `hikvision` 模式 |
| `src/main/java/com/milkfoam/infraredcamera/fire/` | 火点事件、归一化坐标、状态、快照存储、实时画面存储与测温业务模型 |
| `src/main/java/com/milkfoam/infraredcamera/thermal/` | 全屏测温帧解析，支持 4 字节 float 和 2 字节温度数据 |
| `src/main/java/com/milkfoam/infraredcamera/runtime/` | 火点事件源、模拟事件源、SSE 广播总线 |
| `src/main/java/com/milkfoam/infraredcamera/hikvision/` | 海康 JNA 接口、结构体、时间解析、火点报警映射、实时测温客户端、热成像抓图客户端和真实设备事件源 |
| `src/main/java/com/milkfoam/infraredcamera/web/` | JDK 内置 HTTP 服务，提供页面、实时画面 API、最新事件 API、SSE 和报警截图接口 |
| `src/main/resources/web/` | 只显示热成像画面的前端页面、脚本和样式，火点框叠加在画面上 |
| `src/test/java/com/milkfoam/infraredcamera/` | 坐标、测温解析、时间解析和火点映射单元测试 |

---

## 四、本地模拟运行

### 编译与测试

```bash
mvn test
mvn package
```

### 启动模拟火点检测服务

Windows 可直接双击：

```text
start-mock-fire-detection.bat
```

或命令行启动：

```bash
java -jar target/infrared-camera-1.0.0.jar --mode=mock --http-port=8765
```

打开：

```text
http://127.0.0.1:8765/
```

验证 API：

```bash
curl -s "http://127.0.0.1:8765/api/live-frame"
curl -s "http://127.0.0.1:8765/api/fire-events/latest"
curl --max-time 3 -N "http://127.0.0.1:8765/api/fire-events/stream"
```

当前模拟模式会持续生成火点事件，用于验证“是否存在火点”和“火点在画面哪个位置”。

---

## 五、真实海康设备运行方式

仓库已提交可执行包 `target/infrared-camera-1.0.0.jar` 和海康 Windows SDK 运行库 `EN-HCNetSDKV6.1.9.4_build20220412_win64/lib/`，Windows 服务器拉取代码后可直接双击运行真实设备模式：

```text
start-hikvision-fire-detection.bat
```

脚本已按 `HM-TCQ203-S` 当前现场参数预设：摄像头 IP `192.168.1.64`、SDK 端口 `8000`、热成像通道 `2`、账号 `admin` 和 ThingsBoard 地址 `192.168.1.78:8080`。摄像头密码和 ThingsBoard 设备访问令牌直接在 `start-hikvision-fire-detection.bat` 顶部变量中配置，不再读取电脑环境变量，也不再启动时询问。

命令行示例：

```bash
java -jar target/infrared-camera-1.0.0.jar \
  --mode=hikvision \
  --http-port=8765 \
  --camera-id=cam-001 \
  --host=192.168.1.64 \
  --port=8000 \
  --username=admin \
  --password=<设备密码> \
  --channel=2 \
  --sdk-lib=C:/path/to/HCNetSDK.dll \
  --thingsboard-host=192.168.1.78:8080 \
  --thingsboard-token=<设备访问令牌>
```

说明：

- 服务器只需 `git pull` 获取最新代码、`target/infrared-camera-1.0.0.jar` 和 `EN-HCNetSDKV6.1.9.4_build20220412_win64/lib/`，无需先安装 Maven 打包或手动拷贝 SDK 即可运行脚本。
- 页面主体只展示 `/api/live-frame` 返回的热成像抓图，红色像素标注会按火源高亮轮廓叠加在真实画面上。
- 当前抓图刷新为秒级刷新，不是 25fps 视频流；如需低延迟视频，后续需要单独接 RTSP 转 HLS/WebRTC。
- 收到火点事件后会先更新本地页面，再异步向 ThingsBoard 上报遥测；未配置 `--thingsboard-host` 或 `--thingsboard-token` 时不上报云端。
- 启动窗口会实时显示 Java 输出；程序每 5 秒输出一条 `FIRE_CHECK` 检测状态日志，收到火点报警时输出 `FIRE_DETECTED` 事件明细。
- 控制台会输出 ThingsBoard 上传开关、目标地址、事件 ID、请求 JSON、响应状态码、响应体和异常栈，便于排查为什么未上传成功。
- `thingsboard上报.txt` 是手工验证 ThingsBoard 链路的 Python 调试脚本，ThingsBoard 地址和设备访问令牌直接在文件顶部变量中配置，不读取电脑环境变量。
- ThingsBoard 上报地址格式：`http://<thingsboard-host>/api/v1/<thingsboard-token>/telemetry`。
- 上报基础字段包含 `warning_flag=1`、`warning_status=1`，同时附带摄像头、通道、设备 IP、事件 ID、最高温、距离、火点框坐标、最高温点和事件时间。
- `--sdk-lib` 可传绝对路径，也可省略并让 JNA 从系统库路径查找。
- Windows 使用 `HCNetSDK.dll`，Linux 使用 `libhcnetsdk.so`。
- Linux 还需要按海康 Java 开发指南配置 `HCNetSDKCom`、`libcrypto.so`、`libssl.so` 等依赖库路径。
- 当前按现场部署需求采用脚本内固定变量；修改 `CAMERA_PASSWORD` 和 `THINGSBOARD_TOKEN` 后即可运行，注意该仓库为 private 并控制访问权限。

---

## 六、验收清单

### 已完成

- [x] Java Maven 工程可编译。
- [x] 本地模拟模式可启动。
- [x] 火点事件模型包含摄像头、通道、设备 IP、时间、最高温、距离、矩形框、最高温点和原始命令。
- [x] 后端可通过 `/api/fire-events/latest` 返回最新火点事件。
- [x] 后端可通过 `/api/fire-events/stream` 使用 SSE 推送实时火点事件。
- [x] 页面可根据归一化坐标限定搜索范围，并按热成像画面高亮轮廓绘制红色像素标注。
- [x] 后端可通过 `/api/live-frame` 返回最新热成像画面；真实设备模式由 SDK 周期抓取热成像通道 JPEG。
- [x] 收到火点事件后可向 ThingsBoard 上报 `warning_flag`、`warning_status` 和火点遥测字段。
- [x] 真实 SDK 火点报警中的热成像抓拍图可保存到快照仓库，并通过 `/api/fire-events/<eventId>/snapshot` 访问。
- [x] 单元测试覆盖坐标合法性、SDK 时间解析、火点事件映射、测温帧解析、回调约定、实时测温客户端、热成像抓图客户端和快照存储。
- [x] 已建立海康 SDK/JNA 接入骨架。

### 真实设备联调待完成

- [x] 提供 Windows 版 `HCNetSDK.dll` 及其依赖库目录：`EN-HCNetSDKV6.1.9.4_build20220412_win64/lib`。
- [x] 提供摄像头 IP、端口、账号和热成像通道号；密码由启动脚本运行时输入。
- [ ] 查询设备热成像能力并确认支持火点检测。
- [ ] 配置火点检测、报警上传和布防时间。
- [ ] 收到真实 `COMM_FIREDETECTION_ALARM`。
- [ ] 保存真实报警 JSON 与抓拍图。
- [ ] 对照真实抓拍图人工确认火点框位置准确。
- [ ] 验证断线重连、服务重启恢复布防和多摄像头不串台。

---

## 七、完成标准

正式交付真实设备版本时，每条报警至少应保留：

1. 设备能力查询结果。
2. 火点检测配置快照。
3. SDK 回调日志：`COMM_FIREDETECTION_ALARM`。
4. 结构化事件 JSON。
5. 原始抓拍图。
6. 前端带框截图。
7. 最高温、目标距离、最高温点。

数据报告应包含：报警总数、有效报警数、温度范围、距离范围、坐标合法性、推送延迟、标注准确率和稳定性运行结果。
