package com.timesheet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenevaHolidayServiceTest {

    private GenevaHolidayService holidayService;

    @BeforeEach
    void setUp() {
        holidayService = new GenevaHolidayService();
    }

    @Test
    void getHolidaysForYear_alwaysReturnsExactlyNineHolidays() {
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        assertThat(holidays).hasSize(9);
    }

    @Test
    void getHolidaysForYear_alwaysReturnsNineHolidaysForAnyYear() {
        assertThat(holidayService.getHolidaysForYear(2020)).hasSize(9);
        assertThat(holidayService.getHolidaysForYear(2025)).hasSize(9);
        assertThat(holidayService.getHolidaysForYear(2030)).hasSize(9);
    }

    @Test
    void getHolidaysForYear_includesNewYearsDay2026() {
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        assertThat(holidays).contains(LocalDate.of(2026, Month.JANUARY, 1));
    }

    @Test
    void getHolidaysForYear_includesSwissNationalDay() {
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        assertThat(holidays).contains(LocalDate.of(2026, Month.AUGUST, 1));
    }

    @Test
    void getHolidaysForYear_includesChristmasDay() {
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        assertThat(holidays).contains(LocalDate.of(2026, Month.DECEMBER, 25));
    }

    @Test
    void getHolidaysForYear_includesRestorationOfTheRepublic() {
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        assertThat(holidays).contains(LocalDate.of(2026, Month.DECEMBER, 31));
    }

    @Test
    void getHolidaysForYear_includesGoodFriday_whichIsEasterBased() {
        // Easter 2026 is April 5, so Good Friday is April 3
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        LocalDate goodFriday2026 = LocalDate.of(2026, Month.APRIL, 3);
        assertThat(holidays).contains(goodFriday2026);
        assertThat(goodFriday2026.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
    }

    @Test
    void getHolidaysForYear_includesEasterMonday() {
        // Easter 2026 is April 5, so Easter Monday is April 6
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        LocalDate easterMonday2026 = LocalDate.of(2026, Month.APRIL, 6);
        assertThat(holidays).contains(easterMonday2026);
        assertThat(easterMonday2026.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void getHolidaysForYear_includesAscensionThursday() {
        // Ascension is 39 days after Easter (always a Thursday)
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        // Easter 2026 April 5 + 39 = May 14
        LocalDate ascension2026 = LocalDate.of(2026, Month.MAY, 14);
        assertThat(holidays).contains(ascension2026);
        assertThat(ascension2026.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
    }

    @Test
    void getHolidaysForYear_includesWhitMonday() {
        // Whit Monday is 50 days after Easter (always a Monday)
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        // Easter 2026 April 5 + 50 = May 25
        LocalDate whitMonday2026 = LocalDate.of(2026, Month.MAY, 25);
        assertThat(holidays).contains(whitMonday2026);
        assertThat(whitMonday2026.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void getHolidaysForYear_jeuneGenevoisIsAlwaysAThursday() {
        // Jeune Genevois = Thursday after first Sunday of September
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        LocalDate jeuneGenevois = holidays.stream()
                .filter(d -> d.getMonth() == Month.SEPTEMBER)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No September holiday found"));

        assertThat(jeuneGenevois.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
    }

    @Test
    void getHolidaysForYear_jeuneGenevoisIsInSeptember() {
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        boolean hasSeptemberHoliday = holidays.stream()
                .anyMatch(d -> d.getMonth() == Month.SEPTEMBER);
        assertThat(hasSeptemberHoliday).isTrue();
    }

    @Test
    void getHolidaysForYear_holidaysAreReturnedInChronologicalOrder() {
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2026);
        for (int i = 0; i < holidays.size() - 1; i++) {
            assertThat(holidays.get(i)).isBeforeOrEqualTo(holidays.get(i + 1));
        }
    }

    @Test
    void getHolidaysForYear_worksCorrectlyForKnownEasterYear2025() {
        // Easter 2025 is April 20
        List<LocalDate> holidays = holidayService.getHolidaysForYear(2025);
        assertThat(holidays).contains(LocalDate.of(2025, Month.APRIL, 18)); // Good Friday
        assertThat(holidays).contains(LocalDate.of(2025, Month.APRIL, 21)); // Easter Monday
        assertThat(holidays).hasSize(9);
    }
}
