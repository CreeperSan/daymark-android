import { Theme } from './Theme';
import { I18n } from '../core/i18n/Strings';
import { AnniversaryGroup, AnniversaryEntry, AnniversaryStore } from '../service/AnniversaryStore';
import { AnniversaryCalendar } from '../calendar/AnniversaryCalendar';
import { GlassHeader, PageState } from './components/Glass';
import { Icon } from './components/Icon';
import { Routes, RouteParam } from './Routes';
import { Logger } from '../core/Logger';

/** 组内卡片间距。 */
const CARD_GAP: number = 14;
/** 上一组卡片到下一组标题的间距。 */
const GROUP_GAP: number = 22;

/** 首页：按今天 / 即将到来 / 已经过去 / 其他分组，支持下拉刷新与跨天自动刷新。 */
@Component
export struct HomeView {
  @Prop isDark: boolean = false;
  /** 语言标签：只用于触发页面重建，语言切换后所有文案需要重新求值 */
  @Prop langTag: string = '';
  path: NavPathStack = new NavPathStack();
  @StorageLink('dataVersion') dataVersion: number = 0;
  /** 底部手势条高度，悬浮按钮需要避开 */
  @StorageProp('bottomInset') bottomInset: number = 0;
  @State todayMs: number = Date.now();
  @State refreshing: boolean = false;
  /** 页面级加载态：store 是普通单例，改动不会触发刷新，这里用状态变量兜底 */
  @State loadingList: boolean = false;
  /** 以下三个状态同样是从 store 镜像过来的视图状态，用于驱动空态 / 错误态切换 */
  @State loadError: string = '';
  @State offlineState: boolean = false;
  @State started: boolean = false;
  @State manualRefreshing: boolean = false;
  private store: AnniversaryStore = AnniversaryStore.shared();
  private midnightTimer: number = -1;

  aboutToAppear(): void {
    Logger.event('进入首页');
    if (!this.started) {
      this.started = true;
      this.reload(false);
    }
    this.scheduleMidnight();
  }

  aboutToDisappear(): void {
    if (this.midnightTimer !== -1) {
      clearTimeout(this.midnightTimer);
      this.midnightTimer = -1;
    }
  }

  /** 页面可见时等待下一次本地零点，跨天后重新计算分组。 */
  private scheduleMidnight(): void {
    if (this.midnightTimer !== -1) {
      clearTimeout(this.midnightTimer);
    }
    const now: Date = new Date();
    const next: Date = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 0, 0, 2);
    const delay: number = next.getTime() - now.getTime();
    this.midnightTimer = setTimeout(() => {
      Logger.event('跨天，重新计算倒计时');
      this.todayMs = Date.now();
      this.reload(false);
      this.scheduleMidnight();
    }, delay > 1000 ? delay : 1000);
  }

  private async reload(manual: boolean): Promise<void> {
    Logger.event(manual ? '下拉刷新纪念日列表' : '加载纪念日列表');
    this.manualRefreshing = manual;
    this.loadingList = true;
    try {
      await this.store.load();
    } finally {
      this.loadingList = false;
    }
    this.loadError = this.store.error;
    this.offlineState = this.store.offline;
    this.todayMs = Date.now();
    this.refreshing = false;
    this.manualRefreshing = false;
  }

  private statusText(days: number | null): string {
    if (days === null) {
      return I18n.t('dateUnavailable');
    }
    if (days === 0) {
      return I18n.t('todayBadge');
    }
    return days > 0 ? I18n.t('daysRemaining', days.toString()) : I18n.t('daysPassed', (-days).toString());
  }

  @Builder
  groupHeader(title: string) {
    Text(I18n.t(title))
      .fontSize(12)
      .fontWeight(FontWeight.Medium)
      .fontColor(Theme.textSecondary(this.isDark))
      // 分组标题与上一组卡片、与自己的卡片之间都要留白，否则整页看起来糊在一起
      .margin({ top: GROUP_GAP - CARD_GAP, bottom: 10, left: 4 })
  }

  @Builder
  row(entry: AnniversaryEntry) {
    Row({ space: 8 }) {
      Column({ space: 5 }) {
        Text(entry.item.name)
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.textPrimary(this.isDark))
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(AnniversaryCalendar.dateText(entry.item, AnniversaryCalendar.originalDate(entry.item)))
          .fontSize(12)
          .fontColor(Theme.textSecondary(this.isDark))
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      Text(this.statusText(entry.days))
        .fontSize(12)
        .fontWeight(FontWeight.Medium)
        .fontColor(entry.days === 0 ? '#FFFFFF' :
          ((entry.days !== null && entry.days < 0) ? Theme.textSecondary(this.isDark) : Theme.accent(this.isDark)))
        .padding({ left: 10, right: 10, top: 6, bottom: 6 })
        .backgroundColor(entry.days === 0 ? Theme.accent(this.isDark) : Theme.withAlpha(Theme.accent(this.isDark), 0.14))
        .borderRadius(20)
    }
    .width('100%')
    .padding(16)
    .backgroundColor(Theme.glassFill(this.isDark))
    .borderRadius(20)
    .border({ width: 1, color: entry.days === 0 ? Theme.withAlpha(Theme.accent(this.isDark), 0.7) : Theme.glassStroke(this.isDark) })
    .backgroundBlurStyle(BlurStyle.Thin)
    .onClick(() => {
      Logger.event('打开纪念日详情：' + entry.item.name + '（id=' + entry.item.id.toString() + '）');
      this.path.pushPath({ name: Routes.EDIT, param: new RouteParam(entry.item.id) });
    })
  }

  @Builder
  emptyOverlay() {
    if (this.loadError.length > 0) {
      PageState({
        kind: this.offlineState ? 'offline' : 'failed',
        isDark: this.isDark,
        title: this.offlineState ? I18n.t('offlineTitle') : I18n.t('loadingFailed'),
        description: this.offlineState ? I18n.t('offlineDesc') : I18n.t('failedDesc'),
        onRetry: () => {
          this.reload(false);
        }
      })
    } else if (this.store.loaded && this.store.items.length === 0) {
      PageState({
        kind: 'empty',
        isDark: this.isDark,
        title: I18n.t('emptyTitle'),
        description: I18n.t('emptyDesc')
      })
    }
  }

  build() {
    // 用 BottomEnd 对齐，让悬浮的新建按钮固定在右下角
    Stack({ alignContent: Alignment.BottomEnd }) {
      Column() {
        GlassHeader({
          title: I18n.t('myAnniversaries'),
          trailingIcon: 'gear',
          isDark: this.isDark,
          onTrailing: () => {
            this.path.pushPath({ name: Routes.SETTINGS, param: new RouteParam(0) });
          }
        })

        if (this.loadingList && !this.manualRefreshing) {
          LoadingProgress()
            .width(24)
            .height(24)
            .color(Theme.accent(this.isDark))
            .margin({ bottom: 4 })
        }

        Stack({ alignContent: Alignment.Center }) {
          if (this.loadError.length === 0) {
            Refresh({ refreshing: $$this.refreshing }) {
              List() {
                ForEach(this.store.groups(new Date(this.todayMs)), (group: AnniversaryGroup) => {
                  ListItemGroup({ header: this.groupHeader(group.title) }) {
                    ForEach(group.entries, (entry: AnniversaryEntry) => {
                      ListItem() {
                        this.row(entry)
                      }
                      // 组内卡片间距必须加在 ListItem 上：List 的 space 只对 ListItemGroup 之间生效
                      .margin({ bottom: CARD_GAP })
                    }, (entry: AnniversaryEntry) => entry.item.id.toString() + '_' + this.dataVersion.toString())
                  }
                  .backgroundColor('#00000000')
                }, (group: AnniversaryGroup) => group.title + '_' + this.dataVersion.toString())

                ListItem() {
                  Column().height(90)
                }
              }
              .width('100%')
              .height('100%')
              .padding({ left: 20, right: 20, top: 6 })
              .scrollBar(BarState.Off)
              .edgeEffect(EdgeEffect.Spring)
            }
            .onRefreshing(() => {
              this.reload(true);
            })
            .width('100%')
            .height('100%')
          } else {
            this.emptyOverlay()
          }

          if (!this.loadingList && this.store.loaded && this.loadError.length === 0 &&
            this.store.items.length === 0) {
            PageState({
              kind: 'empty',
              isDark: this.isDark,
              title: I18n.t('emptyTitle'),
              description: I18n.t('emptyDesc')
            })
          }
        }
        .layoutWeight(1)
        .width('100%')
      }
      .width('100%')
      .height('100%')

      Button({ type: ButtonType.Circle, stateEffect: true }) {
        Icon({ name: 'plus', glyphSize: 24, color: '#FFFFFF' })
      }
      .width(52)
      .height(52)
      .linearGradient({ angle: 90, colors: [[Theme.accentStart, 0.0], [Theme.accentEnd, 1.0]] })
      .shadow({ radius: 18, color: Theme.withAlpha(Theme.accent(this.isDark), 0.45), offsetY: 6 })
      .margin({ right: 20, bottom: 24 + this.bottomInset })
      .onClick(() => {
        Logger.event('点击新建纪念日');
        this.path.pushPath({ name: Routes.EDIT, param: new RouteParam(0) });
      })
    }
    .width('100%')
    .height('100%')
  }
}
