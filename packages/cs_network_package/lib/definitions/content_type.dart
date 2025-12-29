
class ContentType {
  final String main;
  final String sub;

  ContentType([this.main = '*', this.sub = '*']);

  @override
  String toString() {
    return '$main/$sub';
  }
}


class ContentTypes {
  static final ApplicationJson = ContentType('application', 'json');
}
