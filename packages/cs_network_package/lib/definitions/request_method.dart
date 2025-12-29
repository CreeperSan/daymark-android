
class RequestMethod {
  String method;

  RequestMethod(this.method);
}

class RequestMethods {

  RequestMethods._();

  static final get = RequestMethod('GET');
  static final post = RequestMethod('POST');
  static final put = RequestMethod('PUT');
  static final delete = RequestMethod('DELETE');
  static final patch = RequestMethod('PATCH');
  static final head = RequestMethod('HEAD');
  static final options = RequestMethod('OPTIONS');

}
