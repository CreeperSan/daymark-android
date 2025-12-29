import 'package:daymark/route/daymark_router.dart';
import 'package:get/get.dart';

class DaymarkNavigator {

  DaymarkNavigator._();

  // 跳转到登录页面
  void toLoginPage() {
    Get.toNamed(DaymarkRouter.loginPage.name);
  }

  // 跳转到主页面
  void toHomePage() {
    Get.toNamed(DaymarkRouter.homePage.name);
  }

}
