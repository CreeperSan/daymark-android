## Context

现有认证 Activity 仅为空壳，账号服务接口位于独立的 `account.creepersan.com` Host 下，现有网络基础设施以 `NetworkClient`、`NetworkRequest` 和 Flow 事件流为核心，并已提供 Retrofit、Gson、OkHttp 与 DataStore 依赖。`CSNavigator` 尚未实现，应用中已有 Activity Result 和 RxJava 3 使用场景可作为导航桥接参考。

设计稿提供 375px 移动优先的欢迎/认证视觉体系，认证模块需要覆盖登录、注册、邮箱验证码和找回密码页面。后端当前明确提供登录、注册可用性检查、注册验证码、注册、Token 刷新和 Token 续期接口，但没有提供密码重置接口。

## Goals / Non-Goals

**Goals:**

- 建立可替换 Host 的账号服务客户端和完整的认证请求模型。
- 使用现有网络基础设施完成认证请求、错误映射、取消和 Token 持久化。
- 还原设计稿的登录、注册、验证码和找回密码页面，并复用已有 `CS` 组件。
- 用受控 Compose 状态管理输入、加载、错误、验证码倒计时和页面结果。
- 为 Activity Result 建立类型化的 RxJava 3 `Observable` 导航桥接。

**Non-Goals:**

- 不实现后端未提供的第三方登录服务。
- 不伪造密码重置接口；找回密码页面只实现设计稿、校验、返回登录和可替换的恢复动作。
- 不改造现有业务页面的导航体系，也不引入新的网络框架或全局事件总线。
- 不把认证页面依赖到网络层内部，网络基础设施继续保持不依赖 RxJava 和 Compose。

## Decisions

### 1. 使用独立账号服务配置和客户端

在网络配置层增加账号服务 Host/Client 工厂，默认使用 `https://account.creepersan.com/`，测试时可注入完整的替代 base URL。认证 API 使用相对路径，避免把 Host 拼接散落到请求代码中。

**Alternatives considered:** 修改全局 `NetworkClient` 默认 Host 会影响其他服务；在每个请求中写完整 URL 会降低可测试性并违反现有相对路径约束，因此不采用。

### 2. 认证 API 使用声明式 Retrofit DTO，业务层通过 Flow 适配

用 Retrofit 接口描述六个后端路径，DTO 映射 `code/message/data` 响应包和各接口数据；Repository 将 Retrofit 调用结果转换为页面可消费的成功、参数错误、服务错误和取消状态，并在 Repository 层保存成功 Token。

**Alternatives considered:** 直接在页面中构造 `NetworkRequest` 会混合传输和业务状态；将 RxJava 引入网络基础设施会破坏现有边界校验，因此只在业务/导航层使用 RxJava。

### 3. 页面按设计稿拆分为可复用认证状态与页面

登录页使用账号/邮箱、密码和显隐切换；注册页使用昵称映射为后端 `account` 字段、邮箱、密码、协议勾选；验证码页复用 `CSOtpInput` 并固定六位；找回密码页保留邮箱入口但不调用不存在的重置接口。页面状态使用 `rememberSaveable` 或页面级 ViewModel 提升，网络请求在生命周期结束时取消。

**Alternatives considered:** 为每个输入创建独立隐藏状态会造成重组时值不一致；直接使用 Material 默认 TextField/Button 与设计稿差异明显，因此使用现有 `CSInput`、`CSButton`、`CSOtpInput` 和 token。

### 4. 使用 Activity Result 作为唯一页面结果来源，再桥接为 Observable

`CSNavigator` 为每个页面定义参数/结果模型和明确的取消结果。Observable 在订阅时注册 `ActivityResultLauncher` 并启动页面，在一次结果或宿主销毁时完成；Disposable 负责注销回调和取消等待。页面内部通过统一 close API 写入结果，重复关闭只处理第一次。

**Alternatives considered:** 用全局 EventBus 传递页面结果会造成生命周期和泄漏风险；仅返回 `Intent` 会把 Bundle 键暴露给业务调用方，因此不采用。

### 5. 社交登录和密码找回采用显式未支持状态

设计稿中的社交按钮保留视觉位置和无障碍说明，但在没有对应后端契约时置为不可用或显示“暂未开放”；找回密码提交同样显示可理解的未支持提示。这样避免客户端向不存在的路径发请求或给用户制造成功假象。

## Risks / Trade-offs

- **[Risk] 后端响应包字段或 Header 约定未完全包含在提供的 handler 文件中** → 集中定义可配置响应解码和 Header provider，在联调失败时只调整 API 适配层。
- **[Risk] 注册设计稿包含昵称而接口只接受 account** → 首版将昵称作为 account 提交，并在字段旁使用账号语义；若后端新增独立昵称字段，只需扩展 DTO。
- **[Risk] RxJava Observable 与 Activity Result 生命周期耦合** → Observable 使用一次性结果、原子完成标记和 Disposable 清理，页面销毁时主动取消。
- **[Risk] 认证 Token 过期或刷新并发** → Token 存储和刷新入口集中在认证 Repository，页面只消费结果，不自行拼装刷新请求。
- **[Risk] 375px 设计稿在小屏或系统字体放大时溢出** → 认证根布局使用可滚动容器、输入与按钮使用约束宽度、文本允许换行，并保留最小触控区域。

## Migration Plan

1. 先新增账号网络模型、Host 配置、Repository 和 Token 存储，不改变现有业务请求。
2. 实现页面与导航模型，更新 Manifest 中认证 Activity 的入口。
3. 添加 API 替代 Host、页面状态和导航结果测试，再进行 Compose 编译与设备验证。
4. 如需回滚，移除认证页面调用和账号服务客户端；保留通用网络配置与导航接口不会影响现有页面。

## Open Questions

- 后端是否存在未提供的密码重置/验证码接口；在接口契约补充前，找回密码页面保持未支持动作。
- 账号服务要求的 `App`、设备名、设备码和平台 Header 的最终客户端值，需要联调时由现有应用身份配置确认。
