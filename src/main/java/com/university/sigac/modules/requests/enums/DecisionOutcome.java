package com.university.sigac.modules.requests.enums;

public enum DecisionOutcome {
    APPROVED("Aprobado"), DENIED("Negado"), POSTPONED("Aplazado"), CANCELLED("Cancelado");

    private final String label;

    DecisionOutcome(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
