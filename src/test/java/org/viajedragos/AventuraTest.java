package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.viajedragos.catalogo.RepositorioEnemigos;
import org.viajedragos.modelo.DefinicionEnemigo;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.combate.TiradorDado;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.implementacion.Impetu;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;
import org.viajedragos.habilidad.implementacion.PrimerImpulso;
import org.viajedragos.habilidad.implementacion.Rabia;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Jugador;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Random;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.*;

class AventuraTest {
    private RepositorioEnemigos soloLobo() {
        return () -> java.util.List.of(new DefinicionEnemigo("lobo", "Lobo", "", 5, 2, 2, 1, 2, 18));
    }

    private static final int[] VICTORIA_SIN_DANIO = {1, 1, 1, 1, 6, 1, 6};
    private static final int[] VICTORIA_CON_VIDA_6 = {1, 1, 1, 6, 6, 1, 6, 6, 1, 6};
    // Misma regla de derrota; el Aventurero de consola dispone ahora de 48 PS.
    private static final int[] DERROTA = {1, 1, 1, 6, 6, 1, 6, 1, 6, 6, 1, 6, 1, 6, 1, 6, 1, 6, 4, 2};
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();

    private PrintStream consola() {
        return new PrintStream(salida, true, StandardCharsets.UTF_8);
    }

    private String texto() {
        return salida.toString(StandardCharsets.UTF_8);
    }

    private long veces(String fragmento) {
        return texto().lines().filter(linea -> linea.contains(fragmento)).count();
    }

    private void ejecutar(String entrada, Dados dados) {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.ejecutar(new Scanner(entrada), consola(), dados, new Random(0), soloLobo(), new Random(0)));
        dados.comprobarConsumo();
    }

    private boolean jugar(Jugador jugador, String entrada, Dados dados) {
        boolean volverAlInicio = assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.jugarAventura(jugador, new Scanner(entrada), consola(), dados, new Random(0), soloLobo(), new Random(0)));
        dados.comprobarConsumo();
        return volverAlInicio;
    }

    private Jugador jugador() {
        return new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 24, 24);
    }

    @Test
    void continuarConservaExactamenteVidaActualYMaxima() {
        ejecutar("1\n1\n2\n3\n1\n2\n0\n", new Dados(VICTORIA_CON_VIDA_6, VICTORIA_SIN_DANIO));
        assertEquals(2, veces("Victoria."));
        assertEquals(2, veces("Vida final: Aventurero 30/48 | Lobo 0/18"));
        String segundo = texto().substring(texto().indexOf("Encuentro 2."));
        assertTrue(segundo.contains("Aventurero: 30/48 de vida | Lobo: 18/18 de vida"));
        assertFalse(segundo.contains("Aventurero: 48/48"));
        assertEquals(1, veces("1. Nueva aventura"));
    }

    @Test
    void mismaInstanciaDelJugadorYDistintaInstanciaDeLoboEnCadaEncuentro() {
        Jugador j = jugador();
        List<Combatiente> jugadores = new ArrayList<>();
        List<Combatiente> lobos = new ArrayList<>();
        // Observación exclusiva de prueba: no introduce efectos ni habilidades en app.
        j.aprenderHabilidad(new Habilidad("Registro de prueba") {
            @Override
            public void antesDeAtacar(ContextoAtaque contexto, EstadoHabilidad estado) {
                jugadores.add(contexto.getAtacante());
                lobos.add(contexto.getDefensor());
                assertEquals(18, contexto.getDefensor().getVida());
                assertEquals(18, contexto.getDefensor().getVidaMaxima());
                assertEquals(5, contexto.getDefensor().getFuerza());
                assertEquals(2, contexto.getDefensor().getResistencia());
                assertEquals(2, contexto.getDefensor().getIniciativa());
                assertEquals(1, contexto.getDefensor().getAgilidad());
                assertEquals(2, contexto.getDefensor().getVelocidad());
                assertTrue(contexto.getDefensor().getHabilidades().isEmpty());
                assertTrue(contexto.getDefensor().getEquipoEquipado().isEmpty());
            }
        }, 1);

        assertFalse(jugar(j, "1\n2\n3\n1\n2\n0\n", new Dados(VICTORIA_CON_VIDA_6, VICTORIA_SIN_DANIO)));
        assertEquals(2, jugadores.size());
        assertSame(j, jugadores.get(0));
        assertSame(j, jugadores.get(1));
        assertNotSame(lobos.get(0), lobos.get(1));
        assertEquals(6, j.getVida());
        assertEquals(24, j.getVidaMaxima());
    }

    @Test
    void terminarAventuraVuelveAlInicioSinCrearOtroEncuentro() {
        ejecutar("1\n1\n2\n4\n0\n", new Dados(VICTORIA_SIN_DANIO));
        assertEquals(1, veces("Comienza el combate"));
        assertEquals(2, veces("1. Nueva aventura"));
        assertTrue(texto().contains("La aventura ha terminado."));
        assertFalse(texto().contains("Encuentro 2."));
    }

    @Test
    void nuevaAventuraDespuesDeAbandonarEmpiezaConVidaCompletaYEncuentroUno() {
        ejecutar("1\n1\n2\n4\n1\n1\n2\n0\n", new Dados(VICTORIA_CON_VIDA_6, VICTORIA_SIN_DANIO));
        assertEquals(2, veces("Encuentro 1."));
        assertFalse(texto().contains("Encuentro 2."));
        assertEquals(2, veces("Aventurero: 48/48 de vida | Lobo: 18/18 de vida"));
        assertTrue(texto().contains("Vida final: Aventurero 30/48"));
        assertTrue(texto().contains("Vida final: Aventurero 48/48"));
    }

    @Test
    void muerteTerminaAventuraSinMenuDeContinuacion() {
        ejecutar("1\n1\n1\n0\n", new Dados(DERROTA));
        assertTrue(texto().contains("Derrota."));
        assertEquals(2, veces("1. Nueva aventura"));
        assertFalse(texto().contains("3. Continuar al siguiente encuentro"));
        assertFalse(texto().contains("Encuentro 2."));
    }

    @Test
    void nuevaAventuraTrasMorirNoReutilizaElPersonajeMuerto() {
        ejecutar("1\n1\n1\n1\n1\n2\n0\n", new Dados(DERROTA, VICTORIA_SIN_DANIO));
        assertEquals(1, veces("Derrota."));
        assertEquals(1, veces("Victoria."));
        assertEquals(2, veces("Encuentro 1."));
        assertEquals(2, veces("Aventurero: 48/48 de vida | Lobo: 18/18 de vida"));
        assertTrue(texto().contains("Vida final: Aventurero 48/48 | Lobo 0/18"));
    }

    @Test
    void muerteSimultaneaDevuelveAlInicioSinContinuar() {
        Jugador j = jugador();
        j.setVida(5);
        // Contingencia de prueba como las de CombateCompletoTest; no existe en la demo.
        j.aprenderHabilidad(new Habilidad("Represalia de prueba") {
            @Override
            public void alRecibirDanio(ContextoAtaque contexto, EstadoHabilidad estado) {
                contexto.getAtacante().recibirDanio(contexto.getAtacante().getVida());
            }
        }, 1);
        assertTrue(jugar(j, "", new Dados(new int[]{1, 1, 1, 6, 6, 1, 6})));
        assertEquals(0, j.getVida());
        assertTrue(texto().contains("Ambos han muerto."));
        assertTrue(texto().contains("La aventura ha terminado."));
        assertFalse(texto().contains("3. Continuar al siguiente encuentro"));
        assertEquals(1, veces("Comienza el combate"));
    }

    @Test
    void salirDelMenuEntreEncuentrosNoVuelveAlInicial() {
        ejecutar("1\n1\n2\n0\n", new Dados(VICTORIA_SIN_DANIO));
        assertEquals(1, veces("1. Nueva aventura"));
        assertEquals(1, veces("Comienza el combate"));
        assertTrue(texto().endsWith("Hasta pronto." + System.lineSeparator()));
    }

    @Test
    void invalidosEnAmbosMenusNoCreanEncuentrosNiModificanVida() {
        ejecutar("2\nx\n1\n1\n2\nx\n5\n\n3\n1\n2\n0\n",
                new Dados(VICTORIA_CON_VIDA_6, VICTORIA_SIN_DANIO));
        assertEquals(5, veces("Entrada inválida"));
        assertEquals(2, veces("Comienza el combate"));
        assertEquals(2, veces("Vida final: Aventurero 30/48 | Lobo 0/18"));
    }

    @Test
    void cierreDeEntradaEntreEncuentrosNoIniciaOtro() {
        ejecutar("1\n1\n2\n", new Dados(VICTORIA_SIN_DANIO));
        assertEquals(1, veces("Comienza el combate"));
        assertTrue(texto().contains("Entrada finalizada. Hasta pronto."));
        assertEquals(1, veces("1. Nueva aventura"));
    }

    @Test
    void cierreDuranteElSegundoCombateFinalizaSesionSinReutilizarPersonaje() {
        ejecutar("1\n1\n2\n3\n", new Dados(VICTORIA_CON_VIDA_6, new int[]{1, 1, 1, 1}));
        assertEquals(2, veces("Comienza el combate"));
        assertEquals(1, veces("Victoria."));
        assertEquals(0, veces("Derrota."));
        assertTrue(texto().contains("combate queda sin terminar"));
        assertEquals(1, veces("1. Nueva aventura"));
    }

    @Test
    void efectosTemporalesSeRetiranEntreEncuentrosSinPerderEquipoNiHabilidades() {
        Jugador j = jugador();
        j.setVida(6);
        Rabia rabia = new Rabia();
        PrimerImpulso impulso = new PrimerImpulso(3);
        Impetu impetu = new Impetu();
        InstintoSupervivencia instinto = new InstintoSupervivencia();
        j.aprenderHabilidad(rabia, 1);
        j.aprenderHabilidad(impulso, 3);
        j.aprenderHabilidad(impetu, 1);
        j.aprenderHabilidad(instinto, 1);
        Arma arma = new Arma("Equipo de prueba", TipoEquipo.ARMA,
                Map.of(Atributo.FUERZA, 2, Atributo.RESISTENCIA, -1), Map.of());
        assertTrue(j.equipar(arma));
        Map<Habilidad, EstadoHabilidad> estados = Map.copyOf(j.getHabilidades());
        Dados dados = new Dados(VICTORIA_SIN_DANIO, VICTORIA_SIN_DANIO);
        dados.antes = indice -> {
            if (indice == 0 || indice == 7) {
                assertEquals(13, j.getFuerza());
                assertEquals(3, j.getAgilidad());
                assertEquals(6, j.getIniciativa());
                assertEquals(0, j.getResistencia());
                assertEquals(6, j.getVida());
            }
        };

        assertFalse(jugar(j, "1\n2\n3\n1\n2\n0\n", dados));
        assertEquals(2, veces("Victoria."));
        assertEquals(6, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(8, j.getFuerza());
        assertEquals(2, j.getAgilidad());
        assertEquals(3, j.getIniciativa());
        assertEquals(2, j.getAtributoBase(Atributo.RESISTENCIA));
        assertEquals(1, j.getResistencia());
        assertSame(arma, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(estados.size(), j.getHabilidades().size());
        estados.forEach((habilidad, estado) -> assertSame(estado, j.getHabilidades().get(habilidad)));
        assertEquals(3, j.getHabilidades().get(impulso).getNivel());
    }

    private static class Dados implements TiradorDado {
        private final int[] valores;
        private int usadas;
        private IntConsumer antes = indice -> {};

        private Dados(int[]... secuencias) {
            valores = Arrays.stream(secuencias).flatMapToInt(Arrays::stream).toArray();
        }

        @Override
        public int tirarDado() {
            assertTrue(usadas < valores.length, "Tirada inesperada o encuentro no previsto");
            antes.accept(usadas);
            return valores[usadas++];
        }

        private void comprobarConsumo() {
            assertEquals(valores.length, usadas, "Consumo exacto de dados");
        }
    }
}
