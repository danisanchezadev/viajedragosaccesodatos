package org.viajedragos.modelo;

import java.util.Objects;

/** Disponibilidad de una definición existente; no contiene atributos ni piezas de partida. */
public record DefinicionBotinEquipo(DefinicionEquipo equipo, CalidadEquipo calidad,
                                    int plantaMinima, int plantaMaxima) {
    public DefinicionBotinEquipo {
        Objects.requireNonNull(equipo);
        Objects.requireNonNull(calidad);
        Planta.porNumero(plantaMinima);
        Planta.porNumero(plantaMaxima);
        if (plantaMinima > plantaMaxima) throw new IllegalArgumentException("Rango de plantas invertido");
        int primeraPermitida = switch (calidad) { case BASICO -> 1; case MEJORADO -> 2; case SUPERIOR -> 4; };
        if (plantaMinima < primeraPermitida) throw new IllegalArgumentException("Calidad reservada para plantas posteriores");
    }

    public boolean disponibleEn(int planta) { return planta >= plantaMinima && planta <= plantaMaxima; }
}
