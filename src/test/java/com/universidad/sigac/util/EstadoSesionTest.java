package com.universidad.sigac.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.universidad.sigac.enums.EstadoSesion;
import org.junit.jupiter.api.Test;

class EstadoSesionTest {

    @Test
    void flujoNormal() {
        assertTrue(EstadoSesion.PROGRAMADA.puedePasarA(EstadoSesion.CONVOCADA));
        assertTrue(EstadoSesion.CONVOCADA.puedePasarA(EstadoSesion.INICIADA));
        assertTrue(EstadoSesion.INICIADA.puedePasarA(EstadoSesion.SUSPENDIDA));
        assertTrue(EstadoSesion.SUSPENDIDA.puedePasarA(EstadoSesion.INICIADA));
        assertTrue(EstadoSesion.INICIADA.puedePasarA(EstadoSesion.FINALIZADA));
    }

    @Test
    void transicionesInvalidas() {
        assertFalse(EstadoSesion.PROGRAMADA.puedePasarA(EstadoSesion.INICIADA));
        assertFalse(EstadoSesion.FINALIZADA.puedePasarA(EstadoSesion.INICIADA));
        assertFalse(EstadoSesion.CONVOCADA.puedePasarA(EstadoSesion.FINALIZADA));
    }
}
