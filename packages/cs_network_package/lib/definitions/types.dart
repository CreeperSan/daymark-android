
/// 网络请求发送进度回调
typedef ActionNetworkSendProgress = void Function(int sentBytes, int totalBytes);

/// 网络请求接收进度回调
typedef ActionNetworkReceiveProgress = void Function(int receivedBytes, int totalBytes);
