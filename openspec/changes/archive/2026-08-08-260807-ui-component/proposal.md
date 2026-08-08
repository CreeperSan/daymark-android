## Why

当前 Compose 组件仅有未完成的文本占位实现，设计规范中的颜色、排版、间距、状态和动效没有形成可复用的工程能力，导致页面实现容易重复、视觉不一致且难以维护。现在建立一套以 `CS` 为统一前缀、覆盖设计稿全部 20 类组件的组件库，可以为后续业务页面提供稳定的 API、统一的设计令牌和可视化验收入口。

## What Changes

- 新增基于 Jetpack Compose Material 3 的 `CS` 组件库，覆盖设计稿中列出的文本、按钮、输入框、验证码、开关、单选/复选、卡片、列表、标题栏、进度条、分割线、Toast、模态框、加载、通知、浮岛药丸导航、骨架屏、空状态、错误状态和轮播图。
- 建立可复用的 Daymark 设计令牌，包括色彩、语义色、分类色、渐变、排版、间距、圆角、阴影、触控热区和动效配置，并支持明暗主题及 reduced-motion 友好行为。
- 为交互组件提供完整状态、无障碍语义、尺寸/布局/插槽/样式覆盖等扩展参数；对输入、OTP、列表、轮播、弹窗和异步反馈提供稳定的状态管理边界。
- 创建独立的 Compose 组件展示实例，集中展示所有组件的默认态、交互态、边界态和典型使用场景，便于人工和自动验收。
- 沉淀组件目录、API 用法、扩展点、状态行为、设计令牌和选型建议文档，作为项目内后续 AI 与开发者的使用契约。

## Capabilities

### New Capabilities

- `compose-component-library`: 提供带 `CS` 前缀的 20 类 Compose 组件、设计令牌、可扩展 API、状态行为和鲁棒性约束。
- `compose-component-showcase`: 提供独立的 Compose 组件展示入口，覆盖所有组件和关键交互状态。
- `compose-component-documentation`: 提供与实际 API 同步的组件库使用、场景、扩展和设计令牌文档。

### Modified Capabilities

无。

## Impact

- 主要影响 `app/src/main/java/com/creepersan/memorialday/widget/composable/`、`ui/theme/` 和新增的组件展示 Activity/页面。
- 需要核对现有 Compose Material 3、Activity Compose、工具预览和测试依赖；不引入与项目现有架构冲突的 UI 框架。
- 组件 API 为新增公共能力；现有未完成的 `TextTitle` 等命名将统一迁移为 `CS` 前缀，避免留下不符合命名约束的入口。
- 不改变现有网络、数据库、存储和业务数据协议。
