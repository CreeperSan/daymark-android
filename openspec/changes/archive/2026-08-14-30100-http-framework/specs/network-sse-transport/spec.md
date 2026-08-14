## Purpose

为服务端事件流提供可取消、可解码、可恢复的长连接能力，同时保持消息顺序、心跳语义和连接资源边界清晰。

## ADDED Requirements

### Requirement: SSE 必须按事件帧增量解析
系统 SHALL 将 SSE 字节流按 UTF-8 文本行解析为事件帧，支持 event、id、retry、多个 data 字段、注释心跳和空行提交边界。

#### Scenario: 合并多个 data 字段
- **WHEN** 一个 SSE 事件包含多个 data 行并以空行结束
- **THEN** 系统将 data 行按协议规则合并为一个有序事件后交付

#### Scenario: 心跳注释
- **WHEN** 服务端发送以冒号开头的注释行
- **THEN** 系统维持连接但不向业务交付普通消息

### Requirement: SSE 必须支持类型化消息和长连接生命周期
系统 SHALL 支持将原始事件数据解码为调用方指定类型，并以 Flow 交付开始、消息、连接错误、完成或取消事件；消息顺序不得改变。

#### Scenario: 连续接收事件
- **WHEN** SSE 连接持续接收多个合法事件
- **THEN** 系统按服务端顺序逐条交付类型化消息，不等待连接关闭

### Requirement: SSE 重连必须受策略约束
系统 SHALL 支持可关闭或受限的指数退避重连；重连时可携带最近成功事件的 Last-Event-ID，并且不得对明确不可恢复的 HTTP 状态无限重连。

#### Scenario: 网络断开后恢复
- **WHEN** SSE 因可恢复连接错误断开且重连策略仍有额度
- **THEN** 系统按退避策略重连，并携带最近事件标识

#### Scenario: 调用方取消 SSE
- **WHEN** 调用方取消 SSE Flow
- **THEN** 系统立即停止重连、关闭响应体和底层连接，并结束为取消
