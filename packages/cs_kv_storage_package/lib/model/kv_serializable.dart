mixin KVSerializable {

  /// 将对象序列化为字符串
  String toSerializable();

  /// 从字符串反序列化对象
  void fromSerializable(String value);

}
