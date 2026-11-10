package com.university.sigac.modules.agreements.service;

import com.university.sigac.core.util.DateUtil;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementDefaultsDto;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementRequest;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementTypeDto;
import com.university.sigac.modules.agreements.dto.AgreementDtos.Block;
import com.university.sigac.modules.agreements.enums.DocumentKind;
import com.university.sigac.modules.agreements.util.AgreementTypeCatalog;
import com.university.sigac.modules.requests.entity.AgendaItem;
import com.university.sigac.modules.requests.entity.CouncilRequest;
import com.university.sigac.modules.requests.entity.RequestCourse;
import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.enums.DecisionOutcome;
import com.university.sigac.modules.requests.enums.RequestType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds draft agreements and memorandums from decided agenda items when the minutes of a session are compiled.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementDraftService {

    private static final Pattern FACULTY_PREFIX = Pattern.compile("(?i)^facultad de\\s+");
    private static final String MEMORANDUM = "MEMORANDUM";

    private final AgreementService agreements;
    private final AgreementTypeCatalog types;

    /**
     * One draft per approved or denied request: an agreement when the topic has an agreement model,
     * otherwise a memorandum. Skips agenda items that already have a draft. Returns how many were created.
     */
    @Transactional
    public int createDraftsFromSession(Session session, String minutesSerial) {
        AgreementDefaultsDto defaults = agreements.defaults(session.getFaculty().getId());
        LocalDate issueDate = session.getActualStart() != null
                ? session.getActualStart().toLocalDate()
                : session.getScheduledAt().toLocalDate();
        String sessionDate = DateUtil.longDate(issueDate);
        String facultyShort = FACULTY_PREFIX.matcher(session.getFaculty().getName().trim()).replaceFirst("");

        int created = 0;
        for (AgendaItem item : session.getItems()) {
            CouncilRequest request = item.getRequest();
            DecisionOutcome outcome = item.getOutcome();
            if (request == null || (outcome != DecisionOutcome.APPROVED && outcome != DecisionOutcome.DENIED)) {
                continue;
            }
            if (agreements.hasDraftForAgendaItem(item.getId())) {
                continue;
            }
            String typeCode = resolveTypeCode(request, outcome);
            if (typeCode == null) {
                agreements.create(memorandum(item, defaults, issueDate, facultyShort, sessionDate, minutesSerial),
                        item.getId());
                created++;
                log.info("Draft memorandum created from agenda item {} ({})", item.getId(), request.getCode());
                continue;
            }
            AgreementTypeDto type = types.get(typeCode);
            Map<String, String> values = seedValues(type, request, facultyShort, sessionDate, minutesSerial);
            List<Block> recitals = copyAndFillTables(type.recitals(), request, values);
            List<Block> articles = copyAndFillTables(type.articles(), request, values);
            AgreementRequest body = new AgreementRequest(
                    session.getFaculty().getId(),
                    type.code(),
                    null,
                    issueDate,
                    type.subject(),
                    defaults.councilTitle(),
                    type.legalBasis(),
                    type.closing(),
                    defaults.presidentName(),
                    defaults.secretaryName(),
                    values,
                    recitals,
                    articles);
            agreements.create(body, item.getId());
            created++;
            log.info("Draft agreement {} created from agenda item {} ({})", typeCode, item.getId(),
                    request.getCode());
        }
        return created;
    }

    /** Memorandum addressed to the requester that communicates the decision on the item. */
    private AgreementRequest memorandum(AgendaItem item, AgreementDefaultsDto defaults, LocalDate issueDate,
                                        String facultyShort, String sessionDate, String minutesSerial) {
        CouncilRequest request = item.getRequest();
        AgreementTypeDto type = types.get(MEMORANDUM);
        String title = request.getTitle() == null || request.getTitle().isBlank()
                ? item.getTitle() : request.getTitle().trim();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("facultyName", facultyShort);
        values.put("sessionDate", sessionDate);
        values.put("minutesNumber", minutesSerial);
        values.put("decision", decisionVerb(request.getRequestType(), item.getOutcome()));
        values.put("requestCode", blankToEmpty(request.getCode()));
        values.put("requestTitle", blankToEmpty(title));
        values.put("requesterName", blankToEmpty(request.getRequester()));

        List<Block> body = new ArrayList<>();
        for (Block block : type.recitals()) {
            if ("table".equals(block.type())) {
                List<String> row = List.of("1", values.get("requestCode"), values.get("requestTitle"),
                        values.get("requesterName"));
                body.add(new Block("table", null, List.copyOf(block.columns()), List.of(row)));
            } else {
                body.add(new Block(block.type(), block.text(), null, null));
            }
        }
        String note = item.getDecisionDescription();
        if (note != null && !note.isBlank()) {
            body.add(new Block("paragraph", note.trim(), null, null));
        }
        return new AgreementRequest(
                defaults.facultyId(),
                MEMORANDUM,
                null,
                issueDate,
                blankToEmpty(title).isEmpty() ? type.subject() : title,
                defaults.memoFrom(),
                blankToEmpty(request.getRequester()),
                type.closing(),
                defaults.presidentName(),
                defaults.secretaryName(),
                values,
                body,
                List.of());
    }

    private static String decisionVerb(RequestType topic, DecisionOutcome outcome) {
        boolean endorsement = topic == RequestType.PROJECT_ENDORSEMENT || topic == RequestType.SOCIAL_PROJECT;
        if (outcome == DecisionOutcome.DENIED) {
            return endorsement ? "NO AVALAR" : "NEGAR";
        }
        return endorsement ? "AVALAR" : "APROBAR";
    }

    private String resolveTypeCode(CouncilRequest request, DecisionOutcome outcome) {
        RequestType topic = request.getRequestType();
        if (outcome != DecisionOutcome.APPROVED && outcome != DecisionOutcome.DENIED) {
            return null;
        }
        return switch (topic) {
            case REENTRY -> outcome == DecisionOutcome.APPROVED ? "READMISSION" : null;
            case PERIOD_CANCELLATION -> outcome == DecisionOutcome.DENIED
                    ? "PERIOD_CANCELLATION_DENIED" : "PERIOD_CANCELLATION";
            case COURSE_CANCELLATION, CANCELLATION -> outcome == DecisionOutcome.APPROVED
                    ? "COURSE_CANCELLATION" : null;
            case GRADE_RECTIFICATION -> outcome == DecisionOutcome.APPROVED ? "GRADE_CORRECTION" : null;
            case SEEDBED -> outcome == DecisionOutcome.APPROVED ? "SEEDBED_UPDATE" : null;
            case PETITION, COMPLAINT, CLAIM, SPECIAL_CASE -> outcome == DecisionOutcome.APPROVED
                    ? matchByDetail(request.getOtherDetail()) : null;
            default -> null;
        };
    }

    private String matchByDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }
        String needle = detail.trim().toLowerCase(Locale.ROOT);
        return types.all().stream()
                .filter(t -> DocumentKind.of(t.kind()) == DocumentKind.AGREEMENT && !t.code().startsWith("BLANK_"))
                .filter(t -> t.name().equalsIgnoreCase(detail.trim())
                        || t.name().toLowerCase(Locale.ROOT).contains(needle)
                        || needle.contains(t.name().toLowerCase(Locale.ROOT)))
                .map(AgreementTypeDto::code)
                .findFirst()
                .orElse(null);
    }

    private Map<String, String> seedValues(AgreementTypeDto type, CouncilRequest request, String facultyShort,
                                           String sessionDate, String minutesSerial) {
        Map<String, String> values = new LinkedHashMap<>();
        for (var field : type.fields()) {
            values.put(field.key(), "");
        }
        put(values, "facultyName", facultyShort);
        put(values, "sessionDate", sessionDate);
        put(values, "minutesNumber", minutesSerial);
        put(values, "studentName", request.getRequester());
        put(values, "studentCode", request.getStudentCode());
        put(values, "period", request.getAcademicPeriod());
        if (request.getProgram() != null) {
            put(values, "program", request.getProgram().getName());
        }
        return values;
    }

    private static void put(Map<String, String> values, String key, String value) {
        if (values.containsKey(key) && value != null && !value.isBlank()) {
            values.put(key, value.trim());
        }
    }

    private List<Block> copyAndFillTables(List<Block> source, CouncilRequest request, Map<String, String> values) {
        List<Block> out = new ArrayList<>();
        for (Block block : source) {
            if (!"table".equals(block.type())) {
                out.add(new Block(block.type(), block.text(), null, null));
                continue;
            }
            List<String> columns = block.columns() == null ? List.of() : List.copyOf(block.columns());
            List<List<String>> rows = new ArrayList<>();
            if (block.rows() != null) {
                for (List<String> row : block.rows()) {
                    if (isSampleRow(row)) {
                        continue;
                    }
                    rows.add(new ArrayList<>(row));
                }
            }
            List<List<String>> courseRows = courseRows(request, columns, values);
            if (!courseRows.isEmpty()) {
                rows.addAll(courseRows);
            } else if (rows.isEmpty() && columns.size() > 0) {
                rows.add(blankRow(columns.size()));
            }
            out.add(new Block(block.type(), null, columns, rows));
        }
        return out;
    }

    private static List<List<String>> courseRows(CouncilRequest request, List<String> columns,
                                                 Map<String, String> values) {
        if (request.getCourses() == null || request.getCourses().isEmpty() || columns.isEmpty()) {
            return List.of();
        }
        List<List<String>> rows = new ArrayList<>();
        for (RequestCourse course : request.getCourses()) {
            List<String> row = new ArrayList<>();
            for (String column : columns) {
                row.add(cellForColumn(column, course, request, values));
            }
            rows.add(row);
        }
        return rows;
    }

    private static String cellForColumn(String column, RequestCourse course, CouncilRequest request,
                                        Map<String, String> values) {
        if (column != null && column.contains("{{")) {
            String out = column;
            for (Map.Entry<String, String> entry : values.entrySet()) {
                out = out.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
                out = out.replace("{{ " + entry.getKey() + " }}", entry.getValue() == null ? "" : entry.getValue());
            }
            return out;
        }
        String header = column == null ? "" : column.toLowerCase(Locale.ROOT);
        if (header.contains("cod") && (header.contains("est") || header.contains("estudiante"))) {
            return blankToEmpty(request.getStudentCode());
        }
        if (header.contains("estudiante") || (header.contains("nombre") && !header.contains("asignatura"))) {
            return blankToEmpty(request.getRequester());
        }
        if (header.contains("asignatura") || header.contains("materia") || header.contains("curso")) {
            return blankToEmpty(course.getCourseName());
        }
        if (header.contains("grupo")) {
            return blankToEmpty(course.getGroupCode());
        }
        if (header.contains("programa")) {
            return request.getProgram() != null ? blankToEmpty(request.getProgram().getName()) : "";
        }
        if (header.contains("cred")) {
            return values.getOrDefault("credits", "");
        }
        if (header.contains("nota") || header.contains("calif")) {
            return blankToEmpty(course.getGradeNumber());
        }
        return "";
    }

    private static boolean isSampleRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        boolean hasLiteral = false;
        for (String cell : row) {
            if (cell == null) {
                continue;
            }
            String stripped = cell.replaceAll("\\{\\{\\s*[A-Za-z0-9_]+\\s*\\}\\}", "").replaceAll("[^A-Za-z0-9]", "");
            if (!stripped.isBlank()) {
                hasLiteral = true;
            }
        }
        return hasLiteral;
    }

    private static List<String> blankRow(int width) {
        List<String> row = new ArrayList<>(width);
        for (int i = 0; i < width; i++) {
            row.add("");
        }
        return row;
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
