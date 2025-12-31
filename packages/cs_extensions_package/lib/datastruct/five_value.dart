class FiveValue<T, K, V, U, W> {
  final T first;
  final K second;
  final V third;
  final U fourth;
  final W fifth;

  FiveValue(this.first, this.second, this.third, this.fourth, this.fifth);

  @override
  String toString() {
    return 'FiveValue(first=$first, second=$second, third=$third, fourth=$fourth, fifth=$fifth)';
  }
}

