package org.viajedragos.modelo;

public enum TipoPocion {
    PEQUENA("Poción pequeña", 8),
    GRANDE("Poción grande", 16);

    private final String nombre;
    private final int recuperacionPS;

    TipoPocion(String nombre, int recuperacionPS) {
        this.nombre = nombre;
        this.recuperacionPS = recuperacionPS;
    }

    public String getNombre() {
        return nombre;
    }

    // Dato para la futura utilización; obtener una poción no recupera vida.
    public int getRecuperacionPS() {
        return recuperacionPS;
    }
}
