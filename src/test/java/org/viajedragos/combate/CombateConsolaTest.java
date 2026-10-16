package org.viajedragos.combate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.modelo.TipoPocion;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;
import org.viajedragos.habilidad.implementacion.SegundoAliento;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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

    @ParameterizedTest
    @CsvSource({"PEQUENA,1,8,10", "GRANDE,2,16,18"})
    void pocionSustituyeAtaqueYEnemigoConservaSuAccion(
            TipoPocion tipo, String opcion, int recuperacion, int vidaFinal) {
        jugador.setVida(10);
        jugador.agregarPocion(tipo);
        jugador.agregarPocion(tipo);
        int[] ataques = {0};
        jugador.aprenderHabilidad(new Habilidad("Registro de ataques") {
            @Override
            public void antesDeAtacar(ContextoAtaque contexto, EstadoHabilidad estado) {
                ataques[0]++;
                assertEquals(vidaFinal, jugador.getVida());
            }
        }, 1);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("2\n" + opcion + "\n1\n",
                1, 1, 1, 1, 4, 2, 1, 6, 4, 2));
        assertEquals(1, ataques[0], "Solo se ataca en el segundo turno");
        assertEquals(vidaFinal, jugador.getVida());
        assertEquals(1, jugador.getCantidadPociones(tipo));
        assertTrue(texto().contains("Vida recuperada: +" + recuperacion + " PS"));
        assertTrue(texto().contains("Unidades restantes de " + tipo.getNombre() + ": 1"));
        String primero = texto().substring(0, texto().indexOf("Turno 2."));
        assertFalse(primero.contains("Jugador golpea"));
        assertFalse(primero.contains("Jugador falla"));
        assertEquals(1, primero.split("Actúa Enemigo", -1).length - 1);
        assertTrue(primero.indexOf("Tu acción ha terminado.") < primero.indexOf("Actúa Enemigo"));
        assertTrue(texto().contains("Turno 2. Orden: Jugador, después Enemigo"));
    }

    @Test
    void enemigoPrimeroDebeResolverSuAtaqueAntesDePoderCurarse() {
        jugador.setVida(20);
        jugador.agregarPocion(TipoPocion.PEQUENA);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("2\n1\n1\n",
                1, 1, 1, 6, 4, 2, 1, 6, 4, 2));
        assertEquals(20, jugador.getVida());
        assertEquals(0, jugador.getCantidadPociones(TipoPocion.PEQUENA));
        assertTrue(texto().indexOf("Enemigo golpea a Jugador") < texto().indexOf("TU TURNO"));
        assertTrue(texto().contains("Vida actual: 12/30 PS"));
        assertTrue(texto().contains("Vida actual: 20/30 PS"));
        assertEquals(1, texto().split("Actúa Enemigo", -1).length - 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2\n0\n1\n", "x\n3\n0\n1\n", "2\nx\n3\n\n0\n1\n", "2\n1\n2\n0\n1\n"})
    void cancelarInvalidosOExistenciasCeroConservanAccionYDados(String entrada) {
        jugador.setVida(20);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(entrada, 1, 1, 1, 1, 4, 2));
        assertEquals(20, jugador.getVida());
        assertTrue(jugador.getInventarioPociones().isEmpty());
        assertFalse(texto().contains("Tu acción ha terminado."));
        assertFalse(texto().contains("Has utilizado"));
        assertFalse(texto().contains("Actúa Enemigo"));
        assertEquals(1, texto().split("Jugador golpea a Enemigo", -1).length - 1);
        if (entrada.startsWith("2")) assertTrue(texto().split("TU TURNO", -1).length > 2);
    }

    @Test
    void opcionesFijasConCeroYCancelarPermitenAtacarSinConsumir() {
        jugador.agregarPocion(TipoPocion.GRANDE);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("2\n1\n0\n1\n", 1, 1, 1, 1, 4, 2));
        assertTrue(texto().contains("1. Poción pequeña (x0) +8 PS"));
        assertTrue(texto().contains("2. Poción grande (x1) +16 PS"));
        assertTrue(texto().contains("No tienes esa poción."));
        assertEquals(1, jugador.getCantidadPociones(TipoPocion.GRANDE));
    }

    @ParameterizedTest
    @EnumSource(TipoPocion.class)
    void vidaCompletaNoConsumeNiPierdeLaAccion(TipoPocion tipo) {
        jugador.agregarPocion(tipo);
        jugador.aprenderHabilidad(new Habilidad("No hay curación") {
            @Override
            public void alCurarse(Combatiente p, EstadoHabilidad estado) {
                fail("El intento con vida completa no debe curar");
            }
        }, 1);
        String opcion = tipo == TipoPocion.PEQUENA ? "1" : "2";
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("2\n" + opcion + "\n0\n1\n",
                1, 1, 1, 1, 4, 2));
        assertEquals(30, jugador.getVida());
        assertEquals(1, jugador.getCantidadPociones(tipo));
        assertTrue(texto().contains("La vida está completa"));
        assertFalse(texto().contains("Has utilizado"));
    }

    @ParameterizedTest
    @EnumSource(TipoPocion.class)
    void recuperacionParcialMuestraCantidadRealYEliminaUltimaUnidad(TipoPocion tipo) {
        jugador.setVida(28);
        jugador.agregarPocion(tipo);
        String opcion = tipo == TipoPocion.PEQUENA ? "1" : "2";
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("2\n" + opcion + "\n1\n",
                1, 1, 1, 1, 4, 2, 1, 6, 4, 2));
        assertTrue(texto().contains("Vida recuperada: +2 PS"));
        assertTrue(texto().contains("Vida actual: 30/30 PS"));
        assertTrue(texto().contains("Unidades restantes de " + tipo.getNombre() + ": 0"));
        assertFalse(jugador.getInventarioPociones().containsKey(tipo));
        assertEquals(22, jugador.getVida());
    }

    @Test
    void curacionConservaEventosEstadosYNivelesSinEjecutarAtaqueEnSuAccion() {
        jugador.setVida(10);
        jugador.agregarPocion(TipoPocion.PEQUENA);
        List<String> eventos = new ArrayList<>();
        EstadoHabilidad[] esperado = {null};
        Habilidad registro = new Habilidad("Registro de fases") {
            @Override
            public void alInicioTurno(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                eventos.add("inicio");
            }
            @Override
            public void alCurarse(Combatiente p, EstadoHabilidad estado) {
                assertSame(jugador, p);
                assertSame(esperado[0], estado);
                assertEquals(3, estado.getNivel());
                assertEquals(18, p.getVida());
                eventos.add("curacion");
            }
            @Override
            public void antesDeAtacar(ContextoAtaque ca, EstadoHabilidad estado) {
                eventos.add("ataque");
            }
            @Override
            public void antesDeCalcularDanio(ContextoAtaque ca, EstadoHabilidad estado) {
                eventos.add(ca.getAtacante() == jugador ? "calculo propio" : "calculo enemigo");
            }
            @Override
            public void antesDeAplicarDanio(ContextoAtaque ca, EstadoHabilidad estado) {
                eventos.add(ca.getAtacante() == jugador ? "aplicacion propia" : "aplicacion enemiga");
            }
            @Override
            public void alHacerDanio(ContextoAtaque ca, EstadoHabilidad estado) {
                eventos.add("hacer danio");
            }
            @Override
            public void alRecibirDanio(ContextoAtaque ca, EstadoHabilidad estado) {
                eventos.add("recibir danio");
            }
            @Override
            public void alFinalTurno(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                eventos.add("fin turno");
            }
            @Override
            public void alFinalCombate(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                eventos.add("fin combate");
            }
        };
        jugador.aprenderHabilidad(registro, 3);
        esperado[0] = jugador.getHabilidades().get(registro);
        jugador.getHabilidades().put(new Habilidad("Inactiva") {
            @Override
            public void alCurarse(Combatiente p, EstadoHabilidad estado) {
                fail("Una habilidad inactiva no participa");
            }
        }, new EstadoHabilidad());
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("2\n1\n1\n",
                1, 1, 1, 1, 4, 2, 1, 6, 4, 2));
        assertEquals(List.of("inicio", "curacion", "calculo enemigo", "aplicacion enemiga",
                "recibir danio", "fin turno", "inicio", "ataque", "calculo propio",
                "aplicacion propia", "hacer danio", "fin combate"), eventos);
        assertSame(esperado[0], jugador.getHabilidades().get(registro));
        assertEquals(3, esperado[0].getNivel());
    }

    @Test
    void instintoSeRetiraAlCurarseYEquipoPermaneceTrasFinalizar() {
        jugador.setVida(5);
        InstintoSupervivencia instinto = new InstintoSupervivencia();
        jugador.aprenderHabilidad(instinto, 1);
        Arma arma = new Arma("Espada de prueba", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of());
        assertTrue(jugador.equipar(arma));
        jugador.agregarPocion(TipoPocion.PEQUENA);
        List<Integer> fuerzas = new ArrayList<>();
        enemigo.aprenderHabilidad(new Habilidad("Observador de prueba") {
            @Override
            public void antesDeAtacar(ContextoAtaque ca, EstadoHabilidad estado) {
                fuerzas.add(jugador.getFuerza());
            }
        }, 1);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("2\n1\n1\n",
                1, 1, 1, 1, 4, 2, 1, 6, 4, 2));
        assertEquals(List.of(11), fuerzas, "La poción retiró Instinto antes de actuar el enemigo");
        assertEquals(10, jugador.getAtributoBase(Atributo.FUERZA));
        assertEquals(11, jugador.getFuerza(), "La limpieza conserva el equipo sin retirar Instinto dos veces");
        assertEquals(1, jugador.getModificadoresEquipo().get(Atributo.FUERZA));
        assertSame(arma, jugador.getEquipoEquipado().get(TipoEquipo.ARMA));
    }

    @Test
    void segundoAlientoPuedeCompletarVidaAntesDeLaDecisionYEvitaDesperdicio() {
        jugador.setVida(10);
        jugador.aprenderHabilidad(new SegundoAliento(20), 2);
        jugador.agregarPocion(TipoPocion.PEQUENA);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("2\n1\n0\n1\n", 1, 1, 1, 1, 4, 2));
        assertEquals(30, jugador.getVida());
        assertEquals(1, jugador.getCantidadPociones(TipoPocion.PEQUENA));
        assertTrue(texto().contains("La vida está completa"));
        assertFalse(texto().contains("Has utilizado"));
    }

    @Test
    void morirTrasConsumirResuelveMuerteYLimpiezaSinAtaqueDelJugador() {
        jugador.setVida(1);
        jugador.agregarPocion(TipoPocion.PEQUENA);
        int[] muertes = {0};
        int[] finales = {0};
        jugador.aprenderHabilidad(new Habilidad("Registro muerte") {
            @Override
            public void alMorir(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                muertes[0]++;
            }
            @Override
            public void alFinalCombate(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                finales[0]++;
            }
            @Override
            public void antesDeAtacar(ContextoAtaque ca, EstadoHabilidad estado) {
                fail("El jugador no ataca después de usar la poción");
            }
        }, 1);
        assertEquals(ResultadoCombate.JUGADOR_MUERE, ejecutar("2\n1\n", 1, 1, 1, 1, 6, 1, 6));
        assertEquals(1, muertes[0]);
        assertEquals(1, finales[0]);
        assertEquals(0, jugador.getVida());
        assertTrue(jugador.getInventarioPociones().isEmpty());
    }

    @Test
    void resurreccionTrasConsumirContinuaSinDuplicarConsumoNiAcciones() {
        jugador.setVida(1);
        jugador.agregarPocion(TipoPocion.PEQUENA);
        int[] muertes = {0};
        jugador.aprenderHabilidad(new Habilidad("Resurrección de prueba") {
            @Override
            public void alMorir(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                muertes[0]++;
                p.curarse(20);
            }
        }, 1);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar("2\n1\n1\n",
                1, 1, 1, 1, 6, 1, 6, 1, 6, 4, 2));
        assertEquals(1, muertes[0]);
        assertEquals(20, jugador.getVida());
        assertEquals(1, texto().split("Has utilizado", -1).length - 1);
        assertTrue(jugador.getInventarioPociones().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "2\n", "2\nx\n", "2\n0\n"})
    void cierreEnMenusInterrumpeSinAtaqueNiFinalizacion(String entrada) {
        jugador.setVida(20);
        jugador.agregarPocion(TipoPocion.PEQUENA);
        jugador.aprenderHabilidad(new Habilidad("No se finaliza") {
            @Override
            public void antesDeAtacar(ContextoAtaque ca, EstadoHabilidad estado) {
                fail("No se debe atacar sin elegir una acción");
            }
            @Override
            public void alFinalCombate(Combatiente p, Combatiente o, EstadoHabilidad estado) {
                fail("Una interrupción no es una finalización");
            }
        }, 1);
        int[] tiradas = {1, 1, 1, 1};
        int[] usadas = {0};
        Scanner scanner = new Scanner(entrada);
        assertThrows(NoSuchElementException.class, () -> new Combate(jugador, enemigo, () -> {
            assertTrue(usadas[0] < tiradas.length);
            return tiradas[usadas[0]++];
        }).combatir(scanner, new PrintStream(salida, true, StandardCharsets.UTF_8)));
        assertEquals(4, usadas[0]);
        assertEquals(20, jugador.getVida());
        assertEquals(1, jugador.getCantidadPociones(TipoPocion.PEQUENA));
        assertFalse(scanner.hasNextLine(), "El combate no cierra el Scanner del llamador");
    }

    @Test
    void participanteNoJugadorConservaAtaqueYRechazaObjetoSinPerderAccion() {
        Combatiente participante = new Enemigo("Otro combatiente", "", 10, 0, 3, 0, 0, 30, 30);
        int[] tiradas = {1, 1, 1, 1, 4, 2};
        int[] usadas = {0};
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(participante, enemigo, () -> {
            assertTrue(usadas[0] < tiradas.length);
            return tiradas[usadas[0]++];
        }).combatir(new Scanner("2\nx\n1\n"), new PrintStream(salida, true, StandardCharsets.UTF_8)));
        assertEquals(tiradas.length, usadas[0]);
        assertEquals(30, participante.getVida());
        assertEquals(0, enemigo.getVida());
        assertFalse(texto().contains("2. Usar objeto"));
        assertFalse(texto().contains("USAR OBJETO"));
        assertFalse(texto().contains("Tu acción ha terminado."));
        assertEquals(2, texto().split("Entrada inválida", -1).length - 1);
    }

    @Test
    void automaticoNoConsumePocionesAunqueElJugadorEsteHerido() {
        jugador.setVida(20);
        jugador.agregarPocion(TipoPocion.PEQUENA);
        int[] tiradas = {1, 1, 1, 1, 4, 2};
        int[] usadas = {0};
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(jugador, enemigo, () -> {
            assertTrue(usadas[0] < tiradas.length);
            return tiradas[usadas[0]++];
        }).combatir());
        assertEquals(tiradas.length, usadas[0]);
        assertEquals(20, jugador.getVida());
        assertEquals(1, jugador.getCantidadPociones(TipoPocion.PEQUENA));
    }

    @Test
    void consumoSinRecuperacionPermiteCancelarYAtacarSinPerderAccion() {
        Jugador sinRecuperacion = new Jugador("Jugador", "", 10, 0, 3, 0, 0, 20, 30) {
            @Override
            public int usarPocion(TipoPocion tipo) {
                // Simula un consumo rechazado por el modelo, sin efectos.
                return 0;
            }
        };
        sinRecuperacion.agregarPocion(TipoPocion.PEQUENA);
        int[] tiradas = {1, 1, 1, 1, 4, 2};
        int[] usadas = {0};
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(sinRecuperacion, enemigo, () -> {
            assertTrue(usadas[0] < tiradas.length);
            return tiradas[usadas[0]++];
        }).combatir(new Scanner("2\n1\n0\n1\n"), new PrintStream(salida, true, StandardCharsets.UTF_8)));
        assertEquals(tiradas.length, usadas[0]);
        assertEquals(20, sinRecuperacion.getVida());
        assertEquals(1, sinRecuperacion.getCantidadPociones(TipoPocion.PEQUENA));
        assertTrue(texto().contains("No se ha recuperado vida"));
        assertFalse(texto().contains("Tu acción ha terminado."));
    }

    @Test
    void participanteNoJugadorConservaTurnosYResultadoDelModoAutomatico() {
        Combatiente participante = new Enemigo("Otro", "", 10, 0, 3, 0, 0, 30, 30);
        Combatiente otro = new Enemigo("Otro", "", 10, 0, 3, 0, 0, 30, 30);
        enemigo.setVida(25);
        Enemigo rival = new Enemigo("Enemigo", "", 6, 0, 1, 0, 0, 25, 30);
        int[] tiradas = {1, 1, 1, 1, 4, 2, 4, 2, 6, 1, 4, 2, 4, 2, 1, 1, 4, 2};
        int[] automaticas = {0};
        ResultadoCombate esperado = new Combate(otro, rival, () -> tiradas[automaticas[0]++]).combatir();
        int[] usadas = {0};
        assertEquals(esperado, new Combate(participante, enemigo, () -> {
            assertTrue(usadas[0] < tiradas.length);
            return tiradas[usadas[0]++];
        }).combatir(new Scanner("2\n1\n1\n1\n"), new PrintStream(salida, true, StandardCharsets.UTF_8)));
        assertEquals(tiradas.length, usadas[0]);
        assertEquals(tiradas.length, automaticas[0]);
        assertEquals(otro.getVida(), participante.getVida());
        assertEquals(rival.getVida(), enemigo.getVida());
        assertEquals(2, texto().split("El enemigo elige atacar", -1).length - 1);
        assertTrue(texto().contains("Turno 2. Orden: Enemigo, después Otro"));
        assertFalse(texto().contains("2. Usar objeto"));
    }
}
