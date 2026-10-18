package org.viajedragos.consola;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.viajedragos.consola.RitmoConsola.Velocidad;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RitmoConsolaTest {
    @ParameterizedTest
    @CsvSource({"INSTANTANEA,0", "RAPIDA,250", "NORMAL,700", "LENTA,1200"})
    void velocidadesCentralizadasAplicanSuDuracionSinDormir(Velocidad velocidad, long duracion) {
        List<Long> esperas = new ArrayList<>();
        RitmoConsola ritmo = new RitmoConsola(velocidad, esperas::add);
        assertSame(velocidad, ritmo.getVelocidad());
        assertEquals(duracion, velocidad.getMilisegundos());
        ritmo.pausar(new PrintStream(new ByteArrayOutputStream()));
        assertEquals(duracion == 0 ? List.of() : List.of(duracion), esperas);
    }

    @Test
    void cambiarVelocidadAfectaSoloALaSiguientePausaDeEsaSesion() {
        List<Long> esperas = new ArrayList<>();
        RitmoConsola ritmo = new RitmoConsola(Velocidad.NORMAL, esperas::add);
        RitmoConsola otraSesion = new RitmoConsola(Velocidad.LENTA, ms -> fail("Otra sesión no debe pausar"));
        PrintStream salida = new PrintStream(new ByteArrayOutputStream());
        ritmo.pausar(salida);
        ritmo.setVelocidad(Velocidad.RAPIDA);
        ritmo.pausar(salida);
        ritmo.setVelocidad(Velocidad.LENTA);
        ritmo.pausar(salida);
        ritmo.setVelocidad(Velocidad.INSTANTANEA);
        ritmo.pausar(salida);
        assertEquals(List.of(700L, 250L, 1200L), esperas);
        assertEquals(Velocidad.LENTA, otraSesion.getVelocidad());
    }

    @Test
    void vaciaLaSalidaAntesDeEsperar() {
        boolean[] vaciada = {false};
        PrintStream salida = new PrintStream(new ByteArrayOutputStream()) {
            @Override public void flush() { vaciada[0] = true; super.flush(); }
        };
        new RitmoConsola(Velocidad.NORMAL, ms -> assertTrue(vaciada[0])).pausar(salida);
    }

    @Test
    void interrupcionSeConservaYSeInformaSinRepetirEsperas() {
        boolean interrupcionPrevia = Thread.interrupted();
        try {
            var bytes = new ByteArrayOutputStream();
            PrintStream salida = new PrintStream(bytes, true, StandardCharsets.UTF_8);
            int[] intentos = {0};
            RitmoConsola ritmo = new RitmoConsola(Velocidad.NORMAL, ms -> {
                intentos[0]++;
                throw new InterruptedException("Interrupción controlada");
            });
            ritmo.pausar(salida);
            assertTrue(Thread.currentThread().isInterrupted());
            ritmo.pausar(salida);
            assertEquals(1, intentos[0]);
            assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("Pausa interrumpida"));
        } finally {
            Thread.interrupted();
            if (interrupcionPrevia) Thread.currentThread().interrupt();
        }
    }

    @Test
    void interrupcionYaPresenteNoSeBorraNiIniciaUnaEspera() {
        boolean interrupcionPrevia = Thread.interrupted();
        try {
            Thread.currentThread().interrupt();
            new RitmoConsola(Velocidad.NORMAL, ms -> fail("No debe empezar a esperar"))
                    .pausar(new PrintStream(new ByteArrayOutputStream()));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            if (interrupcionPrevia) Thread.currentThread().interrupt();
        }
    }

    @Test
    void configuracionNulaSeRechazaSinPerderLaVelocidadActual() {
        assertThrows(NullPointerException.class, () -> new RitmoConsola(null));
        assertThrows(NullPointerException.class, () -> new RitmoConsola(Velocidad.NORMAL, null));
        RitmoConsola ritmo = new RitmoConsola(Velocidad.INSTANTANEA, ms -> fail("No debe esperar"));
        assertThrows(NullPointerException.class, () -> ritmo.setVelocidad(null));
        assertEquals(Velocidad.INSTANTANEA, ritmo.getVelocidad());
    }
}
