// import 'package:flutter_test/flutter_test.dart';
// import 'package:cs_ui_package/cs_ui_package.dart';
// import 'package:cs_ui_package/cs_ui_package_platform_interface.dart';
// import 'package:cs_ui_package/cs_ui_package_method_channel.dart';
// import 'package:plugin_platform_interface/plugin_platform_interface.dart';
//
// class MockCsUiPackagePlatform
//     with MockPlatformInterfaceMixin
//     implements CsUiPackagePlatform {
//
//   @override
//   Future<String?> getPlatformVersion() => Future.value('42');
// }
//
// void main() {
//   final CsUiPackagePlatform initialPlatform = CsUiPackagePlatform.instance;
//
//   test('$MethodChannelCsUiPackage is the default instance', () {
//     expect(initialPlatform, isInstanceOf<MethodChannelCsUiPackage>());
//   });
//
//   test('getPlatformVersion', () async {
//     CsUiPackage csUiPackagePlugin = CsUiPackage();
//     MockCsUiPackagePlatform fakePlatform = MockCsUiPackagePlatform();
//     CsUiPackagePlatform.instance = fakePlatform;
//
//     expect(await csUiPackagePlugin.getPlatformVersion(), '42');
//   });
// }
