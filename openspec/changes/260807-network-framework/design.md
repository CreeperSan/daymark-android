## Context

工程为单模块 Kotlin Android 应用，最低 API 为 26，已通过 `lifecycle-runtime-ktx` 使用协程，但尚无 Retrofit 或 OkHttp，现有本地数据逻辑仍使用 RxJava 3。此次新增基础设施不得迁移或破坏既有 RxJava/Room 页面逻辑；变更动机见 `proposal.md`，行为契约见各网络能力规格。

## Goals / Non-Goals

**Goals:**

- 在 `app` 模块建立独立、无 UI 依赖的 `network` 包，对外仅暴露稳定的客户端、请求、事件、错误和中间件 API。
- 以 Retrofit 3 负责声明式服务创建，以 OkHttp 负责传输、取消、缓存和底层拦截，以协程 `Flow` 作为唯一新网络 API 的异步输出。
- 使一次执行从配置校验到释放资源均可追踪、可取消、可测试，并避免在主线程进行网络、文件 I/O 或大对象解析。
- 为所有公开类、公开方法和中间件控制流补充简体中文 KDoc 与关键逻辑中文注释。

**Non-Goals:**

- 不在本变更中迁移已有 RxJava 3 或 Room 代码，不提供 Flow 与 RxJava 的双向适配层。
- 不内置业务接口、令牌存储、业务码协议、离线队列、通用重试策略或磁盘缓存失效规则；这些通过中间件和配置扩展。
- 不在应用启动时预热所有客户端，也不实现断点续传以外的下载分片并行。

## Decisions

### 1. 分层 API：声明式服务与通用请求执行器并存

新增 `network/api`、`network/core`、`network/middleware`、`network/transfer` 和 `network/internal` 包。`NetworkClient` 由 `NetworkClientBuilder` 创建，构建器持有不可变 `NetworkClientConfig`；客户端提供 Retrofit 服务创建能力，同时提供 `RequestSpec`/可选参数驱动的通用执行入口。调用方可为每个服务或测试实例单独构建客户端，不依赖可变全局单例。

选择该方案是因为 Retrofit 注解接口适合稳定 REST API，而动态请求描述更适合下载、二进制、临时头与未来协议扩展。备选方案是只暴露 Retrofit 服务：它无法自然表达单请求中间件、统一进度和多种非 JSON 流；只提供动态请求则会损失 Retrofit 的类型化声明能力。

### 2. 采用可挂起的应用层中间件链，保留 OkHttp 传输拦截器

定义 `suspend fun intercept(context, chain): MiddlewareOutcome` 的应用层中间件 SPI。`MiddlewareOutcome` 明确表示继续、短路成功、拒绝失败或取消；链使用不可变中间件列表及每请求独立 `RequestContext`，按“全局注册顺序 → 请求级注册顺序”下行、反向回程。令牌刷新、业务模型转换、追踪和日志在此层完成，因其可以安全挂起并协调尚未发送的请求。

OkHttp 拦截器仅用于传输层职责（用户代理、低层诊断、连接和缓存），不得承载需要挂起的鉴权逻辑。备选方案是仅使用 OkHttp 拦截器；其同步模型无法满足等待令牌刷新和请求级协程控制。

### 3. 使用冷 `Flow<NetworkEvent<T>>` 统一执行与取消

`execute` 使用 `channelFlow` 或 `callbackFlow` 仅在收集时创建执行协程和 OkHttp 调用。事件类型包括 `Started`、`UploadProgress`、`DownloadProgress`、`Success<T>`、`SseMessage`、`Failure` 和 `Cancelled`；设计保证每次收集独立、一个终态且终态后不再发送进度。`awaitClose`/`finally` 统一取消 OkHttp `Call`、关闭 `ResponseBody`、取消子协程及关闭文件流。

调用方取消收集协程时，`CancellationException` 直接传播并映射为取消终态（若事件流仍可发送）；不得包装成业务错误。备选方案是 `flow {}` 同步阻塞读取：实现更短，但不适合回调取消和持续 SSE，且容易遗漏资源关闭。

### 4. 分别实现载荷编解码与传输包装

`PayloadCodec` SPI 依据媒体类型和目标类型选择 JSON、表单、Multipart、protobuf 或二进制编解码器，默认 JSON 转换器由客户端明确配置。上传使用包装 `RequestBody` 在写入时报告已写字节；下载通过包装读取流报告进度并写入同目录临时文件，成功后原子移动到目标文件。恢复下载使用已存在临时文件长度构造范围请求，只有校验服务端范围响应后才追加。

SSE 使用独立 `SseTransport`，按事件帧解析 `event`、`id`、`data` 和 `retry` 字段；可选重连策略保存最后事件标识，限制重连次数与退避时间。备选方案是让所有内容交由同一个 Retrofit 转换器：它不适合增量进度、流式 SSE 和安全文件提交。

### 5. 错误、可观测性和性能采用显式策略

统一 `NetworkFailure` 密封类型包含配置、连接、超时、HTTP、编码、文件、SSE、取消和中间件失败，并保存脱敏后的请求信息、关联标识和原始 `Throwable`。日志通过可注入 `NetworkLogger` 输出，默认对 `Authorization`、Cookie、文件内容和敏感查询参数脱敏。转换、文件读写、SSE 解析和进度采样全部位于 `Dispatchers.IO`；进度事件按字节阈值或时间阈值采样，防止高速传输导致 UI 或 Flow 背压。

备选方案是将错误直接抛给调用方：实现简单，但会迫使各业务重复分类、脱敏和诊断，并无法稳定测试。

### 6. 依赖与兼容性边界

在 `app/build.gradle.kts` 引入与 Kotlin 1.8.10、AGP 8.1.0 和 minSdk 26 兼容的 Retrofit 3、OkHttp、协程核心/Android、所选 JSON 转换器、protobuf 支持与 MockWebServer。实现前先确认 Retrofit 3 的正式稳定版本及其 OkHttp 兼容范围；不得以 Alpha、Beta 或 SNAPSHOT 版本替代。现有 RxJava 3 依赖保持不变，新增网络包禁止依赖 RxJava。

## Risks / Trade-offs

- [Retrofit 3 的正式发布版本或 API 与现有 Kotlin/AGP 不兼容] → 实施前在 Gradle 解析和最小客户端编译中验证；若正式版不兼容，停止实施并将兼容性结论反馈，而不是静默降级到 Retrofit 2。
- [中间件重复等待令牌刷新导致并发放大] → 将刷新协调抽象为可注入的单飞策略，并用并发测试验证只有一个刷新任务执行。
- [进度事件过密影响渲染与内存] → 以可配置字节/时间阈值采样，并在测试中验证终态后不再发射。
- [下载崩溃遗留临时文件或恢复文件被污染] → 使用稳定的临时命名、原子提交、范围响应校验及显式清理策略。
- [SSE 长连接泄漏或无限重连] → 将连接绑定收集协程，限制重连次数/时长并记录脱敏诊断。
- [上传/下载错误将敏感信息写入日志] → 日志器默认脱敏，测试覆盖认证头和 Multipart 内容不出现在输出中。

## Migration Plan

1. 在隔离的 `network` 包及单独 Gradle 提交中添加依赖、最小客户端和编译验证，不修改现有业务调用。
2. 实现核心客户端、事件流与中间件，并以 MockWebServer 单元测试固定行为。
3. 增加所有载荷传输能力和性能/取消测试；仅在测试通过后向业务模块开放 API。
4. 首次接入真实业务接口时为该接口另建变更；通过按服务创建新客户端逐步采用，不改动现有本地流程。
5. 如发现发布后严重问题，移除应用初始化中的网络装配并停止新调用入口；由于本变更不替换既有调用，可回滚依赖和 `network` 包而不迁移数据。

## Open Questions

- 首个真实后端的 JSON 字段命名、业务错误码封装和认证刷新接口尚未确定；它们由后续具体业务接口变更定义，并不影响本框架的编解码 SPI、错误模型或中间件任务划分。
