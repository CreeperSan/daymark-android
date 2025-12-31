
extension ListExtensions<T> on List<T> {

  T? firstOrNull() {
    if (isNotEmpty) {
      return first;
    }
    return null;
  }


  T? lastOrNull() {
    return isEmpty ? null : last;
  }


  T? firstWhereOrNull(bool Function(T element) test) {
    for (T element in this) {
      if (test(element)) return element;
    }
    return null;
  }

  T? lastWhereOrNull(bool Function(T element) test) {
    late T result;
    bool foundMatching = false;
    for (T element in this) {
      if (test(element)) {
        result = element;
        foundMatching = true;
      }
    }
    if (foundMatching) return result;
    return null;
  }

  T? safeGet(int index) {
    if (index < 0 || index >= length) {
      return null;
    }
    return this[index];
  }

  T safeGetOrDefault(int index, T defaultValue) {
    if (index < 0 || index >= length) {
      return defaultValue;
    }
    return this[index];
  }


  List<T> safeSublist(int start, [int? end]) {
    int actuallyStart = start < 0 ? 0 : start;
    int actuallyEnd = end ?? length;
    if (actuallyEnd > length) {
      actuallyEnd = length;
    }
    return this.sublist(actuallyStart, actuallyEnd);
  }

}
