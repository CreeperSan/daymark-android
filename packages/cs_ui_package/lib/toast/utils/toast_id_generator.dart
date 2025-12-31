
class ToastIDGenerator {
  static int _currentID = 0;

  ToastIDGenerator._();

  static int generateID() {
    _currentID += 1;
    return _currentID;
  }

}
