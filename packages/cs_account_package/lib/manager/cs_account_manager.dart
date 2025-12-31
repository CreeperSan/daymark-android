
import 'package:cs_network_package/manager/network_manager.dart';

class CSAccountManager {
  final String app;
  final NetworkManager networkManager = NetworkManager();


  /// 构造函数
  CSAccountManager(this.app) {
    networkManager.setBaseUrl('https://account.creepersan.com/');
  }


  /// 设置基础地址
  /// [url] 基础地址
  void setBaseUrl(String url) {
    networkManager.setBaseUrl(url);
  }



  Future login(String username, String password) {

  }

  // Future logout() {
  //
  // }

}
