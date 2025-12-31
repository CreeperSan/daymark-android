
import 'dart:async';

import 'package:flutter/material.dart';

class CSToast {

  CSToast._();

  /// 全局导航 Key
  static GlobalKey<NavigatorState>? _navigatorKey;

  /// 当前显示的 OverlayEntry
  static OverlayEntry? _overlayEntry;

  /// 用于计算Toast显示与消失的计时器
  static Timer? _timer;

  /// Toast 队列

  
  /// 初始化 Toast 工具类
  static void init(GlobalKey<NavigatorState> navigatorKey) {
    _navigatorKey = navigatorKey;
  }
  
  
  /// 显示 Toast
  /// [toastWidget] 要显示的 Toast 组件
  /// 返回值为 Toast 的唯一 ID，可用于取消该 Toast
  static void show(Widget toastWidget) {
    final navigatorKey = _navigatorKey;
    if (navigatorKey == null) {
      throw Exception('CSToast 未初始化，请先调用 CSToast.init() 方法进行初始化');
    }
    // 如果已有 Toast 在显示，先移除它
    dismiss();
    // 创建新的 OverlayEntry
    _overlayEntry = OverlayEntry(
      builder: (ctx) {
        return IgnorePointer(
          child: Align(
            alignment: Alignment.center,
            child: Material(
              color: Colors.transparent,
              child: Center(
                child: toastWidget,
              ),
            ),
          ),
        );
      },
    );
    // 将 OverlayEntry 插入到 Overlay 中
    navigatorKey.currentState?.overlay?.insert(_overlayEntry!);
    // 设置定时器，3 秒后移除 Toast
    _timer = Timer(Duration(seconds: 3), () {
      _overlayEntry?.remove();
      _overlayEntry = null;
      _timer = null;
    });
  }
  

  /// 显示信息类 Toast
  /// [message] 要显示的信息内容
  /// 返回值为 Toast 的唯一 ID，可用于取消该 Toast
  static void info(String message) {
    show(Container(
      margin: EdgeInsets.all(36),
      padding: EdgeInsets.symmetric(
        horizontal: 16,
        vertical: 8,
      ),
      decoration: BoxDecoration(
        color: Colors.black.withAlpha((255 * 0.8).toInt()),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(message,
        style: TextStyle(
          color: Colors.white,
          fontSize: 16,
          fontWeight: FontWeight.w400,
        ),
      ),
    ));
  }

  static void dismiss() {
    _overlayEntry?.remove();
    _overlayEntry = null;
    _timer?.cancel();
    _timer = null;
  }


}

