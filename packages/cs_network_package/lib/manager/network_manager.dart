import 'package:cs_network_package/definitions/content_type.dart';
import 'package:dio/dio.dart';
import 'package:cs_network_package/definitions/request_method.dart';
import 'package:cs_network_package/definitions/types.dart';

class NetworkManager {

  final Dio dio = Dio();

  /// 基础地址
  String baseUrl = '';

  /// 全局请求头
  Map<String, dynamic> headers = <String, dynamic>{};


  /// 设置基础地址
  /// [url] 基础地址
  void setBaseUrl(String url) {
    baseUrl = url;
  }


  /// 设置全局请求头
  /// [key] 请求头键
  /// [value] 请求头值
  void setHeader(String key, dynamic value) {
    headers[key] = value;
  }


  /// 移除全局请求头
  /// [key] 请求头键
  void removeHeader(String key) {
    headers.remove(key);
  }


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
    // 合并请求地址
    String requestUrl = path;
    if (!path.startsWith('http://') && !path.startsWith('https://')) {
      requestUrl = baseUrl + path;
    }
    // 合并请求头
    final mergedHeaders = <String, dynamic>{};
    mergedHeaders.addAll(this.headers);
    if (headers != null) {
      mergedHeaders.addAll(headers);
    }
    // 网络请求
    return dio.request(requestUrl,
      queryParameters: query,
      data: data,
      options: Options(
        method: requestMethod.method,
        headers: mergedHeaders,
        responseType: responseType,
        contentType: contentType?.toString(),
        sendTimeout: sendTimeout,
        receiveTimeout: receiveTimeout,
        extra: extra,
      ),
    );
  }



}
