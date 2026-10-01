package com.universidad.sigac.enums;

public enum ResultadoDecision {
    APROBADO("Aprobado"), NEGADO("Negado"), APLAZADO("Aplazado"), CANCELADO("Cancelado");

    private final String etiqueta;

    ResultadoDecision(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
