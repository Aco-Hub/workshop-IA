package com.timesheet.controller;

import com.timesheet.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/developer/{id}")
    public ResponseEntity<byte[]> exportDeveloper(
            @PathVariable final Long id,
            @RequestParam(defaultValue = "pdf") final String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate,
            @RequestParam(defaultValue = "fr") final String lang) {

        final byte[] data = exportService.exportDeveloper(id, format, startDate, endDate, lang);
        final String filename = buildFilename("developer", id, format, startDate, endDate);
        return buildResponse(data, format, filename);
    }

    @GetMapping("/project/{id}")
    public ResponseEntity<byte[]> exportProject(
            @PathVariable final Long id,
            @RequestParam(defaultValue = "pdf") final String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate,
            @RequestParam(defaultValue = "fr") final String lang) {

        final byte[] data = exportService.exportProject(id, format, startDate, endDate, lang);
        final String filename = buildFilename("project", id, format, startDate, endDate);
        return buildResponse(data, format, filename);
    }

    @GetMapping("/client/{id}")
    public ResponseEntity<byte[]> exportClient(
            @PathVariable final Long id,
            @RequestParam(defaultValue = "pdf") final String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate,
            @RequestParam(defaultValue = "fr") final String lang) {

        final byte[] data = exportService.exportClient(id, format, startDate, endDate, lang);
        final String filename = buildFilename("client", id, format, startDate, endDate);
        return buildResponse(data, format, filename);
    }

    private ResponseEntity<byte[]> buildResponse(final byte[] data, final String format, final String filename) {
        final MediaType mediaType = "excel".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.APPLICATION_PDF;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data);
    }

    private String buildFilename(final String entityType, final Long id, final String format,
                                 final LocalDate startDate, final LocalDate endDate) {
        final StringBuilder sb = new StringBuilder("timesheet_").append(entityType).append("_").append(id);
        if (startDate != null) {
            sb.append("_").append(startDate);
        }
        if (endDate != null) {
            sb.append("_").append(endDate);
        }
        final String ext = "excel".equalsIgnoreCase(format) ? ".xlsx" : ".pdf";
        sb.append(ext);
        return sb.toString();
    }
}
