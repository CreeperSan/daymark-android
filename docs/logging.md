# 客户端日志

Android 和 iOS 会在应用私有目录的 `logs` 文件夹中按日期和大小分片保存事件日志，记录页面生命周期、主要操作、控件事件、请求路径及结果状态。Android 的目录位于 `noBackupFilesDir/logs`，iOS 位于 `Application Support/logs`。

Debug 版本还会在 Logcat 或 Xcode 控制台输出网络地址、路由 Host、请求参数及响应 JSON。密码、令牌、验证码、账号和联系方式等字段会脱敏；无法识别的非 JSON 正文不会输出。网络正文不写入日志文件，也不会在 Release 控制台输出。

本地开发服务根据 Host 分发子服务。开发者设置中使用 `localhost` 或 IP 地址时，客户端会为账号和纪念日请求分别发送对应的业务域名作为 Host；自定义域名代理保持原 Host。Android 模拟器访问电脑的本地服务可使用 `10.0.2.2`，iOS 模拟器可使用 `127.0.0.1`。
