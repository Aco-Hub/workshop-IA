package com.timesheet.service;

import com.timesheet.model.Client;
import com.timesheet.model.Developer;
import com.timesheet.model.Project;
import com.timesheet.model.TimeEntry;
import com.timesheet.repository.ClientRepository;
import com.timesheet.repository.DeveloperRepository;
import com.timesheet.repository.ProjectRepository;
import com.timesheet.repository.TimeEntryRepository;
import com.timesheet.service.export.ExcelExportService;
import com.timesheet.service.export.PdfExportService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportService {

    private final DeveloperRepository developerRepository;
    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

    public byte[] exportDeveloper(final Long id, final String format, final LocalDate startDate,
                                  final LocalDate endDate, final String lang) {
        final Developer developer = developerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Developer not found with id: " + id));

        final LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        final LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.of(23, 59, 59)) : null;

        final List<TimeEntry> entries = timeEntryRepository.findWithFilters(id, null, startDateTime, endDateTime);

        return switch (format.toLowerCase()) {
            case "pdf" -> pdfExportService.generateDeveloperReport(developer, entries, startDate, endDate, lang);
            case "excel" -> excelExportService.generateDeveloperReport(developer, entries, startDate, endDate, lang);
            default -> throw new IllegalArgumentException("Unsupported export format: " + format + ". Use 'pdf' or 'excel'.");
        };
    }

    public byte[] exportProject(final Long id, final String format, final LocalDate startDate,
                                final LocalDate endDate, final String lang) {
        final Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));

        final LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        final LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.of(23, 59, 59)) : null;

        final List<TimeEntry> entries = timeEntryRepository.findWithFilters(null, id, startDateTime, endDateTime);

        return switch (format.toLowerCase()) {
            case "pdf" -> pdfExportService.generateProjectReport(project, entries, startDate, endDate, lang);
            case "excel" -> excelExportService.generateProjectReport(project, entries, startDate, endDate, lang);
            default -> throw new IllegalArgumentException("Unsupported export format: " + format + ". Use 'pdf' or 'excel'.");
        };
    }

    public byte[] exportClient(final Long id, final String format, final LocalDate startDate,
                               final LocalDate endDate, final String lang) {
        final Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + id));

        final List<Project> projects = projectRepository.findAll().stream()
                .filter(p -> p.getClient() != null && p.getClient().getId().equals(id))
                .toList();

        final LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        final LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.of(23, 59, 59)) : null;

        final Set<Long> seenIds = new HashSet<>();
        final List<TimeEntry> allEntries = new ArrayList<>();
        for (final Project project : projects) {
            final List<TimeEntry> projectEntries = timeEntryRepository.findWithFilters(null, project.getId(), startDateTime, endDateTime);
            for (final TimeEntry entry : projectEntries) {
                if (seenIds.add(entry.getId())) {
                    allEntries.add(entry);
                }
            }
        }

        return switch (format.toLowerCase()) {
            case "pdf" -> pdfExportService.generateClientReport(client, projects, allEntries, startDate, endDate, lang);
            case "excel" -> excelExportService.generateClientReport(client, projects, allEntries, startDate, endDate, lang);
            default -> throw new IllegalArgumentException("Unsupported export format: " + format + ". Use 'pdf' or 'excel'.");
        };
    }
}
