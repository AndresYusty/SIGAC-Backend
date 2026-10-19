package com.university.sigac.modules.agreements.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class AgreementDtos {

    private AgreementDtos() {
    }

    /**
     * A piece of the recitals or the articles. {@code type}: paragraph, article (numbered "ARTÍCULO n°.")
     * or table. Texts accept {{field}} placeholders and **bold**.
     */
    public record Block(@NotBlank @Size(max = 20) String type, @Size(max = 20000) String text,
                        List<String> columns, List<List<String>> rows) {
    }

    /** Variable the secretary fills in; it replaces {{key}} in the texts. */
    public record FieldDto(String key, String label, String labelEn, String example) {
    }

    public record AgreementTypeDto(String code, String name, String nameEn, String subject, String legalBasis,
                                   String closing, List<FieldDto> fields, List<Block> recitals, List<Block> articles) {
    }

    /** What is stored in the content column. */
    public record AgreementContent(Map<String, String> values, List<Block> recitals, List<Block> articles) {
    }

    public record AgreementRequest(Long facultyId,
                                   @NotBlank @Size(max = 60) String typeCode,
                                   @Min(1) Integer number,
                                   @NotNull LocalDate issueDate,
                                   @NotBlank @Size(max = 500) String subject,
                                   @NotBlank @Size(max = 200) String councilTitle,
                                   @Size(max = 500) String legalBasis,
                                   @Size(max = 120) String closing,
                                   @Size(max = 150) String presidentName,
                                   @Size(max = 150) String secretaryName,
                                   Map<String, String> values,
                                   @NotNull @Valid List<Block> recitals,
                                   @NotNull @Valid List<Block> articles) {
    }

    public record AgreementDto(Long id, String label, int number, int year, String typeCode, String typeName,
                               Long facultyId, String faculty, LocalDate issueDate, String subject,
                               String councilTitle, String legalBasis, String closing, String presidentName,
                               String secretaryName, Map<String, String> values, List<Block> recitals,
                               List<Block> articles, String createdBy, Instant createdAt, Instant updatedAt) {
    }

    public record AgreementSummaryDto(Long id, String label, int number, int year, String typeCode, String typeName,
                                      String faculty, LocalDate issueDate, String subject, String createdBy) {
    }

    /** Values proposed for a new agreement of the Faculty. */
    public record AgreementDefaultsDto(Long facultyId, String faculty, int year, int nextNumber, String councilTitle,
                                       String city, String presidentName, String secretaryName) {
    }
}
