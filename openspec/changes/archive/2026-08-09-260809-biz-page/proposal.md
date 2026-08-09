## Why

认证流程完成后，应用仍缺少主要业务页面，用户无法管理纪念日、查看详情、记录时间线或配置提醒。设计稿已经明确了这些页面的交互和视觉结构，而后端当前仅覆盖纪念日基础接口，因此需要先建立可落地的页面、导航和网络契约，并为尚未提供服务端 API 的功能保留明确占位边界。

## What Changes

- 新增应用首页仪表盘，展示最近纪念日、倒计时、统计信息、即将到来列表和浮岛导航。
- 新增纪念日新建/编辑页，支持公历/农历、循环类型、分类、提醒配置、备注和时间线入口。
- 新增纪念日详情页，支持沉浸式 Hero、倒计时、时间线摘要、编辑、分享和删除。
- 新增设置、分类管理、提醒、权限说明、版本切换、新建/编辑分类页面。
- 新增 Mark 新建/编辑、纪念日时间线页面和时间线事件编辑页面。
- 封装 `daymark.creepersan.com` 可替换 Host、纪念日新增/编辑/删除/查询/列表/排序/刷新 API 及统一错误映射。
- 为 Mark、时间线、分类、提醒、版本切换等后端未提供的能力定义占位状态，不发送虚构请求。
- 扩展 `CSNavigator`，为所有业务页面提供类型化参数、可订阅结果和安全关闭行为。
- 优先复用工程内 Compose `CS` 组件，并保持页面可滚动、最小触控区域和无障碍语义。

## Capabilities

### New Capabilities

- `daymark-anniversary-network`: 纪念日服务 Host、API DTO、请求编排、错误映射和可替换测试服务。
- `daymark-business-pages`: 首页、纪念日 CRUD、详情、设置、分类、提醒、权限、版本和时间线相关页面行为。
- `daymark-placeholder-features`: 后端尚未支持的 Mark、分类、提醒、版本和媒体功能的占位行为与后续接入边界。

### Modified Capabilities

- `reactive-page-navigation`: 增加业务页面的类型化导航入口、返回结果和取消语义。
- `network-client-configuration`: 增加 Daymark 服务 Host 的独立配置和环境覆盖要求。

## Impact

- 影响 `app` 模块的 Activity、Compose 页面、`CSNavigator`、账号 Token 注入和网络模块。
- 新增 Daymark Retrofit/网络 DTO、Repository、Host 配置和 MockWebServer 测试。
- 需要在 AndroidManifest 注册新增 Activity，并接入现有登录成功后的主页入口。
- 不新增第三方 UI 框架；后端未实现的功能保持本地占位，不改变现有认证接口。
