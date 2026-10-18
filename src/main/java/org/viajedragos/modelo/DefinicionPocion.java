package org.viajedragos.modelo;

import java.util.Objects;

public record DefinicionPocion(String id, TipoPocion tipo, String nombre, int recuperacionPS) {
    public DefinicionPocion {
        Objects.requireNonNull(id, "El identificador es obligatorio");
        Objects.requireNonNull(tipo, "El tipo de poción es obligatorio");
        Objects.requireNonNull(nombre, "El nombre es obligatorio");
        if (id.isBlank() || nombre.isBlank() || recuperacionPS <= 0) {
            throw new IllegalArgumentException("La poción necesita identificador, nombre y recuperación positiva");
        }
    }
}
