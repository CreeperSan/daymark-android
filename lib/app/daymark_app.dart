
import 'package:daymark/route/daymark_router.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class DaymarkApp extends StatelessWidget {
  const DaymarkApp({super.key});

  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      title: 'Daymark App',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      initialRoute: DaymarkRouter.loginPage.name,
      getPages: [
        DaymarkRouter.loginPage,
        DaymarkRouter.homePage,
        DaymarkRouter.registerPage,
      ],
    );
  }
}
