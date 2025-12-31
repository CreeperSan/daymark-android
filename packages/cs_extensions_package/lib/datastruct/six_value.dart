class SixValue<T, K, V, U, W, X> {
  final T first;
  final K second;
  final V third;
  final U fourth;
  final W fifth;
  final X sixth;

  SixValue(this.first, this.second, this.third, this.fourth, this.fifth, this.sixth);

  @override
  String toString() {
    return 'SixValue(first=$first, second=$second, third=$third, fourth=$fourth, fifth=$fifth, sixth=$sixth)';
  }
}
