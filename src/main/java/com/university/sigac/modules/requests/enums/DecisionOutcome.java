package com.university.sigac.modules.requests.enums;

public enum DecisionOutcome {
    APPROVED("Approved"), DENIED("Denied"), POSTPONED("Postponed"), CANCELLED("Cancelled");

    private final String label;

    DecisionOutcome(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
