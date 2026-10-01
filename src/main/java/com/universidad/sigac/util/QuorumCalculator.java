package com.universidad.sigac.util;

/** RN-11: quórum mínimo deliberatorio = (integrantes × porcentaje / 100) + 1, sin superar el total de integrantes. */
public final class QuorumCalculator {

    private QuorumCalculator() {
    }

    public static int requeridos(int integrantes, int porcentaje) {
        if (integrantes <= 0) {
            return 0;
        }
        int r = (integrantes * porcentaje) / 100 + 1;
        return Math.min(r, integrantes);
    }

    public static boolean alcanzado(int integrantes, int presentes, int porcentaje) {
        return integrantes > 0 && presentes >= requeridos(integrantes, porcentaje);
    }
}
