package org.viajedragos.consola;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class TiempoAventuraTest {
    @ParameterizedTest @CsvSource({"0,00:00:00", "1,00:00:01", "59,00:00:59", "60,00:01:00",
            "3599,00:59:59", "3600,01:00:00", "360000,100:00:00", "3600000,1000:00:00"})
    void duracionMonotonicaFormateadaSinLimiteDeNoventaYNueveHoras(long segundos, String esperado) {
        long[] reloj = {9_000_000_000L}; var tiempo = new TiempoAventura(() -> reloj[0]);
        reloj[0] += segundos * 1_000_000_000L;
        assertEquals(esperado, tiempo.transcurrido()); assertEquals(esperado, tiempo.transcurrido());
    }

    @Test void lecturasYSubmenusNoAcumulanTiempoYUnaAventuraNuevaReinicia() {
        long[] reloj = {1_000_000_000L}; var primera = new TiempoAventura(() -> reloj[0]);
        reloj[0] += 20_000_000_000L;
        assertEquals("00:00:20", primera.transcurrido()); assertEquals("00:00:20", primera.transcurrido());
        var segunda = new TiempoAventura(() -> reloj[0]);
        assertEquals("00:00:00", segunda.transcurrido());
        reloj[0] += 1_000_000_000L;
        assertEquals("00:00:21", primera.transcurrido()); assertEquals("00:00:01", segunda.transcurrido());
    }

    @Test void admiteSaltoDeSignoDeNanoTimeYNoProduceDuracionesNegativas() {
        long[] reloj = {Long.MAX_VALUE - 1_000_000_000L}; var tiempo = new TiempoAventura(() -> reloj[0]);
        reloj[0] += 2_000_000_000L; assertEquals("00:00:02", tiempo.transcurrido());
        reloj[0] -= 3_000_000_000L; assertEquals("00:00:00", tiempo.transcurrido());
        assertThrows(NullPointerException.class, () -> new TiempoAventura(null));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void panelConfigurableConservaAnchuraAnsiYOpciones(boolean ansi) {
        var bytes = new ByteArrayOutputStream(); var salida = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        var panel = new PanelConsola(salida, "TÍTULO", 100, ansi);
        panel.columnas(new int[]{30,30,32}, "Texto muy largo ".repeat(4), "\u001b[32mColor\u001b[0m", "2147483647");
        panel.opciones(new int[]{1,2,3,4}, "Primera", "Segunda", "Tercera", "Cuarta"); panel.cerrar();
        String bruto = bytes.toString(StandardCharsets.UTF_8);
        String plano = bruto.replaceAll("\u001b\\[[0-9;]*m", "");
        plano.lines().filter(l -> !l.isEmpty()).forEach(l -> assertEquals(102, PanelConsola.anchoVisible(l), l));
        assertTrue(plano.lines().anyMatch(l -> l.contains("[1] Primera") && l.contains("[4] Cuarta")));
        assertEquals(ansi, bruto.contains("\u001b[44m"));
        if (ansi) bruto.lines().filter(l -> !l.isEmpty()).forEach(l -> assertTrue(l.endsWith("\u001b[0m")));
        assertThrows(IllegalArgumentException.class, () -> new PanelConsola(salida, "Inválido", 11));
    }
}
