package org.viajedragos.habilidad;

public abstract class Habilidad implements EventosHabilidad {

    private final String nombre;

    protected Habilidad(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        return getClass() == obj.getClass();
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}