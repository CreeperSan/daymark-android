import { deviceInfo } from '@kit.BasicServicesKit';
import { util } from '@kit.ArkTS';
import { DaymarkApi } from './DaymarkApi';
import { Anniversary } from '../../model/Anniversary';
import { UserSession } from '../../model/UserSession';
import { AppSettings } from '../../service/AppSettings';
import { KvStore } from '../storage/KvStore';
import { HttpClient, HttpResult } from '../network/HttpClient';
import {
  ApiError,
  ListEnvelope,
  LoginEnvelope,
  RefreshEnvelope,
  SaveEnvelope,
  StatusDto,
  UserEnvelope
} from '../network/ApiTypes';
import { Logger } from '../Logger';

const ACCOUNT_HOST: string = 'https://account.creepersan.com';
const DAYMARK_HOST: string = 'https://daymark.creepersan.com';
const APP_VERSION: string = '30000';
const PAGE_SIZE: number = 100;
const DEVICE_KEY: string = 'app.deviceId';

/**
 * 真实服务实现，接口路径与两端保持一致。当前产品阶段默认不启用，
 * 由 ApiProvider 切换到 Mock；接服务器时只需改一行开关。
 */
export class RealApi implements DaymarkApi {
  private static instance: RealApi | null = null;

  static shared(): RealApi {
    if (RealApi.instance === null) {
      RealApi.instance = new RealApi();
    }
    return RealApi.instance;
  }

  private deviceId(): string {
    let value: string = KvStore.getString(DEVICE_KEY, '');
    if (value.length === 0) {
      value = util.generateRandomUUID(true);
      KvStore.set(DEVICE_KEY, value);
    }
    return value;
  }

  private deviceName(): string {
    try {
      return deviceInfo.productModel.length > 0 ? deviceInfo.productModel : deviceInfo.marketName;
    } catch (err) {
      return 'HarmonyOS';
    }
  }

  /** 仅在开发者模式下使用代理地址。 */
  private host(fallback: string, isAccount: boolean): string {
    if (!AppSettings.developer()) {
      return fallback;
    }
    const proxy: string = isAccount ? AppSettings.accountHost() : AppSettings.serverHost();
    return proxy.length > 0 ? proxy : fallback;
  }

  private async post(host: string, path: string, body: string, token: string, query: string = ''): Promise<string> {
    const headers: Record<string, string> = HttpClient.commonHeaders(this.deviceId(), this.deviceName(), 'HarmonyOS',
      APP_VERSION, token);
    const url: string = host + path + query;
    const result: HttpResult = await HttpClient.post(url, body, headers);
    if (path === '/api/user/update' && result.status === 404) {
      throw new ApiError('unavailable', 'unavailable', 404);
    }
    if (result.status < 200 || result.status > 299) {
      throw ApiError.network();
    }
    let status: StatusDto;
    try {
      status = JSON.parse(result.body) as StatusDto;
    } catch (err) {
      throw ApiError.invalidData();
    }
    if (status.code !== 200) {
      throw ApiError.server(status.code, status.msg === undefined ? '' : status.msg);
    }
    return result.body;
  }

  async login(account: string, password: string): Promise<UserSession> {
    const body: string = JSON.stringify({ account: account, password: password });
    const text: string = await this.post(this.host(ACCOUNT_HOST, true), '/api/login', body, '');
    const envelope: LoginEnvelope = JSON.parse(text) as LoginEnvelope;
    const data = envelope.data;
    if (data === undefined) {
      throw ApiError.invalidData();
    }
    const token = data.token;
    if (data.user.id <= 0 || token.token.length === 0 || token.refresh_token.length === 0 ||
      token.token_expire_time <= 0 || token.refresh_token_expire_time <= 0) {
      throw ApiError.invalidData();
    }
    const session: UserSession = new UserSession();
    session.id = data.user.id;
    session.name = data.user.nickname === undefined ? '' : data.user.nickname;
    session.token = token.token;
    session.tokenExpire = token.token_expire_time;
    session.refreshToken = token.refresh_token;
    session.refreshExpire = token.refresh_token_expire_time;
    session.account = data.user.account === undefined ? '' : data.user.account;
    session.email = data.user.email === undefined ? '' : data.user.email;
    Logger.event('登录成功');
    return session;
  }

  /** 令牌刷新由 SessionManager 触发，这里只做一次请求。 */
  async refreshToken(session: UserSession): Promise<UserSession> {
    const body: string = JSON.stringify({ refresh_token: session.refreshToken });
    const text: string = await this.post(this.host(ACCOUNT_HOST, true), '/api/token/refresh', body, session.token);
    const envelope: RefreshEnvelope = JSON.parse(text) as RefreshEnvelope;
    if (envelope.data === undefined) {
      throw ApiError.invalidData();
    }
    const token = envelope.data.token;
    if (token.token.length === 0 || token.refresh_token.length === 0) {
      throw ApiError.invalidData();
    }
    const updated: UserSession = session.copy();
    updated.token = token.token;
    updated.tokenExpire = token.token_expire_time;
    updated.refreshToken = token.refresh_token;
    updated.refreshExpire = token.refresh_token_expire_time;
    return updated;
  }

  async requestCode(email: string, register: boolean): Promise<void> {
    const path: string = register ? '/api/register/request_auth_code' : '/api/password/reset/request_auth_code';
    await this.post(this.host(ACCOUNT_HOST, true), path, JSON.stringify({ email: email }), '');
  }

  async register(account: string, email: string, password: string, code: string): Promise<void> {
    const body: string = JSON.stringify({ account: account, email: email, password: password, code: code });
    await this.post(this.host(ACCOUNT_HOST, true), '/api/register', body, '');
  }

  async resetPassword(email: string, password: string, code: string): Promise<void> {
    const body: string = JSON.stringify({ email: email, new_password: password, code: code });
    await this.post(this.host(ACCOUNT_HOST, true), '/api/password/reset', body, '');
  }

  async changePassword(oldPassword: string, newPassword: string, token: string): Promise<void> {
    const body: string = JSON.stringify({ old_password: oldPassword, new_password: newPassword });
    await this.post(this.host(ACCOUNT_HOST, true), '/api/password/change', body, token);
  }

  async updateNickname(nickname: string, token: string): Promise<string> {
    const text: string = await this.post(this.host(ACCOUNT_HOST, true), '/api/user/update',
      JSON.stringify({ nickname: nickname }), token);
    const envelope: UserEnvelope = JSON.parse(text) as UserEnvelope;
    if (envelope.data === undefined || envelope.data.user.nickname === undefined) {
      throw ApiError.invalidData();
    }
    return envelope.data.user.nickname;
  }

  async list(token: string): Promise<Anniversary[]> {
    const host: string = this.host(DAYMARK_HOST, false);
    await this.post(host, '/api/anniversary/refresh', JSON.stringify({ force: 0 }), token);
    const all: Anniversary[] = [];
    let page: number = 1;
    while (true) {
      const text: string = await this.post(host, '/api/anniversary/list', '{}', token,
        '?page=' + page.toString() + '&page_size=' + PAGE_SIZE.toString());
      const envelope: ListEnvelope = JSON.parse(text) as ListEnvelope;
      if (envelope.data === undefined) {
        throw ApiError.invalidData();
      }
      const items: Anniversary[] = envelope.data.list.map((dto) => Anniversary.fromDto(dto));
      for (let i = 0; i < items.length; i++) {
        all.push(items[i]);
      }
      if (items.length < PAGE_SIZE) {
        break;
      }
      page++;
    }
    return all;
  }

  async get(id: number, token: string): Promise<Anniversary> {
    const text: string = await this.post(this.host(DAYMARK_HOST, false), '/api/anniversary/get',
      JSON.stringify({ id: id }), token);
    const envelope: SaveEnvelope = JSON.parse(text) as SaveEnvelope;
    if (envelope.data === undefined) {
      throw ApiError.invalidData();
    }
    return Anniversary.fromDto(envelope.data.date);
  }

  async save(item: Anniversary, token: string): Promise<Anniversary> {
    const body: string = JSON.stringify(item.toDto());
    const path: string = item.id > 0 ? '/api/anniversary/edit' : '/api/anniversary/add';
    const text: string = await this.post(this.host(DAYMARK_HOST, false), path, body, token);
    const envelope: SaveEnvelope = JSON.parse(text) as SaveEnvelope;
    if (envelope.data === undefined) {
      throw ApiError.invalidData();
    }
    return Anniversary.fromDto(envelope.data.date);
  }

  async remove(id: number, token: string): Promise<void> {
    await this.post(this.host(DAYMARK_HOST, false), '/api/anniversary/delete', JSON.stringify({ id: id }), token);
  }
}
