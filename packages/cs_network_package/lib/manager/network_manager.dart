import 'package:cs_network_package/definitions/content_type.dart';
import 'package:dio/dio.dart';
import 'package:cs_network_package/definitions/request_method.dart';
import 'package:cs_network_package/definitions/types.dart';

class NetworkManager {

  final Dio dio = Dio();

  String baseUrl = '';


  /// request
  /// 进行网络请求
  /// [path] 请求的路径
  Future request(RequestMethod requestMethod, String path, {
    Map<String, dynamic>? query,
    Object? data,
    Map<String, dynamic>? headers,
    Map<String, dynamic>? extra,
    ActionNetworkSendProgress? onSendProgress,
    ActionNetworkReceiveProgress? onReceiveProgress,
    ContentType? contentType,
    ResponseType? responseType,
    Duration? sendTimeout,
    Duration? receiveTimeout,
  }) {
    return dio.request(path,
      queryParameters: query,
      data: data,
      options: Options(
        method: requestMethod.method,
        headers: headers,
        responseType: responseType,
        contentType: contentType?.toString(),
        sendTimeout: sendTimeout,
        receiveTimeout: receiveTimeout,
        extra: extra,
      ),
    );
  }



}
