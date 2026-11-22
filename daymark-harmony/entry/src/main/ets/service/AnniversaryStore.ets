import { Anniversary } from '../model/Anniversary';
import { ApiProvider } from '../core/api/ApiProvider';
import { ApiError } from '../core/network/ApiTypes';
import { AnniversaryCalendar } from '../calendar/AnniversaryCalendar';
import { StoreKeys } from './AppSettings';
import { SessionManager } from './SessionManager';
import { Logger } from '../core/Logger';

export class AnniversaryEntry {
  item: Anniversary;
  days: number | null;

  constructor(item: Anniversary, days: number | null) {
    this.item = item;
    this.days = days;
  }
}

export class AnniversaryGroup {
  title: string;
  entries: AnniversaryEntry[];

  constructor(title: string, entries: AnniversaryEntry[]) {
    this.title = title;
    this.entries = entries;
  }
}

/**
 * 纪念日列表状态。数据来自 DaymarkApi（当前为本地 Mock），
 * 保存/删除后立即合并到内存列表，返回首页无需重新拉取。
 */
export class AnniversaryStore {
  private static instance: AnniversaryStore | null = null;
  items: Anniversary[] = [];
  loading: boolean = false;
  loaded: boolean = false;
  error: string = '';
  offline: boolean = false;

  static shared(): AnniversaryStore {
    if (AnniversaryStore.instance === null) {
      AnniversaryStore.instance = new AnniversaryStore();
    }
    return AnniversaryStore.instance;
  }

  private notify(): void {
    AppStorage.setOrCreate<number>(StoreKeys.DATA_VERSION, Date.now());
  }

  /** 刷新列表；失败时保留已有数据，首页只负责展示错误。 */
  async load(): Promise<void> {
    if (this.loading) {
      return;
    }
    this.loading = true;
    this.error = '';
    this.offline = false;
    this.notify();
    try {
      const session = await SessionManager.shared().validSession();
      const result: Anniversary[] = await ApiProvider.api().list(session.token);
      this.items = result;
      this.loaded = true;
      Logger.event('纪念日列表已刷新，共 ' + result.length + ' 条');
    } catch (err) {
      const error: ApiError = err as ApiError;
      this.offline = error.kind === 'network';
      this.error = error.kind === 'network' ? 'offline' : (error.kind === 'auth' ? 'auth' : 'failed');
      Logger.event('刷新纪念日失败：' + error.kind);
    } finally {
      this.loading = false;
      this.notify();
    }
  }

  /** 首页按今天、即将到来、已经过去、其他分组，与 iOS 顺序一致。 */
  groups(today: Date): AnniversaryGroup[] {
    const todayEntries: AnniversaryEntry[] = [];
    const upcoming: AnniversaryEntry[] = [];
    const past: AnniversaryEntry[] = [];
    const unknown: AnniversaryEntry[] = [];
    for (let i = 0; i < this.items.length; i++) {
      const item: Anniversary = this.items[i];
      const days: number | null = AnniversaryCalendar.daysUntil(item, today);
      const entry: AnniversaryEntry = new AnniversaryEntry(item, days);
      if (days === null) {
        unknown.push(entry);
      } else if (days === 0) {
        todayEntries.push(entry);
      } else if (days > 0) {
        upcoming.push(entry);
      } else {
        past.push(entry);
      }
    }
    upcoming.sort((a: AnniversaryEntry, b: AnniversaryEntry) => (a.days as number) - (b.days as number));
    past.sort((a: AnniversaryEntry, b: AnniversaryEntry) => (b.days as number) - (a.days as number));
    const groups: AnniversaryGroup[] = [];
    if (todayEntries.length > 0) {
      groups.push(new AnniversaryGroup('groupToday', todayEntries));
    }
    if (upcoming.length > 0) {
      groups.push(new AnniversaryGroup('groupUpcoming', upcoming));
    }
    if (past.length > 0) {
      groups.push(new AnniversaryGroup('groupPast', past));
    }
    if (unknown.length > 0) {
      groups.push(new AnniversaryGroup('groupOther', unknown));
    }
    return groups;
  }

  async get(id: number): Promise<Anniversary> {
    const session = await SessionManager.shared().validSession();
    return ApiProvider.api().get(id, session.token);
  }

  /** 保存成功后合并列表，服务端返回的完整记录优先。 */
  async save(item: Anniversary): Promise<Anniversary> {
    const session = await SessionManager.shared().validSession();
    const saved: Anniversary = await ApiProvider.api().save(item, session.token);
    const next: Anniversary[] = [];
    for (let i = 0; i < this.items.length; i++) {
      if (this.items[i].id !== saved.id) {
        next.push(this.items[i]);
      }
    }
    next.push(saved);
    this.items = next;
    this.notify();
    Logger.event('纪念日已保存：' + saved.name + '（id=' + saved.id.toString() +
      '，历法=' + saved.calendarType + '，重复=' + saved.loopType + '）');
    return saved;
  }

  async remove(id: number): Promise<void> {
    const session = await SessionManager.shared().validSession();
    await ApiProvider.api().remove(id, session.token);
    this.items = this.items.filter((item: Anniversary) => item.id !== id);
    this.notify();
    Logger.event('纪念日已删除：id=' + id.toString() + '，剩余 ' + this.items.length.toString() + ' 条');
  }

  /** 退出登录时清空内存列表，避免下一个账号看到上一位的数据。 */
  clear(): void {
    Logger.event('清空纪念日缓存');
    this.items = [];
    this.loaded = false;
    this.error = '';
    this.offline = false;
    this.notify();
  }
}
