package org.viajedragos.modelo;

import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;

import java.util.EnumMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record DefinicionArma(String id, String nombre, Map<Atributo, Integer> bonificaciones) {
    public static final String ID_ESPADA_DESGASTADA = "espada_desgastada";
    public static final String ID_ESPADA_HIERRO = "espada_hierro";

    public DefinicionArma {
        Objects.requireNonNull(id, "El identificador es obligatorio");
        Objects.requireNonNull(nombre, "El nombre es obligatorio");
        if (id.isBlank() || nombre.isBlank()) {
            throw new IllegalArgumentException("El identificador y el nombre no pueden estar vacíos");
        }
        var copia = new EnumMap<Atributo, Integer>(Atributo.class);
        Objects.requireNonNull(bonificaciones).forEach((atributo, cantidad) -> {
            Objects.requireNonNull(atributo);
            Objects.requireNonNull(cantidad);
            if (cantidad == Integer.MIN_VALUE) throw new IllegalArgumentException("Bonificación no reversible");
            copia.put(atributo, cantidad);
        });
        bonificaciones = Collections.unmodifiableMap(copia);
    }

    public Arma crearArma() {
        return new Arma(id, nombre, TipoEquipo.ARMA, bonificaciones, Map.of());
    }

    public Arma crearArma(int mejoras) {
        return new Arma(id, nombre, TipoEquipo.ARMA, bonificaciones, Map.of(), mejoras);
    }

    public Arma crearArma(java.util.Random azar) { return crearArma(Objects.requireNonNull(azar).nextInt(4)); }

    public DefinicionEquipo comoEquipo() { return new DefinicionEquipo(id, nombre, TipoEquipo.ARMA, bonificaciones); }
}
