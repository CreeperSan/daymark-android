import SwiftUI

/// 云端纪念日首页，按日期分类，刷新失败时保留已有列表。
struct HomeView: View {
    @ObservedObject var api: DaymarkAPI
    @Binding var path: [DaymarkRoute]
    @Environment(\.scenePhase) private var scenePhase
    @State private var items: [Anniversary] = []
    @State private var loading = false
    @State private var loaded = false
    @State private var error: String?

    private var groups: [(String, [(Anniversary, Int?)])] {
        let values = items.map { ($0, AnniversaryCalendar.daysUntil($0)) }
        let today = values.filter { $0.1 == 0 }
        let upcoming = values.filter { ($0.1 ?? 0) > 0 }.sorted { ($0.1 ?? 0) < ($1.1 ?? 0) }
        let past = values.filter { ($0.1 ?? 0) < 0 }.sorted { ($0.1 ?? 0) > ($1.1 ?? 0) }
        let unknown = values.filter { $0.1 == nil }
        return [("今天", today), ("即将到来", upcoming), ("已经过去", past), ("其他", unknown)].filter { !$0.1.isEmpty }
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                if loading && !loaded { Text("正在加载…").font(.system(size: 12)).foregroundStyle(.secondary) }
                if let error {
                    Text(error).font(.system(size: 12)).foregroundStyle(.red)
                    Button("重试") { Task { await load() } }.foregroundStyle(GlassPalette.accent)
                }
                if loaded && items.isEmpty { Text("还没有纪念日，点击新建开始记录。").foregroundStyle(.secondary).font(.system(size: 14)) }
                ForEach(groups, id: \.0) { title, entries in
                    Text(LocalizedStringKey(title)).font(.system(size: 12, weight: .medium)).foregroundStyle(.secondary)
                    ForEach(entries, id: \.0.id) { item, days in
                        Button { path.append(.edit(item.id)) } label: { AnniversaryRow(item: item, days: days) }.buttonStyle(.plain)
                    }
                }
            }.padding(.horizontal, 20).padding(.top, 8).padding(.bottom, 100)
        }
        .scrollIndicators(.hidden)
        .refreshable { await load() }
        .safeAreaInset(edge: .top, spacing: 0) {
            GlassHeader(title: "我的纪念日", trailing: AnyView(Button { path.append(.settings) } label: { Image(systemName: "gearshape").font(.system(size: 15)).frame(width: 36, height: 36).modifier(GlassSurface(radius: 18)) }.buttonStyle(.plain)))
        }
        .overlay(alignment: .bottomTrailing) {
            Button { path.append(.edit(nil)) } label: { Image(systemName: "plus").font(.system(size: 24, weight: .light)).foregroundStyle(.white).frame(width: 60, height: 60).background(LinearGradient(colors: GlassPalette.gradient, startPoint: .topLeading, endPoint: .bottomTrailing), in: Circle()) }.buttonStyle(.plain).padding(.trailing, 20).padding(.bottom, 20)
        }
        .onAppear { Task { await load() } }
        .onChange(of: scenePhase) { _, phase in if phase == .active { Task { await load() } } }
    }
    private func load() async {
        guard !loading else { return }
        loading = true; defer { loading = false }
        do { items = try await api.list(); loaded = true; error = nil }
        catch { self.error = error.localizedDescription }
    }
}

/// 单条纪念日卡片使用真实日期和倒计时，不使用设计稿中的示例数据。
struct AnniversaryRow: View {
    let item: Anniversary
    let days: Int?
    private var status: Text {
        guard let days else { return Text("日期不可用") }
        if days == 0 { return Text("就在今天") }
        return days > 0 ? Text("还有 \(days) 天") : Text("已过去 \(-days) 天")
    }
    private var dateText: String {
        guard let date = AnniversaryCalendar.originalDate(item) else { return "日期不可用" }
        let formatter = DateFormatter(); formatter.dateFormat = "yyyy.MM.dd"
        if item.calendarType == CalendarKind.lunar.rawValue {
            let lunar = DateFormatter(); lunar.calendar = AnniversaryCalendar.chinese; lunar.locale = Locale(identifier: "zh_CN"); lunar.dateFormat = "MMMMd"
            return "\(formatter.string(from: date)) · 农历\(lunar.string(from: date))"
        }
        let weekday = DateFormatter(); weekday.dateFormat = "EEE"
        return "\(formatter.string(from: date)) · \(weekday.string(from: date))"
    }
    var body: some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 3) {
                Text(item.name).font(.system(size: 15, weight: .semibold)).lineLimit(1)
                Text(dateText).font(.system(size: 12)).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer(minLength: 4)
            status.font(.system(size: 12, weight: .semibold)).foregroundStyle(days == 0 ? Color.white : (days ?? 0) < 0 ? Color.secondary : GlassPalette.accent)
                .padding(.horizontal, 10).padding(.vertical, 6).background(days == 0 ? GlassPalette.accent : GlassPalette.accent.opacity(0.12), in: Capsule())
        }.padding(16).frame(maxWidth: .infinity, alignment: .leading).modifier(GlassSurface(radius: 20))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(days == 0 ? GlassPalette.accent.opacity(0.7) : .clear, lineWidth: 1))
    }
}
