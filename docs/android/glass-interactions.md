# Android 新版玻璃交互

## 边缘回弹

普通滚动容器在滚动修饰符前加 `glassBounce()`。惰性列表直接传入同一个修饰符。

```kotlin
Column(Modifier.glassBounce().verticalScroll(rememberScrollState())) {
    // 这里放普通滚动内容。
}

LazyColumn(Modifier.glassBounce()) {
    // 这里放列表条目。
}
```

横向容器传入 `glassBounce(Orientation.Horizontal)`。一个滚动轴上的嵌套容器只给需要回弹的滚动节点加修饰符。标题、工具栏和悬浮按钮放在滚动节点外面，滚到边缘时仍保持固定。

`GlassPage` 和 `GlassScrollingPage` 已关闭 Compose 默认的边缘拉伸。独立页面接入时，在滚动容器外用 `CompositionLocalProvider(LocalOverscrollConfiguration provides null)` 关闭默认效果。当前 Compose 版本使用该配置时需要 `@OptIn(ExperimentalFoundationApi::class)`。

## 点击反馈

用 `glassClickable` 替换 `clickable`，保留 `enabled`、`role` 和 `onClickLabel` 等参数。按下用 70 ms 淡到 56% 不透明度，抬起或取消后用 180 ms 恢复。

```kotlin
Box(
    Modifier.glassClickable(enabled = canSave, role = Role.Button, onClick = onSave)
        .background(backgroundColor, shape)
        .padding(16.dp)
) {
    Text(label)
}
```

将 `glassClickable` 放在背景、边框之前，整个控件会一起变淡。放在背景之后时，只淡化后续绘制的内容。

## 滚动页与日期选择器

`GlassScrollingPage` 与内部滚动容器应共用同一个 `ScrollState` 或 `LazyListState`。标题和系统导航栏的渐隐底色会在滚动边界变化时自动淡入淡出。

表单根据当前历法调用 `GlassSolarDatePicker` 或 `GlassLunarDatePicker`。确认回调返回 `CalendarDate`，其中月份为正数，闰月由 `leap` 单独表示。农历滚轮会把闰月排在同名平月后面，换年时若该闰月消失，则选回同名平月。
