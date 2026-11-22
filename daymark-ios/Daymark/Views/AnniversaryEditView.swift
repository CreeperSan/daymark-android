import SwiftUI

/// 新建或编辑纪念日；切换历法时保留同一个实际日期。
struct AnniversaryEditView: View {
    @ObservedObject var api: DaymarkAPI
    let id: Int64?
    @Environment(\.dismiss) private var dismiss
    @Environment(\.locale) private var locale
    @State private var item = Anniversary()
    @State private var selectedDate = Date()
    @State private var datePicker = false
    @State private var loading = false
    @State private var loaded = false
    @State private var saving = false
    @State private var confirmDelete = false
    @State private var error: String?

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
                        Button { datePicker = true } label: { HStack { dateDescription; Spacer(); Image(systemName: "chevron.right") }.font(.system(size: 14)).frame(maxWidth: .infinity, minHeight: 32) }.modifier(SystemGlassButton()).buttonBorderShape(.roundedRectangle(radius: 14)).tint(GlassPalette.accent)
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
        .modifier(SystemNavigationHeader(title: id == nil ? "新建纪念日" : "编辑纪念日", back: { dismiss() }, trailing: id == nil ? nil : AnyView(Button { confirmDelete = true } label: { Image(systemName: "trash") }.tint(.red).disabled(!loaded || saving).accessibilityLabel("删除纪念日"))))
        .alert("删除纪念日", isPresented: $confirmDelete) {
            Button("取消", role: .cancel) {}
            Button("删除", role: .destructive) { Task { await delete() } }
        } message: { Text("确定删除这个纪念日吗？删除后无法恢复。") }
        .overlay {
            if datePicker {
                ZStack(alignment: .bottom) {
                    Color.black.opacity(0.35).ignoresSafeArea().contentShape(Rectangle()).onTapGesture { datePicker = false }
                    DateSelectionSheet(date: $selectedDate, calendarType: $item.calendarType, dismiss: { datePicker = false })
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24)).padding(.horizontal, 12).padding(.bottom, 12)
                }
            }
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

/// 使用系统滚轮日期选择器，农历使用系统中国农历并保留闰月信息。
struct DateSelectionSheet: View {
    @Binding var date: Date
    @Binding var calendarType: String
    let dismiss: () -> Void
    @State private var draftDate: Date = Date()
    @State private var draftCalendar = CalendarKind.solar.rawValue
    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Button("取消") { dismiss() }
                Spacer(); Text("选择纪念日日期").font(.system(size: 16, weight: .semibold)); Spacer()
                Button("确定") { DaymarkLog.event("确认日期选择"); date = draftDate; calendarType = draftCalendar; dismiss() }
            }.foregroundStyle(GlassPalette.accent)
            Picker("日期类型", selection: $draftCalendar) { Text("公历").tag(CalendarKind.solar.rawValue); Text("农历").tag(CalendarKind.lunar.rawValue) }.pickerStyle(.segmented)
            if draftCalendar == CalendarKind.lunar.rawValue {
                LunarDateWheels(date: $draftDate)
            } else {
                DatePicker("日期", selection: $draftDate, displayedComponents: .date).datePickerStyle(.wheel).labelsHidden().environment(\.calendar, AnniversaryCalendar.gregorian)
            }
            Spacer(minLength: 0)
        }.padding(20).onAppear { draftDate = date; draftCalendar = calendarType }
            .onChange(of: draftDate) { _, _ in DaymarkLog.event("日期选择器变更") }
            .onChange(of: draftCalendar) { _, _ in DaymarkLog.event("日期选择器历法变更") }
    }
}

/// 农历滚轮以农历年起始年份标注干支，并按实际月份处理闰月和月末。
private struct LunarDateWheels: View {
    @Binding var date: Date
    private var parts: (year: Int, month: Int, day: Int, leap: Bool)? { AnniversaryCalendar.lunarParts(for: date) }
    private var months: [Date] {
        let calendar = AnniversaryCalendar.chinese
        guard let interval = calendar.dateInterval(of: .year, for: date) else { return [] }
        var result: [Date] = []; var current = interval.start
        while current < interval.end {
            result.append(current)
            guard let next = calendar.date(byAdding: .month, value: 1, to: current), next > current else { break }
            current = next
        }
        return result
    }
    private func monthLabel(_ date: Date) -> String {
        let formatter = DateFormatter(); formatter.calendar = AnniversaryCalendar.chinese; formatter.locale = Locale(identifier: "zh_CN"); formatter.dateFormat = "MMMM"
        return formatter.string(from: date)
    }
    var body: some View {
        if let parts {
            let selectedMonth = months.firstIndex { candidate in
                let value = AnniversaryCalendar.chinese.dateComponents([.month, .isLeapMonth], from: candidate)
                return value.month == parts.month && (value.isLeapMonth ?? false) == parts.leap
            } ?? 0
            let count = AnniversaryCalendar.chinese.range(of: .day, in: .month, for: date)?.count ?? 30
            HStack(spacing: 0) {
                Picker("年", selection: Binding(get: { parts.year }, set: { year in
                    let leap = parts.leap && AnniversaryCalendar.solarDate(year: year, month: parts.month, day: 1, leap: true) != nil
                    if let value = AnniversaryCalendar.solarDate(year: year, month: parts.month, day: parts.day, leap: leap) ?? AnniversaryCalendar.solarDate(year: year, month: parts.month, day: min(parts.day, 29), leap: leap) { date = value }
                })) {
                    ForEach(1...9998, id: \.self) { year in Text("（\(year)）\(AnniversaryCalendar.yearName(year))").tag(year) }
                }.frame(maxWidth: .infinity)
                Picker("月", selection: Binding(get: { selectedMonth }, set: { index in
                    guard months.indices.contains(index), let count = AnniversaryCalendar.chinese.range(of: .day, in: .month, for: months[index])?.count,
                          let value = AnniversaryCalendar.chinese.date(byAdding: .day, value: min(parts.day, count) - 1, to: months[index]) else { return }
                    date = value
                })) {
                    ForEach(months.indices, id: \.self) { index in Text(monthLabel(months[index])).tag(index) }
                }.frame(maxWidth: .infinity)
                Picker("日", selection: Binding(get: { parts.day }, set: { day in
                    if let value = AnniversaryCalendar.chinese.date(byAdding: .day, value: day - parts.day, to: date) { date = value }
                })) {
                    ForEach(1...count, id: \.self) { day in Text(AnniversaryCalendar.dayName(day)).tag(day) }
                }.frame(maxWidth: .infinity)
            }.pickerStyle(.wheel).labelsHidden().frame(height: 216).clipped()
        }
    }
}
