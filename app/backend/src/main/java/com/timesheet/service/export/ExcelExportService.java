package com.timesheet.service.export;

import com.timesheet.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    // ── Colour palette ───────────────────────────────────────────────────────────
    private static final String HEX_DARK_BG    = "37352F";
    private static final String HEX_SECTION_BG = "F7F6F3";
    private static final String HEX_LEAVE_BG   = "E8F5E9";
    private static final String HEX_BLUE       = "2E75B6";
    private static final String HEX_WHITE      = "FFFFFF";

    // ── i18n ─────────────────────────────────────────────────────────────────────
    private static final Map<String, Map<String, String>> I18N = new HashMap<>();

    static {
        final Map<String, String> fr = new LinkedHashMap<>();
        fr.put("timesheetReport",      "Rapport de présence");
        fr.put("period",               "Période");
        fr.put("totalHours",           "Heures totales");
        fr.put("leaveDays",            "Jours de congé");
        fr.put("projects",             "Projets");
        fr.put("developers",           "Développeurs");
        fr.put("sessions",             "Sessions");
        fr.put("developer",            "Développeur");
        fr.put("project",              "Projet");
        fr.put("client",               "Client");
        fr.put("duration",             "Durée");
        fr.put("description",          "Description");
        fr.put("weeklySummary",        "Récapitulatif hebdomadaire");
        fr.put("generatedOn",          "Généré le");
        fr.put("hours",                "Heures");
        fr.put("share",                "Part");
        fr.put("date",                 "Date");
        fr.put("day",                  "Jour");
        fr.put("week",                 "Semaine");
        fr.put("month",                "Mois");
        fr.put("title",                "Titre");
        fr.put("teamBreakdown",        "Répartition de l'équipe");
        fr.put("hoursByProject",       "Heures par projet");
        fr.put("monthlyTrend",         "Tendance mensuelle");
        fr.put("developerContribution","Contribution des développeurs");
        fr.put("dailyLog",             "Journal quotidien");
        fr.put("detailedLog",          "Journal détaillé");
        fr.put("leave",                "Congé");
        fr.put("work",                 "Travail");
        fr.put("weeklyActivity",       "Activité hebdomadaire");
        fr.put("total",                "Total");
        fr.put("workHours",            "Heures travaillées");
        fr.put("leaveHours",           "Heures de congé");
        fr.put("type",                 "Type");
        fr.put("start",                "Début");
        fr.put("end",                  "Fin");
        fr.put("summary",              "Résumé");
        fr.put("heatmap",              "Heatmap");
        fr.put("entries",              "Entrées");
        fr.put("crossTab",             "Tableau croisé");
        fr.put("monthly",              "Mensuel");
        I18N.put("fr", fr);

        final Map<String, String> en = new LinkedHashMap<>();
        en.put("timesheetReport",      "Timesheet Report");
        en.put("period",               "Period");
        en.put("totalHours",           "Total Hours");
        en.put("leaveDays",            "Leave Days");
        en.put("projects",             "Projects");
        en.put("developers",           "Developers");
        en.put("sessions",             "Sessions");
        en.put("developer",            "Developer");
        en.put("project",              "Project");
        en.put("client",               "Client");
        en.put("duration",             "Duration");
        en.put("description",          "Description");
        en.put("weeklySummary",        "Weekly Summary");
        en.put("generatedOn",          "Generated on");
        en.put("hours",                "Hours");
        en.put("share",                "Share");
        en.put("date",                 "Date");
        en.put("day",                  "Day");
        en.put("week",                 "Week");
        en.put("month",                "Month");
        en.put("title",                "Title");
        en.put("teamBreakdown",        "Team Breakdown");
        en.put("hoursByProject",       "Hours by Project");
        en.put("monthlyTrend",         "Monthly Trend");
        en.put("developerContribution","Developer Contribution");
        en.put("dailyLog",             "Daily Log");
        en.put("detailedLog",          "Detailed Log");
        en.put("leave",                "Leave");
        en.put("work",                 "Work");
        en.put("weeklyActivity",       "Weekly Activity");
        en.put("total",                "Total");
        en.put("workHours",            "Work Hours");
        en.put("leaveHours",           "Leave Hours");
        en.put("type",                 "Type");
        en.put("start",                "Start");
        en.put("end",                  "End");
        en.put("summary",              "Summary");
        en.put("heatmap",              "Heatmap");
        en.put("entries",              "Entries");
        en.put("crossTab",             "Cross-Tab");
        en.put("monthly",              "Monthly");
        I18N.put("en", en);
    }

    private String t(final String lang, final String key) {
        final Map<String, String> map = I18N.getOrDefault(lang, I18N.get("fr"));
        return map.getOrDefault(key, key);
    }

    // ── Public API ────────────────────────────────────────────────────────────────

    public byte[] generateDeveloperReport(final Developer dev, final List<TimeEntry> entries,
                                          final LocalDate start, final LocalDate end, final String lang) {
        try (final XSSFWorkbook wb = new XSSFWorkbook()) {
            final Styles styles = new Styles(wb);

            // Sheet 1 – Summary
            final XSSFSheet summary = wb.createSheet(t(lang, "summary"));
            writeDeveloperSummarySheet(wb, styles, summary, dev, entries, start, end, lang);

            // Sheet 2 – Daily Log
            final XSSFSheet dailyLog = wb.createSheet(t(lang, "dailyLog"));
            writeDeveloperDailyLogSheet(wb, styles, dailyLog, entries, lang);

            // Sheet 3 – Weekly
            final XSSFSheet weekly = wb.createSheet(t(lang, "weeklySummary"));
            writeDeveloperWeeklySheet(wb, styles, weekly, entries, lang);

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate developer Excel report", e);
        }
    }

    public byte[] generateProjectReport(final Project project, final List<TimeEntry> entries,
                                        final LocalDate start, final LocalDate end, final String lang) {
        try (final XSSFWorkbook wb = new XSSFWorkbook()) {
            final Styles styles = new Styles(wb);

            final XSSFSheet summary = wb.createSheet(t(lang, "summary"));
            writeProjectSummarySheet(wb, styles, summary, project, entries, start, end, lang);

            final XSSFSheet heatmap = wb.createSheet(t(lang, "heatmap"));
            writeProjectHeatmapSheet(wb, styles, heatmap, entries, lang);

            final XSSFSheet entriesSheet = wb.createSheet(t(lang, "entries"));
            writeProjectEntriesSheet(wb, styles, entriesSheet, entries, lang);

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate project Excel report", e);
        }
    }

    public byte[] generateClientReport(final Client client, final List<Project> projects, final List<TimeEntry> entries,
                                       final LocalDate start, final LocalDate end, final String lang) {
        try (final XSSFWorkbook wb = new XSSFWorkbook()) {
            final Styles styles = new Styles(wb);

            final XSSFSheet summary = wb.createSheet(t(lang, "summary"));
            writeClientSummarySheet(wb, styles, summary, client, entries, start, end, lang);

            final XSSFSheet crossTab = wb.createSheet(t(lang, "crossTab"));
            writeClientCrossTabSheet(wb, styles, crossTab, projects, entries, lang);

            final XSSFSheet monthly = wb.createSheet(t(lang, "monthly"));
            writeClientMonthlySheet(wb, styles, monthly, entries, lang);

            final XSSFSheet entriesSheet = wb.createSheet(t(lang, "entries"));
            writeClientEntriesSheet(wb, styles, entriesSheet, entries, lang);

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate client Excel report", e);
        }
    }

    // ── Developer sheets ──────────────────────────────────────────────────────────

    private void writeDeveloperSummarySheet(final XSSFWorkbook wb, final Styles s, final XSSFSheet sheet,
                                            final Developer dev, final List<TimeEntry> entries,
                                            final LocalDate start, final LocalDate end, final String lang) {
        int rowIdx = 0;

        // Title row
        final Row titleRow = sheet.createRow(rowIdx++);
        final String name = dev.getDisplayUsername() != null ? dev.getDisplayUsername() : dev.getUsername();
        createKpiCell(wb, s, titleRow, 0, t(lang, "timesheetReport") + " – " + name, true);

        // KPI cells
        final double totalHours = sumHours(entries);
        final long projectCount = entries.stream().filter(e -> e.getProject() != null)
                .map(e -> e.getProject().getId()).distinct().count();
        final long leaveDays = entries.stream().filter(e -> e.getType() == TimeEntryType.LEAVE)
                .map(e -> e.getStartTime().toLocalDate()).distinct().count();

        rowIdx = writeKpiRow(wb, s, sheet, rowIdx, new String[]{
                t(lang, "totalHours"), t(lang, "projects"), t(lang, "leaveDays")
        }, new double[]{totalHours, (double) projectCount, (double) leaveDays}, lang);

        rowIdx++; // blank spacer

        // Group entries by project, then list each task individually per project
        final Map<Long, List<TimeEntry>> byProject = entries.stream()
                .filter(e -> e.getProject() != null)
                .collect(Collectors.groupingBy(e -> e.getProject().getId()));

        final List<Map.Entry<Long, List<TimeEntry>>> sortedProjects = byProject.entrySet().stream()
                .sorted((a, b) -> Double.compare(sumHours(b.getValue()), sumHours(a.getValue())))
                .toList();

        // Leave entries (no project)
        final List<TimeEntry> leaveEntries = entries.stream()
                .filter(e -> e.getProject() == null || e.getType() == TimeEntryType.LEAVE)
                .sorted(Comparator.comparing(TimeEntry::getStartTime))
                .toList();

        final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        final Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;

        for (final Map.Entry<Long, List<TimeEntry>> entry : sortedProjects) {
            final Project p = entry.getValue().get(0).getProject();
            final double projectHours = sumHours(entry.getValue());
            final double pct = totalHours > 0 ? (projectHours / totalHours) * 100 : 0;

            // Project section header with total hours
            final String sectionTitle = p.getName()
                    + (p.getClient() != null ? " (" + p.getClient().getName() + ")" : "")
                    + " — " + String.format("%.1fh", projectHours)
                    + " (" + String.format("%.1f%%", pct) + ")";
            rowIdx = writeSectionHeader(s, sheet, rowIdx, sectionTitle, 7);

            // Table header for tasks
            final String[] taskHeaders = {
                    t(lang, "date"), t(lang, "day"), t(lang, "start"), t(lang, "end"),
                    t(lang, "duration"), t(lang, "type"), t(lang, "description")
            };
            rowIdx = writeTableHeader(s, sheet, rowIdx, taskHeaders);

            // Each task individually
            final List<TimeEntry> projectEntries = entry.getValue().stream()
                    .sorted(Comparator.comparing(TimeEntry::getStartTime))
                    .toList();

            int altRow = 0;
            final int dataStart = rowIdx;
            for (final TimeEntry e : projectEntries) {
                final boolean isLeave = e.getType() == TimeEntryType.LEAVE;
                final CellStyle rowStyle = isLeave ? s.leaveRow : ((altRow % 2 != 0) ? s.altRow : s.dataCell);
                final CellStyle numStyle = isLeave ? s.leaveNumRow : ((altRow % 2 != 0) ? s.altNumRow : s.numCell);
                altRow++;

                final Row row = sheet.createRow(rowIdx++);
                createStyledCell(s, row, 0, e.getStartTime().format(dateFmt), rowStyle);
                createStyledCell(s, row, 1, e.getStartTime().getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, locale), rowStyle);
                createStyledCell(s, row, 2, e.getStartTime().format(timeFmt), rowStyle);
                createStyledCell(s, row, 3, e.getEndTime().format(timeFmt), rowStyle);
                createNumericCell(s, row, 4, durationHours(e), numStyle);
                createStyledCell(s, row, 5,
                        e.getType() == TimeEntryType.LEAVE ? t(lang, "leave") : t(lang, "work"), rowStyle);
                createStyledCell(s, row, 6, e.getDescription() != null ? e.getDescription() : "", rowStyle);
            }

            if (dataStart < rowIdx) {
                applyHoursConditionalFormatting(sheet, dataStart, rowIdx - 1, 4);
            }

            rowIdx++; // blank spacer between projects
        }

        // Leave entries section (if any without project)
        final List<TimeEntry> pureLeave = entries.stream()
                .filter(e -> e.getProject() == null && e.getType() == TimeEntryType.LEAVE)
                .sorted(Comparator.comparing(TimeEntry::getStartTime))
                .toList();

        if (!pureLeave.isEmpty()) {
            final double leaveHours = sumHours(pureLeave);
            rowIdx = writeSectionHeader(s, sheet, rowIdx, t(lang, "leave") + " — " + String.format("%.1fh", leaveHours), 7);
            final String[] taskHeaders = {
                    t(lang, "date"), t(lang, "day"), t(lang, "start"), t(lang, "end"),
                    t(lang, "duration"), t(lang, "type"), t(lang, "description")
            };
            rowIdx = writeTableHeader(s, sheet, rowIdx, taskHeaders);

            final int dataStart = rowIdx;
            for (final TimeEntry e : pureLeave) {
                final Row row = sheet.createRow(rowIdx++);
                createStyledCell(s, row, 0, e.getStartTime().format(dateFmt), s.leaveRow);
                createStyledCell(s, row, 1, e.getStartTime().getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, locale), s.leaveRow);
                createStyledCell(s, row, 2, e.getStartTime().format(timeFmt), s.leaveRow);
                createStyledCell(s, row, 3, e.getEndTime().format(timeFmt), s.leaveRow);
                createNumericCell(s, row, 4, durationHours(e), s.leaveNumRow);
                createStyledCell(s, row, 5, t(lang, "leave"), s.leaveRow);
                createStyledCell(s, row, 6, e.getDescription() != null ? e.getDescription() : "", s.leaveRow);
            }

            if (dataStart < rowIdx) {
                applyHoursConditionalFormatting(sheet, dataStart, rowIdx - 1, 4);
            }
        }

        autoSizeColumns(sheet, 7);
    }

    private void writeDeveloperDailyLogSheet(final XSSFWorkbook wb, final Styles s, final XSSFSheet sheet,
                                             final List<TimeEntry> entries, final String lang) {
        int rowIdx = 0;
        final String[] headers = {
                t(lang, "date"), t(lang, "day"), t(lang, "project"), t(lang, "client"),
                t(lang, "type"), t(lang, "start"), t(lang, "end"), t(lang, "duration"), t(lang, "description")
        };
        rowIdx = writeTableHeader(s, sheet, rowIdx, headers);

        final List<TimeEntry> sorted = entries.stream()
                .sorted(Comparator
                        .comparing((TimeEntry e) -> e.getProject() != null ? e.getProject().getName() : "")
                        .thenComparing(TimeEntry::getStartTime))
                .toList();

        final Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;
        final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        int altRow = 0;
        for (final TimeEntry e : sorted) {
            final boolean isLeave = e.getType() == TimeEntryType.LEAVE;
            final CellStyle rowStyle = isLeave ? s.leaveRow : ((altRow % 2 != 0) ? s.altRow : s.dataCell);
            final CellStyle numStyle = isLeave ? s.leaveNumRow : ((altRow % 2 != 0) ? s.altNumRow : s.numCell);
            altRow++;

            final Row row = sheet.createRow(rowIdx++);
            createStyledCell(s, row, 0, e.getStartTime().format(dateFmt), rowStyle);
            createStyledCell(s, row, 1, e.getStartTime().getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, locale), rowStyle);
            createStyledCell(s, row, 2, e.getProject() != null ? e.getProject().getName() : "-", rowStyle);
            createStyledCell(s, row, 3, e.getProject() != null && e.getProject().getClient() != null
                    ? e.getProject().getClient().getName() : "-", rowStyle);
            createStyledCell(s, row, 4,
                    e.getType() == TimeEntryType.LEAVE ? t(lang, "leave") : t(lang, "work"), rowStyle);
            createStyledCell(s, row, 5, e.getStartTime().format(timeFmt), rowStyle);
            createStyledCell(s, row, 6, e.getEndTime().format(timeFmt), rowStyle);
            createNumericCell(s, row, 7, durationHours(e), numStyle);
            createStyledCell(s, row, 8, e.getDescription() != null ? e.getDescription() : "", rowStyle);
        }

        applyHoursConditionalFormatting(sheet, 1, rowIdx - 1, 7);
        autoSizeColumns(sheet, headers.length);
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
    }

    private void writeDeveloperWeeklySheet(final XSSFWorkbook wb, final Styles s, final XSSFSheet sheet,
                                           final List<TimeEntry> entries, final String lang) {
        int rowIdx = 0;
        final String[] headers = {t(lang, "week"), t(lang, "workHours"), t(lang, "leaveHours"), t(lang, "total")};
        rowIdx = writeTableHeader(s, sheet, rowIdx, headers);

        final WeekFields wf = WeekFields.ISO;
        final Map<Integer, double[]> weekMap = new TreeMap<>();
        for (final TimeEntry e : entries) {
            final int week = e.getStartTime().get(wf.weekOfWeekBasedYear());
            weekMap.computeIfAbsent(week, k -> new double[]{0, 0});
            if (e.getType() == TimeEntryType.LEAVE) {
                weekMap.get(week)[1] += durationHours(e);
            } else {
                weekMap.get(week)[0] += durationHours(e);
            }
        }

        int altRow = 0;
        for (final Map.Entry<Integer, double[]> entry : weekMap.entrySet()) {
            final double work = entry.getValue()[0];
            final double leave = entry.getValue()[1];
            final boolean alt = (altRow++ % 2 != 0);
            final Row row = sheet.createRow(rowIdx++);
            createStyledCell(s, row, 0, "W" + entry.getKey(), alt ? s.altRow : s.dataCell);
            createNumericCell(s, row, 1, work, alt ? s.altNumRow : s.numCell);
            createNumericCell(s, row, 2, leave, alt ? s.altNumRow : s.numCell);
            createNumericCell(s, row, 3, work + leave, alt ? s.altNumRow : s.numCell);
        }

        applyHoursConditionalFormatting(sheet, 1, rowIdx - 1, 1);
        applyHoursConditionalFormatting(sheet, 1, rowIdx - 1, 3);
        autoSizeColumns(sheet, headers.length);
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
    }

    // ── Project sheets ────────────────────────────────────────────────────────────

    private void writeProjectSummarySheet(final XSSFWorkbook wb, final Styles s, final XSSFSheet sheet,
                                          final Project project, final List<TimeEntry> entries,
                                          final LocalDate start, final LocalDate end, final String lang) {
        int rowIdx = 0;

        final Row titleRow = sheet.createRow(rowIdx++);
        createKpiCell(wb, s, titleRow, 0, t(lang, "timesheetReport") + " – " + project.getName(), true);

        final double totalHours = sumHours(entries);
        final long devCount = entries.stream().map(e -> e.getDeveloper().getId()).distinct().count();
        final long sessionCount = entries.size();

        rowIdx = writeKpiRow(wb, s, sheet, rowIdx, new String[]{
                t(lang, "totalHours"), t(lang, "developers"), t(lang, "sessions")
        }, new double[]{totalHours, (double) devCount, (double) sessionCount}, lang);

        rowIdx++;

        // Group by developer, list each task individually
        final Map<Long, List<TimeEntry>> byDev = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getDeveloper().getId()));

        final List<Map.Entry<Long, List<TimeEntry>>> sorted = byDev.entrySet().stream()
                .sorted((a, b) -> Double.compare(sumHours(b.getValue()), sumHours(a.getValue())))
                .toList();

        final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        final Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;

        for (final Map.Entry<Long, List<TimeEntry>> entry : sorted) {
            final Developer dev = entry.getValue().get(0).getDeveloper();
            final double devHours = sumHours(entry.getValue());
            final double pct = totalHours > 0 ? (devHours / totalHours) * 100 : 0;
            final String devName = dev.getDisplayUsername() != null ? dev.getDisplayUsername() : dev.getUsername();

            // Developer section header
            final String sectionTitle = devName
                    + (dev.getTitle() != null ? " – " + dev.getTitle() : "")
                    + " — " + String.format("%.1fh", devHours)
                    + " (" + String.format("%.1f%%", pct) + ")";
            rowIdx = writeSectionHeader(s, sheet, rowIdx, sectionTitle, 6);

            final String[] taskHeaders = {
                    t(lang, "date"), t(lang, "day"), t(lang, "start"), t(lang, "end"),
                    t(lang, "duration"), t(lang, "description")
            };
            rowIdx = writeTableHeader(s, sheet, rowIdx, taskHeaders);

            final List<TimeEntry> devEntries = entry.getValue().stream()
                    .sorted(Comparator.comparing(TimeEntry::getStartTime))
                    .toList();

            int altRow = 0;
            final int dataStart = rowIdx;
            for (final TimeEntry e : devEntries) {
                final boolean isLeave = e.getType() == TimeEntryType.LEAVE;
                final CellStyle rowStyle = isLeave ? s.leaveRow : ((altRow % 2 != 0) ? s.altRow : s.dataCell);
                final CellStyle numStyle = isLeave ? s.leaveNumRow : ((altRow % 2 != 0) ? s.altNumRow : s.numCell);
                altRow++;

                final Row row = sheet.createRow(rowIdx++);
                createStyledCell(s, row, 0, e.getStartTime().format(dateFmt), rowStyle);
                createStyledCell(s, row, 1, e.getStartTime().getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, locale), rowStyle);
                createStyledCell(s, row, 2, e.getStartTime().format(timeFmt), rowStyle);
                createStyledCell(s, row, 3, e.getEndTime().format(timeFmt), rowStyle);
                createNumericCell(s, row, 4, durationHours(e), numStyle);
                createStyledCell(s, row, 5, e.getDescription() != null ? e.getDescription() : "", rowStyle);
            }

            if (dataStart < rowIdx) {
                applyHoursConditionalFormatting(sheet, dataStart, rowIdx - 1, 4);
            }

            rowIdx++; // spacer
        }

        autoSizeColumns(sheet, 6);
    }

    private void writeProjectHeatmapSheet(final XSSFWorkbook wb, final Styles s, final XSSFSheet sheet,
                                          final List<TimeEntry> entries, final String lang) {
        final WeekFields wf = WeekFields.ISO;

        final List<Long> devIds = entries.stream()
                .map(e -> e.getDeveloper().getId())
                .distinct().sorted().toList();
        final Map<Long, Developer> devMap = entries.stream()
                .collect(Collectors.toMap(e -> e.getDeveloper().getId(), TimeEntry::getDeveloper, (a, b) -> a));
        final List<Integer> weeks = entries.stream()
                .map(e -> e.getStartTime().get(wf.weekOfWeekBasedYear()))
                .distinct().sorted().toList();

        if (weeks.isEmpty() || devIds.isEmpty()) {
            return;
        }

        // Build header: Developer | W1 | W2 | ... | Total
        int rowIdx = 0;
        final Row headerRow = sheet.createRow(rowIdx++);
        createSectionCell(s, headerRow, 0, t(lang, "developer"));
        for (int i = 0; i < weeks.size(); i++) {
            createSectionCell(s, headerRow, i + 1, "W" + weeks.get(i));
        }
        createSectionCell(s, headerRow, weeks.size() + 1, t(lang, "total"));

        final Map<String, Double> hoursMap = new HashMap<>();
        double globalMax = 0;
        for (final TimeEntry e : entries) {
            final int week = e.getStartTime().get(wf.weekOfWeekBasedYear());
            final String key = e.getDeveloper().getId() + "_" + week;
            final double h = durationHours(e);
            hoursMap.merge(key, h, Double::sum);
            globalMax = Math.max(globalMax, hoursMap.get(key));
        }

        int altRow = 0;
        for (final Long devId : devIds) {
            final boolean alt = (altRow++ % 2 != 0);
            final Row row = sheet.createRow(rowIdx++);
            String devName = devMap.get(devId).getDisplayUsername();
            if (devName == null) {
                devName = devMap.get(devId).getUsername();
            }
            createStyledCell(s, row, 0, devName, alt ? s.altRow : s.dataCell);
            double devTotal = 0;
            for (int i = 0; i < weeks.size(); i++) {
                final double h = hoursMap.getOrDefault(devId + "_" + weeks.get(i), 0.0);
                devTotal += h;
                final Cell cell = row.createCell(i + 1);
                if (h > 0) {
                    cell.setCellValue(h);
                    cell.setCellStyle(s.numCell);
                } else {
                    cell.setCellStyle(alt ? s.altRow : s.dataCell);
                }
            }
            createNumericCell(s, row, weeks.size() + 1, devTotal, alt ? s.altNumRow : s.numCell);
        }

        // Total row
        final Row totalRow = sheet.createRow(rowIdx++);
        createSectionCell(s, totalRow, 0, t(lang, "total"));
        final double[] colTotals = new double[weeks.size()];
        double grandTotal = 0;
        for (final Long devId : devIds) {
            for (int i = 0; i < weeks.size(); i++) {
                colTotals[i] += hoursMap.getOrDefault(devId + "_" + weeks.get(i), 0.0);
                grandTotal += hoursMap.getOrDefault(devId + "_" + weeks.get(i), 0.0);
            }
        }
        for (int i = 0; i < weeks.size(); i++) {
            createNumericCell(s, totalRow, i + 1, colTotals[i], s.numCell);
        }
        createNumericCell(s, totalRow, weeks.size() + 1, grandTotal, s.numCell);

        // Apply conditional formatting to data region
        final int totalCols = weeks.size() + 2;
        if (rowIdx > 2) {
            applyHoursConditionalFormatting(sheet, 1, rowIdx - 2, 1, weeks.size());
        }

        autoSizeColumns(sheet, totalCols);
        sheet.createFreezePane(1, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, totalCols - 1));
    }

    private void writeProjectEntriesSheet(final XSSFWorkbook wb, final Styles s, final XSSFSheet sheet,
                                          final List<TimeEntry> entries, final String lang) {
        int rowIdx = 0;
        final String[] headers = {
                t(lang, "date"), t(lang, "developer"),
                t(lang, "start"), t(lang, "end"), t(lang, "duration"), t(lang, "description")
        };
        rowIdx = writeTableHeader(s, sheet, rowIdx, headers);

        final List<TimeEntry> sorted = entries.stream()
                .sorted(Comparator
                        .comparing((TimeEntry e) -> {
                            final String name = e.getDeveloper().getDisplayUsername();
                            return name != null ? name : e.getDeveloper().getUsername();
                        })
                        .thenComparing(TimeEntry::getStartTime))
                .toList();

        final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        int altRow = 0;
        for (final TimeEntry e : sorted) {
            final boolean isLeave = e.getType() == TimeEntryType.LEAVE;
            final CellStyle rowStyle = isLeave ? s.leaveRow : ((altRow % 2 != 0) ? s.altRow : s.dataCell);
            final CellStyle numStyle = isLeave ? s.leaveNumRow : ((altRow % 2 != 0) ? s.altNumRow : s.numCell);
            altRow++;

            final Row row = sheet.createRow(rowIdx++);
            createStyledCell(s, row, 0, e.getStartTime().format(dateFmt), rowStyle);
            final String devName = e.getDeveloper().getDisplayUsername() != null
                    ? e.getDeveloper().getDisplayUsername() : e.getDeveloper().getUsername();
            createStyledCell(s, row, 1, devName, rowStyle);
            createStyledCell(s, row, 2, e.getStartTime().format(timeFmt), rowStyle);
            createStyledCell(s, row, 3, e.getEndTime().format(timeFmt), rowStyle);
            createNumericCell(s, row, 4, durationHours(e), numStyle);
            createStyledCell(s, row, 5, e.getDescription() != null ? e.getDescription() : "", rowStyle);
        }

        applyHoursConditionalFormatting(sheet, 1, rowIdx - 1, 4);
        autoSizeColumns(sheet, headers.length);
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
    }

    // ── Client sheets ─────────────────────────────────────────────────────────────

    private void writeClientSummarySheet(final XSSFWorkbook wb, final Styles s, final XSSFSheet sheet,
                                         final Client client, final List<TimeEntry> entries,
                                         final LocalDate start, final LocalDate end, final String lang) {
        int rowIdx = 0;

        final Row titleRow = sheet.createRow(rowIdx++);
        createKpiCell(wb, s, titleRow, 0, t(lang, "timesheetReport") + " – " + client.getName(), true);

        final double totalHours = sumHours(entries);
        final long projectCount = entries.stream().filter(e -> e.getProject() != null)
                .map(e -> e.getProject().getId()).distinct().count();
        final long devCount = entries.stream().map(e -> e.getDeveloper().getId()).distinct().count();

        rowIdx = writeKpiRow(wb, s, sheet, rowIdx, new String[]{
                t(lang, "totalHours"), t(lang, "projects"), t(lang, "developers")
        }, new double[]{totalHours, (double) projectCount, (double) devCount}, lang);

        rowIdx++;

        // Group by project, list each task individually
        final Map<Long, List<TimeEntry>> byProject = entries.stream()
                .filter(e -> e.getProject() != null)
                .collect(Collectors.groupingBy(e -> e.getProject().getId()));

        final List<Map.Entry<Long, List<TimeEntry>>> sortedProjects = byProject.entrySet().stream()
                .sorted((a, b) -> Double.compare(sumHours(b.getValue()), sumHours(a.getValue())))
                .toList();

        final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        final Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;

        for (final Map.Entry<Long, List<TimeEntry>> entry : sortedProjects) {
            final Project p = entry.getValue().get(0).getProject();
            final double projectHours = sumHours(entry.getValue());
            final double pct = totalHours > 0 ? (projectHours / totalHours) * 100 : 0;

            // Project section header
            final String sectionTitle = p.getName()
                    + " — " + String.format("%.1fh", projectHours)
                    + " (" + String.format("%.1f%%", pct) + ")";
            rowIdx = writeSectionHeader(s, sheet, rowIdx, sectionTitle, 7);

            final String[] taskHeaders = {
                    t(lang, "date"), t(lang, "day"), t(lang, "developer"),
                    t(lang, "start"), t(lang, "end"), t(lang, "duration"), t(lang, "description")
            };
            rowIdx = writeTableHeader(s, sheet, rowIdx, taskHeaders);

            final List<TimeEntry> projectEntries = entry.getValue().stream()
                    .sorted(Comparator.comparing(TimeEntry::getStartTime))
                    .toList();

            int altRow = 0;
            final int dataStart = rowIdx;
            for (final TimeEntry e : projectEntries) {
                final boolean isLeave = e.getType() == TimeEntryType.LEAVE;
                final CellStyle rowStyle = isLeave ? s.leaveRow : ((altRow % 2 != 0) ? s.altRow : s.dataCell);
                final CellStyle numStyle = isLeave ? s.leaveNumRow : ((altRow % 2 != 0) ? s.altNumRow : s.numCell);
                altRow++;

                final Row row = sheet.createRow(rowIdx++);
                createStyledCell(s, row, 0, e.getStartTime().format(dateFmt), rowStyle);
                createStyledCell(s, row, 1, e.getStartTime().getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, locale), rowStyle);
                final String devName = e.getDeveloper().getDisplayUsername() != null
                        ? e.getDeveloper().getDisplayUsername() : e.getDeveloper().getUsername();
                createStyledCell(s, row, 2, devName, rowStyle);
                createStyledCell(s, row, 3, e.getStartTime().format(timeFmt), rowStyle);
                createStyledCell(s, row, 4, e.getEndTime().format(timeFmt), rowStyle);
                createNumericCell(s, row, 5, durationHours(e), numStyle);
                createStyledCell(s, row, 6, e.getDescription() != null ? e.getDescription() : "", rowStyle);
            }

            if (dataStart < rowIdx) {
                applyHoursConditionalFormatting(sheet, dataStart, rowIdx - 1, 5);
            }

            rowIdx++; // spacer
        }

        autoSizeColumns(sheet, 7);
    }

    private void writeClientCrossTabSheet(final XSSFWorkbook wb, final Styles s, final XSSFSheet sheet,
                                          final List<Project> projects, final List<TimeEntry> entries, final String lang) {
        final List<Long> devIds = entries.stream()
                .map(e -> e.getDeveloper().getId())
                .distinct().sorted().toList();
        final Map<Long, Developer> devMap = entries.stream()
                .collect(Collectors.toMap(e -> e.getDeveloper().getId(), TimeEntry::getDeveloper, (a, b) -> a));
        final List<Project> involvedProjects = projects.stream()
                .filter(p -> entries.stream().anyMatch(e -> e.getProject() != null
                        && e.getProject().getId().equals(p.getId())))
                .toList();

        if (involvedProjects.isEmpty() || devIds.isEmpty()) {
            return;
        }

        int rowIdx = 0;
        final Row headerRow = sheet.createRow(rowIdx++);
        createSectionCell(s, headerRow, 0, t(lang, "developer"));
        for (int i = 0; i < involvedProjects.size(); i++) {
            createSectionCell(s, headerRow, i + 1, involvedProjects.get(i).getName());
        }
        createSectionCell(s, headerRow, involvedProjects.size() + 1, t(lang, "total"));

        final Map<String, Double> hoursMap = new HashMap<>();
        for (final TimeEntry e : entries) {
            if (e.getProject() == null) {
                continue;
            }
            final String key = e.getDeveloper().getId() + "_" + e.getProject().getId();
            hoursMap.merge(key, durationHours(e), Double::sum);
        }

        int altRow = 0;
        final double[] projectTotals = new double[involvedProjects.size()];
        for (final Long devId : devIds) {
            final boolean alt = (altRow++ % 2 != 0);
            final Row row = sheet.createRow(rowIdx++);
            String devName = devMap.get(devId).getDisplayUsername();
            if (devName == null) {
                devName = devMap.get(devId).getUsername();
            }
            createStyledCell(s, row, 0, devName, alt ? s.altRow : s.dataCell);
            double devTotal = 0;
            for (int i = 0; i < involvedProjects.size(); i++) {
                final double h = hoursMap.getOrDefault(devId + "_" + involvedProjects.get(i).getId(), 0.0);
                devTotal += h;
                projectTotals[i] += h;
                if (h > 0) {
                    createNumericCell(s, row, i + 1, h, alt ? s.altNumRow : s.numCell);
                } else {
                    createStyledCell(s, row, i + 1, "-", alt ? s.altRow : s.dataCell);
                }
            }
            createNumericCell(s, row, involvedProjects.size() + 1, devTotal, alt ? s.altNumRow : s.numCell);
        }

        // Total row
        final Row totalRow = sheet.createRow(rowIdx++);
        createSectionCell(s, totalRow, 0, t(lang, "total"));
        double grandTotal = 0;
        for (int i = 0; i < involvedProjects.size(); i++) {
            createNumericCell(s, totalRow, i + 1, projectTotals[i], s.numCell);
            grandTotal += projectTotals[i];
        }
        createNumericCell(s, totalRow, involvedProjects.size() + 1, grandTotal, s.numCell);

        final int totalCols = involvedProjects.size() + 2;
        if (rowIdx > 2) {
            applyHoursConditionalFormatting(sheet, 1, rowIdx - 2, 1, involvedProjects.size());
        }

        autoSizeColumns(sheet, totalCols);
        sheet.createFreezePane(1, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, totalCols - 1));
    }

    private void writeClientMonthlySheet(final XSSFWorkbook wb, final Styles s, final XSSFSheet sheet,
                                         final List<TimeEntry> entries, final String lang) {
        int rowIdx = 0;
        final String[] headers = {t(lang, "month"), t(lang, "hours")};
        rowIdx = writeTableHeader(s, sheet, rowIdx, headers);

        final Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;
        final DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        final DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("MMMM yyyy", locale);

        final Map<String, Double> monthMap = new TreeMap<>();
        for (final TimeEntry e : entries) {
            final String key = e.getStartTime().format(monthFmt);
            monthMap.merge(key, durationHours(e), Double::sum);
        }

        int altRow = 0;
        for (final Map.Entry<String, Double> entry : monthMap.entrySet()) {
            final boolean alt = (altRow++ % 2 != 0);
            final Row row = sheet.createRow(rowIdx++);
            final LocalDate d = LocalDate.parse(entry.getKey() + "-01");
            String monthLabel = d.format(displayFmt);
            monthLabel = monthLabel.substring(0, 1).toUpperCase() + monthLabel.substring(1);
            createStyledCell(s, row, 0, monthLabel, alt ? s.altRow : s.dataCell);
            createNumericCell(s, row, 1, entry.getValue(), alt ? s.altNumRow : s.numCell);
        }

        applyHoursConditionalFormatting(sheet, 1, rowIdx - 1, 1);
        autoSizeColumns(sheet, headers.length);
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
    }

    private void writeClientEntriesSheet(final XSSFWorkbook wb, final Styles s, final XSSFSheet sheet,
                                         final List<TimeEntry> entries, final String lang) {
        int rowIdx = 0;
        final String[] headers = {
                t(lang, "date"), t(lang, "project"), t(lang, "developer"),
                t(lang, "start"), t(lang, "end"), t(lang, "duration"), t(lang, "description")
        };
        rowIdx = writeTableHeader(s, sheet, rowIdx, headers);

        final List<TimeEntry> sorted = entries.stream()
                .sorted(Comparator
                        .comparing((TimeEntry e) -> e.getProject() != null ? e.getProject().getName() : "")
                        .thenComparing(TimeEntry::getStartTime))
                .toList();

        final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        int altRow = 0;
        for (final TimeEntry e : sorted) {
            final boolean isLeave = e.getType() == TimeEntryType.LEAVE;
            final CellStyle rowStyle = isLeave ? s.leaveRow : ((altRow % 2 != 0) ? s.altRow : s.dataCell);
            final CellStyle numStyle = isLeave ? s.leaveNumRow : ((altRow % 2 != 0) ? s.altNumRow : s.numCell);
            altRow++;

            final Row row = sheet.createRow(rowIdx++);
            createStyledCell(s, row, 0, e.getStartTime().format(dateFmt), rowStyle);
            createStyledCell(s, row, 1, e.getProject() != null ? e.getProject().getName() : "-", rowStyle);
            final String devName = e.getDeveloper().getDisplayUsername() != null
                    ? e.getDeveloper().getDisplayUsername() : e.getDeveloper().getUsername();
            createStyledCell(s, row, 2, devName, rowStyle);
            createStyledCell(s, row, 3, e.getStartTime().format(timeFmt), rowStyle);
            createStyledCell(s, row, 4, e.getEndTime().format(timeFmt), rowStyle);
            createNumericCell(s, row, 5, durationHours(e), numStyle);
            createStyledCell(s, row, 6, e.getDescription() != null ? e.getDescription() : "", rowStyle);
        }

        applyHoursConditionalFormatting(sheet, 1, rowIdx - 1, 5);
        autoSizeColumns(sheet, headers.length);
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
    }

    // ── Layout helpers ────────────────────────────────────────────────────────────

    private int writeKpiRow(final XSSFWorkbook wb, final Styles s, final XSSFSheet sheet, int rowIdx,
                            final String[] labels, final double[] values, final String lang) {
        final Row labelRow = sheet.createRow(rowIdx++);
        final Row valueRow = sheet.createRow(rowIdx++);

        for (int i = 0; i < labels.length; i++) {
            final Cell labelCell = labelRow.createCell(i);
            labelCell.setCellValue(labels[i]);
            labelCell.setCellStyle(s.kpiLabel);

            final Cell valueCell = valueRow.createCell(i);
            valueCell.setCellValue(values[i]);
            valueCell.setCellStyle(s.kpiValue);
        }
        return rowIdx;
    }

    private int writeSectionHeader(final Styles s, final XSSFSheet sheet, int rowIdx, final String title, final int span) {
        final Row row = sheet.createRow(rowIdx++);
        final Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(s.sectionHeader);
        if (span > 1) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, span - 1));
        }
        return rowIdx;
    }

    private int writeTableHeader(final Styles s, final XSSFSheet sheet, int rowIdx, final String[] headers) {
        final Row row = sheet.createRow(rowIdx++);
        for (int i = 0; i < headers.length; i++) {
            final Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(s.tableHeader);
        }
        return rowIdx;
    }

    private void createKpiCell(final XSSFWorkbook wb, final Styles s, final Row row, final int col,
                               final String value, final boolean bold) {
        final Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(bold ? s.kpiTitle : s.kpiLabel);
    }

    private void createStyledCell(final Styles s, final Row row, final int col, final String value, final CellStyle style) {
        final Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createNumericCell(final Styles s, final Row row, final int col, final double value, final CellStyle style) {
        final Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createSectionCell(final Styles s, final Row row, final int col, final String value) {
        final Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(s.sectionHeader);
    }

    private void autoSizeColumns(final XSSFSheet sheet, final int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
        }
    }

    private void freezeAndFilter(final XSSFSheet sheet, final int freezeRow, final int filterRow) {
        sheet.createFreezePane(0, Math.max(1, freezeRow));
        sheet.setAutoFilter(new CellRangeAddress(Math.max(0, filterRow), Math.max(0, filterRow),
                0, sheet.getRow(Math.max(0, filterRow)) != null
                        ? sheet.getRow(Math.max(0, filterRow)).getLastCellNum() - 1 : 3));
    }

    /**
     * Apply a white-to-blue colour scale (3-colour, 0 -> white, max -> blue) on a single column.
     */
    private void applyHoursConditionalFormatting(final XSSFSheet sheet, final int firstRow, final int lastRow, final int col) {
        if (firstRow > lastRow) {
            return;
        }
        final SheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();
        final ConditionalFormattingRule rule = scf.createConditionalFormattingColorScaleRule();
        final ColorScaleFormatting csf = rule.getColorScaleFormatting();
        csf.getThresholds()[0].setRangeType(ConditionalFormattingThreshold.RangeType.MIN);
        csf.getThresholds()[1].setRangeType(ConditionalFormattingThreshold.RangeType.PERCENTILE);
        csf.getThresholds()[1].setValue(50.0);
        csf.getThresholds()[2].setRangeType(ConditionalFormattingThreshold.RangeType.MAX);
        ((XSSFColor) csf.getColors()[0]).setARGBHex("FF" + HEX_WHITE);
        ((XSSFColor) csf.getColors()[1]).setARGBHex("FFADD8E6");
        ((XSSFColor) csf.getColors()[2]).setARGBHex("FF" + HEX_BLUE);
        final CellRangeAddress[] regions = {new CellRangeAddress(firstRow, lastRow, col, col)};
        scf.addConditionalFormatting(regions, rule);
    }

    /**
     * Apply colour scale across a range of columns (fromCol to toCol inclusive).
     */
    private void applyHoursConditionalFormatting(final XSSFSheet sheet, final int firstRow, final int lastRow,
                                                 final int fromCol, final int toCol) {
        if (firstRow > lastRow || fromCol > toCol) {
            return;
        }
        final SheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();
        final ConditionalFormattingRule rule = scf.createConditionalFormattingColorScaleRule();
        final ColorScaleFormatting csf = rule.getColorScaleFormatting();
        csf.getThresholds()[0].setRangeType(ConditionalFormattingThreshold.RangeType.MIN);
        csf.getThresholds()[1].setRangeType(ConditionalFormattingThreshold.RangeType.PERCENTILE);
        csf.getThresholds()[1].setValue(50.0);
        csf.getThresholds()[2].setRangeType(ConditionalFormattingThreshold.RangeType.MAX);
        ((XSSFColor) csf.getColors()[0]).setARGBHex("FF" + HEX_WHITE);
        ((XSSFColor) csf.getColors()[1]).setARGBHex("FFADD8E6");
        ((XSSFColor) csf.getColors()[2]).setARGBHex("FF" + HEX_BLUE);
        final CellRangeAddress[] regions = {new CellRangeAddress(firstRow, lastRow, fromCol, toCol)};
        scf.addConditionalFormatting(regions, rule);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────────

    private double sumHours(final List<TimeEntry> entries) {
        return entries.stream().mapToDouble(this::durationHours).sum();
    }

    private double durationHours(final TimeEntry e) {
        return java.time.Duration.between(e.getStartTime(), e.getEndTime()).toMinutes() / 60.0;
    }

    private byte[] toBytes(final XSSFWorkbook wb) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        return baos.toByteArray();
    }

    // ── Style registry ────────────────────────────────────────────────────────────

    private static class Styles {
        final CellStyle tableHeader;
        final CellStyle sectionHeader;
        final CellStyle dataCell;
        final CellStyle altRow;
        final CellStyle numCell;
        final CellStyle altNumRow;
        final CellStyle leaveRow;
        final CellStyle leaveNumRow;
        final CellStyle kpiLabel;
        final CellStyle kpiValue;
        final CellStyle kpiTitle;

        Styles(final XSSFWorkbook wb) {
            final XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 9);
            headerFont.setColor(new XSSFColor(parseHex(HEX_WHITE), null));

            final XSSFFont sectionFont = wb.createFont();
            sectionFont.setBold(true);
            sectionFont.setFontHeightInPoints((short) 9);
            sectionFont.setColor(new XSSFColor(parseHex(HEX_WHITE), null));

            final XSSFFont dataFont = wb.createFont();
            dataFont.setFontHeightInPoints((short) 9);

            final XSSFFont kpiLabelFnt = wb.createFont();
            kpiLabelFnt.setFontHeightInPoints((short) 9);
            kpiLabelFnt.setColor(new XSSFColor(new byte[]{(byte) 0x9B, (byte) 0x9A, (byte) 0x97}, null));

            final XSSFFont kpiValueFnt = wb.createFont();
            kpiValueFnt.setBold(true);
            kpiValueFnt.setFontHeightInPoints((short) 14);
            kpiValueFnt.setColor(new XSSFColor(parseHex(HEX_DARK_BG), null));

            final XSSFFont kpiTitleFnt = wb.createFont();
            kpiTitleFnt.setBold(true);
            kpiTitleFnt.setFontHeightInPoints((short) 12);
            kpiTitleFnt.setColor(new XSSFColor(parseHex(HEX_DARK_BG), null));

            final DataFormat fmt = wb.createDataFormat();
            final short hoursFormat = fmt.getFormat("0.0\"h\"");

            tableHeader = createStyle(wb, parseHex(HEX_DARK_BG), headerFont, false, (short) -1);
            sectionHeader = createStyle(wb, parseHex(HEX_DARK_BG), sectionFont, false, (short) -1);
            dataCell = createStyle(wb, parseHex(HEX_WHITE), dataFont, false, (short) -1);
            altRow = createStyle(wb, parseHex(HEX_SECTION_BG), dataFont, false, (short) -1);
            numCell = createStyle(wb, parseHex(HEX_WHITE), dataFont, true, hoursFormat);
            altNumRow = createStyle(wb, parseHex(HEX_SECTION_BG), dataFont, true, hoursFormat);
            leaveRow = createStyle(wb, parseHex(HEX_LEAVE_BG), dataFont, false, (short) -1);
            leaveNumRow = createStyle(wb, parseHex(HEX_LEAVE_BG), dataFont, true, hoursFormat);

            kpiLabel = wb.createCellStyle();
            ((XSSFCellStyle) kpiLabel).setFillForegroundColor(new XSSFColor(parseHex(HEX_WHITE), null));
            kpiLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            kpiLabel.setFont(kpiLabelFnt);
            ((XSSFCellStyle) kpiLabel).setBorderBottom(BorderStyle.MEDIUM);
            ((XSSFCellStyle) kpiLabel).setBottomBorderColor(new XSSFColor(parseHex(HEX_BLUE), null));

            kpiValue = wb.createCellStyle();
            ((XSSFCellStyle) kpiValue).setFillForegroundColor(new XSSFColor(parseHex(HEX_WHITE), null));
            kpiValue.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            kpiValue.setFont(kpiValueFnt);
            kpiValue.setDataFormat(hoursFormat);
            ((XSSFCellStyle) kpiValue).setBorderBottom(BorderStyle.MEDIUM);
            ((XSSFCellStyle) kpiValue).setBottomBorderColor(new XSSFColor(parseHex(HEX_BLUE), null));

            kpiTitle = wb.createCellStyle();
            ((XSSFCellStyle) kpiTitle).setFillForegroundColor(new XSSFColor(parseHex(HEX_WHITE), null));
            kpiTitle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            kpiTitle.setFont(kpiTitleFnt);
        }

        private static CellStyle createStyle(final XSSFWorkbook wb, final byte[] bgRgb, final XSSFFont font,
                                             final boolean rightAlign, final short dataFormat) {
            final XSSFCellStyle style = wb.createCellStyle();
            style.setFillForegroundColor(new XSSFColor(bgRgb, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setFont(font);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBottomBorderColor(new XSSFColor(parseHex("E9E9E7"), null));
            style.setTopBorderColor(new XSSFColor(parseHex("E9E9E7"), null));
            style.setLeftBorderColor(new XSSFColor(parseHex("E9E9E7"), null));
            style.setRightBorderColor(new XSSFColor(parseHex("E9E9E7"), null));
            if (rightAlign) {
                style.setAlignment(HorizontalAlignment.RIGHT);
            }
            if (dataFormat >= 0) {
                style.setDataFormat(dataFormat);
            }
            return style;
        }

        private static byte[] parseHex(final String hex) {
            final int r = Integer.parseInt(hex.substring(0, 2), 16);
            final int g = Integer.parseInt(hex.substring(2, 4), 16);
            final int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new byte[]{(byte) r, (byte) g, (byte) b};
        }
    }
}
