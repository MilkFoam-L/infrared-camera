## 2026-06-22 - Task: Java 热成像火点检测与实时标注原型

### What was done
- 新增 Java Maven 工程，落地热成像火点检测本地模拟原型。
- 实现火点事件统一模型，支持判断是否存在火点，并表达火点矩形框、最高温点、最高温度、目标距离、设备 IP 和通道。
- 实现热成像测温帧解析，支持海康文档中的 4 字节 float 温度数据和 2 字节温度数据换算。
- 实现本地 HTTP/SSE 服务，前端页面可实时接收火点事件并标注火焰位置和最高温点。
- 按海康 Java 开发指南与 CHM 文档建立 JNA SDK 接入骨架，包含初始化、登录、能力查询、报警回调、布防、撤防、登出和清理流程。
- 将原实施文档更新为 Java 路线，并标出真实设备联调所需 SDK 动态库、设备参数和验收项。

### Testing
- 已执行 `mvn test`，结果通过：10 个测试全部通过，0 失败，0 错误。
- 已执行 `mvn package`，结果通过：生成 `target/infrared-camera-1.0.0.jar` 可执行包。
- 已执行 `java -jar target/infrared-camera-1.0.0.jar --mode=mock --http-port=8765` 启动本地模拟服务，服务成功输出访问地址。
- 已用 `curl -s http://127.0.0.1:8765/api/fire-events/latest` 验证最新火点事件 API，返回 `fireDetected=true`，并包含火点框、最高温点、温度、距离和 `COMM_FIREDETECTION_ALARM`。
- 已用 `curl --max-time 3 -N http://127.0.0.1:8765/api/fire-events/stream` 验证 SSE 实时推送，连续收到多条 `event: fire` 火点事件。
- 已用 `curl` 验证 `/`、`/app.js`、`/style.css` 均可访问。
- 真实海康设备未联调，原因是仓库内未提供 `HCNetSDK.dll/.so`、SDK 依赖库、摄像头地址和账号密码；当前真实设备模式为可配置接入骨架。

### Notes
- `pom.xml`：新增 Maven 构建、JNA、JUnit 5 和可执行 jar 打包配置。
- `src/main/java/com/milkfoam/infraredcamera/App.java`：新增程序入口，支持 `mock` 和 `hikvision` 两种启动模式。
- `src/main/java/com/milkfoam/infraredcamera/fire/NormalizedRect.java`：新增火点矩形框归一化坐标模型与合法性校验。
- `src/main/java/com/milkfoam/infraredcamera/fire/NormalizedPoint.java`：新增最高温点归一化坐标模型与合法性校验。
- `src/main/java/com/milkfoam/infraredcamera/fire/FireDetectionEvent.java`：新增统一火点事件模型和 JSON 输出。
- `src/main/java/com/milkfoam/infraredcamera/fire/FireDetectionStatus.java`：新增是否检测到火点的业务状态模型。
- `src/main/java/com/milkfoam/infraredcamera/fire/ThermalMeasurement.java`：新增测温结果模型。
- `src/main/java/com/milkfoam/infraredcamera/thermal/ThermalFrame.java`：新增热成像温度帧模型，计算最高温、最低温、平均温和最高温点。
- `src/main/java/com/milkfoam/infraredcamera/thermal/ThermalFrameParser.java`：新增 4 字节 float 与 2 字节温度数据解析逻辑。
- `src/main/java/com/milkfoam/infraredcamera/runtime/FireEventSource.java`：新增火点事件源接口。
- `src/main/java/com/milkfoam/infraredcamera/runtime/FireEventBus.java`：新增 SSE 订阅与火点事件广播总线。
- `src/main/java/com/milkfoam/infraredcamera/runtime/MockFireEventSource.java`：新增本地模拟火点事件源。
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HCNetSdkLibrary.java`：新增海康 SDK JNA 接口和关键结构体定义。
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionClientConfig.java`：新增真实设备连接配置模型。
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionPackedTime.java`：新增海康打包时间解析工具。
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionFireAlarmMapper.java`：新增火点报警到统一事件的映射逻辑。
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionFireEventSource.java`：新增真实海康设备事件源骨架。
- `src/main/java/com/milkfoam/infraredcamera/web/FireDetectionHttpServer.java`：新增本地 HTTP 服务、最新事件 API、SSE 推送和截图占位接口。
- `src/main/resources/web/index.html`：新增火点实时标注演示页面。
- `src/main/resources/web/app.js`：新增 SSE 接收、火点框绘制和信息面板更新逻辑。
- `src/main/resources/web/style.css`：新增热成像演示页面样式。
- `src/test/java/com/milkfoam/infraredcamera/fire/FireModelTest.java`：新增火点模型与状态测试。
- `src/test/java/com/milkfoam/infraredcamera/thermal/ThermalFrameParserTest.java`：新增热成像测温帧解析测试。
- `src/test/java/com/milkfoam/infraredcamera/hikvision/HikvisionPackedTimeTest.java`：新增海康打包时间解析测试。
- `src/test/java/com/milkfoam/infraredcamera/hikvision/HikvisionFireAlarmMapperTest.java`：新增火点报警映射测试。
- `docs/thermal-fire-detection-plan.md`：更新为 Java 实施路线、运行方式、真实设备联调说明和验收清单。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮改动，可删除 `pom.xml`、`src/`、`target/`、`progress.md`，并将 `docs/thermal-fire-detection-plan.md` 恢复为本轮修改前版本。

## 2026-06-22 - Task: 修正海康 Windows SDK 回调约定

### What was done
- 自检真实设备接入路径时，发现海康 Windows SDK 报警回调应使用 `stdcall` 回调约定。
- 将火点报警回调接口修正为 `StdCallLibrary.StdCallCallback`，避免 Windows 真机联调时因调用约定不一致导致回调异常。
- 新增单元测试锁定该约定，防止后续误改。

### Testing
- 已执行 `mvn test`，结果通过：11 个测试全部通过，0 失败，0 错误。
- 已执行 `mvn package`，结果通过：重新生成 `target/infrared-camera-1.0.0.jar` 可执行包。

### Notes
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HCNetSdkLibrary.java`：将 `FMSGCallBack_V50` 从普通 JNA `Callback` 修正为 Windows SDK 需要的 `StdCallLibrary.StdCallCallback`。
- `src/test/java/com/milkfoam/infraredcamera/hikvision/HCNetSdkLibraryTest.java`：新增回调调用约定测试，确认火点报警回调继承 stdcall 回调类型。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮补充修正，可将 `FMSGCallBack_V50` 恢复为普通 `Callback`，删除 `HCNetSdkLibraryTest.java`，并重新执行 `mvn test` 验证。

## 2026-06-22 - Task: 修正 SDK 回调指针解析时机

### What was done
- 自检 SDK 报警回调流程时，发现不能把 `pAlarmInfo` 原始指针跨线程传递后再解析。
- 调整为在 SDK 回调内立即解析 `NET_DVR_FIREDETECTION_ALARM` 并转换为 Java 业务事件，异步线程只负责分发已生成的事件对象。

### Testing
- 已执行 `mvn test && mvn package`，结果通过：测试 11 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。

### Notes
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionFireEventSource.java`：将 SDK 指针解析保留在回调生命周期内，避免异步线程访问已失效的原生内存。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮修正，可恢复回调中直接把 `pAlarmInfo` 传给异步线程的旧写法，但不建议这样做，因为真实 SDK 回调内存生命周期不可控。

## 2026-06-22 - Task: 补齐实时测温 SDK 接口骨架

### What was done
- 根据海康实时测温文档补充 `NET_DVR_GET_REALTIME_THERMOMETRY` 常量、远程配置启动/停止接口和远程配置回调类型。
- 补充实时测温条件结构体 `NET_DVR_REALTIME_THERMOMETRY_COND` 与实时测温上传结构体 `NET_DVR_THERMOMETRY_UPLOAD`。
- 补充点测温、线/区域测温、多边形区域等依赖结构，确保后续真实设备可继续接入实时测温长连接。

### Testing
- 已执行 `mvn test && mvn package`，结果通过：测试 15 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。

### Notes
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HCNetSdkLibrary.java`：新增实时测温命令、远程配置接口、回调类型和测温相关 JNA 结构体。
- `src/test/java/com/milkfoam/infraredcamera/hikvision/HCNetSdkLibraryTest.java`：新增实时测温命令常量、远程配置回调约定和测温结构体初始化测试。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮补充，可删除 `NET_DVR_GET_REALTIME_THERMOMETRY`、远程配置接口、实时测温结构体和对应测试，再执行 `mvn test` 验证。

## 2026-06-22 - Task: 增加火点报警快照存储与访问

### What was done
- 新增火点报警快照内存仓库，用于保存真实 SDK 回调中的热成像抓拍图。
- 将海康火点报警回调中的 `pBuffer` 图片数据保存为事件快照，前端事件中的 `snapshotUrl` 可访问对应报警图片。
- 调整 HTTP 截图接口：优先返回真实事件快照；模拟模式或没有图片时继续返回占位图。
- 更新实施文档，明确快照接口已从占位能力升级为真实报警图片访问能力。

### Testing
- 已执行 `mvn test && mvn package`，结果通过：测试 17 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。

### Notes
- `src/main/java/com/milkfoam/infraredcamera/fire/FireSnapshot.java`：新增火点报警快照模型，并对二进制数据做防御性拷贝。
- `src/main/java/com/milkfoam/infraredcamera/fire/FireSnapshotStore.java`：新增线程安全的快照内存仓库。
- `src/main/java/com/milkfoam/infraredcamera/App.java`：新增快照仓库并传入 HTTP 服务和真实海康事件源。
- `src/main/java/com/milkfoam/infraredcamera/web/FireDetectionHttpServer.java`：截图接口优先读取快照仓库中的真实图片。
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionFireEventSource.java`：从 `NET_DVR_FIREDETECTION_ALARM.pBuffer` 保存热成像抓拍图。
- `src/test/java/com/milkfoam/infraredcamera/fire/FireSnapshotStoreTest.java`：新增快照保存、读取和防御性拷贝测试。
- `docs/thermal-fire-detection-plan.md`：更新工程结构和验收清单中的快照能力描述。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮快照能力，可删除 `FireSnapshot.java`、`FireSnapshotStore.java`、`FireSnapshotStoreTest.java`，并恢复 `App.java`、`FireDetectionHttpServer.java`、`HikvisionFireEventSource.java` 和 `docs/thermal-fire-detection-plan.md` 中本轮快照相关改动。

## 2026-06-22 - Task: 增加真实设备实时测温客户端

### What was done
- 新增海康实时测温客户端，可通过 `NET_DVR_StartRemoteConfig` 启动 `NET_DVR_GET_REALTIME_THERMOMETRY` 长连接。
- 实现实时测温数据回调解析，将 `NET_DVR_THERMOMETRY_UPLOAD` 转换为统一 `ThermalMeasurement`。
- 增加停止逻辑，通过 `NET_DVR_StopRemoteConfig` 释放实时测温长连接资源。
- 更新实施文档，明确海康模块已包含实时测温客户端能力。

### Testing
- 已执行 `mvn test && mvn package`，结果通过：测试 19 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。
- 首次运行测试时发现浮点坐标断言过严，已改为容差断言后通过。

### Notes
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionRealtimeThermometryClient.java`：新增真实设备实时测温客户端，支持启动、解析回调和停止。
- `src/test/java/com/milkfoam/infraredcamera/hikvision/HikvisionRealtimeThermometryTest.java`：新增实时测温条件构造和测温上传映射测试。
- `docs/thermal-fire-detection-plan.md`：补充海康模块包含实时测温客户端，更新测试覆盖说明。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮实时测温客户端，可删除 `HikvisionRealtimeThermometryClient.java`、`HikvisionRealtimeThermometryTest.java`，并恢复 `docs/thermal-fire-detection-plan.md` 中本轮说明。

## 2026-06-22 - Task: 接入真实海康事件源实时测温生命周期

### What was done
- 将实时测温客户端接入 `hikvision` 真实设备事件源启动流程，登录、能力查询和布防后自动启动 `NET_DVR_GET_REALTIME_THERMOMETRY`。
- 在真实设备事件源关闭时优先停止实时测温远程配置，再撤防、登出并释放 SDK，避免远程配置句柄泄漏。
- 新增事件源级测试，验证启动时会传入热成像通道和默认规则号，并验证关闭时会释放实时测温句柄。
- 更新实施文档，补充真实设备实时测温长连接启动与退出释放步骤。

### Testing
- 已先执行 `mvn -Dtest=HikvisionFireEventSourceTest test`，新增事件源实时测温生命周期测试通过：2 个测试全部通过。
- 已执行 `mvn test && mvn package`，结果通过：测试 21 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。

### Notes
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionFireEventSource.java`：接入实时测温客户端启动和关闭释放。
- `src/test/java/com/milkfoam/infraredcamera/hikvision/HikvisionFireEventSourceTest.java`：新增真实事件源启动/停止实时测温的单元测试。
- `docs/thermal-fire-detection-plan.md`：补充实时测温长连接启动步骤和资源释放顺序。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮改动，可移除 `HikvisionFireEventSource` 中的 `HikvisionRealtimeThermometryClient` 字段、启动和关闭调用，删除 `HikvisionFireEventSourceTest.java`，并恢复实施文档中第六/第七步说明。

## 2026-06-23 - Task: 增加 Windows 懒人启动脚本

### What was done
- 新增海康真实设备模式一键启动脚本，自动定位项目 JAR、海康 SDK `HCNetSDK.dll`、SDK 依赖目录，并按 HM-TCQ203-S 现场参数预设 IP、端口、账号和热成像通道。
- 启动脚本运行时提示输入摄像头密码，不把真实密码写入仓库文件，降低泄露风险。
- 新增本地模拟模式启动脚本，便于不连接摄像头时验证页面和事件标注能力。
- 更新实施文档，补充 Windows 双击启动方式。

### Testing
- 已执行脚本静态校验，确认 `start-hikvision-fire-detection.bat`、`start-mock-fire-detection.bat`、`target/infrared-camera-1.0.0.jar` 和 `EN-HCNetSDKV6.1.9.4_build20220412_win64/lib/HCNetSDK.dll` 均存在。
- 已执行 `mvn test && mvn package`，结果通过：测试 21 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。
- 未直接执行真实摄像头连接脚本，原因是需要在交互窗口输入密码并连接现场设备；脚本已具备本地双击运行条件。

### Notes
- `start-hikvision-fire-detection.bat`：新增 Windows 海康真实设备模式懒人启动脚本。
- `start-mock-fire-detection.bat`：新增 Windows 本地模拟模式启动脚本。
- `docs/thermal-fire-detection-plan.md`：补充双击脚本启动说明和现场参数说明。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮改动，可删除两个 `.bat` 启动脚本，并恢复 `docs/thermal-fire-detection-plan.md` 中脚本启动相关说明。

## 2026-06-23 - Task: 增加真实热成像抓图画面并简化页面

### What was done
- 新增实时画面模型和线程安全画面仓库，用于保存摄像头热成像通道最新 JPEG 抓图。
- 扩展海康 SDK JNA 接口，接入 `NET_DVR_CaptureJPEGPicture_NEW` 和 `NET_DVR_JPEGPARA`，新增热成像抓图客户端，每秒抓取一次真实热成像通道画面。
- 将热成像抓图客户端接入真实海康事件源生命周期，登录、能力查询和布防后立即抓取第一帧，并周期刷新；退出时先停止抓图线程。
- HTTP 服务新增 `/api/live-frame`，有真实画面时返回 JPEG，无画面时返回等待占位图。
- 前端页面改为只展示摄像头热成像画面，删除右侧报警信息面板和其他文字内容，仅保留火点框、最高温点和温度标签叠加层。
- 更新实施文档，明确当前为秒级热成像抓图刷新，不是 RTSP 视频流。

### Testing
- 已执行 `mvn test`，结果通过：测试 24 个全部通过，0 失败，0 错误。
- 已执行 `mvn package`，结果通过：成功重新生成 `target/infrared-camera-1.0.0.jar`。
- 已短时启动 mock 模式，并验证 `/`、`/api/live-frame`、`/app.js`、`/style.css` 均可访问且非空。
- 未直接执行真实摄像头连接脚本；真实画面需双击 `start-hikvision-fire-detection.bat` 后输入摄像头密码现场验证。

### Notes
- `src/main/java/com/milkfoam/infraredcamera/fire/LiveFrame.java`：新增实时画面数据模型。
- `src/main/java/com/milkfoam/infraredcamera/fire/LiveFrameStore.java`：新增最新实时画面仓库。
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HCNetSdkLibrary.java`：新增 JPEG 抓图接口和参数结构体。
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionThermalSnapshotClient.java`：新增海康热成像抓图客户端。
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionFireEventSource.java`：接入抓图客户端启动与关闭生命周期。
- `src/main/java/com/milkfoam/infraredcamera/App.java`：新增实时画面仓库并传入真实事件源和 HTTP 服务。
- `src/main/java/com/milkfoam/infraredcamera/web/FireDetectionHttpServer.java`：新增 `/api/live-frame` 实时画面接口。
- `src/main/resources/web/index.html`：简化页面，只保留热成像画面和标注层。
- `src/main/resources/web/app.js`：新增实时画面刷新逻辑，保留火点框叠加。
- `src/main/resources/web/style.css`：改为全屏摄像头画面样式。
- `src/test/java/com/milkfoam/infraredcamera/fire/LiveFrameStoreTest.java`：新增实时画面仓库测试。
- `src/test/java/com/milkfoam/infraredcamera/hikvision/HikvisionThermalSnapshotClientTest.java`：新增抓图客户端测试。
- `src/test/java/com/milkfoam/infraredcamera/hikvision/HikvisionFireEventSourceTest.java`：补充真实事件源启动后写入实时画面的测试。
- `docs/thermal-fire-detection-plan.md`：更新真实抓图、纯画面页面和验收清单说明。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮改动，可删除 `LiveFrame.java`、`LiveFrameStore.java`、`HikvisionThermalSnapshotClient.java` 及对应测试，恢复 `HCNetSdkLibrary.java`、`HikvisionFireEventSource.java`、`App.java`、`FireDetectionHttpServer.java`、前端三个资源文件和实施文档中本轮相关内容。

## 2026-06-23 - Task: 火点事件上报 ThingsBoard

### What was done
- 根据 `thingsboard上报.txt` 新增 ThingsBoard 遥测上报配置，支持 `--thingsboard-host` 和 `--thingsboard-token` 启用云端上报。
- 新增 ThingsBoard 异步遥测客户端，火点事件产生后向 ThingsBoard 遥测接口发送 JSON。
- 上报内容保留示例字段 `warning_flag=1`、`warning_status=1`，并补充摄像头、通道、设备 IP、事件 ID、最高温、目标距离、火点框坐标、最高温点和事件时间。
- 将上报链路接入主程序事件处理流程：先推送本地页面，再异步上报 ThingsBoard；上报失败只打印日志，不中断检测服务。
- 更新海康真实设备 Windows 启动脚本，默认带上 ThingsBoard 地址 `192.168.1.78:8080` 和设备访问令牌。
- 更新实施文档，补充 ThingsBoard 参数、上报地址和上报字段说明。

### Testing
- 已执行 `mvn -Dtest=ThingsBoardTelemetryClientTest test`，结果通过：ThingsBoard URL 和遥测 JSON 测试 3 个全部通过。
- 已执行 `mvn test && mvn package`，结果通过：测试 27 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。
- 未直接向现场 ThingsBoard 发真实报警数据；真实上报需等待摄像头产生火点事件后由脚本启动服务验证。

### Notes
- `thingsboard上报.txt`：作为 ThingsBoard 上报目标、token 和基础字段依据，未修改。
- `src/main/java/com/milkfoam/infraredcamera/thingsboard/ThingsBoardConfig.java`：新增 ThingsBoard 地址和 token 配置。
- `src/main/java/com/milkfoam/infraredcamera/thingsboard/ThingsBoardTelemetryClient.java`：新增异步 HTTP 遥测上报客户端。
- `src/test/java/com/milkfoam/infraredcamera/thingsboard/ThingsBoardTelemetryClientTest.java`：新增 URL 拼接和遥测 JSON 测试。
- `src/main/java/com/milkfoam/infraredcamera/App.java`：接入火点事件 ThingsBoard 上报流程和关闭释放。
- `start-hikvision-fire-detection.bat`：新增 ThingsBoard host/token 并传给 Java 程序。
- `docs/thermal-fire-detection-plan.md`：补充 ThingsBoard 上报参数、字段和验收说明。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮改动，可删除 `src/main/java/com/milkfoam/infraredcamera/thingsboard/` 和对应测试目录，恢复 `App.java`、`start-hikvision-fire-detection.bat`、`docs/thermal-fire-detection-plan.md` 中 ThingsBoard 相关内容。

## 2026-06-24 - Task: 修复 Windows 启动脚本报错

### What was done
- 根据海康 SDK 自带 Demo 的初始化方式，补充 `NET_DVR_SetSDKInitCfg`、SDK 本地路径、组件检查和 OpenSSL DLL 路径配置，降低双击脚本时 SDK 组件加载失败风险。
- 真实海康事件源在 `NET_DVR_Init` 前会自动根据 `--sdk-lib` 所在目录配置 SDK 路径和依赖 DLL 路径。
- 启动脚本新增日志输出，双击运行后会生成 `start-hikvision-fire-detection.log`，便于定位后续真实设备登录或 SDK 错误。

### Testing
- 已执行 `mvn test && mvn package`，结果通过：测试 27 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。
- 未用明文密码直接连接真实摄像头，避免在工具调用中暴露密码和触发现场设备副作用；修复后需用户双击脚本输入密码验证。

### Notes
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HCNetSdkLibrary.java`：新增 `NET_DVR_SetSDKInitCfg`、SDK 初始化配置常量和路径结构体。
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionSdkInitializer.java`：新增 SDK 路径、组件检查和 OpenSSL 路径初始化工具。
- `src/main/java/com/milkfoam/infraredcamera/hikvision/HikvisionFireEventSource.java`：在 SDK 初始化前调用路径配置。
- `src/test/java/com/milkfoam/infraredcamera/hikvision/HikvisionFireEventSourceTest.java`：补充 Fake SDK 新增接口实现。
- `src/test/java/com/milkfoam/infraredcamera/hikvision/HikvisionThermalSnapshotClientTest.java`：补充 Fake SDK 新增接口实现。
- `start-hikvision-fire-detection.bat`：新增启动日志文件输出和 Java 版本记录。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮改动，可删除 `HikvisionSdkInitializer.java`，恢复 `HCNetSdkLibrary.java`、`HikvisionFireEventSource.java`、两个测试 Fake SDK 和启动脚本中本轮 SDK 初始化与日志相关改动。

## 2026-06-24 - Task: 保持 Windows 启动窗口并显示日志

### What was done
- 修改海康真实设备启动脚本，程序退出后自动打印 `start-hikvision-fire-detection.log` 内容到当前窗口。
- 启动脚本末尾增加保持窗口逻辑，避免 CMD 直接关闭导致无法复制错误信息。
- 保留日志文件输出，后续既可以复制窗口内容，也可以直接发送日志文件内容。

### Testing
- 已执行脚本静态校验，确认 `start-hikvision-fire-detection.bat`、`target/infrared-camera-1.0.0.jar` 和 `EN-HCNetSDKV6.1.9.4_build20220412_win64/lib/HCNetSDK.dll` 均存在。
- 未直接连接真实摄像头，避免在工具调用中暴露密码和触发现场设备副作用；需用户双击脚本输入密码验证窗口保留效果。

### Notes
- `start-hikvision-fire-detection.bat`：程序退出后打印日志内容，并通过 `pause` 和 `cmd /k` 保持窗口打开。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮改动，可恢复启动脚本末尾为只输出退出码和 `pause` 的旧逻辑。

## 2026-06-24 - Task: 修复 BAT 中文编码导致的 CMD 命令解析错误

### What was done
- 将 `start-hikvision-fire-detection.bat` 改写为纯英文 ASCII 输出，移除中文提示和 `chcp 65001`，避免 Windows CMD 将 UTF-8 中文误解析为命令。
- 保留原有自动定位 JAR、SDK、输入摄像头密码、启动 Java、输出日志和保持窗口逻辑。
- 统一错误跳转到日志显示区域，启动前失败也会保持窗口并打印可复制日志。

### Testing
- 已执行脚本静态校验，确认 `start-hikvision-fire-detection.bat`、`target/infrared-camera-1.0.0.jar` 和 `EN-HCNetSDKV6.1.9.4_build20220412_win64/lib/HCNetSDK.dll` 均存在。
- 未直接连接真实摄像头；需用户重新双击脚本输入密码验证 CMD 编码问题已消失。

### Notes
- `start-hikvision-fire-detection.bat`：改为纯英文 ASCII 启动脚本，避免中文乱码和命令断行。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮改动，可恢复上一版包含中文提示的启动脚本，但不建议这样做，因为 CMD 编码兼容性差。

## 2026-06-24 - Task: 恢复火点红色热区标注效果

### What was done
- 将前端火点标注从红色矩形边框改回半透明红色热区效果。
- 保留最高温点和温度标签，不再显示矩形边框线。
- 红色热区仍使用设备报警返回的火点区域坐标定位，但视觉上改为柔和径向热区。

### Testing
- 已执行 `mvn test && mvn package`，结果通过：测试 27 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。

### Notes
- `src/main/resources/web/style.css`：将 `.fire-box` 从边框矩形改为径向渐变热区，并移除伪元素边框。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮改动，可恢复 `.fire-box`、`.fire-box::before`、`.fire-box::after` 和 `.temperature-tag` 的上一版矩形边框样式。

## 2026-06-24 - Task: 调整为朴素红色火点位置标注

### What was done
- 将前端火点标注改为热成像画面上朴素的红色实心区域，用于直接标出火点位置。
- 去除原先夸张的发光、模糊、扩散热区效果。
- 隐藏最高温点和温度标签，避免画面上出现额外标注干扰。

### Testing
- 已执行 `mvn test && mvn package`，结果通过：测试 27 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。

### Notes
- `src/main/resources/web/style.css`：将 `.fire-box` 改为朴素红色实心区域，并隐藏 `.temperature-tag` 与 `.hot-point`。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮改动，可恢复 `.fire-box` 为径向渐变热区样式，并恢复 `.hot-point` 与 `.temperature-tag` 的显示样式。

## 2026-06-24 - Task: 改为火源轮廓红色像素标注

### What was done
- 将前端火点标注从整块矩形覆盖改为画布像素覆盖层。
- 前端收到火点事件后读取当前热成像画面像素，只在报警范围内按亮度阈值筛出火源高亮轮廓，并把对应像素点标成红色。
- 继续用设备报警返回的矩形坐标限定搜索范围，但不再直接显示整个方框。
- 同步更新实施文档，说明当前标注方式是火源轮廓像素标注。

### Testing
- 已执行 `mvn test && mvn package`，结果通过：测试 27 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。

### Notes
- `src/main/resources/web/index.html`：将原火点方框和最高温点元素替换为 `fireMask` 画布层。
- `src/main/resources/web/app.js`：新增热成像画面像素读取、报警范围裁剪、亮度阈值筛选和红色像素绘制逻辑。
- `src/main/resources/web/style.css`：删除方框、温度标签和最高温点样式，新增全屏像素标注画布样式。
- `docs/thermal-fire-detection-plan.md`：更新前端实时标注说明和验收清单，明确不再绘制整块矩形框。
- 回滚方式：本目录不是 Git 仓库；如需回滚本轮改动，可恢复 `index.html` 中的 `fireBox`、`temperatureTag`、`hotPoint` 元素，恢复 `app.js` 中按矩形坐标设置 DOM 样式的逻辑，并恢复 `style.css` 中 `.fire-box`、`.temperature-tag` 和 `.hot-point` 样式。

## 2026-06-24 - Task: 建立 Git 仓库并增强 ThingsBoard 上传日志

### What was done
- 在项目目录初始化 Git 仓库，并完成首次代码提交。
- 新增 `.gitignore`，排除构建产物、运行日志、海康 SDK 二进制包和本地 ThingsBoard 调试文件，避免敏感信息和大文件进入仓库。
- 将真实设备启动脚本中的 ThingsBoard 设备令牌改为运行时输入或环境变量注入，不再把令牌写死到脚本中。
- 增强 ThingsBoard 上传控制台日志，输出上传开关、目标地址、事件 ID、请求 JSON、响应状态码、响应体和异常栈。
- 调整 ThingsBoard HTTP 客户端执行器使用方式，避免上传任务与 HttpClient 共用单线程执行器导致排查困难。
- 同步更新实施文档，说明启动脚本的令牌输入方式和 ThingsBoard 上传日志内容。

### Testing
- 已执行 `mvn test && mvn package`，结果通过：测试 27 个全部通过，并成功重新生成 `target/infrared-camera-1.0.0.jar`。
- 已完成首次 Git 提交：`63c3fe3 Initial infrared camera fire detection app`。

### Notes
- `.gitignore`：新增 Git 忽略规则，排除 `target/`、日志、SDK 二进制目录和本地 token 调试文件。
- `start-hikvision-fire-detection.bat`：ThingsBoard 令牌改为运行时输入或环境变量注入，并对 ThingsBoard 参数加引号。
- `src/main/java/com/milkfoam/infraredcamera/thingsboard/ThingsBoardTelemetryClient.java`：新增上传调试日志和异常栈输出，并调整 HttpClient 执行器配置。
- `docs/thermal-fire-detection-plan.md`：补充 ThingsBoard 令牌输入和上传日志说明。
- `progress.md`：追加本轮变更记录。
- 回滚方式：可执行 `git revert 63c3fe3` 回滚首次提交；ThingsBoard 日志增强改动如需回滚，可恢复上述文件到 `63c3fe3` 对应版本后重新打包。
