
import 'package:cs_network_package/cs_network_package.dart';
import 'package:cs_network_package/manager/network_manager.dart';

class CsAccountService {

  CsAccountService._();


  static Future<void> login(NetworkManager manager, {
    String? account,
    String? email,
    required String password,
  }) {
    // FIXME: 待补充 - 登录请求
    return Future.value().then((value) {
      /* 构造请求参数 */
      final requestData = <String, dynamic>{};
      if (account != null) {
        requestData['account'] = account;
      }
      if (email != null) {
        requestData['email'] = email;
      }
      requestData['password'] = password;
      return requestData;
    }).then((requestData) {
      /* 发送请求 */
      return manager.request(RequestMethods.post, '/api/login',
        data: requestData,
      );
    }).then((response) {
      /* 处理响应 */
    });
  }


  static Future register(NetworkManager manager) {
    // FIXME: 待补充 - 注册请求
    manager.request(RequestMethods.post, '/api/register');
    throw UnimplementedError();
  }


  static Future registerRequestAuthCode(NetworkManager manager) {
    // FIXME: 待补充 - 注册请求
    manager.request(RequestMethods.post, '/api/register/request_auth_code');
    throw UnimplementedError();
  }


  static Future registerCheckUserExist(NetworkManager manager) {
    // FIXME: 待补充 - 注册请求
    manager.request(RequestMethods.post, '/api/register/check_user_exist');
    throw UnimplementedError();
  }


  static Future logout(NetworkManager manager) {
    // FIXME: 待补充 - 登出请求
    manager.request(RequestMethods.post, '/api/logout');
    throw UnimplementedError();
  }


  static Future tokenRenew(NetworkManager manager) {
    // FIXME: 待补充 - 登出请求
    manager.request(RequestMethods.post, '/api/token/renew');
    throw UnimplementedError();
  }


  static Future tokenRefresh(NetworkManager manager) {
    // FIXME: 待补充 - 登出请求
    manager.request(RequestMethods.post, '/api/token/refresh');
    throw UnimplementedError();
  }


}
