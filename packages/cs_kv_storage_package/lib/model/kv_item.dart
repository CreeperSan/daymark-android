
import 'package:cs_kv_storage_package/model/kv_serializable.dart';

typedef DefaultValueGetter<T> = T Function();

class KVItem<T> {
  final String key;
  final T? defaultValue;
  final DefaultValueGetter? getDefaultValue;

  KVItem({
    required this.key,
    required this.defaultValue,
    required this.getDefaultValue,
  }) {
    // 检查泛型类型是否受支持
    assert(
      T == double || T == String || T == int || T == bool || T == KVSerializable,
      'KVItem<T> 的泛型类型不支持'
    );
    // 检查是否提供了默认值或默认值获取器
    assert(
      defaultValue != null || getDefaultValue != null,
      'KVItem<T> 必须提供 defaultValue 或 getDefaultValue'
    );
  }

}
