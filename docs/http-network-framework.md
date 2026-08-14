# 新网络框架使用说明

新框架位于 `module/http`，与旧网络实现完全隔离。核心 API 不要求业务层直接依赖
OkHttp、Retrofit 或具体序列化库，传输实现通过 `TransportEngine` 注入。

## 创建客户端

```kotlin
val transport = OkHttpTransportEngine(
    client = OkHttpClient.Builder().build(),
    baseHost = "https://example.com"
)

val networkManager = NetworkManager(
    config = NetworkClientConfig(
        defaultHost = "https://example.com",
        defaultHeaders = mapOf("Accept" to "application/json")
    ),
    transport = transport
)
```

## 普通请求

```kotlin
val request = NetworkRequest(
    path = "api/tags",
    method = HttpMethod.GET,
    query = mapOf("page" to "1"),
    decoder = ResponseDecoder { response ->
        response.bodyText()
    }
)

networkManager.request(request).events.collect { event ->
    when (event) {
        is NetworkEvent.Started -> Unit
        is NetworkEvent.Progress -> Unit
        is NetworkEvent.Success -> println(event.value)
        is NetworkEvent.Failure -> println(event.error)
        NetworkEvent.Cancelled -> Unit
    }
}
```

Flow 是冷流，只有 `collect` 或 `await` 时才开始请求。取消收集协程会同时取消中间件等待、
OkHttp 调用和响应体读取。

## 只处理业务结果并使用 map/flatMapConcat

`events` 适合观察完整生命周期；普通单次请求还提供 `result`，它会自动过滤
`Started`、`Progress` 等控制事件：

```kotlin
networkManager
    .request(request)
    .result
    .map { response -> response.data }
    .flatMapConcat { data ->
        flowOf(data.size)
    }
    .collect { size ->
        println("数据量：$size")
    }
```

也可以直接使用调用对象上的便捷方法：

```kotlin
networkManager
    .request(request)
    .map { response -> response.data }
    .collect { data ->
        showData(data)
    }
```

如果请求失败，`result`、`map` 和 `flatMapConcat` 会抛出 `NetworkException`；
如果请求被取消，则保留协程取消语义。

## 不使用响应解码器

如果只想获取状态码、响应头和原始响应内容，可以将请求类型声明为 `NetworkResponse`，
并省略 `decoder`：

```kotlin
val request = NetworkRequest<NetworkResponse>(
    path = "/health",
    method = HttpMethod.GET,
    decoder = null
)

val response = networkManager.request(request).await()
println(response.statusCode)
println(response.bodyText())
```

`NetworkResponse` 是已经读取完成的响应快照，底层响应体仍由框架自动关闭。
对于大文件或持续响应，不应使用这种方式，应改用原始字节流或文件下载 API。

## 中间件

```kotlin
val authMiddleware = NetworkMiddleware { request, context, chain ->
    val token = tokenProvider.getValidToken()
    chain.proceed(
        request.copy(
            headers = request.headers + ("Authorization" to "Bearer $token")
        ),
        context
    )
}

networkManager.registerMiddleware(authMiddleware)
```

全局中间件先于请求级中间件执行。中间件可以挂起等待、改写请求、读取或写入
`RequestContext`，也可以返回 `MiddlewareResult.Success` 或 `MiddlewareResult.Failure`
短路请求。

## 原始流和文件下载

```kotlin
networkManager.stream(request).events.collect { event ->
    when (event) {
        is ByteStreamEvent.Chunk -> consume(event.bytes)
        is ByteStreamEvent.Progress -> updateProgress(event.transferred, event.total)
        else -> Unit
    }
}

networkManager.downloadTo(
    request = request,
    target = File(cacheDir, "archive.bin"),
    resume = true
)
```

文件下载先写入 `.part` 临时文件，成功后再原子移动到目标文件。取消或失败不会覆盖已有
目标文件。

## SSE

```kotlin
val sseCall = networkManager.sse(
    request = NetworkRequest(
        path = "api/events",
        decoder = ResponseDecoder { error("SSE 使用独立解码器") }
    ),
    decoder = SseEventDecoder { raw ->
        json.decodeFromString<TagEvent>(raw.data)
    },
    options = SseOptions(
        reconnectPolicy = ReconnectPolicy.Limited(maxAttempts = 3)
    )
)

sseCall.events.collect { event ->
    when (event) {
        is SseEvent.Message -> handle(event.data)
        is SseEvent.Started -> Unit
        SseEvent.Heartbeat -> Unit
        is SseEvent.Failed -> report(event.error)
        SseEvent.Completed,
        SseEvent.Cancelled -> Unit
    }
}
```

SSE 会按协议解析 `event`、`id`、`retry`、多行 `data` 和心跳注释。开启重连后会限制次数、
使用指数退避，并在请求头中携带最近成功事件的 `Last-Event-ID`。
