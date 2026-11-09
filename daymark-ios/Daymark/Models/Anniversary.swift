import Foundation

/// 服务端纪念日：日期字段保留用户选择的公历或农历原始值。
struct Anniversary: Codable, Identifiable, Hashable {
    var id: Int64 = 0
    var name: String = ""
    var description: String = ""
    var calendarType: String = CalendarKind.solar.rawValue
    var loopType: String = RepeatKind.yearly.rawValue
    var year: Int = 0
    var month: Int = 0
    var day: Int = 0
    var isLeapMonth: Int = 0
    var nextTimestamp: Int64 = 0

    enum CodingKeys: String, CodingKey {
        case id, name, description, year, month, day
        case calendarType = "calendar_type", loopType = "loop_type"
        case isLeapMonth = "is_leap_month", nextTimestamp = "next_timestamp"
    }
}

enum CalendarKind: String, CaseIterable { case solar, lunar }
enum RepeatKind: String, CaseIterable { case none, monthly, yearly }

/// 使用 Foundation 中国农历进行日期转换，避免将农历月份误当作公历月份。
enum AnniversaryCalendar {
    // 每次读取系统时区，跨时区后不会沿用应用启动时的旧日期。
    static var gregorian: Calendar { Calendar(identifier: .gregorian) }
    static var chinese: Calendar { Calendar(identifier: .chinese) }

    /// 由农历年月日查找对应公历日期；闰月必须与用户选择一致。
    static func solarDate(year: Int, month: Int, day: Int, leap: Bool) -> Date? {
        let gregorian = Self.gregorian, chinese = Self.chinese
        guard (1...9998).contains(year), (1...12).contains(month), (1...30).contains(day),
              let start = gregorian.date(from: DateComponents(year: year, month: 1, day: 1)),
              let end = gregorian.date(from: DateComponents(year: year + 1, month: 3, day: 1)) else { return nil }
        var candidate = start
        while candidate < end {
            let parts = chinese.dateComponents([.month, .day, .isLeapMonth], from: candidate)
            if parts.month == month && parts.day == day && (parts.isLeapMonth ?? false) == leap,
               let lunarYearStart = chinese.dateInterval(of: .year, for: candidate)?.start,
               gregorian.component(.year, from: lunarYearStart) == year { return candidate }
            guard let next = gregorian.date(byAdding: .day, value: 1, to: candidate) else { return nil }
            candidate = next
        }
        return nil
    }

    /// 获取选中公历日对应的农历年月日和闰月标记。
    static func lunarParts(for date: Date) -> (year: Int, month: Int, day: Int, leap: Bool)? {
        let gregorian = Self.gregorian, chinese = Self.chinese
        let parts = chinese.dateComponents([.month, .day, .isLeapMonth], from: date)
        guard let start = chinese.dateInterval(of: .year, for: date)?.start,
              let month = parts.month, let day = parts.day else { return nil }
        return (gregorian.component(.year, from: start), month, day, parts.isLeapMonth ?? false)
    }

    static func originalDate(_ item: Anniversary) -> Date? {
        let gregorian = Self.gregorian
        if item.calendarType == CalendarKind.lunar.rawValue {
            return solarDate(year: item.year, month: item.month, day: item.day, leap: item.isLeapMonth == 1)
        }
        // 未知历法不能按公历解释，否则会展示错误的纪念日。
        guard item.calendarType == CalendarKind.solar.rawValue else { return nil }
        guard let date = gregorian.date(from: DateComponents(year: item.year, month: item.month, day: item.day)),
              gregorian.component(.year, from: date) == item.year,
              gregorian.component(.month, from: date) == item.month,
              gregorian.component(.day, from: date) == item.day else { return nil }
        return date
    }

    /// 与 Android 保持一致：过期的循环日期跨周期后在本地推算。
    static func daysUntil(_ item: Anniversary, today: Date = Date()) -> Int? {
        let gregorian = Self.gregorian
        let today = gregorian.startOfDay(for: today)
        let serverDate = item.nextTimestamp == 0 ? nil : gregorian.startOfDay(for: Date(timeIntervalSince1970: Double(item.nextTimestamp) / 1000))
        let target: Date?
        switch item.loopType {
        case RepeatKind.none.rawValue:
            target = serverDate ?? originalDate(item)
        case RepeatKind.monthly.rawValue, RepeatKind.yearly.rawValue:
            let samePeriod = serverDate.map { date in
                gregorian.component(.year, from: date) == gregorian.component(.year, from: today) &&
                (item.loopType == RepeatKind.yearly.rawValue || gregorian.component(.month, from: date) == gregorian.component(.month, from: today))
            } ?? false
            target = serverDate != nil && (samePeriod || serverDate! >= today) ? serverDate : (nextLocalDate(item, today: today) ?? serverDate)
        default:
            target = serverDate
        }
        guard let target else { return nil }
        return gregorian.dateComponents([.day], from: today, to: gregorian.startOfDay(for: target)).day
    }

    /// 农历月循环包含闰月，年循环遇到不存在的闰月时回退普通月。
    private static func nextLocalDate(_ item: Anniversary, today: Date) -> Date? {
        let gregorian = Self.gregorian, chinese = Self.chinese
        guard let origin = originalDate(item) else { return nil }
        if origin >= today { return origin }
        if item.calendarType == CalendarKind.solar.rawValue {
            let parts = gregorian.dateComponents([.year, .month], from: today)
            let month = item.loopType == RepeatKind.monthly.rawValue ? parts.month! : item.month
            guard let start = gregorian.date(from: DateComponents(year: parts.year, month: month, day: 1)) else { return nil }
            for offset in 0...1 {
                let unit: Calendar.Component = item.loopType == RepeatKind.monthly.rawValue ? .month : .year
                guard let period = gregorian.date(byAdding: unit, value: offset, to: start),
                      let count = gregorian.range(of: .day, in: .month, for: period)?.count,
                      let date = gregorian.date(byAdding: .day, value: min(item.day, count) - 1, to: period) else { continue }
                if date >= today { return date }
            }
        } else if item.calendarType == CalendarKind.lunar.rawValue {
            if item.loopType == RepeatKind.monthly.rawValue {
                guard let start = chinese.dateInterval(of: .month, for: today)?.start else { return nil }
                for offset in 0...1 {
                    guard let month = chinese.date(byAdding: .month, value: offset, to: start),
                          let count = chinese.range(of: .day, in: .month, for: month)?.count,
                          let date = chinese.date(byAdding: .day, value: min(item.day, count) - 1, to: month) else { continue }
                    if date >= today { return date }
                }
            } else if let year = lunarParts(for: today)?.year {
                for offset in 0...1 {
                    let targetYear = year + offset
                    let leap = item.isLeapMonth == 1 && solarDate(year: targetYear, month: item.month, day: 1, leap: true) != nil
                    // 农历月只有 29 或 30 天，第 30 天不存在时取月末。
                    let date = solarDate(year: targetYear, month: item.month, day: item.day, leap: leap) ??
                        solarDate(year: targetYear, month: item.month, day: min(item.day, 29), leap: leap)
                    if let date, date >= today { return date }
                }
            }
        }
        return nil
    }
}
