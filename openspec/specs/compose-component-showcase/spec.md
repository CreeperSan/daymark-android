# compose-component-showcase Specification

## Purpose

提供一个独立、可进入且可滚动的 Compose 组件展示实例，集中验证组件库的默认样式、交互状态、边界状态和典型使用场景，而不依赖业务网络或数据库。

## Requirements

### Requirement: Standalone showcase entry

展示实例 MUST 能作为独立 Activity 或等价页面运行，使用组件库自身的主题和本地静态数据，不得要求登录、网络、数据库或业务 ViewModel 才能显示。

#### Scenario: Showcase opens without business dependencies

- **WHEN** a developer launches the showcase entry
- **THEN** the page renders with local sample data even when the network is unavailable and no user session exists

### Requirement: Complete component coverage

展示实例 MUST 按设计稿的基础组件、反馈提示和展示组件分组，展示全部 20 类组件、至少一个默认态、一个交互态或状态态，以及对关键扩展参数的示例。

#### Scenario: Every category is visible

- **WHEN** a developer scrolls through the showcase
- **THEN** each of the 20 component categories has a labeled demonstration and a visible `CS` component instance

#### Scenario: States and extension points are demonstrated

- **WHEN** a developer interacts with controls or views the examples
- **THEN** enabled/disabled、filled/error、selected/unselected、loading/success/failure、empty/non-empty and custom-slot examples are available where applicable

### Requirement: Safe interactive preview

展示实例 MUST 使用状态提升和稳定的本地示例数据；重复点击、快速切换、旋转重组或返回页面后再次进入 MUST 不导致崩溃、状态越界或泄漏后台动画。

#### Scenario: Showcase survives repeated interaction

- **WHEN** a developer rapidly toggles controls, changes carousel pages, opens and closes modal/toast, or edits OTP/input examples
- **THEN** the showcase remains responsive and displays valid state without exceptions
