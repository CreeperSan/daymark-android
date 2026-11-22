import { UserSession } from '../model/UserSession';
import { ApiProvider } from '../core/api/ApiProvider';
import { RealApi } from '../core/api/RealApi';
import { ApiError } from '../core/network/ApiTypes';
import { KvStore } from '../core/storage/KvStore';
import { Keys, StoreKeys } from './AppSettings';
import { Logger } from '../core/Logger';

/**
 * 会话管理：负责持久化、登录态与令牌有效性。页面只拿 UserSession，
 * 不直接接触令牌刷新细节；Mock 模式下没有真实鉴权，直接复用本地会话。
 */
export class SessionManager {
  private static instance: SessionManager | null = null;
  session: UserSession | null = null;
  private refreshing: Promise<UserSession> | null = null;

  static shared(): SessionManager {
    if (SessionManager.instance === null) {
      SessionManager.instance = new SessionManager();
    }
    return SessionManager.instance;
  }

  init(): void {
    this.session = UserSession.fromJson(KvStore.getString(Keys.SESSION, ''));
    SessionManager.notify();
    if (this.session === null) {
      Logger.event('会话恢复：本地没有登录记录');
    } else {
      Logger.event('会话恢复：账号=' + this.session.account + '，令牌有效=' +
        this.session.tokenValid(Date.now()).toString());
    }
  }

  private persist(session: UserSession): void {
    this.session = session;
    KvStore.set(Keys.SESSION, session.toJson());
    SessionManager.notify();
  }

  /** 会话变化通过 AppStorage 通知界面，避免各页面自行轮询。 */
  private static notify(): void {
    AppStorage.setOrCreate<number>(StoreKeys.SESSION_VERSION, Date.now());
  }

  async login(account: string, password: string): Promise<void> {
    Logger.event('提交登录请求，账号=' + account);
    const session: UserSession = await ApiProvider.api().login(account, password);
    this.persist(session);
    Logger.event('登录成功，已保存会话');
  }

  logout(): void {
    Logger.event('退出登录，清除本机会话');
    this.session = null;
    this.refreshing = null;
    KvStore.remove(Keys.SESSION);
    SessionManager.notify();
  }

  /** 取可用令牌；令牌将过期时先刷新，服务端明确拒绝才清除会话。 */
  async validSession(): Promise<UserSession> {
    const current: UserSession | null = this.session;
    if (current === null) {
      throw ApiError.authentication();
    }
    if (ApiProvider.useMock) {
      return current;
    }
    const now: number = Date.now();
    if (current.tokenValid(now)) {
      return current;
    }
    if (current.refreshExpire <= now || current.refreshToken.length === 0) {
      this.logout();
      throw ApiError.authentication();
    }
    if (this.refreshing !== null) {
      return this.refreshing;
    }
    const task: Promise<UserSession> = this.refresh(current);
    this.refreshing = task;
    try {
      return await task;
    } finally {
      this.refreshing = null;
    }
  }

  private async refresh(current: UserSession): Promise<UserSession> {
    try {
      const updated: UserSession = await RealApi.shared().refreshToken(current);
      if (this.session === null || this.session.id !== current.id) {
        throw ApiError.authentication();
      }
      // 刷新期间昵称可能已更新，保留最新资料，只替换令牌。
      const latest: UserSession = this.session.copy();
      latest.token = updated.token;
      latest.tokenExpire = updated.tokenExpire;
      latest.refreshToken = updated.refreshToken;
      latest.refreshExpire = updated.refreshExpire;
      this.persist(latest);
      return latest;
    } catch (err) {
      const error: ApiError = err as ApiError;
      if (error.kind === 'server' && (error.code === 400 || error.code === 401)) {
        this.logout();
        throw ApiError.authentication();
      }
      throw error;
    }
  }

  async updateNickname(nickname: string): Promise<void> {
    const current: UserSession = await this.validSession();
    const name: string = await ApiProvider.api().updateNickname(nickname, current.token);
    if (this.session === null || this.session.id !== current.id) {
      return;
    }
    const latest: UserSession = this.session.copy();
    latest.name = name;
    this.persist(latest);
  }

  async changePassword(oldPassword: string, newPassword: string): Promise<void> {
    const current: UserSession = await this.validSession();
    await ApiProvider.api().changePassword(oldPassword, newPassword, current.token);
    this.logout();
  }
}
