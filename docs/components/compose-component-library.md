# Daymark Compose 组件库

Daymark Compose 组件库以 `CS` 为公共组件前缀，基于 Jetpack Compose Material 3，视觉来源于：

- [`docs/design/design-spec.html`](../design/design-spec.html)
- [`docs/design/component-library.html`](../design/component-library.html)

独立展示入口为 `CSComponentShowcaseActivity`，用于查看全部组件和交互状态。所有组件源码位于
`app/src/main/java/com/creepersan/memorialday/widget/composable/`，每个组件家族各自维护一个文件。

## 快速开始

```kotlin
setContent {
    MemorialDayTheme {
        CSComponentShowcaseScreen()
    }
}
```

业务页面可以直接使用组件，状态建议由页面或 ViewModel 提升：

```kotlin
var progress by rememberSaveable { mutableFloatStateOf(0.68f) }

CSProgressBar(
    progress = progress,
    variant = CSProgressVariant.Linear,
    showLabel = true
)
```

## 组件目录与扩展点

| 类别 | 公共入口 | 适用场景 | 主要扩展 |
| --- | --- | --- | --- |
| 文本 | `CSTextDisplay`, `CSTextTitle`, `CSTextSectionTitle`, `CSTextCardTitle`, `CSTextBody`, `CSTextBodySmall`, `CSTextHint`, `CSTextEmphasis` | 页面标题、卡片、正文、提示和强调数字 | `modifier`, `color`, `style`, `maxLines`, `overflow`, `softWrap` |
| 按钮 | `CSButton`, `CSButtonSecondary`, `CSButtonText`, `CSButtonLoading` | 主操作、编辑、取消、加载和图标操作 | `enabled`, `small`, `fullWidth`, `leadingIcon`, `trailingIcon`, `content` |
| 输入框 | `CSInput` | 普通、搜索、密码、错误输入 | `label`, `placeholder`, `supportingText`, `isError`, `readOnly`, `leadingContent`, `trailingContent`, 键盘参数 |
| 验证码 | `CSOtpInput` | 数字、字母或自定义字符验证码 | `length`, `spacing`, `inputType`, `allowedCharacters`, `mask`, `enabled`, `isError`, `slotContent`, 状态提升 |
| 开关 | `CSSwitch` | 布尔设置项 | `checked`, `onCheckedChange`, `enabled`, `contentDescription` |
| 单选/复选 | `CSRadioButton`, `CSCheckbox` | 互斥选项和多选项 | 选中值、标签、`enabled`、自定义内容 |
| 卡片 | `CSCard` | 任意内容卡片 | `content`, `onClick`, `shape`, `elevation`, `modifier` |
| 列表 | `CSList` | 稳定 key 的可滚动列表 | `items`, `key`, `itemContent`, `header`, `footer`, `emptyContent`, `onItemClick` |
| 标题栏 | `CSNavigationBar` | 标准、透明、渐变和柔化标题栏 | `leftContent`, `centerContent`, `rightContent`, `height`, `CSNavigationBackground` |
| 进度条 | `CSProgressBar` | 长条/环形进度 | `CSProgressStyle`, `secondaryProgress`, `showPercent`, `colors`, `completedOverlay` |
| 分割线 | `CSDivider` | 实线、虚线和带标签分割 | `CSDividerVariant`, `label`, `thickness`, `color` |
| Toast | `CSToast`, `CSToastHost` | 全局消息、加载反馈和自定义提示 | `CSToastType`, `CSToastState`, 位置、持续时间和主动关闭 |
| 模态框 | `CSModal` | 居中确认和底部抽屉 | `CSModalVariant`, `visible`, `onDismissRequest`, 标题、按钮和内容插槽 |
| 加载 | `CSLoading` | Spinner、Dots、带文字加载 | `CSLoadingVariant`, `size`, `label`, `reducedMotion` |
| 通知 | `CSNotification` | 页面内提醒、可关闭通知 | `CSNotificationType`, 图标、操作和关闭回调 |
| 浮岛导航 | `CSPillNavigation` | 手机底部浮岛导航 | `CSPillNavigationItem`, `selectedIndex`, 中央操作、描述 |
| 骨架屏 | `CSSkeleton`, `CSSkeletonCard` | 网络加载占位 | `shape`, `color`, `highlightColor`, `animated` |
| 空状态 | `CSEmptyState` | 空列表和首次使用 | 插画、标题、说明、操作和内容插槽 |
| 错误状态 | `CSErrorState` | 网络或数据加载失败 | 错误说明、插画、重试操作和内容插槽 |
| 轮播图 | `CSCarousel` | 纪念日横向卡片和分页展示 | `items`, `key`, `currentIndex`, `onIndexChange`, 自动播放、指示器和 item 插槽 |

## 常用示例

### 按钮与输入

```kotlin
if (isSaving) {
    CSButtonLoading(text = "保存中")
} else {
    CSButton(text = "保存", fullWidth = true, onClick = onSave)
}

CSInput(
    value = name,
    onValueChange = onNameChange,
    label = "纪念日名称",
    placeholder = "给这个日子起个名字",
    supportingText = errorMessage,
    isError = errorMessage != null
)
```

### Toast

```kotlin
val toastState = CSRememberToastState()

CSButton("保存", onClick = {
    toastState.show(CSToastData("保存成功", CSToastType.Success))
})
CSToastHost(state = toastState)
```

### 空态与错误态

```kotlin
if (items.isEmpty()) {
    CSEmptyState(
        title = "还没有纪念日",
        description = "记录你的第一个重要日子",
        action = { CSButton("新建纪念日", onClick = onCreate) }
    )
} else if (loadError != null) {
    CSErrorState(
        title = "网络开小差了",
        description = loadError,
        retryAction = { CSButton("重新加载", onClick = onRetry) }
    )
}
```

## 设计令牌

| 令牌 | 默认值 |
| --- | --- |
| 主色 | Terracotta `#D9622E` |
| 辅助色 | Sage `#4A7A51` |
| 页面背景 | Warm Gray `#FAF6F1` |
| 主要文字 | `#2A241D` |
| 分类色 | Rose `#E8675A`、Amber `#E8A838`、Teal `#3B8B8E`、Lavender `#8B76B5` |
| 间距 | 4 / 8 / 12 / 16 / 20 / 24 / 32dp |
| 圆角 | 4 / 6 / 10 / 16 / 20 / 28dp，药丸使用 full |
| 触控热区 | 最小 44dp |
| 动效 | 标准 250ms、强调 400ms、退出 150ms |

组件默认从 `CSTheme.colors`、`CSTheme.motion` 和 `CSSpacing` 等令牌读取。页面级品牌变化优先通过 `CSTheme` 或组件的显式参数覆盖，不要在业务页面复制颜色和尺寸常量。

## 状态、鲁棒性与无障碍

- 文本、输入、OTP、开关、选择、弹窗和轮播状态由调用方持有；组件回调是唯一状态更新入口。
- `CSProgressBar` 会把进度限制在 `0f..1f`。
- `CSCarousel` 会把索引限制在当前 item 范围；空列表不启动滚动或自动播放。
- 列表和轮播优先提供稳定 `key`，避免重组时错位。
- 交互组件保留至少 44dp 有效触控区域，并设置角色、选中态、错误描述和内容描述。
- `CSTheme(reducedMotion = true)` 会关闭非必要动效；加载和骨架屏退化为静态但仍保留状态提示，轮播停止自动播放。
- 所有组件内部点击区域都使用无 ripple 的交互修饰符；禁用、选中、焦点和语义仍会清晰反馈。
- `CSToastHost` 是页面根布局；Toast 默认居中，以内容宽度显示半透明黑色圆角背景，支持队列/替换策略、缩放淡入淡出、加载态外部取消以及 `CSToastData.content` 自定义内容。
- `CSOtpInput` 使用一个隐藏输入框统一处理光标、键盘、粘贴和删除，展示格仅渲染状态。`CSOtpInputType` 可限制为数字、字母、字母数字或调用方给定的自定义字符集合。
- `CSCarousel` 基于分页器，停止滑动时始终停在单独的完整页面。

## 交互与布局配置

- `CSButton`、`CSButtonSecondary` 和 `CSButtonText` 在按下时以 50ms 过渡至 50% 透明度，松开后在 200ms 内恢复；`CSButtonLoading` 使用持续旋转的圆形指示器。
- `CSOtpInput` 只保留一个不可选择的隐藏输入目标。焦点、输入法移动和输入变化都会将光标复位到末尾；视觉焦点始终位于下一个待输入格，填满后停留在最后一格。
- `CSSwitch` 的拇指位置和轨道渐变会在 300ms 内过渡；`CSProgressBar` 的主/次级进度也以 300ms 动画变化。
- `CSToastData.position` 支持九宫格位置，默认 `CSToastPosition.Center`。Toast 使用 80% 到 100% 的缩放与透明度联合过渡，不提供视觉关闭按钮；调用 `CSToastState.dismiss()`/`cancel()` 手动关闭当前消息，或调用 `clearAll()` 清空当前消息及队列。
- `CSModal.actionArrangement` 支持 `Horizontal`、`Vertical` 和 `Auto`。自动模式会在可用宽度足够时横排，否则纵排。
- `CSCarousel` 通过 `pageSpacing` 控制分页间距；使用 `CSCarouselIndicatorPosition.Inside(anchor)` 或 `Outside(edge, alignment)` 配置轮播图内外的指示器位置。
- `CSList` 和 `CSCarousel` 关闭平台边缘波纹，并在触及边界时使用更明显的弹性位移后回弹。
- 长文本默认允许换行；需要截断时通过 `maxLines` 和 `overflow` 明确指定。
- 禁用或只读组件不得触发回调，也不得因状态切换抛出异常。

## 展示与验收清单

`CSComponentShowcaseActivity` 按设计稿顺序展示：

1. 文本
2. 按钮
3. 输入框
4. 验证码框
5. 开关
6. 单选/复选
7. 卡片
8. 列表
9. 标题栏
10. 进度条
11. 分割线
12. Toast
13. 模态框
14. 加载
15. 通知
16. 浮岛药丸导航
17. 骨架屏
18. 空状态
19. 错误状态
20. 轮播图

新增或修改公共组件时，必须同步更新组件实现、展示实例和本文档，并确认所有公共组件入口均以 `CS` 开头。
