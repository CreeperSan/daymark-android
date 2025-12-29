import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'cs_ui_package_platform_interface.dart';

/// An implementation of [CsUiPackagePlatform] that uses method channels.
class MethodChannelCsUiPackage extends CsUiPackagePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('cs_ui_package');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
