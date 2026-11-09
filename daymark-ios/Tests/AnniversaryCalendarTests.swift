import XCTest
@testable import Daymark

/// 验证循环日期跨周期、月末和农历闰月的计算。
final class AnniversaryCalendarTests: XCTestCase {
    private func date(_ year: Int, _ month: Int, _ day: Int) -> Date {
        AnniversaryCalendar.gregorian.date(from: DateComponents(year: year, month: month, day: day))!
    }

    func testSolarRecurrenceClampsMonthEnd() {
        var item = Anniversary(calendarType: "solar", loopType: "monthly", year: 2020, month: 1, day: 31)
        XCTAssertEqual(AnniversaryCalendar.daysUntil(item, today: date(2024, 2, 1)), 28)
        item.loopType = "yearly"; item.month = 2; item.day = 29
        XCTAssertEqual(AnniversaryCalendar.daysUntil(item, today: date(2025, 2, 1)), 27)
    }

    func testLunarLeapMonthFallsBackToRegularMonth() {
        let leapDate = AnniversaryCalendar.solarDate(year: 2023, month: 2, day: 1, leap: true)
        XCTAssertEqual(leapDate, date(2023, 3, 22))
        let item = Anniversary(calendarType: "lunar", loopType: "yearly", year: 2023, month: 2, day: 1, isLeapMonth: 1)
        XCTAssertEqual(AnniversaryCalendar.daysUntil(item, today: date(2024, 1, 1)), 69)
    }

    func testMonthlyLunarRecurrenceIncludesLeapMonth() {
        let item = Anniversary(calendarType: "lunar", loopType: "monthly", year: 2020, month: 2, day: 1)
        XCTAssertEqual(AnniversaryCalendar.daysUntil(item, today: date(2023, 3, 1)), 21)
    }

    func testCurrentPeriodServerDateRemainsVisibleAfterItPassed() {
        let serverDate = Int64(date(2024, 2, 5).timeIntervalSince1970 * 1000)
        let item = Anniversary(calendarType: "solar", loopType: "monthly", year: 2020, month: 1, day: 5, nextTimestamp: serverDate)
        XCTAssertEqual(AnniversaryCalendar.daysUntil(item, today: date(2024, 2, 10)), -5)
    }

    func testCalendarsUseCurrentTimeZoneAfterChange() {
        let original = NSTimeZone.default
        let tokyoOffset = 9 * 60 * 60
        defer { NSTimeZone.default = original }
        NSTimeZone.default = TimeZone(secondsFromGMT: tokyoOffset)!
        XCTAssertEqual(AnniversaryCalendar.gregorian.timeZone.secondsFromGMT(), tokyoOffset)
        XCTAssertEqual(AnniversaryCalendar.chinese.timeZone.secondsFromGMT(), tokyoOffset)
    }

    func testExpiredServerDateIsRecalculatedAcrossPeriods() {
        let old = Int64(date(2023, 12, 31).timeIntervalSince1970 * 1000)
        let item = Anniversary(calendarType: "solar", loopType: "monthly", year: 2020, month: 1, day: 31, nextTimestamp: old)
        XCTAssertEqual(AnniversaryCalendar.daysUntil(item, today: date(2024, 2, 1)), 28)
    }

    func testUnknownCalendarIsNotInterpretedAsSolar() {
        let item = Anniversary(calendarType: "unknown", loopType: "none", year: 2024, month: 2, day: 1)
        XCTAssertNil(AnniversaryCalendar.originalDate(item))
        XCTAssertNil(AnniversaryCalendar.daysUntil(item, today: date(2024, 2, 1)))
    }
}
