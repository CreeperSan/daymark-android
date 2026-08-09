## Why

当前登录、注册和找回密码页面只有空壳 Activity，认证网络请求也未接入，用户无法完成账号登录或注册。需要依据认证设计稿和现有账号服务接口建立完整的认证模块，并提供可观察的页面导航结果，方便后续业务页面复用和测试替换账号服务地址。

## What Changes

- 新增可配置账号服务 Host，默认指向 `account.creepersan.com`，并集中封装登录、注册检查、注册验证码、注册、Token 刷新和 Token 续期接口。
- 实现登录页、注册页、邮箱验证码页和忘记密码入口的 Compose UI，按 `auth-pages.html` 的暖光渐变、填充式输入框、按钮、验证码和页面间距还原。
- 将登录、注册、忘记密码、验证码和登录成功后的页面跳转接入现有 Activity 结构，并复用工程内 `CS` 组件库。
- 扩展 `CSNavigator`，提供页面进入与关闭的受控参数/结果模型，以及基于协程桥接为可订阅结果的通用导航能力。
- 为认证状态、Token 保存、加载/错误提示、输入校验、验证码倒计时和重复提交提供安全的状态处理。
- **BREAKING** 将认证页面从空壳 Activity 变为受控 Compose 页面，并要求调用方通过导航参数和结果模型传递页面输入与退出结果。

## Capabilities

### New Capabilities

- `account-auth-network`: 封装账号服务的认证 API、可替换 Host、请求模型、响应模型和 Token 生命周期请求。
- `account-auth-pages`: 实现登录、注册、邮箱验证码和忘记密码相关页面及认证状态交互。
- `reactive-page-navigation`: 提供统一的可观察页面进入、结果回传和页面关闭导航契约。

### Modified Capabilities

- `network-client-configuration`: 增加账号服务 Host 的独立配置和测试替换要求。

## Impact

- 影响 `app/src/main/java/com/creepersan/memorialday/activity/` 下登录、注册、忘记密码及新增验证码页面。
- 影响 `app/src/main/java/com/creepersan/memorialday/module/route/navigator/CSNavigator.kt` 和认证路由定义。
- 新增账号网络 API、认证 Repository/状态模型、Token 持久化和测试替换配置。
- 依赖现有 `NetworkClient`、Compose 组件库、应用主题与 Android Activity 生命周期；不引入新的网络框架。
- 后端接口路径依据 `creepersan.com/app/account/account.go`：`/api/login`、`/api/register/check_user_exist`、`/api/register/request_auth_code`、`/api/register`、`/api/token/refresh`、`/api/token/renew`。
