package org.viajedragos.consola;

import java.util.Locale;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Duración de una aventura; no acumula lecturas ni guarda datos entre aventuras. */
public final class TiempoAventura {
    private final LongSupplier reloj;
    private final long inicio;

    public TiempoAventura() { this(System::nanoTime); }

    public TiempoAventura(LongSupplier reloj) {
        this.reloj = Objects.requireNonNull(reloj);
        inicio = reloj.getAsLong();
    }

    public String transcurrido() {
        // La resta admite el salto de signo de nanoTime para duraciones inferiores a 292 años.
        long segundos = Math.max(0, reloj.getAsLong() - inicio) / 1_000_000_000L;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", segundos / 3600, segundos / 60 % 60, segundos % 60);
    }
}
