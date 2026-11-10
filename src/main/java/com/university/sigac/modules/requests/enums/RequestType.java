package com.university.sigac.modules.requests.enums;

import java.util.List;

/**
 * Topic inside a council branch. Flags describe which parts of MI-FOR-028 the topic uses
 * and which supporting documents are mandatory.
 * Course vs period cancellation are separate topics (not a secondary choice).
 */
public enum RequestType {
    // Student topics: PQRS → academic procedures → other (catalog order).
    PETITION(RequestBranch.STUDENT, List.of("PETITION"), false, false, true),
    COMPLAINT(RequestBranch.STUDENT, List.of("COMPLAINT"), false, false, true),
    CLAIM(RequestBranch.STUDENT, List.of("CLAIM"), false, false, true),
    REENTRY(RequestBranch.STUDENT, List.of("REENTRY_LETTER", "TRANSCRIPT"), false, false, false),
    GRADE_RECTIFICATION(RequestBranch.STUDENT, List.of("GRADE_SUPPORT"), true, true, false),
    COURSE_CANCELLATION(RequestBranch.STUDENT, List.of("CANCELLATION_SUPPORT"), true, false, false),
    PERIOD_CANCELLATION(RequestBranch.STUDENT, List.of("CANCELLATION_SUPPORT"), false, false, false),
    SPECIAL_CASE(RequestBranch.STUDENT, List.of("CASE_SUPPORT"), false, false, true),
    /** Kept so older rows still load; no longer offered in the catalog. */
    CANCELLATION(RequestBranch.STUDENT, List.of("CANCELLATION_SUPPORT"), false, false, false),
    EVALUATION(RequestBranch.TEACHING, List.of("EVALUATION_SUPPORT"), false, false, false),
    PERMISSION(RequestBranch.TEACHING, List.of("PERMISSION_SUPPORT"), false, false, false),
    COMMISSION(RequestBranch.TEACHING, List.of("COMMISSION_SUPPORT"), false, false, false),
    CONTEST(RequestBranch.TEACHING, List.of("CONTEST_SUPPORT"), false, false, false),
    STUDY_PLAN(RequestBranch.CURRICULUM, List.of("STUDY_PLAN"), false, false, false),
    SUBJECT(RequestBranch.CURRICULUM, List.of("SUBJECT_PROPOSAL"), false, false, false),
    PROJECT_ENDORSEMENT(RequestBranch.RESEARCH, List.of("PROJECT"), false, false, false),
    CALL(RequestBranch.RESEARCH, List.of("CALL_TERMS"), false, false, false),
    RESOURCES(RequestBranch.RESEARCH, List.of("BUDGET"), false, false, false),
    SEEDBED(RequestBranch.RESEARCH, List.of("SEEDBED_PROPOSAL"), false, false, false),
    SOCIAL_PROJECT(RequestBranch.SOCIAL, List.of("SOCIAL_PROJECT"), false, false, false),
    SOCIAL_PRACTICE(RequestBranch.SOCIAL, List.of("SOCIAL_PRACTICE"), false, false, false),
    EXTENSION_AGREEMENT(RequestBranch.SOCIAL, List.of("EXTENSION_AGREEMENT"), false, false, false),
    SOCIAL_ACTIVITY(RequestBranch.SOCIAL, List.of("SOCIAL_ACTIVITY"), false, false, false);

    private final RequestBranch branch;
    private final List<String> documents;
    private final boolean courses;
    private final boolean grades;
    private final boolean otherDetail;

    RequestType(RequestBranch branch, List<String> documents, boolean courses, boolean grades, boolean otherDetail) {
        this.branch = branch;
        this.documents = documents;
        this.courses = courses;
        this.grades = grades;
        this.otherDetail = otherDetail;
    }

    public RequestBranch branch() {
        return branch;
    }

    public List<String> documents() {
        return documents;
    }

    /** Course rows of MI-FOR-028 are shown (addition, course cancellation, grade rectification). */
    public boolean courses() {
        return courses;
    }

    /** Grade and attendance columns, reserved for the teacher on the official form. */
    public boolean grades() {
        return grades;
    }

    /** Free-text "which?" used by a special case. */
    public boolean otherDetail() {
        return otherDetail;
    }

    /** Topics still offered when filing a request. */
    public boolean inCatalog() {
        return this != CANCELLATION;
    }

    /** Stored on the request for detail views; derived from the topic itself. */
    public CancellationKind cancellationKind() {
        return switch (this) {
            case COURSE_CANCELLATION, CANCELLATION -> CancellationKind.COURSE;
            case PERIOD_CANCELLATION -> CancellationKind.PERIOD;
            default -> null;
        };
    }

    public boolean requiresCourses(CancellationKind kind) {
        if (courses) {
            return true;
        }
        return this == CANCELLATION && kind == CancellationKind.COURSE;
    }
}
