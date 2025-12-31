class ThreeValue<T, K, V> {
  final T first;
  final K second;
  final V third;

  ThreeValue(this.first, this.second, this.third);

  @override
  String toString() {
    return 'ThreeValue(first=$first, second=$second, third=$third)';
  }
}

