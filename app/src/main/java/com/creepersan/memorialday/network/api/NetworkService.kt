package com.creepersan.memorialday.network.api

/**
 * Retrofit 声明式服务接口的标记父接口。
 *
 * 业务服务可选择继承此接口，以便在模块边界中统一识别网络 API；不继承时仍可通过
 * `NetworkClient.createService()` 创建标准 Retrofit 接口。
 */
interface NetworkService
