## Purpose

为 Activity 页面提供统一的参数进入、结果回传和关闭契约，使页面跳转既能适配现有 Android Activity Result 生命周期，也能被 RxJava 3 调用方以可订阅方式组合。

## ADDED Requirements

### Requirement: Typed page entry and exit

导航 MUST 支持类型化的进入参数和退出结果；页面正常完成、取消、返回和异常关闭 MUST 能被调用方区分，且未返回结果时不得产生伪造成功值。

#### Scenario: Open a page with parameters

- **WHEN** a caller navigates with a typed parameter object
- **THEN** the destination receives the same parameter values without relying on untyped string conventions

#### Scenario: Return a typed result

- **WHEN** the destination finishes with a result
- **THEN** the caller receives the typed result exactly once

### Requirement: Observable navigation bridge

导航 MUST 提供返回 RxJava 3 `Observable` 的通用桥接方法，订阅建立后才发起页面进入；订阅取消或 Activity 销毁时 MUST 取消未完成的等待并释放回调注册。

#### Scenario: Subscribe to page completion

- **WHEN** a caller subscribes to a page navigation observable
- **THEN** the page launches and the observable emits one exit result or a distinct cancellation outcome before completing

#### Scenario: Dispose before the page closes

- **WHEN** the subscriber disposes while the destination remains open
- **THEN** the result callback is unregistered and no later emission reaches the disposed subscriber

### Requirement: Safe page close

导航 MUST 提供统一关闭入口，支持由页面返回结果、取消结果或无结果关闭；重复关闭、宿主已销毁和无效结果调用 MUST 安全忽略或返回明确错误，不得崩溃。

#### Scenario: Close a page twice

- **WHEN** a page calls its close action more than once
- **THEN** only the first valid close produces a result and the Activity finishes once

#### Scenario: Host activity is destroyed

- **WHEN** the host activity is destroyed before a result is returned
- **THEN** the navigation observer is completed or cancelled without retaining the Activity
