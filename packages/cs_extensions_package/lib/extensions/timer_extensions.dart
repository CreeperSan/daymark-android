
import 'dart:async';

extension TimerExtensions on Timer {

  void safeCancel() {
    if (isActive) {
      try {
        cancel();
      } catch (e) {
        print(e);
      }
    }
  }

}

