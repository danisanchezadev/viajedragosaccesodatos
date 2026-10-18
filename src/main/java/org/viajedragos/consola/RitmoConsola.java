package org.viajedragos.consola;

import java.io.PrintStream;
import java.util.Objects;

/** Ritmo secuencial de una sesión; nunca dibuja ni modifica los paneles. */
public final class RitmoConsola {
    public enum Velocidad {
        INSTANTANEA("Instantánea", 0), RAPIDA("Rápida", 250), NORMAL("Normal", 700), LENTA("Lenta", 1200);

        private final String nombre;
        private final long milisegundos;

        Velocidad(String nombre, long milisegundos) {
            this.nombre = nombre;
            this.milisegundos = milisegundos;
        }

        public String getNombre() { return nombre; }
        public long getMilisegundos() { return milisegundos; }
    }

    @FunctionalInterface
    public interface Espera {
        void esperar(long milisegundos) throws InterruptedException;
    }

    private Velocidad velocidad;
    private final Espera espera;

    public RitmoConsola(Velocidad velocidad) {
        this(velocidad, Thread::sleep);
    }

    // Las pruebas proporcionan una espera que solo registra las llamadas, sin dormir.
    public RitmoConsola(Velocidad velocidad, Espera espera) {
        this.velocidad = Objects.requireNonNull(velocidad);
        this.espera = Objects.requireNonNull(espera);
    }

    public Velocidad getVelocidad() { return velocidad; }
    public void setVelocidad(Velocidad velocidad) { this.velocidad = Objects.requireNonNull(velocidad); }

    public void pausar(PrintStream salida) {
        if (velocidad.getMilisegundos() == 0 || Thread.currentThread().isInterrupted()) return;
        salida.flush();
        try {
            espera.esperar(velocidad.getMilisegundos());
        } catch (InterruptedException interrumpida) {
            Thread.currentThread().interrupt();
            salida.println("Pausa interrumpida. Se continúa sin esperas mientras persista la interrupción.");
        }
    }
}
