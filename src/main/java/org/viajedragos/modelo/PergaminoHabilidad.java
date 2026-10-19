package org.viajedragos.modelo;

import java.util.Objects;
import org.viajedragos.habilidad.HabilidadDisponible;

/** El contenido queda fijado al obtener el pergamino, no al utilizarlo. */
public record PergaminoHabilidad(HabilidadDisponible habilidad) {
    public PergaminoHabilidad { Objects.requireNonNull(habilidad); }
    public String nombre() { return "Pergamino de habilidad: " + habilidad.getNombre(); }
}
