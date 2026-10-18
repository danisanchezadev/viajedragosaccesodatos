package org.viajedragos.habilidad;

import org.viajedragos.modelo.Combatiente;

public abstract class Habilidad implements EventosHabilidad {

    private final String nombre;

    protected Habilidad(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    // Limpieza del efecto propio; no representa un evento de final de combate.
    public void retirarEfectosTemporales(Combatiente portador, EstadoHabilidad estado) {
    }

    // Compatibilidad para extensiones con datos internos: las habilidades temporales
    // actuales usan EstadoHabilidad, cuya copia completa guarda Combatiente.
    public Runnable guardarEstadoTemporal() {
        return () -> {};
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
