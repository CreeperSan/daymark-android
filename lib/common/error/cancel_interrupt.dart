
import 'package:daymark/common/error/interrupt.dart';

class CancelInterrupt extends Interrupt {

  CancelInterrupt([super.message = '操作已取消']);

  @override
  String toString() {
    return 'CancelInterrupt($message)';
  }
}
