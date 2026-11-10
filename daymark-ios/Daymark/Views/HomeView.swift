import SwiftUI

/// 云端纪念日首页，按日期分类，刷新失败时保留已有列表。
struct HomeView: View {
    @ObservedObject var api: DaymarkAPI
    @Binding var path: [DaymarkRoute]
    @Environment(\.scenePhase) private var scenePhase
    @State private var items: [Anniversary] = []
    @State private var loading = false
    @State private var manualRefreshing = false
    @State private var loadTask: Task<Void, Never>?
    @State private var loaded = false
    @State private var error: String?
    @State private var offline = false
    @State private var started = false
    @State private var currentDay = Date()
    @State private var lastDay = Calendar.current.startOfDay(for: Date())
    @State private var changesDuringLoad: [Int64: Anniversary] = [:]
    @State private var deletionsDuringLoad: Set<Int64> = []

    private var groups: [(String, [(Anniversary, Int?)])] {
        let values = items.map { ($0, AnniversaryCalendar.daysUntil($0, today: currentDay)) }
        let today = values.filter { $0.1 == 0 }
        let upcoming = values.filter { ($0.1 ?? 0) > 0 }.sorted { ($0.1 ?? 0) < ($1.1 ?? 0) }
        let past = values.filter { ($0.1 ?? 0) < 0 }.sorted { ($0.1 ?? 0) > ($1.1 ?? 0) }
        let unknown = values.filter { $0.1 == nil }
        return [("今天", today), ("即将到来", upcoming), ("已经过去", past), ("其他", unknown)].filter { !$0.1.isEmpty }
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                if error == nil { ForEach(groups, id: \.0) { title, entries in
                    Text(LocalizedStringKey(title)).font(.system(size: 12, weight: .medium)).foregroundStyle(.secondary)
                    ForEach(entries, id: \.0.id) { item, days in
                        Button { DaymarkLog.event("打开纪念日编辑"); path.append(.edit(item.id)) } label: { AnniversaryRow(item: item, days: days) }.buttonStyle(.plain)
                    }
                } }
            }.padding(.horizontal, 20).padding(.top, 8).padding(.bottom, 100)
        }
        .overlay {
            if !loading {
                if error != nil { GlassPageState(kind: offline ? .offline : .failed, retry: { Task { await load() } }) }
                else if loaded && items.isEmpty { GlassPageState(kind: .empty).allowsHitTesting(false) }
            }
        }
        .overlay(alignment: .top) {
            // 系统下拉刷新已有指示器；首次和跨天加载使用同样的系统进度样式。
            if loading && !manualRefreshing { ProgressView().padding(.top, 8).allowsHitTesting(false) }
        }
        .scrollIndicators(.hidden)
        .scrollBounceBehavior(.always, axes: .vertical)
        .refreshable {
            manualRefreshing = true; defer { manualRefreshing = false }
            currentDay = Date(); lastDay = Calendar.current.startOfDay(for: currentDay)
            await load()
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            GlassHeader(title: "我的纪念日", trailing: AnyView(Button { DaymarkLog.event("打开设置"); path.append(.settings) } label: { Image(systemName: "gearshape").font(.system(size: 15)) }.modifier(SystemGlassButton()).buttonBorderShape(.circle).tint(GlassPalette.accent)))
        }
        .overlay(alignment: .bottomTrailing) {
            Button { DaymarkLog.event("新建纪念日"); path.append(.edit(nil)) } label: { Image(systemName: "plus").font(.system(size: 24, weight: .light)).frame(width: 42, height: 42) }.modifier(SystemGlassButton(prominent: true)).buttonBorderShape(.circle).tint(GlassPalette.accent).padding(.trailing, 20).padding(.bottom, 20)
        }
        .onReceive(api.$lastSavedAnniversary.compactMap { $0 }) { item in
            // 编辑成功后合并服务端结果，不因为页面返回而刷新。
            items.removeAll { $0.id == item.id }; items.append(item)
            if loading { changesDuringLoad[item.id] = item }
        }
        .onReceive(api.$lastDeletedAnniversaryID.compactMap { $0 }) { id in
            items.removeAll { $0.id == id }
            if loading { changesDuringLoad.removeValue(forKey: id); deletionsDuringLoad.insert(id) }
        }
        .task {
            if !started { started = true; await load() }
            await refreshIfDayChanged()
            await observeDayChanges()
        }
        .onChange(of: scenePhase) { _, phase in if phase == .active { Task { await refreshIfDayChanged() } } }
    }
    /// 仅在本地日期变化时自动刷新；普通页面返回或前后台切换不请求列表。
    private func refreshIfDayChanged() async {
        let now = Date(), day = Calendar.current.startOfDay(for: Date())
        guard day != lastDay else { return }
        lastDay = day; currentDay = now
        await load()
    }
    /// 页面可见时等待下一次本地零点；重新出现时先检查是否已经跨天。
    private func observeDayChanges() async {
        while !Task.isCancelled {
            let now = Date()
            guard let next = Calendar.current.date(byAdding: .day, value: 1, to: Calendar.current.startOfDay(for: now)) else { return }
            do { try await Task.sleep(for: .seconds(max(1, next.timeIntervalSince(now)))) } catch { return }
            await refreshIfDayChanged()
        }
    }
    private func load() async {
        // 下拉时若已有请求，等待该请求完成，避免刷新指示器提前消失。
        if let loadTask { await loadTask.value; return }
        let task = Task { @MainActor in
            DaymarkLog.event("刷新纪念日列表")
            loading = true; error = nil; changesDuringLoad.removeAll(); deletionsDuringLoad.removeAll(); defer { loading = false }
            do {
                let result = try await api.list()
                items = result.filter { changesDuringLoad[$0.id] == nil && !deletionsDuringLoad.contains($0.id) } + Array(changesDuringLoad.values)
                changesDuringLoad.removeAll(); loaded = true
            } catch {
                DaymarkLog.event("刷新纪念日失败：\(type(of: error))")
                changesDuringLoad.removeAll(); self.error = error.localizedDescription
                if let network = error as? URLError { offline = [.notConnectedToInternet, .networkConnectionLost, .timedOut, .cannotFindHost, .cannotConnectToHost, .dnsLookupFailed].contains(network.code) }
                else { offline = false }
            }
        }
        loadTask = task
        await task.value
        loadTask = nil
    }
}

/// 单条纪念日卡片使用真实日期和倒计时，不使用设计稿中的示例数据。
struct AnniversaryRow: View {
    let item: Anniversary
    let days: Int?
    @Environment(\.locale) private var locale
    private var status: Text {
        guard let days else { return Text("日期不可用") }
        if days == 0 { return Text("就在今天") }
        return days > 0 ? Text("还有 \(days) 天") : Text("已过去 \(-days) 天")
    }
    private var dateText: Text {
        guard let date = AnniversaryCalendar.originalDate(item) else { return Text("日期不可用") }
        let formatter = DateFormatter(); formatter.locale = locale; formatter.dateFormat = "yyyy.MM.dd"
        if item.calendarType == CalendarKind.lunar.rawValue {
            let lunar = DateFormatter(); lunar.calendar = AnniversaryCalendar.chinese; lunar.locale = locale; lunar.setLocalizedDateFormatFromTemplate("MMMMd")
            return Text(verbatim: formatter.string(from: date) + " · ") + Text("农历") + Text(verbatim: " " + lunar.string(from: date))
        }
        let weekday = DateFormatter(); weekday.locale = locale; weekday.dateFormat = "EEE"
        return Text(verbatim: "\(formatter.string(from: date)) · \(weekday.string(from: date))")
    }
    var body: some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 3) {
                Text(item.name).font(.system(size: 15, weight: .semibold)).lineLimit(1)
                dateText.font(.system(size: 12)).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer(minLength: 4)
            status.font(.system(size: 12, weight: .semibold)).foregroundStyle(days == 0 ? Color.white : (days ?? 0) < 0 ? Color.secondary : GlassPalette.accent)
                .padding(.horizontal, 10).padding(.vertical, 6).background(days == 0 ? GlassPalette.accent : GlassPalette.accent.opacity(0.12), in: Capsule())
        }.padding(16).frame(maxWidth: .infinity, alignment: .leading).modifier(GlassSurface(radius: 20))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(days == 0 ? GlassPalette.accent.opacity(0.7) : .clear, lineWidth: 1))
    }
}
