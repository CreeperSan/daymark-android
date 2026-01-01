import 'dart:async';

import 'package:cs_account_package/cs_account_package.dart';
import 'package:cs_ui_package/cs_ui_package.dart';
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
  Get.put(CSAccountManager('daymark')..setBaseUrl('http://account.test.com:8080/'));

  // 组件库初始化
  CSToast.init(DaymarkApp.navigatorKey);

  // 未处理的 Dart 异常捕获
  runZonedGuarded(() {
    runApp(DaymarkApp());
  }, (Object error, StackTrace stackTrace) {
    print('Uncaught Dart Error: $error');
  });
}