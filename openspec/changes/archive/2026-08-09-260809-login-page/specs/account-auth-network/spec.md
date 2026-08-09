## Purpose

为客户端提供与账号服务一致的登录、注册、验证码和 Token 生命周期能力，使认证流程可以被页面和其他业务安全复用，并能在测试环境替换服务地址。

## ADDED Requirements

### Requirement: Account service endpoint configuration

客户端 MUST 将账号服务 Host 作为独立配置项提供，默认值为 `account.creepersan.com`，调用方 MUST 能在测试或不同部署环境中替换该 Host，而无需修改认证业务代码。

#### Scenario: Use the production account host

- **WHEN** the account client is created without an override
- **THEN** requests are sent to `account.creepersan.com` using the configured HTTPS base URL

#### Scenario: Replace the host for tests

- **WHEN** a test supplies a local or staging account Host
- **THEN** all account requests use the replacement Host and no request is sent to the production Host

### Requirement: Login and registration API contracts

客户端 MUST 提供登录、检查账号/邮箱是否存在、请求注册邮箱验证码和注册接口，并使用后端约定的 JSON 字段、相对路径和结构化成功/失败结果。登录请求 MUST 支持账号或邮箱与密码；注册请求 MUST 支持账号、邮箱、密码和验证码。

#### Scenario: Login with account credentials

- **WHEN** the caller submits a valid account and password
- **THEN** the client sends a request to `/api/login` and exposes the returned user and token data

#### Scenario: Check registration availability

- **WHEN** the caller submits account and/or email availability data
- **THEN** the client sends a request to `/api/register/check_user_exist` and exposes both account and email existence flags

#### Scenario: Request a registration code

- **WHEN** the caller submits a valid email
- **THEN** the client sends a request to `/api/register/request_auth_code` and exposes the server creation and expiration timestamps

#### Scenario: Complete registration

- **WHEN** the caller submits account, email, password, and a valid code
- **THEN** the client sends a request to `/api/register` and exposes the created account identity

### Requirement: Token lifecycle API contracts

客户端 MUST 提供刷新 Token 和续期 Token 的接口，并区分刷新令牌请求体与当前访问令牌请求头；认证失败 MUST 以可展示的结构化错误返回，不得伪装成成功结果。

#### Scenario: Refresh an expired access token

- **WHEN** the caller provides a valid refresh token and current authentication context
- **THEN** the client sends `/api/token/refresh` and exposes the replacement token

#### Scenario: Renew the current token

- **WHEN** the caller has a valid current token
- **THEN** the client sends `/api/token/renew` and exposes the renewed token

### Requirement: Authentication error and cancellation behavior

网络请求 MUST 暴露参数错误、认证失败、服务端错误、解析失败和用户取消等可区分状态；取消页面或请求时 MUST 停止后续状态回调，不得将取消报告为成功。

#### Scenario: Server rejects credentials

- **WHEN** the account service returns an authentication or parameter error
- **THEN** the caller receives the server message and no token is persisted

#### Scenario: Caller cancels an in-flight request

- **WHEN** the collecting scope or page leaves before the request completes
- **THEN** the transport is cancelled and no success or stale error state is emitted afterward
