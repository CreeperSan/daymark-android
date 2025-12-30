
import 'package:cs_account_package/manager/cs_account_manager.dart';
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
      // 参数检查
      final username = usernameTextEditController.text.trim();
      if (username.isEmpty) {
        Get.snackbar('错误', '请输入账号');
        throw Exception('用户名为空');
      }
      final password = passwordTextEditController.text.trim();
      if (password.isEmpty) {
        Get.snackbar('错误', '请输入密码');
        throw Exception('密码为空');
      }
    }).then((value) {
      return Future.delayed(Duration(seconds: 2));
    }).catchError((err) {
      print(err);
    }).whenComplete(() {
      _isLoggingIn.value = false;
    });
  }

}
