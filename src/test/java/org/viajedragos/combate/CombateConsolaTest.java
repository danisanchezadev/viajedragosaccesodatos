package org.viajedragos.combate;

import org.junit.jupiter.api.Test;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CombateConsolaTest {
    private final Jugador jugador = new Jugador("Jugador", "", 10, 0, 3, 0, 0, 30, 30);
    private final Enemigo enemigo = new Enemigo("Enemigo", "", 6, 0, 1, 0, 0, 10, 30);
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();

    private ResultadoCombate ejecutar(String entrada, int... tiradas) {
        int[] usadas = {0};
        TiradorDado dados = () -> {
            assertTrue(usadas[0] < tiradas.length, "Tirada inesperada");
            return tiradas[usadas[0]++];
        };
        ResultadoCombate resultado = assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                new Combate(jugador, enemigo, dados).combatir(new Scanner(entrada),
                        new PrintStream(salida, true, StandardCharsets.UTF_8)));
        assertEquals(tiradas.length, usadas[0]);
        return resultado;
    }

    private String texto() {
        return salida.toString(StandardCharsets.UTF_8);
    }

    @Test
    void jugadorActuaPrimeroCuandoGanaIniciativa() {
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("1\n", 1, 1, 1, 1, 4, 2));
        assertTrue(texto().contains("Orden: Jugador, después Enemigo"));
        assertTrue(texto().contains("Jugador: 30/30 de vida | Enemigo: 10/30 de vida"));
        assertTrue(texto().contains("Jugador golpea a Enemigo y causa 10 puntos de daño"));
        assertFalse(texto().contains("Actúa Enemigo"));
    }

    @Test
    void enemigoResuelveSuAtaqueAntesDePedirDecisionAlJugadorSegundo() {
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("1\n", 1, 1, 1, 6, 4, 2, 4, 2));
        assertTrue(texto().contains("Orden: Enemigo, después Jugador"));
        assertTrue(texto().indexOf("Enemigo golpea a Jugador") < texto().indexOf("1. Atacar"));
        assertTrue(texto().contains("Jugador: 22/30 de vida"));
        assertEquals(22, jugador.getVida());
    }

    @Test
    void jugadorMuertoNoRecibeMenuNiEjecutaSuAccion() {
        jugador.setVida(5);
        assertEquals(ResultadoCombate.JUGADOR_MUERE, ejecutar("", 1, 1, 1, 6, 4, 2));
        assertFalse(texto().contains("1. Atacar"));
        assertFalse(texto().contains("Actúa Jugador"));
    }

    @Test
    void ambosActuanUnaVezYSeVuelveATirarElOrdenEnSiguienteTurno() {
        enemigo.setVida(25);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("1\n1\n1\n",
                1, 1, 1, 1, 4, 2, 4, 2, 6, 1, 4, 2, 4, 2, 1, 1, 4, 2));
        assertTrue(texto().contains("Turno 1. Orden: Jugador, después Enemigo"));
        assertTrue(texto().contains("Turno 2. Orden: Enemigo, después Jugador"));
        assertTrue(texto().contains("Turno 3. Orden: Jugador, después Enemigo"));
        assertEquals(3, texto().split("1. Atacar", -1).length - 1);
        assertEquals(14, jugador.getVida());
    }

    @Test
    void invalidosRepitenPreguntaSinRepetirEventosNiTiradas() {
        int[] iniciosTurno = {0};
        jugador.aprenderHabilidad(new Habilidad("Registro") {
            @Override
            public void alInicioTurno(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                iniciosTurno[0]++;
            }
        }, 1);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("\nx\n0\n1\n", 1, 1, 1, 1, 4, 2));
        assertEquals(1, iniciosTurno[0]);
        assertEquals(3, texto().split("Entrada inválida", -1).length - 1);
    }

    @Test
    void falloNoImpideLaAccionDelSegundo() {
        jugador.setVida(5);
        assertEquals(ResultadoCombate.JUGADOR_MUERE, ejecutar("1\n", 1, 1, 1, 1, 1, 2, 4, 2));
        assertTrue(texto().contains("Jugador falla su ataque contra Enemigo"));
        assertTrue(texto().contains("Enemigo golpea a Jugador y causa 5 puntos de daño"));
    }

    @Test
    void contraataqueLetalNoDuplicaAtaqueNiPideOtraAccion() {
        jugador.setVida(5);
        assertEquals(ResultadoCombate.JUGADOR_MUERE, ejecutar("1\n", 1, 1, 1, 1, 1, 6));
        assertTrue(texto().contains("Enemigo contraataca a Jugador y causa 5 puntos de daño"));
        assertFalse(texto().contains("Actúa Enemigo"));
        assertEquals(1, texto().split("contraataca", -1).length - 1);
    }

    @Test
    void contraataqueNoLetalNoSustituyeLaAccionPendienteDelDefensor() {
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("1\n1\n",
                1, 1, 1, 1, 1, 6, 2, 1, 1, 6, 4, 2));
        assertEquals(16, jugador.getVida());
        assertEquals(1, texto().split("contraataca", -1).length - 1);
        assertEquals(1, texto().split("Actúa Enemigo", -1).length - 1);
        assertTrue(texto().contains("Enemigo golpea a Jugador y causa 8 puntos de daño"));
    }

    @Test
    void ataqueSinDanioSeMuestraYSigueLaSegundaAccion() {
        enemigo.setResistencia(100);
        jugador.setVida(5);
        assertEquals(ResultadoCombate.JUGADOR_MUERE, ejecutar("1\n", 1, 1, 1, 1, 4, 2, 4, 2));
        assertTrue(texto().contains("Jugador golpea a Enemigo y causa 0 puntos de daño"));
        assertEquals(10, enemigo.getVida());
    }

    @Test
    void resurreccionTrasPrimeraAccionPermiteDecidirAlJugadorSegundo() {
        jugador.setVida(5);
        int[] muertes = {0};
        jugador.aprenderHabilidad(new Habilidad("Resurrección") {
            @Override
            public void alMorir(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                muertes[0]++;
                p.curarse(20);
            }
        }, 1);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("1\n", 1, 1, 1, 6, 4, 2, 4, 2));
        assertEquals(1, muertes[0]);
        assertTrue(texto().contains("Jugador: 20/30 de vida"));
        assertEquals(20, jugador.getVida());
    }

    @Test
    void muerteSimultaneaEnPrimeraAccionImpideLaSegunda() {
        enemigo.aprenderHabilidad(new Habilidad("Represalia") {
            @Override
            public void alRecibirDanio(ContextoAtaque ca, EstadoHabilidad estado) {
                ca.getAtacante().recibirDanio(30);
            }
        }, 1);
        assertEquals(ResultadoCombate.AMBOS_MUEREN, ejecutar("1\n", 1, 1, 1, 1, 4, 2));
        assertFalse(texto().contains("Actúa Enemigo"));
    }

    @Test
    void automaticoEInteractivoCompartenResultadoVidaYConsumoDeDados() {
        Jugador otroJugador = new Jugador("Jugador", "", 10, 0, 3, 0, 0, 30, 30);
        Enemigo otroEnemigo = new Enemigo("Enemigo", "", 6, 0, 1, 0, 0, 10, 30);
        int[] tiradas = {1, 1, 1, 6, 4, 2, 4, 2};
        int[] usadas = {0};
        ResultadoCombate automatico = new Combate(otroJugador, otroEnemigo,
                () -> tiradas[usadas[0]++]).combatir();
        assertEquals(automatico, ejecutar("1\n", tiradas));
        assertEquals(tiradas.length, usadas[0]);
        assertEquals(otroJugador.getVida(), jugador.getVida());
        assertEquals(otroEnemigo.getVida(), enemigo.getVida());
    }
}
