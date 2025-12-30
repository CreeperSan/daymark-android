
import 'package:cs_kv_storage_package/exceptions/kv_type_unsupported_exception.dart';
import 'package:cs_kv_storage_package/model/kv_serializable.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:cs_kv_storage_package/model/kv_item.dart';

class KVStorageManager {

  KVStorageManager._();

  /// SharedPreferences 实例
  static SharedPreferences? _prefs;


  /// 获取 SharedPreferences 实例
  static Future<SharedPreferences> _getPrefs() {
    if (_prefs == null) {
      return SharedPreferences.getInstance().then((prefs) {
        _prefs = prefs;
        return prefs;
      });
    } else {
      return Future.value(_prefs);
    }
  }


  /// 设置键值对
  /// [item] 键值对项
  /// [value] 要设置的值
  /// 返回 Future 对象
  static Future set<T>(KVItem<T> item, T value) {
    return _getPrefs().then((prefs) {
      if (value is int) {
        prefs.setInt(item.key, value);
      } else if (value is double) {
        prefs.setDouble(item.key, value);
      } else if (value is bool) {
        prefs.setBool(item.key, value);
      } else if (value is KVSerializable) {
        prefs.setString(item.key, value.toSerializable());
      } else if (value is String) {
        prefs.setString(item.key, value);
      } else {
        throw KVTypeUnsupportedException('不支持的类型: ${T.toString()}');
      }
    });
  }


  /// 获取键值对
  /// [item] 键值对项
  /// 返回对应类型的值
  static Future<T> get<T>(KVItem<T> item) {
    return _getPrefs().then((prefs) {
      dynamic value;
      if (T == int) {
        value = prefs.getInt(item.key);
      } else if (T == double) {
        value = prefs.getDouble(item.key);
      } else if (T == bool) {
        value = prefs.getBool(item.key);
      } else if (T == String) {
        value = prefs.getString(item.key);
      } else {
        // 尝试读取 String (针对 KVSerializable 存储的情况)
        // 注意：由于 KVItem 缺乏反序列化方法，读取出的 String 无法自动转回 KVSerializable 对象
        // 因此对于 KVSerializable，通常会走到下方的默认值逻辑
        value = prefs.getString(item.key);
      }

      // 检查值是否存在且类型匹配
      if (value != null && value is T) {
        return value;
      }

      // 返回默认值
      if (item.defaultValue != null) {
        return item.defaultValue!;
      } else if (item.getDefaultValue != null) {
        return item.getDefaultValue!();
      }

      throw KVTypeUnsupportedException('无法获取值且无默认值: ${item.key}');
    });
  }


  /// 移除键值对
  /// [item] 键值对项
  /// 返回 Future 对象，表示移除操作是否成功
  static Future<bool> remove(KVItem item) {
    return _getPrefs().then((prefs) {
      return prefs.remove(item.key);
    });
  }


}
