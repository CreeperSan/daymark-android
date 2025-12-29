import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'cs_ui_package_method_channel.dart';

abstract class CsUiPackagePlatform extends PlatformInterface {
  /// Constructs a CsUiPackagePlatform.
  CsUiPackagePlatform() : super(token: _token);

  static final Object _token = Object();

  static CsUiPackagePlatform _instance = MethodChannelCsUiPackage();

  /// The default instance of [CsUiPackagePlatform] to use.
  ///
  /// Defaults to [MethodChannelCsUiPackage].
  static CsUiPackagePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [CsUiPackagePlatform] when
  /// they register themselves.
  static set instance(CsUiPackagePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
