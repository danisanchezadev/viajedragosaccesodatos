package org.viajedragos.modelo;

import org.viajedragos.arma.Atributo;
import java.util.Locale;
import java.util.Objects;

/** Objeto generado por el juego, ajeno a los catálogos de repositorios. */
public record Pergamino(Atributo atributo, int cantidad) {
    public Pergamino {
        Objects.requireNonNull(atributo);
        if (cantidad != 1) throw new IllegalArgumentException("El pergamino mejora exactamente un punto");
    }
    public Pergamino(Atributo atributo) { this(atributo, 1); }
    public String nombre() {
        return "Pergamino de " + (atributo == Atributo.VIDA ? "vida máxima" : atributo.name().toLowerCase(Locale.ROOT));
    }
}
