import SwiftUI
import UIKit

/// 新建或编辑纪念日；切换历法时保留同一个实际日期。
struct AnniversaryEditView: View {
    @ObservedObject var api: DaymarkAPI
    let id: Int64?
    @Environment(\.dismiss) private var dismiss
    @Environment(\.locale) private var locale
    @State private var item = Anniversary()
    @State private var selectedDate = Date()
    @State private var dialog: Dialog?
    @State private var loading = false
    @State private var loaded = false
    @State private var saving = false
    @State private var deleteAfterDismiss = false
    @State private var error: String?

    private enum Dialog: String, Identifiable { case date, delete; var id: String { rawValue } }

    var body: some View {
        VStack(spacing: 0) {
            if loading { ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity) }
            else if !loaded {
                VStack(spacing: 12) { Text(LocalizedStringKey(error ?? "加载失败")).foregroundStyle(.secondary); GlassPrimaryButton(title: "重试") { Task { await load() } } }.padding(24)
                Spacer()
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        label("标题")
                        GlassInput(placeholder: "标题", text: $item.name)
                        label("备注")
                        TextEditor(text: $item.description).scrollContentBackground(.hidden).font(.system(size: 14)).frame(height: 92).padding(.horizontal, 12).modifier(GlassSurface(radius: 16))
                        label("日期类型")
                        Picker("日期类型", selection: $item.calendarType) {
                            Text("公历").tag(CalendarKind.solar.rawValue)
                            Text("农历").tag(CalendarKind.lunar.rawValue)
                        }.pickerStyle(.segmented)
                        label("纪念日日期")
                        Button { withoutChoiceDialogPresentationAnimation { dialog = .date } } label: { HStack { dateDescription; Spacer(); Image(systemName: "chevron.right") }.font(.system(size: 14)).frame(maxWidth: .infinity, minHeight: 32) }.modifier(SystemGlassButton()).buttonBorderShape(.roundedRectangle(radius: 14)).tint(GlassPalette.accent)
                        label("重复模式")
                        HStack(spacing: 8) {
                            repeatOption("不重复", .none)
                            repeatOption("每月重复", .monthly)
                            repeatOption("每年重复", .yearly)
                        }
                        if let error { Text(LocalizedStringKey(error)).font(.system(size: 12)).foregroundStyle(.red) }
                    }.padding(.horizontal, 24).padding(.top, 10).padding(.bottom, 20)
                }.scrollIndicators(.hidden)
                GlassPrimaryButton(title: saving ? "保存中…" : "保存纪念日", disabled: saving || item.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) { Task { await save() } }.padding(.horizontal, 24).padding(.bottom, 16)
            }
        }
        .modifier(SystemNavigationHeader(title: id == nil ? "新建纪念日" : "编辑纪念日", back: { dismiss() }, trailing: id == nil ? nil : AnyView(Button { withoutChoiceDialogPresentationAnimation { dialog = .delete } } label: { Image(systemName: "trash") }.tint(.red).disabled(!loaded || saving).accessibilityLabel("删除纪念日"))))
        .fullScreenCover(item: $dialog, onDismiss: {
            guard deleteAfterDismiss else { return }
            deleteAfterDismiss = false
            Task { await delete() }
        }) { selected in
            Group {
                switch selected {
                case .date:
                    DateSelectionSheet(date: $selectedDate, calendarType: item.calendarType, dismiss: { dialog = nil })
                case .delete:
                    ChoiceDialog(title: "删除纪念日", placement: .center, confirmTitle: "删除", destructive: true, cancel: { dialog = nil }, confirm: {
                        deleteAfterDismiss = true
                        dialog = nil
                    }) {
                        Text("确定删除这个纪念日吗？删除后无法恢复。").font(.subheadline)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }.presentationBackground(.clear)
        }
        .task { await load() }
    }
    private var dateDescription: Text {
        let formatter = DateFormatter(); formatter.locale = locale
        let chinese = locale.language.languageCode?.identifier == "zh" || locale.language.languageCode?.identifier == "yue"
        if item.calendarType == CalendarKind.lunar.rawValue, let parts = AnniversaryCalendar.lunarParts(for: selectedDate), chinese {
            formatter.calendar = AnniversaryCalendar.chinese; formatter.locale = Locale(identifier: "zh_CN"); formatter.dateFormat = "MMMM"
            return Text(verbatim: "（\(parts.year)）\(AnniversaryCalendar.yearName(parts.year))年\(formatter.string(from: selectedDate))\(AnniversaryCalendar.dayName(parts.day))")
        }
        formatter.dateFormat = chinese ? "yyyy 年 M 月 d 日" : DateFormatter.dateFormat(fromTemplate: "yMMMMd", options: 0, locale: locale)
        if item.calendarType == CalendarKind.lunar.rawValue { formatter.calendar = AnniversaryCalendar.chinese }
        return Text(verbatim: formatter.string(from: selectedDate))
    }
    private func label(_ text: String) -> some View { Text(LocalizedStringKey(text)).font(.system(size: 12, weight: .medium)).foregroundStyle(.secondary) }
    private func repeatOption(_ title: String, _ value: RepeatKind) -> some View {
        Button { item.loopType = value.rawValue } label: { Text(LocalizedStringKey(title)).font(.system(size: 13, weight: .medium)).frame(maxWidth: .infinity, minHeight: 48) }.modifier(SystemGlassButton(prominent: item.loopType == value.rawValue)).buttonBorderShape(.roundedRectangle(radius: 16)).tint(GlassPalette.accent)
    }
    private func load() async {
        guard !loaded && !loading else { return }
        guard let id else { loaded = true; return }
        loading = true; error = nil; defer { loading = false }
        do {
            let existing = try await api.get(id: id)
            guard let date = AnniversaryCalendar.originalDate(existing) else { throw DaymarkError.invalidData }
            item = existing; selectedDate = date; loaded = true
        } catch { self.error = error.localizedDescription }
    }
    /// 等待服务端确认删除后再返回首页。
    private func delete() async {
        guard let id, loaded, !saving else { return }
        saving = true; error = nil; defer { saving = false }
        do { try await api.delete(id: id); dismiss() } catch { self.error = error.localizedDescription }
    }
    private func save() async {
        guard loaded && !saving, !item.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        DaymarkLog.event(id == nil ? "保存新建纪念日" : "保存修改纪念日")
        var updated = item
        updated.name = updated.name.trimmingCharacters(in: .whitespacesAndNewlines)
        if updated.calendarType == CalendarKind.lunar.rawValue {
            guard let lunar = AnniversaryCalendar.lunarParts(for: selectedDate) else { error = "日期无效"; return }
            updated.year = lunar.year; updated.month = lunar.month; updated.day = lunar.day; updated.isLeapMonth = lunar.leap ? 1 : 0
        } else {
            let parts = AnniversaryCalendar.gregorian.dateComponents([.year, .month, .day], from: selectedDate)
            guard let year = parts.year, let month = parts.month, let day = parts.day else { error = "日期无效"; return }
            updated.year = year; updated.month = month; updated.day = day; updated.isLeapMonth = 0
        }
        saving = true; error = nil; defer { saving = false }
        do { try await api.save(updated); DaymarkLog.event("纪念日保存成功"); dismiss() } catch { DaymarkLog.event("纪念日保存失败：\(type(of: error))"); self.error = error.localizedDescription }
    }
}

/// 日期弹窗只在确认时提交草稿，取消时保留原日期。
struct DateSelectionSheet: View {
    @Binding var date: Date
    let calendarType: String
    var actionPlacement: ChoiceDialogActionPlacement
    let dismiss: () -> Void
    @State private var draftDate: Date

    init(date: Binding<Date>, calendarType: String, actionPlacement: ChoiceDialogActionPlacement = .footer, dismiss: @escaping () -> Void) {
        _date = date
        self.calendarType = calendarType
        self.actionPlacement = actionPlacement
        self.dismiss = dismiss
        var initialDate = date.wrappedValue
        // 首次布局就使用已保存的日期，超出农历滚轮范围时沿用原有边界处理。
        let years = AnniversaryCalendar.selectableLunarYears
        if calendarType == CalendarKind.lunar.rawValue, let parts = AnniversaryCalendar.lunarParts(for: initialDate), !years.contains(parts.year) {
            let year = min(max(parts.year, years.lowerBound), years.upperBound)
            let leap = parts.leap && AnniversaryCalendar.solarDate(year: year, month: parts.month, day: 1, leap: true) != nil
            initialDate = AnniversaryCalendar.solarDate(year: year, month: parts.month, day: parts.day, leap: leap)
                ?? AnniversaryCalendar.solarDate(year: year, month: parts.month, day: min(parts.day, 29), leap: leap)
                ?? initialDate
        }
        _draftDate = State(initialValue: initialDate)
    }

    var body: some View {
        let isLunar = calendarType == CalendarKind.lunar.rawValue
        // UIKit 农历滚轮直接在最终位置显示，避免入场位移影响首个拖动手势。
        ChoiceDialog(title: "选择纪念日日期", actionPlacement: actionPlacement, animatesEntrance: !isLunar, cancel: dismiss, confirm: {
            DaymarkLog.event("确认日期选择")
            date = draftDate
            dismiss()
        }) {
            if isLunar {
                // 先拿到弹窗实际宽度，再创建滚轮，避免首次布局重载列。
                GeometryReader { geometry in
                    let width = floor(geometry.size.width)
                    if width > 0 {
                        LunarDateWheels(date: $draftDate, width: width)
                            .frame(width: width, height: LunarDateWheels.height)
                    }
                }.frame(height: LunarDateWheels.height)
            } else {
                DatePicker("日期", selection: $draftDate, displayedComponents: .date).datePickerStyle(.wheel).labelsHidden().environment(\.calendar, AnniversaryCalendar.gregorian)
            }
        }.onChange(of: draftDate) { _, _ in DaymarkLog.event("日期选择器变更") }
    }
}

/// 由同一个系统滚轮排布年月日三列，并根据实际农历日期更新闰月和月末。
private struct LunarDateWheels: UIViewRepresentable {
    static let height: CGFloat = 216

    @Binding var date: Date
    let width: CGFloat

    func makeCoordinator() -> Coordinator { Coordinator(date: $date) }

    func makeUIView(context: Context) -> UIPickerView {
        let picker = UIPickerView(frame: CGRect(x: 0, y: 0, width: width, height: Self.height))
        context.coordinator.updateSelection(in: picker, width: width)
        return picker
    }

    func updateUIView(_ picker: UIPickerView, context: Context) {
        context.coordinator.date = $date
        context.coordinator.updateSelection(in: picker, width: width)
    }

    /// 按需提供滚轮行，并在年月变化时刷新受影响的列。
    final class Coordinator: NSObject, UIPickerViewDataSource, UIPickerViewAccessibilityDelegate {
        private enum Column: Int, CaseIterable { case year, month, day }

        var date: Binding<Date>
        private let years = AnniversaryCalendar.selectableLunarYears
        private let columnInsets: CGFloat = 16
        private let yearWidthRatio: CGFloat = 0.5
        private let monthWidthRatio: CGFloat = 0.28
        private var layoutWidth: CGFloat = 0
        private var displayedDate: Date?
        private var months: [Date] = []
        private var displayedYear: Int?
        private var dayCount = 0
        private let monthFormatter: DateFormatter = {
            let formatter = DateFormatter()
            formatter.calendar = AnniversaryCalendar.chinese; formatter.locale = Locale(identifier: "zh_CN"); formatter.dateFormat = "MMMM"
            return formatter
        }()

        init(date: Binding<Date>) { self.date = date }

        /// 首次填好行数据再交给 UIKit；无关的视图刷新不打断手指滚动。
        func updateSelection(in picker: UIPickerView, width: CGFloat) {
            let value = date.wrappedValue
            let widthChanged = layoutWidth != width
            guard widthChanged || displayedDate != value else { return }
            let calendar = AnniversaryCalendar.chinese
            guard let parts = AnniversaryCalendar.lunarParts(for: value), years.contains(parts.year) else { return }
            let monthsChanged = displayedYear != parts.year
            if monthsChanged, let interval = calendar.dateInterval(of: .year, for: value) {
                months.removeAll()
                var current = interval.start
                while current < interval.end {
                    months.append(current)
                    guard let next = calendar.date(byAdding: .month, value: 1, to: current), next > current else { break }
                    current = next
                }
                displayedYear = parts.year
            }
            guard let month = months.firstIndex(where: { start in
                let candidate = calendar.dateComponents([.month, .isLeapMonth], from: start)
                return candidate.month == parts.month && (candidate.isLeapMonth ?? false) == parts.leap
            }), let count = AnniversaryCalendar.lunarMonthLength(containing: months[month]) else { return }
            let daysChanged = dayCount != count
            dayCount = count
            layoutWidth = width
            let firstUpdate = displayedDate == nil
            displayedDate = value
            if firstUpdate {
                picker.dataSource = self; picker.delegate = self
            } else if widthChanged {
                picker.reloadAllComponents()
            } else {
                if monthsChanged { picker.reloadComponent(Column.month.rawValue) }
                if daysChanged { picker.reloadComponent(Column.day.rawValue) }
            }
            select(parts.year - years.lowerBound, in: .year, picker: picker)
            select(month, in: .month, picker: picker)
            select(parts.day - 1, in: .day, picker: picker)
        }

        private func select(_ row: Int, in column: Column, picker: UIPickerView) {
            if picker.selectedRow(inComponent: column.rawValue) != row { picker.selectRow(row, inComponent: column.rawValue, animated: false) }
        }

        func numberOfComponents(in pickerView: UIPickerView) -> Int { Column.allCases.count }

        /// 年份同时显示数字与干支，月份也比日期多预留一个闰月字位。
        func pickerView(_ pickerView: UIPickerView, widthForComponent component: Int) -> CGFloat {
            let width = layoutWidth - columnInsets
            let ratio: CGFloat
            switch Column(rawValue: component) {
            case .year: ratio = yearWidthRatio
            case .month: ratio = monthWidthRatio
            case .day: ratio = 1 - yearWidthRatio - monthWidthRatio
            case nil: return 0
            }
            return max(1, floor(width * ratio))
        }

        func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
            switch Column(rawValue: component) {
            case .year: return years.count
            case .month: return months.count
            case .day: return dayCount
            case nil: return 0
            }
        }

        func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
            switch Column(rawValue: component) {
            case .year:
                let year = row + years.lowerBound
                return "\(year) \(AnniversaryCalendar.yearName(year))"
            case .month: return monthFormatter.string(from: months[row])
            case .day: return AnniversaryCalendar.dayName(row + 1)
            case nil: return nil
            }
        }

        func pickerView(_ pickerView: UIPickerView, accessibilityLabelForComponent component: Int) -> String? {
            switch Column(rawValue: component) {
            case .year: return NSLocalizedString("年", comment: "农历年份")
            case .month: return NSLocalizedString("月", comment: "农历月份")
            case .day: return NSLocalizedString("日", comment: "农历日期")
            case nil: return nil
            }
        }

        /// 切换年份或月份时尽量保留原日期，目标月份少一天时选中月末。
        func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
            let value = date.wrappedValue, calendar = AnniversaryCalendar.chinese
            guard let parts = AnniversaryCalendar.lunarParts(for: value) else { return }
            let selected: Date?
            switch Column(rawValue: component) {
            case .year:
                let year = row + years.lowerBound
                let leap = parts.leap && AnniversaryCalendar.solarDate(year: year, month: parts.month, day: 1, leap: true) != nil
                selected = AnniversaryCalendar.solarDate(year: year, month: parts.month, day: parts.day, leap: leap)
                    ?? AnniversaryCalendar.solarDate(year: year, month: parts.month, day: min(parts.day, 29), leap: leap)
            case .month:
                let start = months[row]
                guard let count = AnniversaryCalendar.lunarMonthLength(containing: start) else { return }
                selected = calendar.date(byAdding: .day, value: min(parts.day, count) - 1, to: start)
            case .day:
                selected = calendar.date(byAdding: .day, value: row + 1 - parts.day, to: value)
            case nil: return
            }
            if let selected { date.wrappedValue = selected }
        }
    }
}
