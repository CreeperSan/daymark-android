import { Anniversary } from '../../model/Anniversary';
import { UserSession } from '../../model/UserSession';

/**
 * 业务接口。真实实现走 HTTPS，Mock 实现走本地存储，
 * 两者签名一致，切换数据源不影响页面与状态管理。
 * token 由调用方（SessionManager）注入，接口本身不持有会话，便于测试。
 */
export interface DaymarkApi {
  login(account: string, password: string): Promise<UserSession>;
  requestCode(email: string, register: boolean): Promise<void>;
  register(account: string, email: string, password: string, code: string): Promise<void>;
  resetPassword(email: string, password: string, code: string): Promise<void>;
  changePassword(oldPassword: string, newPassword: string, token: string): Promise<void>;
  updateNickname(nickname: string, token: string): Promise<string>;
  list(token: string): Promise<Anniversary[]>;
  get(id: number, token: string): Promise<Anniversary>;
  save(item: Anniversary, token: string): Promise<Anniversary>;
  remove(id: number, token: string): Promise<void>;
}
