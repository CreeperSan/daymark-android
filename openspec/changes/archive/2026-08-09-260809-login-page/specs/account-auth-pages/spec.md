## Purpose

提供与认证设计稿一致的登录、注册、邮箱验证码和找回密码页面，让用户能够完成账号认证流程并在加载、错误、输入校验和页面返回时获得明确反馈。

## ADDED Requirements

### Requirement: Login page behavior

登录页 MUST 提供邮箱/账号输入、密码输入与显隐切换、登录操作、忘记密码入口、注册入口和第三方入口的视觉位置；登录操作 MUST 防重复提交并展示加载、成功和错误状态。

#### Scenario: Submit valid login credentials

- **WHEN** the user enters credentials and taps login
- **THEN** the page validates required fields, submits once, persists the returned token, and reports a successful login result

#### Scenario: Reject incomplete login input

- **WHEN** the user submits with a missing account/email or password
- **THEN** the page shows field-level guidance and does not issue a network request

#### Scenario: Login request fails

- **WHEN** the account service rejects the credentials or returns an error
- **THEN** the page stops loading, keeps entered values, and shows a user-readable error

### Requirement: Registration and verification flow

注册页 MUST 提供昵称/账号、邮箱、密码、协议勾选和注册操作；页面 MUST 先校验账号或邮箱占用情况，再请求注册验证码并进入六位邮箱验证码页。注册提交 MUST 使用后端所需的账号、邮箱、密码和验证码字段。

#### Scenario: Request a registration code

- **WHEN** the user enters a valid available email and requests a code
- **THEN** the page starts a countdown using the server expiration information and navigates to the verification page

#### Scenario: Enter and submit verification code

- **WHEN** the user enters exactly six valid code characters
- **THEN** the verification page enables confirmation and returns the email and code to the registration flow

#### Scenario: Register with unchecked terms

- **WHEN** the user submits registration without accepting the terms
- **THEN** the page blocks submission and explains that the terms must be accepted

#### Scenario: Registration fails

- **WHEN** the service reports an occupied account/email, invalid code, or other failure
- **THEN** the page stops loading and shows the server or validation message without clearing unrelated input

### Requirement: Password recovery boundary

找回密码页 MUST 按设计稿提供返回登录、邮箱输入和发送验证码的页面状态；在未提供密码重置后端接口前，客户端 MUST 不伪造不存在的请求路径，并 MUST 通过明确的未支持状态或可替换动作保留后续接入点。

#### Scenario: Open password recovery

- **WHEN** the user taps forgot password from login
- **THEN** the recovery page opens with an email field and return-to-login action

#### Scenario: Submit recovery email without reset API

- **WHEN** the user submits a recovery email while no reset endpoint is configured
- **THEN** the page reports that password recovery service is unavailable and does not send a fabricated request

### Requirement: Design fidelity and component reuse

认证页面 MUST 采用设计稿的暖光渐变背景、48dp 标识、28sp 主标题、15sp 描述、52dp 填充式输入框、全宽圆角主按钮、验证码单元格、分割线和底部链接布局；可复用交互优先使用工程内 `CS` 组件，并保留最小触控区域与无障碍语义。

#### Scenario: Render the login screen on a phone

- **WHEN** the login page is displayed on a narrow portrait device
- **THEN** content remains vertically scrollable, controls do not overflow, and the visual hierarchy matches the reference design

#### Scenario: Toggle password visibility

- **WHEN** the user taps the password visibility affordance
- **THEN** only password masking changes and the field value remains unchanged

### Requirement: Authentication state and lifecycle safety

页面 MUST 使用受控状态处理输入、倒计时、加载和错误；页面离开、旋转重组或快速重复点击不得导致重复提交、过期回调、倒计时泄漏或崩溃。

#### Scenario: Leave during submission

- **WHEN** the user leaves a page while an authentication request is active
- **THEN** the request collection is cancelled or ignored and no stale navigation or toast is emitted

#### Scenario: Re-enter an authentication page

- **WHEN** the user opens the same page again after a previous attempt
- **THEN** the page starts from a valid controlled state without reusing stale loading or countdown state unexpectedly
