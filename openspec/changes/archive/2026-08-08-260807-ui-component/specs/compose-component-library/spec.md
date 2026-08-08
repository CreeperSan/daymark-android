## Purpose

为 Daymark 建立一套可复用、可扩展且状态完整的 Compose UI 组件契约，使业务页面能够在不重复实现视觉和交互细节的前提下，稳定复用设计规范中的 20 类组件。

## ADDED Requirements

### Requirement: Design token consistency

组件库 MUST 提供统一的 Daymark 设计令牌，覆盖主色、辅助色、中性色、语义色、四组分类色、渐变、排版层级、4px 间距阶梯、圆角、柔和阴影、图标尺寸、触控热区和动效时长。组件默认样式 MUST 来源于这些令牌，而不是散落的硬编码视觉值。

#### Scenario: Default component styling follows the design system
- **WHEN** a consumer renders any component without overriding visual parameters
- **THEN** the component uses the documented Daymark colors, typography, spacing, radius, elevation, and motion defaults

#### Scenario: Consumer overrides a supported token
- **WHEN** a consumer supplies a custom color, typography, shape, spacing, or animation parameter
- **THEN** only the targeted value changes and unrelated defaults remain intact

### Requirement: CS-prefixed public components

所有组件入口和组件变体 MUST 使用 `CS` 前缀。组件库 MUST 覆盖以下 20 类：`CSText` 文本、`CSButton` 按钮、`CSInput` 输入框、`CSOtpInput` 验证码框、`CSSwitch` 开关、`CSRadioButton`/`CSCheckbox` 单选复选、`CSCard` 卡片、`CSList` 列表、`CSNavigationBar` 标题栏、`CSProgressBar` 进度条、`CSDivider` 分割线、`CSToast` Toast、`CSModal` 模态框、`CSLoading` 加载、`CSNotification` 通知、`CSPillNavigation` 浮岛药丸导航、`CSSkeleton` 骨架屏、`CSEmptyState` 空状态、`CSErrorState` 错误状态和 `CSCarousel` 轮播图。

#### Scenario: Component inventory is complete
- **WHEN** a developer opens the component catalog or showcase
- **THEN** every listed category has at least one usable `CS`-prefixed composable and no category is represented only by a placeholder

#### Scenario: Naming rule is enforced
- **WHEN** a developer imports a public component entry point
- **THEN** its component name begins with `CS`, including text variants and featured/card variants

### Requirement: Extensible component APIs

每个组件 MUST 支持 `Modifier` 和无障碍语义；交互组件 MUST 暴露 `enabled`/`readOnly` 等可用性参数、状态回调和必要的内容插槽。列表、卡片、导航、通知、空态、错误态和轮播图 MUST 支持调用方注入内容、图标、操作或 item 渲染器，避免只能服务于纪念日这一种业务。

#### Scenario: Consumer customizes content and layout
- **WHEN** a consumer provides a modifier, custom slot content, or item renderer
- **THEN** the component keeps its state and accessibility behavior while rendering the supplied content and layout changes

#### Scenario: Disabled interaction is rendered safely
- **WHEN** an interactive component is disabled or read-only
- **THEN** it exposes the correct semantics, ignores disallowed input, and renders a distinct disabled/read-only visual state without throwing

### Requirement: Robust state and edge handling

输入、验证码、开关、选择、进度、加载、Toast、模态框、轮播和列表组件 MUST 支持状态提升或受控状态；组件 MUST 对空文本、超长文本、空列表、非法进度、越界索引、重复点击和快速状态切换进行安全处理，不得崩溃或产生越界访问。

#### Scenario: Invalid progress and carousel input are clamped
- **WHEN** progress is outside 0..1 or the requested carousel index is outside the item range
- **THEN** the component clamps progress or selects a valid fallback index and continues rendering

#### Scenario: Empty and large content remain usable
- **WHEN** a list is empty or a text/card value is much longer than the phone width
- **THEN** the component renders an intentional empty/wrapped/ellipsized state without layout overflow or exception

#### Scenario: State is controlled by the caller
- **WHEN** the caller updates a hoisted value or receives an interaction callback
- **THEN** the component reflects the new value on the next composition and does not maintain conflicting hidden state

### Requirement: Accessibility and touch targets

可交互组件 MUST 提供可配置或默认的语义标签、角色、选中态、进度描述和错误描述；按钮、输入、选择、导航、关闭和轮播控制的有效触控区域 MUST 不小于 44dp，验证码输入框 MUST 支持键盘、粘贴和删除行为。

#### Scenario: Screen reader can identify state and action
- **WHEN** an accessibility service focuses a button, input, switch, selection control, progress indicator, or navigation item
- **THEN** it receives the component label, role, current state, and action description appropriate to that component

#### Scenario: Minimum touch area is preserved
- **WHEN** a consumer supplies a small icon or compact visual style
- **THEN** the visual can shrink but the interactive semantics and hit target remain at least 44dp

### Requirement: Motion respects reduced motion

组件库 MUST 将按压、展开、加载、骨架 shimmer、轮播自动播放和导航选中动效集中配置，并在系统或调用方请求 reduced-motion 时停用非必要动画，同时保留状态变化和可操作性。

#### Scenario: Reduced motion is enabled
- **WHEN** reduced-motion is requested by the system or component configuration
- **THEN** non-essential transitions and automatic carousel motion are disabled while the final state remains visible

#### Scenario: Normal motion is enabled
- **WHEN** reduced-motion is not requested
- **THEN** components use the documented warm, short-duration motion defaults and do not start unbounded animation work for static states

### Requirement: Atomic source layout and interaction policy

The library MUST place public component families directly under `widget/composable/` in files named after the component family. It MUST NOT use a `cs` package directory or aggregate concern files such as `CSContent.kt` and `CSFeedback.kt`. All library-owned click targets MUST suppress platform ripple while retaining semantic, enabled, and selected feedback.

#### Scenario: Consumer locates a component
- **WHEN** a developer looks up `CSButton`, `CSInput`, or `CSToast`
- **THEN** its source is in a corresponding single-purpose file under `widget/composable/`

### Requirement: Corrected component contracts

Buttons MUST use dedicated APIs for materially different visual styles rather than an exposed variant enum. Cards MUST provide a reusable styled surface and caller content without category, icon, or business-specific properties. Inputs, OTP, switch, navigation, progress, modal, loading, and carousel MUST use custom visuals matching the design source rather than default Material component appearance.

#### Scenario: OTP field receives input
- **WHEN** the OTP control is focused, pasted into, or edited
- **THEN** exactly one hidden editable field owns focus and the visual cells only render its state

#### Scenario: Carousel settles after a gesture
- **WHEN** the user stops dragging a carousel
- **THEN** it snaps to exactly one page and never rests between pages

### Requirement: Root toast delivery

Toast state MUST support ordered queue and immediate replacement modes. The host MUST render at the root overlay with enter/exit motion, support loading/custom content, and optionally dismiss a loading toast when the user taps outside.

#### Scenario: Loading toast is replaced
- **WHEN** a caller displays a loading toast with replacement delivery policy
- **THEN** the active toast is replaced immediately, the loading content animates into the root overlay, and outside taps dismiss it only when configured
