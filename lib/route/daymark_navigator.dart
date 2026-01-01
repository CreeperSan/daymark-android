import 'package:daymark/route/daymark_router.dart';
import 'package:get/get.dart';

class DaymarkNavigator {

  DaymarkNavigator._();

  // 跳转到登录页面
  static void toLoginPage() {
    Get.toNamed(DaymarkRouter.loginPage.name);
  }

  // 跳转到主页面
  static void toHomePage() {
    Get.toNamed(DaymarkRouter.homePage.name);
  }

  // 跳转到注册页面
  static void toRegisterPage() {
    Get.toNamed(DaymarkRouter.registerPage.name);
  }

}
