package com.timesheet.service.export;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.timesheet.model.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PdfExportService {

    // ── Colour palette ──────────────────────────────────────────────────────────
    private static final Color COLOR_DARK_BG     = new Color(0x37, 0x35, 0x2F);
    private static final Color COLOR_SECTION_BG  = new Color(0xF7, 0xF6, 0xF3);
    private static final Color COLOR_ROW_ALT     = new Color(0xF7, 0xF6, 0xF3);
    private static final Color COLOR_BORDER      = new Color(0xE9, 0xE9, 0xE7);
    private static final Color COLOR_LEAVE_BG    = new Color(0xE8, 0xF5, 0xE9);
    private static final Color COLOR_FOOTER      = new Color(0x9B, 0x9A, 0x97);
    private static final Color COLOR_WHITE       = Color.WHITE;

    // ── i18n ────────────────────────────────────────────────────────────────────
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
        en.put("weeklyActivity",       "Week-by-week Activity");
        en.put("total",                "Total");
        en.put("workHours",            "Work Hours");
        en.put("leaveHours",           "Leave Hours");
        I18N.put("en", en);
    }

    private String t(final String lang, final String key) {
        final Map<String, String> map = I18N.getOrDefault(lang, I18N.get("fr"));
        return map.getOrDefault(key, key);
    }

    // ── Public API ───────────────────────────────────────────────────────────────

    public byte[] generateDeveloperReport(final Developer dev, final List<TimeEntry> entries,
                                          final LocalDate start, final LocalDate end, final String lang) {
        final String subject = dev.getDisplayUsername() != null ? dev.getDisplayUsername() : dev.getUsername();
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final Document doc = createDocument();
            final PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new HeaderFooterEvent(subject, formatPeriod(start, end, lang), lang));
            doc.open();

            // KPI row
            final double totalHours = sumHours(entries);
            final long projectCount = entries.stream()
                    .filter(e -> e.getProject() != null)
                    .map(e -> e.getProject().getId())
                    .distinct().count();
            final long leaveDays = entries.stream()
                    .filter(e -> e.getType() == TimeEntryType.LEAVE)
                    .map(e -> e.getStartTime().toLocalDate())
                    .distinct().count();

            doc.add(buildKpiTable(lang, new String[]{
                    t(lang, "totalHours"),
                    t(lang, "projects"),
                    t(lang, "leaveDays")
            }, new String[]{
                    formatHours(totalHours),
                    String.valueOf(projectCount),
                    String.valueOf(leaveDays)
            }));
            doc.add(Chunk.NEWLINE);

            // Hours by project
            doc.add(buildSectionTitle(t(lang, "hoursByProject")));
            doc.add(buildHoursByProjectTable(entries, totalHours, lang));
            doc.add(Chunk.NEWLINE);

            // Daily log
            doc.add(buildSectionTitle(t(lang, "dailyLog")));
            doc.add(buildDailyLogTable(entries, lang));
            doc.add(Chunk.NEWLINE);

            // Weekly summary
            doc.add(buildSectionTitle(t(lang, "weeklySummary")));
            doc.add(buildWeeklySummaryTable(entries, lang));

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate developer PDF report", e);
        }
    }

    public byte[] generateProjectReport(final Project project, final List<TimeEntry> entries,
                                        final LocalDate start, final LocalDate end, final String lang) {
        final String subject = project.getName();
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final Document doc = createDocument();
            final PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new HeaderFooterEvent(subject, formatPeriod(start, end, lang), lang));
            doc.open();

            // KPI row
            final double totalHours = sumHours(entries);
            final long developerCount = entries.stream()
                    .map(e -> e.getDeveloper().getId())
                    .distinct().count();
            final long sessionCount = entries.size();

            doc.add(buildKpiTable(lang, new String[]{
                    t(lang, "totalHours"),
                    t(lang, "developers"),
                    t(lang, "sessions")
            }, new String[]{
                    formatHours(totalHours),
                    String.valueOf(developerCount),
                    String.valueOf(sessionCount)
            }));
            doc.add(Chunk.NEWLINE);

            // Team breakdown
            doc.add(buildSectionTitle(t(lang, "teamBreakdown")));
            doc.add(buildTeamBreakdownTable(entries, totalHours, lang));
            doc.add(Chunk.NEWLINE);

            // Week-by-week activity
            doc.add(buildSectionTitle(t(lang, "weeklyActivity")));
            doc.add(buildWeekByWeekTable(entries, lang));
            doc.add(Chunk.NEWLINE);

            // Detailed log
            doc.add(buildSectionTitle(t(lang, "detailedLog")));
            doc.add(buildProjectDetailedLogTable(entries, lang));

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate project PDF report", e);
        }
    }

    public byte[] generateClientReport(final Client client, final List<Project> projects, final List<TimeEntry> entries,
                                       final LocalDate start, final LocalDate end, final String lang) {
        final String subject = client.getName();
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final Document doc = createDocument();
            final PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new HeaderFooterEvent(subject, formatPeriod(start, end, lang), lang));
            doc.open();

            // KPI row
            final double totalHours = sumHours(entries);
            final long projectCount = entries.stream()
                    .filter(e -> e.getProject() != null)
                    .map(e -> e.getProject().getId())
                    .distinct().count();
            final long developerCount = entries.stream()
                    .map(e -> e.getDeveloper().getId())
                    .distinct().count();

            doc.add(buildKpiTable(lang, new String[]{
                    t(lang, "totalHours"),
                    t(lang, "projects"),
                    t(lang, "developers")
            }, new String[]{
                    formatHours(totalHours),
                    String.valueOf(projectCount),
                    String.valueOf(developerCount)
            }));
            doc.add(Chunk.NEWLINE);

            // Hours by project
            doc.add(buildSectionTitle(t(lang, "hoursByProject")));
            doc.add(buildClientHoursByProjectTable(entries, totalHours, lang));
            doc.add(Chunk.NEWLINE);

            // Developer contribution cross-tab
            doc.add(buildSectionTitle(t(lang, "developerContribution")));
            doc.add(buildDeveloperCrossTab(projects, entries, lang));
            doc.add(Chunk.NEWLINE);

            // Monthly trend
            doc.add(buildSectionTitle(t(lang, "monthlyTrend")));
            doc.add(buildMonthlyTrendTable(entries, lang));
            doc.add(Chunk.NEWLINE);

            // Detailed log
            doc.add(buildSectionTitle(t(lang, "detailedLog")));
            doc.add(buildClientDetailedLogTable(entries, lang));

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate client PDF report", e);
        }
    }

    // ── Document setup ───────────────────────────────────────────────────────────

    private Document createDocument() {
        final Document doc = new Document(PageSize.A4, 40, 40, 80, 60);
        return doc;
    }

    // ── KPI row ──────────────────────────────────────────────────────────────────

    private PdfPTable buildKpiTable(final String lang, final String[] labels, final String[] values) throws DocumentException {
        final PdfPTable table = new PdfPTable(labels.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);

        final Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, COLOR_FOOTER);
        final Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, COLOR_DARK_BG);

        for (int i = 0; i < labels.length; i++) {
            final PdfPCell cell = new PdfPCell();
            cell.setBorderColor(new Color(0x2E, 0x75, 0xB6));
            cell.setBorderWidthBottom(2f);
            cell.setBorderWidthTop(0);
            cell.setBorderWidthLeft(0);
            cell.setBorderWidthRight(0);
            cell.setPadding(10f);
            cell.setBackgroundColor(COLOR_WHITE);

            final Paragraph p = new Paragraph();
            p.add(new Phrase(values[i] + "\n", valueFont));
            p.add(new Phrase(labels[i], labelFont));
            cell.addElement(p);
            table.addCell(cell);
        }
        return table;
    }

    // ── Section title ────────────────────────────────────────────────────────────

    private PdfPTable buildSectionTitle(final String title) throws DocumentException {
        final PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(12f);
        table.setSpacingAfter(4f);

        final Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, COLOR_DARK_BG);
        final PdfPCell cell = new PdfPCell(new Phrase(title, font));
        cell.setBackgroundColor(COLOR_SECTION_BG);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(6f);
        cell.setPaddingBottom(6f);
        cell.setPaddingLeft(8f);
        table.addCell(cell);
        return table;
    }

    // ── Developer report tables ───────────────────────────────────────────────────

    private PdfPTable buildHoursByProjectTable(final List<TimeEntry> entries, final double totalHours, final String lang)
            throws DocumentException {
        final PdfPTable table = new PdfPTable(new float[]{3, 2, 1.5f, 1});
        table.setWidthPercentage(100);
        addTableHeader(table, new String[]{
                t(lang, "project"), t(lang, "client"), t(lang, "hours"), t(lang, "share")
        });

        final Map<Long, List<TimeEntry>> byProject = entries.stream()
                .filter(e -> e.getProject() != null)
                .collect(Collectors.groupingBy(e -> e.getProject().getId()));

        final List<Map.Entry<Long, List<TimeEntry>>> sorted = byProject.entrySet().stream()
                .sorted((a, b) -> Double.compare(sumHours(b.getValue()), sumHours(a.getValue())))
                .toList();

        int row = 0;
        for (final Map.Entry<Long, List<TimeEntry>> entry : sorted) {
            final Project p = entry.getValue().get(0).getProject();
            final double h = sumHours(entry.getValue());
            final double pct = totalHours > 0 ? (h / totalHours) * 100 : 0;
            final Color bg = (row++ % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT;
            addRow(table, bg, false,
                    p.getName(),
                    p.getClient() != null ? p.getClient().getName() : "-",
                    formatHours(h),
                    String.format("%.1f%%", pct));
        }

        // Entries with no project
        final List<TimeEntry> noProject = entries.stream().filter(e -> e.getProject() == null).toList();
        if (!noProject.isEmpty()) {
            final double h = sumHours(noProject);
            final double pct = totalHours > 0 ? (h / totalHours) * 100 : 0;
            final Color bg = (row % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT;
            addRow(table, bg, false, "-", "-", formatHours(h), String.format("%.1f%%", pct));
        }

        return table;
    }

    private PdfPTable buildDailyLogTable(final List<TimeEntry> entries, final String lang)
            throws DocumentException {
        final PdfPTable table = new PdfPTable(new float[]{2, 1.5f, 2.5f, 2, 1.5f, 3});
        table.setWidthPercentage(100);
        addTableHeader(table, new String[]{
                t(lang, "date"), t(lang, "day"), t(lang, "project"),
                t(lang, "client"), t(lang, "duration"), t(lang, "description")
        });

        final List<TimeEntry> sorted = entries.stream()
                .sorted(Comparator.comparing(TimeEntry::getStartTime))
                .toList();

        final Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;
        final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", locale);

        int row = 0;
        for (final TimeEntry e : sorted) {
            final boolean isLeave = e.getType() == TimeEntryType.LEAVE;
            final Color bg = isLeave ? COLOR_LEAVE_BG : ((row % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT);
            final double h = durationHours(e);
            final String projectName = e.getProject() != null ? e.getProject().getName() : "-";
            final String clientName  = e.getProject() != null && e.getProject().getClient() != null
                    ? e.getProject().getClient().getName() : "-";
            final String dayName = e.getStartTime().getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, locale);
            addRow(table, bg, false,
                    e.getStartTime().format(dateFmt),
                    capitalize(dayName),
                    projectName,
                    clientName,
                    formatHours(h),
                    e.getDescription() != null ? e.getDescription() : "");
            row++;
        }
        return table;
    }

    private PdfPTable buildWeeklySummaryTable(final List<TimeEntry> entries, final String lang)
            throws DocumentException {
        final PdfPTable table = new PdfPTable(new float[]{1.5f, 2, 2, 2});
        table.setWidthPercentage(100);
        addTableHeader(table, new String[]{
                t(lang, "week"), t(lang, "workHours"), t(lang, "leaveHours"), t(lang, "total")
        });

        final WeekFields wf = WeekFields.ISO;
        final Map<Integer, double[]> weekMap = new TreeMap<>();
        for (final TimeEntry e : entries) {
            final int week = e.getStartTime().get(wf.weekOfWeekBasedYear());
            final double h = durationHours(e);
            weekMap.computeIfAbsent(week, k -> new double[]{0, 0});
            if (e.getType() == TimeEntryType.LEAVE) {
                weekMap.get(week)[1] += h;
            } else {
                weekMap.get(week)[0] += h;
            }
        }

        int row = 0;
        for (final Map.Entry<Integer, double[]> entry : weekMap.entrySet()) {
            final double work = entry.getValue()[0];
            final double leave = entry.getValue()[1];
            final Color bg = (row++ % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT;
            addRow(table, bg, false,
                    "W" + entry.getKey(),
                    formatHours(work),
                    formatHours(leave),
                    formatHours(work + leave));
        }
        return table;
    }

    // ── Project report tables ─────────────────────────────────────────────────────

    private PdfPTable buildTeamBreakdownTable(final List<TimeEntry> entries, final double totalHours, final String lang)
            throws DocumentException {
        final PdfPTable table = new PdfPTable(new float[]{3, 2.5f, 1.5f, 1});
        table.setWidthPercentage(100);
        addTableHeader(table, new String[]{
                t(lang, "developer"), t(lang, "title"), t(lang, "hours"), t(lang, "share")
        });

        final Map<Long, List<TimeEntry>> byDev = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getDeveloper().getId()));

        final List<Map.Entry<Long, List<TimeEntry>>> sorted = byDev.entrySet().stream()
                .sorted((a, b) -> Double.compare(sumHours(b.getValue()), sumHours(a.getValue())))
                .toList();

        int row = 0;
        for (final Map.Entry<Long, List<TimeEntry>> entry : sorted) {
            final Developer dev = entry.getValue().get(0).getDeveloper();
            final double h = sumHours(entry.getValue());
            final double pct = totalHours > 0 ? (h / totalHours) * 100 : 0;
            final Color bg = (row++ % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT;
            addRow(table, bg, false,
                    dev.getDisplayUsername() != null ? dev.getDisplayUsername() : dev.getUsername(),
                    dev.getTitle() != null ? dev.getTitle() : "-",
                    formatHours(h),
                    String.format("%.1f%%", pct));
        }
        return table;
    }

    private PdfPTable buildWeekByWeekTable(final List<TimeEntry> entries, final String lang)
            throws DocumentException {
        final WeekFields wf = WeekFields.ISO;

        // Collect developers and weeks
        final List<Long> devIds = entries.stream()
                .map(e -> e.getDeveloper().getId())
                .distinct()
                .sorted()
                .toList();
        final Map<Long, Developer> devMap = entries.stream()
                .collect(Collectors.toMap(e -> e.getDeveloper().getId(), TimeEntry::getDeveloper, (a, b) -> a));
        final List<Integer> weeks = entries.stream()
                .map(e -> e.getStartTime().get(wf.weekOfWeekBasedYear()))
                .distinct()
                .sorted()
                .toList();

        if (weeks.isEmpty()) {
            return new PdfPTable(1);
        }

        // cols: Week + one per developer
        final float[] colWidths = new float[1 + devIds.size()];
        colWidths[0] = 1f;
        Arrays.fill(colWidths, 1, colWidths.length, 2f);
        final PdfPTable table = new PdfPTable(colWidths);
        table.setWidthPercentage(100);

        // Build header
        final Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, COLOR_WHITE);
        addStyledCell(table, t(lang, "week"), COLOR_DARK_BG, headerFont, Element.ALIGN_LEFT);
        for (final Long devId : devIds) {
            String name = devMap.get(devId).getDisplayUsername();
            if (name == null) {
                name = devMap.get(devId).getUsername();
            }
            addStyledCell(table, name, COLOR_DARK_BG, headerFont, Element.ALIGN_CENTER);
        }

        // Group by devId and week
        final Map<String, Double> hoursMap = new HashMap<>();
        for (final TimeEntry e : entries) {
            final int week = e.getStartTime().get(wf.weekOfWeekBasedYear());
            final String key = e.getDeveloper().getId() + "_" + week;
            hoursMap.merge(key, durationHours(e), Double::sum);
        }

        // Find max for heatmap intensity
        final double max = hoursMap.values().stream().mapToDouble(d -> d).max().orElse(1);

        int row = 0;
        final Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COLOR_DARK_BG);
        for (final int week : weeks) {
            final Color rowBase = (row++ % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT;
            addStyledCell(table, "W" + week, rowBase, cellFont, Element.ALIGN_LEFT);
            for (final Long devId : devIds) {
                final double h = hoursMap.getOrDefault(devId + "_" + week, 0.0);
                final Color bg = h == 0 ? rowBase : heatmapColor(h, max);
                final Font f = h > 0
                        ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, COLOR_DARK_BG)
                        : cellFont;
                addStyledCell(table, h > 0 ? formatHours(h) : "", bg, f, Element.ALIGN_CENTER);
            }
        }
        return table;
    }

    private PdfPTable buildProjectDetailedLogTable(final List<TimeEntry> entries, final String lang)
            throws DocumentException {
        final PdfPTable table = new PdfPTable(new float[]{2, 2.5f, 1.5f, 3});
        table.setWidthPercentage(100);
        addTableHeader(table, new String[]{
                t(lang, "date"), t(lang, "developer"), t(lang, "duration"), t(lang, "description")
        });

        final Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;
        final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", locale);

        final List<TimeEntry> sorted = entries.stream()
                .sorted(Comparator.comparing(TimeEntry::getStartTime))
                .toList();

        int row = 0;
        for (final TimeEntry e : sorted) {
            final boolean isLeave = e.getType() == TimeEntryType.LEAVE;
            final Color bg = isLeave ? COLOR_LEAVE_BG : ((row % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT);
            final String devName = e.getDeveloper().getDisplayUsername() != null
                    ? e.getDeveloper().getDisplayUsername() : e.getDeveloper().getUsername();
            addRow(table, bg, false,
                    e.getStartTime().format(dateFmt),
                    devName,
                    formatHours(durationHours(e)),
                    e.getDescription() != null ? e.getDescription() : "");
            row++;
        }
        return table;
    }

    // ── Client report tables ──────────────────────────────────────────────────────

    private PdfPTable buildClientHoursByProjectTable(final List<TimeEntry> entries, final double totalHours, final String lang)
            throws DocumentException {
        final PdfPTable table = new PdfPTable(new float[]{3, 2, 1});
        table.setWidthPercentage(100);
        addTableHeader(table, new String[]{
                t(lang, "project"), t(lang, "hours"), t(lang, "share")
        });

        final Map<Long, List<TimeEntry>> byProject = entries.stream()
                .filter(e -> e.getProject() != null)
                .collect(Collectors.groupingBy(e -> e.getProject().getId()));

        final List<Map.Entry<Long, List<TimeEntry>>> sorted = byProject.entrySet().stream()
                .sorted((a, b) -> Double.compare(sumHours(b.getValue()), sumHours(a.getValue())))
                .toList();

        int row = 0;
        for (final Map.Entry<Long, List<TimeEntry>> entry : sorted) {
            final Project p = entry.getValue().get(0).getProject();
            final double h = sumHours(entry.getValue());
            final double pct = totalHours > 0 ? (h / totalHours) * 100 : 0;
            final Color bg = (row++ % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT;
            addRow(table, bg, false, p.getName(), formatHours(h), String.format("%.1f%%", pct));
        }
        return table;
    }

    private PdfPTable buildDeveloperCrossTab(final List<Project> projects, final List<TimeEntry> entries, final String lang)
            throws DocumentException {
        final List<Long> devIds = entries.stream()
                .map(e -> e.getDeveloper().getId())
                .distinct().sorted().toList();
        final Map<Long, Developer> devMap = entries.stream()
                .collect(Collectors.toMap(e -> e.getDeveloper().getId(), TimeEntry::getDeveloper, (a, b) -> a));
        final List<Project> involvedProjects = projects.stream()
                .filter(p -> entries.stream().anyMatch(e -> e.getProject() != null && e.getProject().getId().equals(p.getId())))
                .toList();

        if (involvedProjects.isEmpty() || devIds.isEmpty()) {
            return new PdfPTable(1);
        }

        final int cols = 1 + involvedProjects.size() + 1;
        final float[] widths = new float[cols];
        widths[0] = 2.5f;
        Arrays.fill(widths, 1, cols - 1, 1.5f);
        widths[cols - 1] = 1.5f;

        final PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);

        final Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, COLOR_WHITE);
        addStyledCell(table, t(lang, "developer"), COLOR_DARK_BG, headerFont, Element.ALIGN_LEFT);
        for (final Project p : involvedProjects) {
            addStyledCell(table, p.getName(), COLOR_DARK_BG, headerFont, Element.ALIGN_CENTER);
        }
        addStyledCell(table, t(lang, "total"), COLOR_DARK_BG, headerFont, Element.ALIGN_CENTER);

        final Map<String, Double> hoursMap = new HashMap<>();
        for (final TimeEntry e : entries) {
            if (e.getProject() == null) {
                continue;
            }
            final String key = e.getDeveloper().getId() + "_" + e.getProject().getId();
            hoursMap.merge(key, durationHours(e), Double::sum);
        }

        final Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COLOR_DARK_BG);
        final Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.BOLD, COLOR_DARK_BG);

        int row = 0;
        final double[] projectTotals = new double[involvedProjects.size()];
        for (final Long devId : devIds) {
            final Color bg = (row++ % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT;
            String devName = devMap.get(devId).getDisplayUsername();
            if (devName == null) {
                devName = devMap.get(devId).getUsername();
            }
            addStyledCell(table, devName, bg, boldFont, Element.ALIGN_LEFT);
            double devTotal = 0;
            for (int i = 0; i < involvedProjects.size(); i++) {
                final double h = hoursMap.getOrDefault(devId + "_" + involvedProjects.get(i).getId(), 0.0);
                devTotal += h;
                projectTotals[i] += h;
                addStyledCell(table, h > 0 ? formatHours(h) : "-", bg, cellFont, Element.ALIGN_CENTER);
            }
            addStyledCell(table, formatHours(devTotal), bg, boldFont, Element.ALIGN_CENTER);
        }

        // Total row
        addStyledCell(table, t(lang, "total"), COLOR_SECTION_BG, boldFont, Element.ALIGN_LEFT);
        double grandTotal = 0;
        for (int i = 0; i < involvedProjects.size(); i++) {
            grandTotal += projectTotals[i];
            addStyledCell(table, formatHours(projectTotals[i]), COLOR_SECTION_BG, boldFont, Element.ALIGN_CENTER);
        }
        addStyledCell(table, formatHours(grandTotal), COLOR_SECTION_BG, boldFont, Element.ALIGN_CENTER);

        return table;
    }

    private PdfPTable buildMonthlyTrendTable(final List<TimeEntry> entries, final String lang)
            throws DocumentException {
        final PdfPTable table = new PdfPTable(new float[]{2, 2});
        table.setWidthPercentage(100);
        addTableHeader(table, new String[]{t(lang, "month"), t(lang, "hours")});

        final Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;
        final Map<String, Double> monthMap = new TreeMap<>();
        final DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        final DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("MMMM yyyy", locale);

        for (final TimeEntry e : entries) {
            final String key = e.getStartTime().format(monthFmt);
            monthMap.merge(key, durationHours(e), Double::sum);
        }

        int row = 0;
        for (final Map.Entry<String, Double> entry : monthMap.entrySet()) {
            final Color bg = (row++ % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT;
            final LocalDate d = LocalDate.parse(entry.getKey() + "-01");
            addRow(table, bg, false,
                    capitalize(d.format(displayFmt)),
                    formatHours(entry.getValue()));
        }
        return table;
    }

    private PdfPTable buildClientDetailedLogTable(final List<TimeEntry> entries, final String lang)
            throws DocumentException {
        final PdfPTable table = new PdfPTable(new float[]{2, 2, 2.5f, 1.5f, 3});
        table.setWidthPercentage(100);
        addTableHeader(table, new String[]{
                t(lang, "date"), t(lang, "project"),
                t(lang, "developer"), t(lang, "duration"), t(lang, "description")
        });

        final Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;
        final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", locale);

        final List<TimeEntry> sorted = entries.stream()
                .sorted(Comparator.comparing(TimeEntry::getStartTime))
                .toList();

        int row = 0;
        for (final TimeEntry e : sorted) {
            final boolean isLeave = e.getType() == TimeEntryType.LEAVE;
            final Color bg = isLeave ? COLOR_LEAVE_BG : ((row % 2 == 0) ? COLOR_WHITE : COLOR_ROW_ALT);
            final String projectName = e.getProject() != null ? e.getProject().getName() : "-";
            final String devName = e.getDeveloper().getDisplayUsername() != null
                    ? e.getDeveloper().getDisplayUsername() : e.getDeveloper().getUsername();
            addRow(table, bg, false,
                    e.getStartTime().format(dateFmt),
                    projectName,
                    devName,
                    formatHours(durationHours(e)),
                    e.getDescription() != null ? e.getDescription() : "");
            row++;
        }
        return table;
    }

    // ── Table helpers ─────────────────────────────────────────────────────────────

    private void addTableHeader(final PdfPTable table, final String[] headers) {
        final Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, COLOR_WHITE);
        for (final String header : headers) {
            final PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(COLOR_DARK_BG);
            cell.setBorderColor(COLOR_BORDER);
            cell.setBorderWidth(0.5f);
            cell.setPaddingTop(6f);
            cell.setPaddingBottom(6f);
            cell.setPaddingLeft(5f);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    private void addRow(final PdfPTable table, final Color bg, final boolean rightAlignLast, final String... values) {
        final Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COLOR_DARK_BG);
        for (int i = 0; i < values.length; i++) {
            final PdfPCell cell = new PdfPCell(new Phrase(values[i], font));
            cell.setBackgroundColor(bg);
            cell.setBorderColor(COLOR_BORDER);
            cell.setBorderWidth(0.5f);
            cell.setPaddingTop(4f);
            cell.setPaddingBottom(4f);
            cell.setPaddingLeft(5f);
            final boolean isLast = (i == values.length - 1);
            cell.setHorizontalAlignment(rightAlignLast && isLast ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    private void addStyledCell(final PdfPTable table, final String text, final Color bg, final Font font, final int align) {
        final PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(5f);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────────

    private double sumHours(final List<TimeEntry> entries) {
        return entries.stream().mapToDouble(this::durationHours).sum();
    }

    private double durationHours(final TimeEntry e) {
        return java.time.Duration.between(e.getStartTime(), e.getEndTime()).toMinutes() / 60.0;
    }

    private String formatHours(final double hours) {
        final int h = (int) hours;
        final int m = (int) ((hours % 1) * 60);
        return String.format("%dh %02dm", h, m);
    }

    private String formatPeriod(final LocalDate start, final LocalDate end, final String lang) {
        if (start == null && end == null) {
            return "";
        }
        final Locale locale = "fr".equals(lang) ? Locale.FRENCH : Locale.ENGLISH;
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", locale);
        if (start != null && end != null) {
            return start.format(fmt) + " – " + end.format(fmt);
        }
        if (start != null) {
            return start.format(fmt) + " →";
        }
        return "→ " + end.format(fmt);
    }

    private Color heatmapColor(final double value, final double max) {
        if (max == 0) {
            return COLOR_WHITE;
        }
        final double ratio = Math.min(value / max, 1.0);
        final int r = (int) (255 - ratio * (255 - 0x2E));
        final int g = (int) (255 - ratio * (255 - 0x75));
        final int b = (int) (255 - ratio * (255 - 0xB6));
        return new Color(Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b)));
    }

    private String capitalize(final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── Page event: header + footer ───────────────────────────────────────────────

    private class HeaderFooterEvent extends PdfPageEventHelper {

        private final String subject;
        private final String period;
        private final String lang;
        private PdfTemplate total;
        private BaseFont baseFont;

        HeaderFooterEvent(final String subject, final String period, final String lang) {
            this.subject = subject;
            this.period = period;
            this.lang = lang;
            try {
                this.baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onOpenDocument(final PdfWriter writer, final Document document) {
            total = writer.getDirectContent().createTemplate(30, 16);
        }

        @Override
        public void onEndPage(final PdfWriter writer, final Document document) {
            final PdfContentByte cb = writer.getDirectContent();
            final float pageWidth = document.getPageSize().getWidth();
            final float marginLeft = document.leftMargin();
            final float marginRight = document.rightMargin();
            final float usableWidth = pageWidth - marginLeft - marginRight;

            // Header band
            final float headerTop = document.top() + 30f;
            final float headerHeight = 28f;
            cb.setColorFill(COLOR_DARK_BG);
            cb.rectangle(marginLeft, headerTop - headerHeight, usableWidth, headerHeight);
            cb.fill();

            cb.beginText();
            cb.setFontAndSize(baseFont, 11);
            cb.setColorFill(COLOR_WHITE);
            cb.setTextMatrix(marginLeft + 8, headerTop - headerHeight + 8);
            cb.showText(subject);

            if (period != null && !period.isEmpty()) {
                final float periodWidth = baseFont.getWidthPoint(period, 9);
                cb.setFontAndSize(baseFont, 9);
                cb.setTextMatrix(marginLeft + usableWidth - periodWidth - 4, headerTop - headerHeight + 10);
                cb.showText(period);
            }
            cb.endText();

            // Footer
            final float footerY = document.bottom() - 20f;
            final String generatedLabel = t(lang, "generatedOn") + " " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            final String pageLabel = "Page " + writer.getPageNumber() + " / ";

            cb.setColorFill(COLOR_FOOTER);

            cb.beginText();
            cb.setFontAndSize(baseFont, 8);
            cb.setTextMatrix(marginLeft, footerY);
            cb.showText(generatedLabel);
            cb.endText();

            cb.beginText();
            cb.setFontAndSize(baseFont, 8);
            final float pageNumWidth = baseFont.getWidthPoint(pageLabel, 8);
            cb.setTextMatrix(marginLeft + usableWidth - pageNumWidth - 30, footerY);
            cb.showText(pageLabel);
            cb.endText();

            // Template placeholder for total page count
            cb.addTemplate(total, marginLeft + usableWidth - 28, footerY);
        }

        @Override
        public void onCloseDocument(final PdfWriter writer, final Document document) {
            total.beginText();
            total.setFontAndSize(baseFont, 8);
            total.setColorFill(COLOR_FOOTER);
            total.setTextMatrix(0, 0);
            total.showText(String.valueOf(writer.getPageNumber() - 1));
            total.endText();
        }
    }
}
