import { http } from '@kit.NetworkKit';
import { ApiError } from './ApiTypes';
import { Logger } from '../Logger';

export class HttpResult {
  status: number = 0;
  body: string = '';
}

/**
 * 网络底层封装：只负责发一次 JSON POST 并返回状态码与原文。
 * 业务状态码（信封 code）与令牌刷新由上层 API 处理，便于替换测试实现。
 */
export class HttpClient {
  static readonly TIMEOUT_MS: number = 30000;

  static commonHeaders(deviceId: string, deviceName: string, platform: string, version: string,
    token: string = ''): Record<string, string> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'CS-App': 'Daymark',
      'CS-Device-Code': deviceId,
      'CS-Device-Name': deviceName,
      'CS-Device-Platform': platform,
      'CS-Version': version
    };
    if (token.length > 0) {
      headers['CS-Token'] = token;
    }
    return headers;
  }

  static async post(url: string, body: string, headers: Record<string, string>): Promise<HttpResult> {
    const request: http.HttpRequest = http.createHttp();
    try {
      Logger.network('请求 POST ' + url + ' 参数 ' + Logger.redact(body));
      const options: http.HttpRequestOptions = {
        method: http.RequestMethod.POST,
        header: headers,
        extraData: body,
        connectTimeout: HttpClient.TIMEOUT_MS,
        readTimeout: HttpClient.TIMEOUT_MS,
        expectDataType: http.HttpDataType.STRING
      };
      const response: http.HttpResponse = await request.request(url, options);
      const result: HttpResult = new HttpResult();
      result.status = response.responseCode;
      result.body = typeof response.result === 'string' ? response.result : JSON.stringify(response.result);
      Logger.network('响应 ' + url + ' HTTP ' + result.status + ' 结果 ' + Logger.redact(result.body));
      return result;
    } catch (err) {
      Logger.event('请求失败：' + url + ' 错误类型 ' + (err as Error).name);
      throw ApiError.network();
    } finally {
      request.destroy();
    }
  }
}
