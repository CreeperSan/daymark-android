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
            GlassHeader(title: id == nil ? "新建纪念日" : "编辑纪念日", back: { dismiss() }, trailing: AnyView(Button("保存") { Task { await save() } }.font(.system(size: 14, weight: .semibold)).foregroundStyle(GlassPalette.accent).disabled(!loaded || saving || item.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)))
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
                if id != nil { Button("删除纪念日", role: .destructive) { confirmDelete = true }.buttonStyle(WarningButtonStyle()).disabled(saving).padding(.horizontal, 24).padding(.bottom, 8) }
                GlassPrimaryButton(title: saving ? "保存中…" : "保存纪念日", disabled: saving || item.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) { Task { await save() } }.padding(.horizontal, 24).padding(.bottom, 16)
            }
        }
        .alert("删除纪念日", isPresented: $confirmDelete) {
            Button("取消", role: .cancel) {}
            Button("删除", role: .destructive) { Task { await delete() } }
        } message: { Text("确定删除这个纪念日吗？删除后无法恢复。") }
        .sheet(isPresented: $datePicker) {
            DateSelectionSheet(date: $selectedDate, calendarType: $item.calendarType).presentationDetents([.height(360), .large]).presentationDragIndicator(.visible)
        }
        .task { await load() }
    }
    private var dateDescription: Text {
        let formatter = DateFormatter(); formatter.locale = locale; formatter.dateFormat = "yyyy.MM.dd"
        if item.calendarType == CalendarKind.lunar.rawValue {
            let lunar = DateFormatter(); lunar.calendar = AnniversaryCalendar.chinese; lunar.locale = locale; lunar.setLocalizedDateFormatFromTemplate("MMMMd")
            return Text(verbatim: formatter.string(from: selectedDate) + " · ") + Text("农历") + Text(verbatim: " " + lunar.string(from: selectedDate))
        }
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
    @Environment(\.dismiss) private var dismiss
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
            DatePicker("日期", selection: $draftDate, displayedComponents: .date).datePickerStyle(.wheel).labelsHidden().environment(\.calendar, draftCalendar == CalendarKind.lunar.rawValue ? AnniversaryCalendar.chinese : AnniversaryCalendar.gregorian)
            Spacer(minLength: 0)
        }.padding(20).onAppear { draftDate = date; draftCalendar = calendarType }
            .onChange(of: draftDate) { _, _ in DaymarkLog.event("日期选择器变更") }
            .onChange(of: draftCalendar) { _, _ in DaymarkLog.event("日期选择器历法变更") }
    }
}
