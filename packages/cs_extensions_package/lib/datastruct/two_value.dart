class TwoValue<T, K> {
  final T first;
  final K second;

  TwoValue(this.first, this.second);

  @override
  String toString() {
    return 'TwoValue(first=$first, second=$second)';
  }
}

