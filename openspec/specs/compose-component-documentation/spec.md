# compose-component-documentation Specification

## Purpose

将组件库的设计令牌、组件清单、参数扩展点、状态行为、使用场景和示例用法沉淀为工程内长期可维护的参考文档，帮助人类和 AI 按同一契约使用与扩展组件。

## Requirements

### Requirement: Component catalog documentation

文档 MUST 列出全部 20 类组件及其 `CS` 前缀名称、用途、默认行为、主要参数、状态回调、可插槽区域和典型使用场景，并标注哪些参数用于业务扩展、哪些参数用于视觉覆盖。

#### Scenario: Developer selects a component

- **WHEN** a developer reads the catalog for a UI need
- **THEN** the document explains the matching component, its intended scenario, and a minimal usage example

### Requirement: Design token and accessibility guidance

文档 MUST 记录颜色、排版、间距、圆角、阴影、动效、响应式布局、触控热区和 reduced-motion 规则，并说明如何通过主题或参数覆盖而不破坏可读性和无障碍行为。

#### Scenario: Developer customizes a component

- **WHEN** a developer needs a brand or screen-specific variation
- **THEN** the document identifies the supported extension point and the constraints that must remain unchanged

### Requirement: Documentation stays aligned with implementation

文档 MUST 使用实际导出的 API 名称和可编译的示例；当组件 API、状态语义或设计令牌发生变化时，相关文档 MUST 在同一变更中更新，禁止保留已删除或未实现的入口。

#### Scenario: API changes are reviewed

- **WHEN** a component is added, renamed, or its public parameter contract changes
- **THEN** the component documentation includes the same public name and updated example before the change is considered complete
