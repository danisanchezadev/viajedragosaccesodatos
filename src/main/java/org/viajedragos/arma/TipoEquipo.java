package org.viajedragos.arma;

public enum TipoEquipo {

    ARMA("Arma"),
    ARMADURA("Armadura"),
    CASCO("Casco"),
    BOTAS("Botas"),
    GUANTES("Guantes"),
    ANILLO("Anillo"),
    AMULETO("Amuleto");

    private String nombre;

    TipoEquipo(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
