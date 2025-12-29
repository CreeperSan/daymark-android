import 'package:dio/dio.dart' as Dio;


class ResponseType {
  Dio.ResponseType type;

  ResponseType(this.type);

}

class ResponseTypes {

  static final json = ResponseType(Dio.ResponseType.json);

  static final stream = ResponseType(Dio.ResponseType.stream);

  static final plain = ResponseType(Dio.ResponseType.plain);

  static final bytes = ResponseType(Dio.ResponseType.bytes);

}
