
import 'package:daymark/pages/home/home_page.dart';
import 'package:daymark/pages/login/login_page.dart';
import 'package:daymark/pages/register/register_page.dart';
import 'package:get/get.dart';

class DaymarkRouter {

  DaymarkRouter._();


  static final GetPage homePage = GetPage(
    name: '/home/index',
    page: () => HomePage(),
  );

  static final GetPage loginPage = GetPage(
    name: '/user/login',
    page: () => LoginPage(),
  );

  static final GetPage registerPage = GetPage(
    name: '/user/register',
    page: () => RegisterPage(),
  );

}
