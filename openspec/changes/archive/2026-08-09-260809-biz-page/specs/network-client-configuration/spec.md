## MODIFIED Requirements

### Requirement: 网络客户端可显式配置和复用

系统 SHALL 允许调用方通过公开的构建入口创建命名或匿名网络客户端，并以不可变配置指定基础地址、连接与读写超时、默认请求头、响应转换策略和传输客户端。相同有效配置的客户端 SHALL 可被安全复用，且一个客户端的配置不得影响其他服务客户端。账号服务 Host MUST 能作为独立的服务级配置覆盖通用网络客户端基础地址，默认使用 `account.creepersan.com`；Daymark 服务 Host MUST 作为另一项独立服务级配置，默认使用 `daymark.creepersan.com`。两者均允许测试或不同部署环境替换而不影响其他服务客户端。

#### Scenario: 创建带默认请求头的客户端

- **WHEN** 调用方以基础地址、超时和默认认证头创建客户端
- **THEN** 该客户端发出的每个请求均携带默认认证头，并使用指定的超时

#### Scenario: 单请求覆盖客户端默认值

- **WHEN** 调用方为一次请求提供与客户端默认值同名的请求头或超时
- **THEN** 仅该请求使用显式提供的值，客户端后续请求保持原默认配置

#### Scenario: Account client uses its default host

- **WHEN** the account service is created without an environment override
- **THEN** its requests use the configured default account host and other service clients retain their own base addresses

#### Scenario: Account client uses a test host

- **WHEN** a test injects a replacement account host
- **THEN** only account requests use that host and the production account host is not contacted

#### Scenario: Daymark client uses a test host

- **WHEN** a test injects a replacement Daymark host
- **THEN** only Daymark requests use that host and account requests retain their configured account host
