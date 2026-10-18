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
            if (atributo == Atributo.VIDA) {
                throw new IllegalArgumentException("Las bonificaciones de vida necesitan una regla específica");
            }
            copia.put(atributo, cantidad);
        });
        bonificaciones = Collections.unmodifiableMap(copia);
    }

    public Arma crearArma() {
        return new Arma(nombre, TipoEquipo.ARMA, bonificaciones, Map.of());
    }
}
