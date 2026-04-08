package com.timesheet.controller;

import com.timesheet.service.GenevaHolidayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final GenevaHolidayService holidayService;

    private static final Map<String, String> HOLIDAY_NAMES = Map.of(
        "01-01", "New Year's Day",
        "08-01", "Swiss National Day",
        "12-25", "Christmas Day",
        "12-31", "Restoration of the Republic"
    );

    public HolidayController(final GenevaHolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getHolidays(
            @RequestParam(defaultValue = "0") final int year) {
        final int resolvedYear = (year == 0) ? Year.now().getValue() : year;
        final List<LocalDate> holidays = holidayService.getHolidaysForYear(resolvedYear);
        final List<Map<String, String>> result = holidays.stream()
                .map(date -> Map.of(
                        "date", date.toString(),
                        "name", getHolidayName(date, holidays)
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    private String getHolidayName(final LocalDate date, final List<LocalDate> allHolidays) {
        final String key = String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth());
        if (HOLIDAY_NAMES.containsKey(key)) {
            return HOLIDAY_NAMES.get(key);
        }
        // Easter-based holidays - calculate from position
        final LocalDate easter = calculateEaster(date.getYear());
        if (date.equals(easter.minusDays(2))) {
            return "Good Friday";
        }
        if (date.equals(easter.plusDays(1))) {
            return "Easter Monday";
        }
        if (date.equals(easter.plusDays(39))) {
            return "Ascension Day";
        }
        if (date.equals(easter.plusDays(50))) {
            return "Whit Monday";
        }
        return "Jeûne Genevois";
    }

    private LocalDate calculateEaster(final int year) {
        final int a = year % 19;
        final int b = year / 100;
        final int c = year % 100;
        final int d = b / 4;
        final int e = b % 4;
        final int f = (b + 8) / 25;
        final int g = (b - f + 1) / 3;
        final int h = (19 * a + b - d - g + 15) % 30;
        final int i = c / 4;
        final int k = c % 4;
        final int l = (32 + 2 * e + 2 * i - h - k) % 7;
        final int m = (a + 11 * h + 22 * l) / 451;
        final int month = (h + l - 7 * m + 114) / 31;
        final int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
