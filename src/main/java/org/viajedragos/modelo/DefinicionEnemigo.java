package org.viajedragos.modelo;

import java.util.Objects;

public record DefinicionEnemigo(String id, String nombre, String descripcion,
                               int fuerza, int resistencia, int iniciativa,
                               int agilidad, int velocidad, int vidaMaxima) {
    public DefinicionEnemigo {
        Objects.requireNonNull(id, "El identificador es obligatorio");
        Objects.requireNonNull(nombre, "El nombre es obligatorio");
        Objects.requireNonNull(descripcion, "La descripción es obligatoria");
        if (id.isBlank() || nombre.isBlank()) {
            throw new IllegalArgumentException("El identificador y el nombre no pueden estar vacíos");
        }
        if (fuerza < 0 || resistencia < 0 || iniciativa < 0 || agilidad < 0 || velocidad < 0 || vidaMaxima <= 0) {
            throw new IllegalArgumentException("Los atributos deben ser no negativos y la vida máxima positiva");
        }
    }

    public Enemigo crearEnemigo() {
        return new Enemigo(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad,
                velocidad, vidaMaxima, vidaMaxima);
    }
}
