import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:cs_ui_package/cs_ui_package_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelCsUiPackage platform = MethodChannelCsUiPackage();
  const MethodChannel channel = MethodChannel('cs_ui_package');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
