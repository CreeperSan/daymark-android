class FourValue<T, K, V, U> {
  final T first;
  final K second;
  final V third;
  final U fourth;

  FourValue(this.first, this.second, this.third, this.fourth);

  @override
  String toString() {
    return 'FourValue(first=$first, second=$second, third=$third, fourth=$fourth)';
  }
}

