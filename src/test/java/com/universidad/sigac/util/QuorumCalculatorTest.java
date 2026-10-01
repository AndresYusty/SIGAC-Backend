package com.universidad.sigac.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.universidad.sigac.util.QuorumCalculator;
import org.junit.jupiter.api.Test;

class QuorumCalculatorTest {

    @Test
    void cincuentaPorCientoMasUno() {
        assertEquals(6, QuorumCalculator.requeridos(10, 50));
        assertEquals(5, QuorumCalculator.requeridos(9, 50));
        assertEquals(3, QuorumCalculator.requeridos(4, 50));
    }

    @Test
    void nuncaExigeMasQueElTotal() {
        assertEquals(1, QuorumCalculator.requeridos(1, 50));
        assertEquals(3, QuorumCalculator.requeridos(3, 100));
    }

    @Test
    void sinIntegrantesNoHayQuorum() {
        assertEquals(0, QuorumCalculator.requeridos(0, 50));
        assertFalse(QuorumCalculator.alcanzado(0, 0, 50));
    }

    @Test
    void alcanzadoSegunAsistencia() {
        assertTrue(QuorumCalculator.alcanzado(10, 6, 50));
        assertFalse(QuorumCalculator.alcanzado(10, 5, 50));
    }
}
