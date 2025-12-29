import 'dart:async';

import 'package:daymark/app/daymark_app.dart';
import 'package:daymark/module/account_module.dart';
import 'package:daymark/module/device_module.dart';
import 'package:daymark/module/theme_module.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

void main() {
  // Dart 层异常捕获
  FlutterError.onError = (FlutterErrorDetails details) {
    print('Flutter Error: ${details.exceptionAsString()}');
  };

  // 全局状态初始化
  Get.put(ThemeModule());
  Get.put(DeviceModule());
  Get.put(AccountModule());

  // 未处理的 Dart 异常捕获
  runZonedGuarded(() {
    runApp(DaymarkApp());
  }, (Object error, StackTrace stackTrace) {
    print('Uncaught Dart Error: $error');
  });
}