package com.universidad.sigac.enums;

/** Ciclo de vida de una sesión de Consejo. */
public enum EstadoSesion {
    PROGRAMADA,   // se está armando el Orden del Día
    CONVOCADA,    // agenda cerrada y citación enviada
    INICIADA,     // sesión en curso (hay quórum)
    SUSPENDIDA,
    FINALIZADA;

    public boolean puedePasarA(EstadoSesion destino) {
        return switch (this) {
            case PROGRAMADA -> destino == CONVOCADA;
            case CONVOCADA -> destino == INICIADA;
            case INICIADA -> destino == SUSPENDIDA || destino == FINALIZADA;
            case SUSPENDIDA -> destino == INICIADA || destino == FINALIZADA;
            case FINALIZADA -> false;
        };
    }
}
