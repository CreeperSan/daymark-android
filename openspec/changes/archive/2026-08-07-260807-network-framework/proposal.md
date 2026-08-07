## 为什么

当前工程仅具备本地数据能力，尚未建立统一的远程网络访问基础设施；后续接入接口时容易出现重复配置、取消语义不一致、传输进度不可观测及鉴权并发失控等问题。现在需要以 Retrofit 3 和 Kotlin 协程 Flow 建立可组合、可扩展且适用于常见协议与载荷类型的网络框架。

## 变更内容

- 引入 Retrofit 3、OkHttp、Kotlin 协程与 Flow 所需依赖，并提供统一的网络客户端构建入口。
- 提供可由调用方实例化和配置的请求抽象与选项模型，支持基础地址、超时、请求头、查询参数、请求体及响应解析策略。
- 建立全局和单请求两级协程中间件链，支持拦截、挂起等待、恢复、短路终止和请求上下文传递。
- 将网络请求结果、下载与上传进度、SSE 事件及终态错误统一建模为可取消的 Flow 事件流，支持链式转换和收集端生命周期管理。
- 支持 JSON、表单、Multipart 文件上传、断点安全的文件下载、Protocol Buffers、二进制载荷及 Server-Sent Events。
- 补充覆盖中间件顺序、取消传播、请求体编码、进度报告、SSE 断连和错误映射的自动化测试及中文 API 注释。

## 能力

### 新增能力

- `network-client-configuration`：创建和配置基于 Retrofit 3 的可复用网络客户端及请求选项。
- `network-request-middleware`：按确定顺序执行可挂起、等待和短路的全局与请求级中间件。
- `flow-network-execution`：以可取消的 Kotlin Flow 事件流执行请求、传递结果并支持链式处理。
- `network-payload-transfer`：处理 JSON、表单、文件上传下载及进度、Protocol Buffers、二进制和 SSE 传输。

### 修改能力

- 无。

## 影响

- 受影响模块：`app` 模块新增 `network` 基础设施包、应用初始化入口及测试源码。
- 受影响依赖：新增 Retrofit 3、OkHttp、协程 Flow 适配、序列化/转换器及 MockWebServer 测试依赖；保留现有 RxJava 3，避免影响本地数据逻辑。
- 对外 API：新增网络客户端、请求描述、事件流和中间件 SPI；暂不修改现有页面或数据库 API。
