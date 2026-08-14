## Why

现有网络实现包含多种传输和异步模型，难以统一请求配置、中间件、取消、进度和错误语义。现在需要以全新工程视角建立一套独立、可替换传输实现的 Android 网络框架，并同时覆盖普通请求、原始流式传输与 SSE 长连接。

## What Changes

- **BREAKING**：新增独立的网络核心 API，不兼容或复用旧网络请求框架。
- 以不可变请求描述统一配置 host、path、method、header、query、body、请求级中间件和类型化自定义属性。
- 提供线程安全的全局中间件注册机制，以及按请求配置的中间件链；中间件支持挂起等待、修改请求、修改上下文、短路成功和拒绝失败。
- 抽象传输引擎，首个实现基于 OkHttp，核心层不暴露 OkHttp、Retrofit 或具体序列化库类型。
- 以冷 Flow 暴露普通请求、上传下载进度、原始字节流和 SSE 事件，并保证取消传播与资源释放。
- 为 SSE 提供事件帧解析、心跳处理、Last-Event-ID、受限指数退避重连和取消关闭。
- 为流式传输提供增量字节读取、背压、进度采样和文件安全落盘能力。
- 建立结构化错误模型、脱敏日志、响应解码器和可测试的链式调用 API。

## Capabilities

### New Capabilities

- `network-request-core`: 请求模型、类型化属性、错误、解码器、链式调用和客户端配置。
- `network-middleware-pipeline`: 全局/请求级可挂起中间件、上下文修改和短路控制。
- `network-flow-execution`: 冷 Flow、普通请求事件、取消传播、进度和资源生命周期。
- `network-stream-transport`: 原始字节流、上传下载、背压、进度采样和安全文件传输。
- `network-sse-transport`: SSE 帧解析、消息解码、心跳、重连和 Last-Event-ID。

### Modified Capabilities

- 无。

## Impact

- 新增独立网络模块及其公开 SPI、OkHttp 传输实现、Flow 适配、编解码器和测试。
- 新增协程、Flow、OkHttp、序列化与 MockWebServer 等实现和测试依赖，具体依赖由设计阶段确定。
- 不迁移旧网络代码，不为旧调用方提供兼容层；后续业务接口直接使用新框架。
