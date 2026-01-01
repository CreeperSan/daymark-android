
import 'dart:io';

class CSAccountParamsIncorrectException implements Exception {
  String message;

  CSAccountParamsIncorrectException([this.message = '参数错误']);

}

