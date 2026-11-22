import { DaymarkApi } from './DaymarkApi';
import { Anniversary, AnniversaryDto, CalendarKind, RepeatKind } from '../../model/Anniversary';
import { UserSession } from '../../model/UserSession';
import { AppSettings, Keys } from '../../service/AppSettings';
import { KvStore } from '../storage/KvStore';
import { ApiError } from '../network/ApiTypes';
import { DateUtils } from '../../common/DateUtils';
import { LunarCalendar, LunarParts } from '../../calendar/LunarCalendar';
import { Logger } from '../Logger';

/**
 * 本地 Mock 数据源。不请求服务器，但完整模拟服务端的字段、增删改与延迟，
 * 数据落在 preferences 中，重启应用后仍然存在，便于验证列表合并与编辑返回。
 */
export class MockApi implements DaymarkApi {
  private static instance: MockApi | null = null;
  private pendingEmail: string = '';

  static shared(): MockApi {
    if (MockApi.instance === null) {
      MockApi.instance = new MockApi();
    }
    return MockApi.instance;
  }

  /** 统一延迟与失败注入；开发者模式可以打开“模拟网络异常”验证失败态。 */
  private async simulate(ms: number): Promise<void> {
    await new Promise<void>((resolve: (value: void) => void) => {
      setTimeout(() => {
        resolve();
      }, ms);
    });
    if (AppSettings.mockFailure()) {
      throw ApiError.network();
    }
  }

  private read(): Anniversary[] {
    const text: string = KvStore.getString(Keys.MOCK_ANNIVERSARIES, '');
    if (text.length === 0) {
      return [];
    }
    try {
      const list: AnniversaryDto[] = JSON.parse(text) as AnniversaryDto[];
      return list.map((dto: AnniversaryDto) => Anniversary.fromDto(dto));
    } catch (err) {
      Logger.warn('本地纪念日解析失败');
      return [];
    }
  }

  private write(items: Anniversary[]): void {
    const list: AnniversaryDto[] = items.map((item: Anniversary) => item.toDto());
    KvStore.set(Keys.MOCK_ANNIVERSARIES, JSON.stringify(list));
  }

  /** 首次启动生成示例数据，覆盖“今天 / 即将到来 / 已经过去”与农历。 */
  private seedIfNeeded(): Anniversary[] {
    let items: Anniversary[] = this.read();
    if (items.length > 0) {
      return items;
    }
    items = [];
    const today: Date = DateUtils.today();

    const together: Anniversary = new Anniversary();
    together.id = 1;
    together.name = '在一起的 1000 天';
    together.description = '从那天开始，日子有了刻度。';
    together.calendarType = CalendarKind.SOLAR;
    together.loopType = RepeatKind.YEARLY;
    together.year = today.getFullYear() - 2;
    together.month = today.getMonth() + 1;
    together.day = today.getDate();
    items.push(together);

    const momDate: Date = DateUtils.addDays(today, 12);
    const momParts: LunarParts | null = LunarCalendar.parts(momDate);
    const mom: Anniversary = new Anniversary();
    mom.id = 2;
    mom.name = '妈妈的生日';
    mom.description = '记得提前订蛋糕。';
    mom.calendarType = CalendarKind.LUNAR;
    mom.loopType = RepeatKind.YEARLY;
    if (momParts !== null) {
      mom.year = momParts.year;
      mom.month = momParts.month;
      mom.day = momParts.day;
      mom.isLeapMonth = momParts.leap ? 1 : 0;
    } else {
      mom.calendarType = CalendarKind.SOLAR;
      mom.year = momDate.getFullYear();
      mom.month = momDate.getMonth() + 1;
      mom.day = momDate.getDate();
    }
    items.push(mom);

    const meetDate: Date = DateUtils.addDays(today, 45);
    const meet: Anniversary = new Anniversary();
    meet.id = 3;
    meet.name = '相识纪念日';
    meet.description = '第一次一起吃饭的那家小店。';
    meet.calendarType = CalendarKind.SOLAR;
    meet.loopType = RepeatKind.NONE;
    meet.year = meetDate.getFullYear();
    meet.month = meetDate.getMonth() + 1;
    meet.day = meetDate.getDate();
    items.push(meet);

    const graduation: Anniversary = new Anniversary();
    graduation.id = 4;
    graduation.name = '毕业典礼';
    graduation.description = '那天的雨下得很大。';
    graduation.calendarType = CalendarKind.SOLAR;
    graduation.loopType = RepeatKind.NONE;
    graduation.year = 2019;
    graduation.month = 6;
    graduation.day = 20;
    items.push(graduation);

    const clubDate: Date = DateUtils.addDays(today, 3);
    const club: Anniversary = new Anniversary();
    club.id = 5;
    club.name = '每月读书会';
    club.description = '';
    club.calendarType = CalendarKind.SOLAR;
    club.loopType = RepeatKind.MONTHLY;
    club.year = clubDate.getFullYear();
    club.month = clubDate.getMonth() + 1;
    club.day = clubDate.getDate();
    items.push(club);

    this.write(items);
    Logger.event('已写入本地示例数据');
    return items;
  }

  /** 本地登录：任意非空账号密码都可进入，便于页面联调。 */
  async login(account: string, password: string): Promise<UserSession> {
    await this.simulate(600);
    const trimmed: string = account.trim();
    if (trimmed.length === 0 || password.length === 0) {
      throw ApiError.server(400, '账号或密码不能为空');
    }
    const session: UserSession = new UserSession();
    let hash: number = 0;
    for (let i = 0; i < trimmed.length; i++) {
      hash = (hash * 31 + trimmed.charCodeAt(i)) % 900000;
    }
    session.id = 100000 + hash;
    session.name = '小满';
    const isEmail: boolean = trimmed.indexOf('@') > 0;
    session.account = isEmail ? trimmed.substring(0, trimmed.indexOf('@')) : trimmed;
    session.email = isEmail ? trimmed : trimmed + '@example.com';
    session.token = 'mock-token-' + Date.now().toString();
    session.tokenExpire = Date.now() + 7200000;
    session.refreshToken = 'mock-refresh-' + Date.now().toString();
    session.refreshExpire = Date.now() + 2592000000;
    this.seedIfNeeded();
    Logger.event('本地登录成功');
    return session;
  }

  async requestCode(email: string, register: boolean): Promise<void> {
    await this.simulate(500);
    if (email.trim().length === 0) {
      throw ApiError.server(400, '邮箱不能为空');
    }
    this.pendingEmail = email.trim();
    Logger.event(register ? '已下发注册验证码' : '已下发重置验证码');
  }

  async register(account: string, email: string, password: string, code: string): Promise<void> {
    await this.simulate(600);
    if (code.length !== 6) {
      throw ApiError.server(400, '验证码不正确');
    }
    Logger.event('本地注册完成');
  }

  async resetPassword(email: string, password: string, code: string): Promise<void> {
    await this.simulate(600);
    if (code.length !== 6) {
      throw ApiError.server(400, '验证码不正确');
    }
    Logger.event('本地重置密码完成');
  }

  async changePassword(oldPassword: string, newPassword: string, token: string): Promise<void> {
    await this.simulate(600);
    if (oldPassword.length === 0) {
      throw ApiError.server(400, '原密码不正确');
    }
    Logger.event('本地修改密码完成');
  }

  async updateNickname(nickname: string, token: string): Promise<string> {
    await this.simulate(500);
    if (nickname.trim().length === 0) {
      throw ApiError.server(400, '昵称不能为空');
    }
    return nickname.trim();
  }

  async list(token: string): Promise<Anniversary[]> {
    await this.simulate(400);
    const items: Anniversary[] = this.seedIfNeeded();
    return items.map((item: Anniversary) => item.copy());
  }

  async get(id: number, token: string): Promise<Anniversary> {
    await this.simulate(300);
    const items: Anniversary[] = this.seedIfNeeded();
    for (let i = 0; i < items.length; i++) {
      if (items[i].id === id) {
        return items[i].copy();
      }
    }
    throw ApiError.server(404, '纪念日不存在');
  }

  async save(item: Anniversary, token: string): Promise<Anniversary> {
    await this.simulate(500);
    const items: Anniversary[] = this.seedIfNeeded();
    const saved: Anniversary = item.copy();
    saved.nextTimestamp = 0;
    if (saved.id > 0) {
      let found: boolean = false;
      for (let i = 0; i < items.length; i++) {
        if (items[i].id === saved.id) {
          items[i] = saved;
          found = true;
          break;
        }
      }
      if (!found) {
        throw ApiError.server(404, '纪念日不存在');
      }
    } else {
      let maxId: number = 0;
      for (let i = 0; i < items.length; i++) {
        maxId = items[i].id > maxId ? items[i].id : maxId;
      }
      saved.id = maxId + 1;
      items.push(saved);
    }
    this.write(items);
    Logger.event(saved.id > 0 ? '本地保存纪念日' : '本地新建纪念日');
    return saved;
  }

  async remove(id: number, token: string): Promise<void> {
    await this.simulate(400);
    const items: Anniversary[] = this.seedIfNeeded();
    const remain: Anniversary[] = items.filter((item: Anniversary) => item.id !== id);
    if (remain.length === items.length) {
      throw ApiError.server(404, '纪念日不存在');
    }
    this.write(remain);
    Logger.event('本地删除纪念日');
  }
}
