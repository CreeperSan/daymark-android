import { Theme, Metrics } from './Theme';
import { I18n } from '../core/i18n/Strings';
import { Logger } from '../core/Logger';
import { Toast } from '../core/Toast';
import { ErrorText } from '../core/ErrorText';
import { GlassCard, GlassChoice, GlassHeader, GlassInput, GlassPrimaryButton, GlassSegments } from './components/Glass';
import { Icon } from './components/Icon';
import { DatePickerSheet } from './components/DatePickerSheet';
import { ConfirmOverlay } from './components/ConfirmOverlay';
import { Anniversary, CalendarKind, RepeatKind } from '../model/Anniversary';
import { AnniversaryStore } from '../service/AnniversaryStore';
import { AnniversaryCalendar } from '../calendar/AnniversaryCalendar';
import { LunarCalendar, LunarParts } from '../calendar/LunarCalendar';
import { DateUtils } from '../common/DateUtils';

/**
 * 新建 / 编辑纪念日。日期以“实际公历日期”为准，保存时按当前历法写回原始年月日与闰月标记，
 * 这样切换公历 / 农历不会改变真实日期，跨平台数据也保持一致。
 */
@Component
export struct EditView {
  @Prop isDark: boolean = false;
  /** 语言标签：只用于触发页面重建，语言切换后所有文案需要重新求值 */
  @Prop langTag: string = '';
  @Prop anniversaryId: number = 0;
  path: NavPathStack = new NavPathStack();
  @State item: Anniversary = new Anniversary();
  @State name: string = '';
  @State note: string = '';
  @State calendarType: string = CalendarKind.SOLAR;
  @State repeatType: string = RepeatKind.YEARLY;
  @State selectedMs: number = Date.now();
  @State loading: boolean = false;
  @State loaded: boolean = false;
  @State saving: boolean = false;
  @State error: string = '';
  @State showDateSheet: boolean = false;
  @StorageProp('bottomInset') bottomInset: number = 0;
  @State showDeleteConfirm: boolean = false;
  private store: AnniversaryStore = AnniversaryStore.shared();

  aboutToAppear(): void {
    Logger.event(this.anniversaryId > 0 ? '打开编辑纪念日：' + this.anniversaryId : '打开新建纪念日');
    this.load();
  }

  private async load(): Promise<void> {
    if (this.anniversaryId === 0) {
      this.loaded = true;
      return;
    }
    this.loading = true;
    this.error = '';
    try {
      const existing: Anniversary = await this.store.get(this.anniversaryId);
      this.item = existing;
      this.name = existing.name;
      this.note = existing.description;
      this.calendarType = existing.calendarType;
      this.repeatType = existing.loopType;
      const original: Date | null = AnniversaryCalendar.originalDate(existing);
      this.selectedMs = original === null ? Date.now() : original.getTime();
      this.loaded = true;
    } catch (err) {
      this.error = ErrorText.of(err as Object);
      Logger.event('加载纪念日失败');
    } finally {
      this.loading = false;
    }
  }

  private canSave(): boolean {
    return this.loaded && !this.saving && this.name.trim().length > 0;
  }

  /** 写入原始日期字段：农历记录闰月，公历固定 isLeapMonth = 0。 */
  private applyDate(target: Anniversary): boolean {
    const date: Date = new Date(this.selectedMs);
    if (this.calendarType === CalendarKind.LUNAR) {
      const parts: LunarParts | null = LunarCalendar.parts(date);
      if (parts === null) {
        this.error = I18n.t('invalidDate');
        return false;
      }
      target.year = parts.year;
      target.month = parts.month;
      target.day = parts.day;
      target.isLeapMonth = parts.leap ? 1 : 0;
      return true;
    }
    target.year = date.getFullYear();
    target.month = date.getMonth() + 1;
    target.day = date.getDate();
    target.isLeapMonth = 0;
    return true;
  }

  private async save(): Promise<void> {
    if (!this.canSave()) {
      return;
    }
    this.saving = true;
    this.error = '';
    const updated: Anniversary = this.item.copy();
    updated.name = this.name.trim();
    updated.description = this.note;
    updated.calendarType = this.calendarType;
    updated.loopType = this.repeatType;
    if (!this.applyDate(updated)) {
      this.saving = false;
      return;
    }
    try {
      const saved: Anniversary = await this.store.save(updated);
      Logger.event('保存纪念日成功：' + saved.id.toString());
      this.path.pop();
    } catch (err) {
      this.error = ErrorText.of(err as Object);
      Logger.event('保存纪念日失败');
    } finally {
      this.saving = false;
    }
  }

  private confirmDelete(): void {
    this.showDeleteConfirm = true;
  }

  private async deleteItem(): Promise<void> {
    if (this.loaded === false || this.saving) {
      return;
    }
    this.saving = true;
    try {
      await this.store.remove(this.anniversaryId);
      Logger.event('删除纪念日成功：' + this.anniversaryId.toString());
      this.path.pop();
    } catch (err) {
      this.error = ErrorText.of(err as Object);
      Logger.event('删除纪念日失败');
    } finally {
      this.saving = false;
    }
  }

  @Builder
  label(text: string) {
    Text(I18n.t(text))
      .fontSize(12)
      .fontWeight(FontWeight.Medium)
      .fontColor(Theme.textSecondary(this.isDark))
      .margin({ top: 10, left: 4 })
  }

  build() {
    Stack() {
      Column() {
        GlassHeader({
          title: this.anniversaryId > 0 ? I18n.t('editAnniversary') : I18n.t('newAnniversary'),
          showBack: true,
          trailingText: I18n.t('save'),
          trailingEnabled: this.canSave(),
          isDark: this.isDark,
          onBack: () => {
            this.path.pop();
          },
          onTrailing: () => {
            this.save();
          }
        })

        if (this.loading) {
          Column() {
            LoadingProgress()
              .width(28)
              .height(28)
              .color(Theme.accent(this.isDark))
          }
          .layoutWeight(1)
          .width('100%')
          .justifyContent(FlexAlign.Center)
        } else if (!this.loaded) {
          Column({ space: 12 }) {
            Text(this.error.length > 0 ? this.error : I18n.t('loadingFailed'))
              .fontSize(14)
              .fontColor(Theme.textSecondary(this.isDark))
            Row() {
              GlassPrimaryButton({
                title: I18n.t('retry'),
                isDark: this.isDark,
                onTap: () => {
                  this.load();
                }
              })
            }
            .width(160)
          }
          .layoutWeight(1)
          .width('100%')
          .padding(24)
          .justifyContent(FlexAlign.Center)
        } else {
          Scroll() {
            Column({ space: 8 }) {
              this.label('titleLabel')
              GlassInput({
                placeholder: I18n.t('titlePlaceholder'),
                text: this.name,
                isDark: this.isDark,
                onTextChange: (value: string) => {
                  this.name = value;
                }
              })

              this.label('noteLabel')
              TextArea({ text: this.note, placeholder: I18n.t('notePlaceholder') })
                .width('100%')
                .height(92)
                .fontSize(14)
                .fontColor(Theme.textPrimary(this.isDark))
                .placeholderColor(Theme.textSecondary(this.isDark))
                .backgroundColor(Theme.inputFill(this.isDark))
                .borderRadius(Metrics.INPUT_RADIUS)
                .border({ width: 1, color: Theme.inputStroke(this.isDark) })
                .padding(12)
                .onChange((value: string) => {
                  this.note = value;
                })

              this.label('dateType')
              GlassSegments({
                values: [CalendarKind.SOLAR, CalendarKind.LUNAR],
                labels: [I18n.t('solar'), I18n.t('lunar')],
                selected: this.calendarType,
                isDark: this.isDark,
                onSelect: (value: string) => {
                  this.calendarType = value;
                  Logger.event('切换历法：' + value);
                }
              })

              this.label('anniversaryDate')
              Row() {
                Text(AnniversaryCalendar.selectedDateText(new Date(this.selectedMs), this.calendarType))
                  .fontSize(14)
                  .fontColor(Theme.textPrimary(this.isDark))
                  .layoutWeight(1)
                  .maxLines(1)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })
                Icon({ name: 'chevron', glyphSize: 13, color: Theme.textSecondary(this.isDark) })
              }
              .width('100%')
              .height(48)
              .padding({ left: 16, right: 14 })
              .backgroundColor(Theme.inputFill(this.isDark))
              .borderRadius(14)
              .border({ width: 1, color: Theme.inputStroke(this.isDark) })
              .onClick(() => {
                this.showDateSheet = true;
              })

              this.label('repeatMode')
              Row({ space: 8 }) {
                GlassChoice({
                  title: I18n.t('repeatNone'),
                  selected: this.repeatType === RepeatKind.NONE,
                  isDark: this.isDark,
                  onTap: () => {
                    this.repeatType = RepeatKind.NONE;
                  }
                })
                GlassChoice({
                  title: I18n.t('repeatMonthly'),
                  selected: this.repeatType === RepeatKind.MONTHLY,
                  isDark: this.isDark,
                  onTap: () => {
                    this.repeatType = RepeatKind.MONTHLY;
                  }
                })
                GlassChoice({
                  title: I18n.t('repeatYearly'),
                  selected: this.repeatType === RepeatKind.YEARLY,
                  isDark: this.isDark,
                  onTap: () => {
                    this.repeatType = RepeatKind.YEARLY;
                  }
                })
              }
              .width('100%')

              if (this.error.length > 0) {
                Text(this.error)
                  .fontSize(12)
                  .fontColor(Theme.danger(this.isDark))
                  .margin({ top: 4, left: 4 })
              }
            }
            .width('100%')
            // Scroll 内容不足一屏时会垂直居中，表单必须从顶部开始排布
            .constraintSize({ minHeight: '100%' })
            .justifyContent(FlexAlign.Start)
            .alignItems(HorizontalAlign.Start)
            .padding({ left: 24, right: 24, top: 10, bottom: 24 })
          }
          .width('100%')
          .layoutWeight(1)
          .scrollBar(BarState.Off)

          Column({ space: 8 }) {
            if (this.anniversaryId > 0) {
              Button({ type: ButtonType.Capsule, stateEffect: true }) {
                Text(I18n.t('deleteAnniversary'))
                  .fontSize(16)
                  .fontWeight(FontWeight.Medium)
                  .fontColor('#FFFFFF')
              }
              .width('100%')
              .height(Metrics.BUTTON_HEIGHT)
              .backgroundColor(Theme.danger(this.isDark))
              .enabled(!this.saving)
              .onClick(() => {
                this.confirmDelete();
              })
            }

            GlassPrimaryButton({
              title: this.saving ? I18n.t('saving') : I18n.t('saveAnniversary'),
              active: this.canSave(),
              isDark: this.isDark,
              onTap: () => {
                this.save();
              }
            })
          }
          .width('100%')
          .padding({ left: 24, right: 24, top: 12, bottom: 14 + this.bottomInset })
        }
    }
      .width('100%')
      .height('100%')

      if (this.showDeleteConfirm) {
        ConfirmOverlay({
          isDark: this.isDark,
          title: I18n.t('deleteConfirmTitle'),
          message: I18n.t('deleteConfirmMessage'),
          confirmText: I18n.t('delete'),
          danger: true,
          onCancel: () => {
            this.showDeleteConfirm = false;
          },
          onConfirm: () => {
            this.showDeleteConfirm = false;
            this.deleteItem();
          }
        })
      }
    }
    .width('100%')
    .height('100%')
    .bindSheet($$this.showDateSheet, this.dateSheet(), {
      // 高度由内容决定，避免写死高度后底部留白或截断滚轮
      detents: [SheetSize.FIT_CONTENT],
      dragBar: true,
      showClose: false,
      backgroundColor: Theme.glassSolid(this.isDark),
      onDisappear: () => {
        this.showDateSheet = false;
      }
    })
  }

  @Builder
  dateSheet() {
    DatePickerSheet({
      initialTime: this.selectedMs,
      initialType: this.calendarType,
      isDark: this.isDark,
      onCancel: () => {
        this.showDateSheet = false;
      },
      onConfirm: (time: number, type: string) => {
        this.selectedMs = time;
        this.calendarType = type;
        this.showDateSheet = false;
        Logger.event('确认日期选择：' + DateUtils.formatDot(new Date(time)) + ' / ' + type);
      }
    })
  }
}
