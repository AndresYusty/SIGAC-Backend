package com.university.sigac.modules.requests.enums;

import java.util.List;

/**
 * Topic inside a council branch. Flags describe which parts of MI-FOR-028 the topic uses
 * and which supporting documents are mandatory.
 */
public enum RequestType {
    PETITION(RequestBranch.STUDENT, List.of("PETITION"), false, false, false, false),
    COMPLAINT(RequestBranch.STUDENT, List.of("COMPLAINT"), false, false, false, false),
    CLAIM(RequestBranch.STUDENT, List.of("CLAIM"), false, false, false, false),
    REENTRY(RequestBranch.STUDENT, List.of("REENTRY_LETTER", "TRANSCRIPT"), false, false, false, false),
    CANCELLATION(RequestBranch.STUDENT, List.of("CANCELLATION_SUPPORT"), false, false, false, true),
    GRADE_RECTIFICATION(RequestBranch.STUDENT, List.of("GRADE_SUPPORT"), true, true, false, false),
    SPECIAL_CASE(RequestBranch.STUDENT, List.of("CASE_SUPPORT"), false, false, true, false),
    EVALUATION(RequestBranch.TEACHING, List.of("EVALUATION_SUPPORT"), false, false, false, false),
    PERMISSION(RequestBranch.TEACHING, List.of("PERMISSION_SUPPORT"), false, false, false, false),
    COMMISSION(RequestBranch.TEACHING, List.of("COMMISSION_SUPPORT"), false, false, false, false),
    CONTEST(RequestBranch.TEACHING, List.of("CONTEST_SUPPORT"), false, false, false, false),
    STUDY_PLAN(RequestBranch.CURRICULUM, List.of("STUDY_PLAN"), false, false, false, false),
    SUBJECT(RequestBranch.CURRICULUM, List.of("SUBJECT_PROPOSAL"), false, false, false, false),
    PROJECT_ENDORSEMENT(RequestBranch.RESEARCH, List.of("PROJECT"), false, false, false, false),
    CALL(RequestBranch.RESEARCH, List.of("CALL_TERMS"), false, false, false, false),
    RESOURCES(RequestBranch.RESEARCH, List.of("BUDGET"), false, false, false, false),
    SEEDBED(RequestBranch.RESEARCH, List.of("SEEDBED_PROPOSAL"), false, false, false, false),
    SOCIAL_PROJECT(RequestBranch.SOCIAL, List.of("SOCIAL_PROJECT"), false, false, false, false),
    SOCIAL_PRACTICE(RequestBranch.SOCIAL, List.of("SOCIAL_PRACTICE"), false, false, false, false),
    EXTENSION_AGREEMENT(RequestBranch.SOCIAL, List.of("EXTENSION_AGREEMENT"), false, false, false, false),
    SOCIAL_ACTIVITY(RequestBranch.SOCIAL, List.of("SOCIAL_ACTIVITY"), false, false, false, false);

    private final RequestBranch branch;
    private final List<String> documents;
    private final boolean courses;
    private final boolean grades;
    private final boolean otherDetail;
    private final boolean cancellationChoice;

    RequestType(RequestBranch branch, List<String> documents, boolean courses, boolean grades,
                 boolean otherDetail, boolean cancellationChoice) {
        this.branch = branch;
        this.documents = documents;
        this.courses = courses;
        this.grades = grades;
        this.otherDetail = otherDetail;
        this.cancellationChoice = cancellationChoice;
    }

    public RequestBranch branch() {
        return branch;
    }

    public List<String> documents() {
        return documents;
    }

    /** Course rows of MI-FOR-028 are always shown (grade rectification). */
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

    /** Cancellation asks whether it is a course or the academic period. */
    public boolean cancellationChoice() {
        return cancellationChoice;
    }

    public boolean requiresCourses(CancellationKind kind) {
        return courses || (cancellationChoice && kind == CancellationKind.COURSE);
    }
}
