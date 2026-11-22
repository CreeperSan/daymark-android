import { Theme } from '../Theme';

/**
 * 滚轮列。List 居中吸附 + 上下留白，使第一项和最后一项也能停在中心，
 * 吸附停止后把当前中心索引交给父组件，由父组件决定是否联动其它列。
 */
@Component
export struct WheelColumn {
  @Prop items: string[] = [];
  @Prop @Watch('onIndexChanged') index: number = 0;
  @Prop rowHeight: number = 40;
  @Prop viewHeight: number = 200;
  /** 普通行字号；农历年份这类长标签需要调小。 */
  @Prop itemFontSize: number = 15;
  /** 居中行字号。 */
  @Prop activeFontSize: number = 18;
  @Prop isDark: boolean = false;
  @State centerIndex: number = 0;
  onSettle: (index: number) => void = () => {
  };
  private scroller: Scroller = new Scroller();

  aboutToAppear(): void {
    this.centerIndex = this.index;
  }

  onIndexChanged(): void {
    if (this.centerIndex !== this.index) {
      this.centerIndex = this.index;
      this.scroller.scrollToIndex(this.index, false);
    }
  }

  build() {
    List({ scroller: this.scroller, initialIndex: this.index }) {
      ForEach(this.items, (item: string, position: number) => {
        ListItem() {
          Text(item)
            .width('100%')
            .height(this.rowHeight)
            .textAlign(TextAlign.Center)
            .fontSize(position === this.centerIndex ? this.activeFontSize : this.itemFontSize)
            .fontWeight(position === this.centerIndex ? FontWeight.Medium : FontWeight.Normal)
            .fontColor(position === this.centerIndex ? Theme.textPrimary(this.isDark) : Theme.textSecondary(this.isDark))
            .opacity(position === this.centerIndex ? 1 : 0.62)
            // 长标签（如“甲辰龙年（2024）”）必须单行显示，否则会把行高顶开
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
      }, (item: string, position: number) => item + '_' + position.toString())
    }
    .width('100%')
    .height(this.viewHeight)
    .scrollBar(BarState.Off)
    .edgeEffect(EdgeEffect.None)
    .scrollSnapAlign(ScrollSnapAlign.CENTER)
    .contentStartOffset((this.viewHeight - this.rowHeight) / 2)
    .contentEndOffset((this.viewHeight - this.rowHeight) / 2)
    .onScrollIndex((start: number, end: number, center: number) => {
      if (center >= 0 && center < this.items.length && center !== this.centerIndex) {
        this.centerIndex = center;
      }
    })
    .onScrollStop(() => {
      this.onSettle(this.centerIndex);
    })
  }
}
