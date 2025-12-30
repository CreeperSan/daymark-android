
class KVTypeUnsupportedException extends Error {
  final String message;

  KVTypeUnsupportedException(this.message);

  @override
  String toString() {
    return "KVTypeUnsupportedException($message)";
  }
}
