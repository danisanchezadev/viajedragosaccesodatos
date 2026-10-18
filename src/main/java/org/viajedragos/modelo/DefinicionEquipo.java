package org.viajedragos.modelo;

import org.viajedragos.arma.*;
import java.util.*;

/** Datos inmutables; cada creación produce un ejemplar independiente. VIDA significa vida máxima. */
public record DefinicionEquipo(String id, String nombre, TipoEquipo tipo, Map<Atributo, Integer> bonificaciones) {
    public DefinicionEquipo {
        Objects.requireNonNull(id);
        Objects.requireNonNull(nombre);
        Objects.requireNonNull(tipo);
        if (id.isBlank() || nombre.isBlank()) throw new IllegalArgumentException("ID y nombre obligatorios");
        var copia = new EnumMap<Atributo, Integer>(Atributo.class);
        Objects.requireNonNull(bonificaciones).forEach((a, n) -> {
            Objects.requireNonNull(a);
            Objects.requireNonNull(n);
            if (n == Integer.MIN_VALUE) throw new IllegalArgumentException("Bonificación no reversible");
            copia.put(a, n);
        });
        bonificaciones = Collections.unmodifiableMap(copia);
    }

    public Equipo crearEquipo() {
        return tipo == TipoEquipo.ARMA ? new Arma(id, nombre, tipo, bonificaciones, Map.of())
                : new Equipo(id, nombre, tipo, bonificaciones, Map.of());
    }
}
