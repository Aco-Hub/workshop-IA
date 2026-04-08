package com.timesheet.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides Geneva canton (Switzerland) public holidays for a given year.
 * Official holidays: New Year, Good Friday, Easter Monday, Ascension,
 * Whit Monday, Swiss National Day, Jeune Genevois, Christmas, Restoration Day.
 */
@Service
public class GenevaHolidayService {

    /**
     * Returns all Geneva public holidays for the given year.
     *
     * @param year the calendar year
     * @return list of holiday dates
     */
    public List<LocalDate> getHolidaysForYear(final int year) {
        final List<LocalDate> holidays = new ArrayList<>();

        // Fixed-date holidays
        holidays.add(LocalDate.of(year, Month.JANUARY, 1));    // New Year's Day
        holidays.add(LocalDate.of(year, Month.AUGUST, 1));     // Swiss National Day
        holidays.add(LocalDate.of(year, Month.DECEMBER, 25));  // Christmas Day
        holidays.add(LocalDate.of(year, Month.DECEMBER, 31));  // Restoration of the Republic (Restauration genevoise)

        // Easter-based holidays
        final LocalDate easter = calculateEaster(year);
        holidays.add(easter.minusDays(2));  // Good Friday
        holidays.add(easter.plusDays(1));   // Easter Monday
        holidays.add(easter.plusDays(39));  // Ascension (39 days after Easter)
        holidays.add(easter.plusDays(50));  // Whit Monday (Pentecost Monday, 50 days after Easter)

        // Jeune Genevois: Thursday after the first Sunday of September
        holidays.add(calculateJeuneGenevois(year));

        holidays.sort(LocalDate::compareTo);
        return holidays;
    }

    /**
     * Calculates Easter Sunday using the Anonymous Gregorian algorithm.
     */
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

    /**
     * Calculates Jeune Genevois: the Thursday following the first Sunday of September.
     */
    private LocalDate calculateJeuneGenevois(final int year) {
        final LocalDate septemberFirst = LocalDate.of(year, Month.SEPTEMBER, 1);
        // Find the first Sunday of September
        LocalDate firstSunday = septemberFirst;
        while (firstSunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            firstSunday = firstSunday.plusDays(1);
        }
        // The Thursday following the first Sunday
        return firstSunday.plusDays(4);
    }
}
