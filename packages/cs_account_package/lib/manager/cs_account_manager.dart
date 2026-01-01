
import 'package:cs_account_package/exceptions/cs_account_params_incorrect_exception.dart';
import 'package:cs_network_package/cs_network_package.dart';
import 'package:cs_network_package/manager/network_manager.dart';

class CSAccountManager {
  final String app;
  final NetworkManager networkManager = NetworkManager();


  /// 构造函数
  CSAccountManager(this.app) {
    networkManager.setBaseUrl('https://account.creepersan.com/');
    networkManager.setHeader('CS-App', 'app');
    networkManager.setHeader('CS-Device-Platform', '');
    networkManager.setHeader('CS-Device-Code', '');
    networkManager.setHeader('CS-Device-Name', '');
    networkManager.setHeader('CS-Token', '');
    networkManager.setHeader('CS-Version', '');
  }


  /// 设置基础地址
  /// [url] 基础地址
  void setBaseUrl(String url) {
    networkManager.setBaseUrl(url);
  }


  /// 登录
  Future login(String username, String password) {
    return Future.value().then((_) {
      // 参数检查
      final realUsername = username.trim();
      final realPassword = password.trim();
      if (realUsername.isEmpty) {
        throw CSAccountParamsIncorrectException('用户名不能为空');
      }
      if (realPassword.isEmpty) {
        throw CSAccountParamsIncorrectException('密码不能为空');
      }
      // 发起请求
      return networkManager.request(RequestMethods.post, '/api/login',
        data: {
          'account': realUsername,
          'password': realPassword,
        },
      );
    });
  }

  // Future logout() {
  //
  // }

}
