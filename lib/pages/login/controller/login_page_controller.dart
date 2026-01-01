
import 'package:cs_account_package/manager/cs_account_manager.dart';
import 'package:cs_extensions_package/datastruct/two_value.dart';
import 'package:cs_ui_package/cs_ui_package.dart';
import 'package:daymark/common/error/cancel_interrupt.dart';
import 'package:daymark/common/error/interrupt.dart';
import 'package:daymark/route/daymark_navigator.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class LoginPageController extends GetxController {
  final usernameTextEditController = TextEditingController();
  final passwordTextEditController = TextEditingController();

  /// 是否正在登录中
  final _isLoggingIn = false.obs;
  bool get isLoggingIn => _isLoggingIn.value;

  @override
  void onInit() {
    super.onInit();
    // TODO 读取记住的用户名密码
  }

  @override
  void onClose() {
    usernameTextEditController.dispose();
    passwordTextEditController.dispose();
    super.onClose();
  }


  void onClickLogin() {
    if (isLoggingIn) {
      return;
    }
    _isLoggingIn.value = true;
    // 参数准备
    final accountManager = Get.find<CSAccountManager>();
    // 开始操作
    Future.value().then((_) {
      /* 参数检查 */
      final username = usernameTextEditController.text.trim();
      if (username.isEmpty) {
        throw Interrupt('请输入用户名');
      }
      final password = passwordTextEditController.text.trim();
      if (password.isEmpty) {
        throw Interrupt('请输入密码');
      }
      return TwoValue(username, password);
    }).then((valueBundle) {
      /* 网络请求 */
      final account = valueBundle.first;
      final password = valueBundle.second;
      return accountManager.login(account, password);
    }).then((value) {

    }).catchError((err) {
      String hintMessage = '登录失败，请稍后重试';
      if (err is Interrupt) {
        hintMessage = err.message;
      }
      CSToast.info(hintMessage);
    }).whenComplete(() {
      _isLoggingIn.value = false;
    });
  }


  /// 点击注册
  void onClickRegister() {
    DaymarkNavigator.toRegisterPage();
  }


  /// 点击忘记密码
  void onClickForgotPassword() {
    CSToast.info('忘记密码功能暂未实现');
  }

}
