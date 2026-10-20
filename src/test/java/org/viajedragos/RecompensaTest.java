package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.viajedragos.catalogo.RepositorioEnemigos;
import org.viajedragos.modelo.DefinicionEnemigo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.Equipo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.combate.Combate;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.combate.TiradorDado;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.implementacion.Impetu;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;
import org.viajedragos.habilidad.implementacion.PrimerImpulso;
import org.viajedragos.habilidad.implementacion.Rabia;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.modelo.TipoPocion;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Random;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.*;

class RecompensaTest {
    private RepositorioEnemigos soloLobo() {
        return () -> java.util.List.of(new DefinicionEnemigo("lobo", "Lobo", "", 5, 2, 2, 1, 2, 18));
    }

    private static final int[] VICTORIA = {1, 1, 1, 1, 6, 1, 6};
    private static final int[] VICTORIA_CON_VIDA_6 = {1, 1, 1, 6, 6, 1, 6, 6, 1, 6};
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();

    private Jugador jugador() {
        return new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 24, 24);
    }

    private PrintStream consola() {
        return new PrintStream(salida, true, StandardCharsets.UTF_8);
    }

    private String texto() {
        return org.viajedragos.TextoConsola.contenido(salida.toString(StandardCharsets.UTF_8));
    }

    private long veces(String fragmento) {
        return texto().lines().filter(linea -> linea.contains(fragmento)).count();
    }

    private boolean jugar(Jugador j, String entrada, Dados dados) {
        boolean volver = assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.jugarAventura(j, new Scanner("3\n" + entrada), consola(), dados, new Random(0), soloLobo(), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento()));
        dados.comprobarConsumo();
        return volver;
    }

    @Test
    void primeraVictoriaOfreceEspadaDespuesDelCombateSinEquipoInicial() {
        Jugador j = jugador();
        Dados dados = new Dados(VICTORIA);
        dados.antes = indice -> {
            assertTrue(j.getEquipoEquipado().isEmpty());
            assertTrue(j.getHabilidades().isEmpty());
            assertEquals(6, j.getFuerza());
            assertFalse(texto().contains("Has encontrado"));
        };

        assertFalse(jugar(j, "1\n1\n0\n", dados));
        Equipo espada = j.getEquipoEquipado().get(TipoEquipo.ARMA);
        assertInstanceOf(Arma.class, espada);
        assertEquals("Espada desgastada", espada.getNombre());
        assertEquals(Map.of(Atributo.FUERZA, 1), espada.getBonificaciones());
        assertTrue(espada.getHabilidadesOtorgadas().isEmpty());
        assertTrue(j.getHabilidades().isEmpty());
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(7, j.getFuerza());
        assertEquals(24, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertTrue(texto().indexOf("Victoria.") < texto().indexOf("Has encontrado"));
        assertTrue(texto().contains("FUERZA +1"));
        assertTrue(texto().indexOf("Espada desgastada equipada")
                < texto().lastIndexOf("3. Explorar"));
    }

    @Test
    void espadaPersisteTresEncuentrosSinAcumularseNiVolverAOfrecerse() {
        Jugador j = jugador();
        Equipo[] espada = {null};
        Dados dados = new Dados(VICTORIA_CON_VIDA_6, VICTORIA, VICTORIA);
        dados.antes = indice -> {
            if (indice == 10 || indice == 17) {
                Equipo actual = j.getEquipoEquipado().get(TipoEquipo.ARMA);
                assertNotNull(actual);
                if (espada[0] == null) espada[0] = actual;
                else assertSame(espada[0], actual);
                assertEquals(1, j.getEquipoEquipado().size());
                assertEquals(1, j.getModificadoresEquipo().get(Atributo.FUERZA));
                assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
                assertEquals(7, j.getFuerza());
                assertEquals(6, j.getVida());
            }
        };

        assertFalse(jugar(j, "1\n1\n3\n1\n2\n3\n1\n0\n", dados));
        assertEquals(3, veces("Victoria."));
        assertEquals(1, veces("Has encontrado una Espada desgastada"));
        assertEquals(1, veces("1. Equipar Espada desgastada"));
        assertSame(espada[0], j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(7, j.getFuerza());
        assertEquals(6, j.getVida());
    }

    @Test
    void rechazoNoAlteraPersonajeNiPermiteRecuperarLaEspadaDespues() {
        Jugador j = jugador();
        Map<Atributo, Integer> modificadores = new EnumMap<>(j.getModificadoresEquipo());
        assertFalse(jugar(j, "1\n2\n3\n1\n2\n0\n", new Dados(VICTORIA, VICTORIA)));
        assertEquals(2, veces("Victoria."));
        assertEquals(1, veces("Has encontrado una Espada desgastada"));
        assertEquals(1, veces("2. Rechazar Espada desgastada"));
        assertFalse(texto().contains("Espada desgastada equipada"));
        assertEquals(modificadores, j.getModificadoresEquipo());
        assertEquals(6, j.getFuerza());
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(2, j.getResistencia());
        assertEquals(2, j.getAgilidad());
        assertEquals(3, j.getIniciativa());
        assertEquals(2, j.getVelocidad());
        assertEquals(24, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertTrue(j.getHabilidades().isEmpty());
    }

    @Test
    void invalidosRepitenEleccionYEquiparSeInvocaUnaSolaVezSinDadosExtra() {
        int[] llamadas = {0};
        Jugador j = new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 24, 24) {
            @Override
            public boolean equipar(Equipo equipo) {
                llamadas[0]++;
                return super.equipar(equipo);
            }
        };
        assertFalse(jugar(j, "1\nx\n0\n\n 1 \n0\n", new Dados(VICTORIA)));
        assertEquals(3, veces("Entrada inválida"));
        assertEquals(1, veces("Has encontrado una Espada desgastada"));
        assertEquals(1, veces("Comienza el combate"));
        assertEquals(1, llamadas[0]);
        assertEquals(7, j.getFuerza());
        assertEquals(1, j.getModificadoresEquipo().get(Atributo.FUERZA));
    }

    @Test
    void cierreDeEntradaEnRecompensaTerminaSesionSinEquipar() {
        Jugador j = jugador();
        assertFalse(jugar(j, "1\n", new Dados(VICTORIA)));
        assertTrue(texto().contains("Victoria."));
        assertTrue(texto().contains("Entrada finalizada. Hasta pronto."));
        assertEquals(1, texto().split("3. Explorar", -1).length - 1);
        assertEquals(1, veces("Comienza el combate"));
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertEquals(6, j.getFuerza());
    }

    @Test
    void derrotaNoOfreceRecompensa() {
        Jugador j = jugador();
        j.setVida(5);
        assertTrue(jugar(j, "", new Dados(new int[]{1, 1, 1, 6, 6, 1, 6})));
        assertTrue(texto().contains("Derrota."));
        assertFalse(texto().contains("Espada desgastada"));
        assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @Test
    void muerteSimultaneaNoOfreceRecompensa() {
        Jugador j = jugador();
        j.setVida(5);
        j.aprenderHabilidad(new Habilidad("Contingencia exclusiva de prueba") {
            @Override
            public void alRecibirDanio(ContextoAtaque contexto, EstadoHabilidad estado) {
                contexto.getAtacante().recibirDanio(contexto.getAtacante().getVida());
            }
        }, 1);
        assertTrue(jugar(j, "", new Dados(new int[]{1, 1, 1, 6, 6, 1, 6})));
        assertTrue(texto().contains("Ambos han muerto."));
        assertFalse(texto().contains("Espada desgastada"));
        assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @Test
    void aventuraNuevaTrasEquiparEmpiezaSinLaBonificacionAnterior() {
        // Segundo personaje: ataque normal 4/2 causa 7, no los 8 que daría la espada.
        Dados dados = new Dados(VICTORIA, new int[]{1, 1, 1, 1, 4, 2, 1, 2, 1, 6, 4, 1});
        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                TextoConsola.ejecutarSesiones(new Scanner("1\nAventurero\n3\n1\n1\n0\n1\nAventurero\n3\n1\n1\n2\n0\n"), entradaSesion -> app.ejecutar(entradaSesion, consola(), dados, new Random(0), soloLobo(), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento())));
        dados.comprobarConsumo();
        assertEquals(2, veces("ENCUENTRO 1"));
        assertEquals(2, veces("Has encontrado una Espada desgastada"));
        assertEquals(1, veces("Espada desgastada equipada"));
        String nueva = texto().substring(texto().lastIndexOf("ENCUENTRO 1"));
        assertTrue(nueva.contains("Aventurero Lobo PS 48/48 PS 18/18"));
        assertTrue(nueva.contains("Aventurero golpea a Lobo y causa 7 puntos de daño"));
        assertFalse(nueva.contains("Espada desgastada equipada"));
    }

    @Test
    void espadaObtenidaPorLaOfertaModificaElAtaqueRealDeSieteAOcho() {
        Jugador sinEspada = jugador();
        Jugador conEspada = jugador();
        assertTrue(app.ofrecerEspada(conEspada, new Scanner("1\n"), consola()));
        Enemigo loboSin = new Enemigo("Lobo", "", 5, 2, 2, 1, 2, 18, 18);
        Enemigo loboCon = new Enemigo("Lobo", "", 5, 2, 2, 1, 2, 18, 18);
        Dados dadosSin = new Dados(new int[]{4, 2});
        Dados dadosCon = new Dados(new int[]{4, 2});

        new Combate(sinEspada, loboSin, dadosSin).ataque(sinEspada, loboSin);
        new Combate(conEspada, loboCon, dadosCon).ataque(conEspada, loboCon);

        dadosSin.comprobarConsumo();
        dadosCon.comprobarConsumo();
        assertEquals(11, loboSin.getVida());
        assertEquals(10, loboCon.getVida());
        assertEquals(6, conEspada.getAtributoBase(Atributo.FUERZA));
        assertEquals(7, conEspada.getFuerza());
    }

    @Test
    void limpiezaTemporalConservaLaEspadaRecibidaEntreEncuentros() {
        Jugador j = jugador();
        j.setVida(6);
        j.aprenderHabilidad(new Rabia(), 1);
        j.aprenderHabilidad(new PrimerImpulso(3), 3);
        j.aprenderHabilidad(new Impetu(), 1);
        j.aprenderHabilidad(new InstintoSupervivencia(), 1);
        Map<Habilidad, EstadoHabilidad> estados = Map.copyOf(j.getHabilidades());
        Dados dados = new Dados(VICTORIA, VICTORIA);
        dados.antes = indice -> {
            if (indice == 0) {
                assertTrue(j.getEquipoEquipado().isEmpty());
                assertEquals(11, j.getFuerza());
            }
            if (indice == 7) {
                assertEquals(12, j.getFuerza());
                assertEquals(1, j.getModificadoresEquipo().get(Atributo.FUERZA));
                assertEquals(3, j.getAgilidad());
                assertEquals(6, j.getVida());
            }
        };
        assertFalse(jugar(j, "1\n1\n3\n1\n2\n0\n", dados));
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(7, j.getFuerza());
        assertEquals(2, j.getAgilidad());
        assertEquals(2, j.getResistencia());
        assertEquals(3, j.getIniciativa());
        assertEquals(6, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertEquals("Espada desgastada", j.getEquipoEquipado().get(TipoEquipo.ARMA).getNombre());
        assertEquals(estados.size(), j.getHabilidades().size());
        estados.forEach((habilidad, estado) -> assertSame(estado, j.getHabilidades().get(habilidad)));
    }

    @Test
    void ranuraOcupadaNoSustituyeElEquipoNiAnunciaExito() {
        Jugador j = jugador();
        Arma anterior = new Arma("Arma de prueba", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 5), Map.of());
        assertTrue(j.equipar(anterior));
        assertTrue(app.ofrecerEspada(j, new Scanner("1\n"), consola()));
        assertSame(anterior, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(11, j.getFuerza());
        assertTrue(texto().contains("ranura de arma está ocupada"));
        assertFalse(texto().contains("Espada desgastada equipada"));
    }

    @Test
    void huirVuelveAExploracionSinPremiosNiCambiosDeVidaEquipoOInventario() {
        Jugador j = jugador();
        j.setVida(13);
        j.agregarPocion(TipoPocion.PEQUENA);
        Arma arma = new Arma("Arma de prueba", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of());
        j.equipar(arma);
        Dados dados = new Dados(new int[]{1, 1, 1, 1, 6, 1});
        Random sinBotin = new Random(0) {
            @Override
            public int nextInt(int bound) {
                fail("Escapar no genera botín");
                return 0;
            }
        };
        assertFalse(assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.jugarAventura(j, new Scanner("3\n3\n0\n"), consola(), dados, sinBotin, soloLobo(), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento())));
        dados.comprobarConsumo();
        assertEquals(13, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertSame(arma, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(7, j.getFuerza());
        assertTrue(texto().contains("Has escapado."));
        assertTrue(texto().contains("Regresas a exploración sin recompensas"));
        assertTrue(texto().contains("EXPLORACIÓN"));
        assertTrue(texto().contains("Vida: 13/24 PS"));
        assertFalse(texto().contains("Botín:"));
        assertFalse(texto().contains("Has encontrado"));
        assertFalse(texto().contains("Victoria."));
        assertFalse(texto().contains("Derrota."));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void primeraVictoriaRealTrasHuirOfreceEspadaUnaSolaVez(boolean equipar) {
        Jugador j = jugador();
        List<Combatiente> enemigos = new ArrayList<>();
        j.aprenderHabilidad(new Habilidad("Registro de encuentros") {
            @Override
            public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
                assertSame(j, portador);
                assertEquals(24, portador.getVida());
                assertEquals(18, oponente.getVida());
                assertEquals(18, oponente.getVidaMaxima());
                enemigos.add(oponente);
            }
        }, 1);
        Dados dados = new Dados(new int[]{1, 1, 1, 1, 6, 1}, VICTORIA, VICTORIA);
        int[] botin = {0};
        Random azar = new Random(0) {
            @Override
            public int nextInt(int bound) {
                assertEquals(100, bound);
                assertTrue(enemigos.size() >= 2, "La huida inicial no concede botín");
                assertTrue(botin[0]++ < 4);
                return 0;
            }
        };
        String entrada = "3\n3\n1\n" + (equipar ? "1" : "2") + "\n3\n1\n2\n0\n";
        assertFalse(assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.jugarAventura(j, new Scanner("3\n" + entrada), consola(), dados, azar, soloLobo(), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento())));
        dados.comprobarConsumo();
        assertEquals(4, botin[0]);
        assertEquals(3, enemigos.size());
        assertNotSame(enemigos.get(0), enemigos.get(1));
        assertNotSame(enemigos.get(1), enemigos.get(2));
        assertEquals(18, enemigos.get(0).getVida(), "El enemigo abandonado no se vuelve a usar");
        assertEquals(1, veces("Has encontrado una Espada desgastada"));
        assertEquals(2, veces("Victoria."));
        assertEquals(equipar, j.getEquipoEquipado().containsKey(TipoEquipo.ARMA));
        assertEquals(equipar ? 7 : 6, j.getFuerza());
        assertEquals(2, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(2, j.getCantidadPociones(TipoPocion.GRANDE));
        String primero = texto().substring(0, texto().indexOf("ENCUENTRO 2"));
        assertFalse(primero.contains("Has encontrado"));
        assertFalse(primero.contains("Botín:"));
        assertTrue(texto().indexOf("ENCUENTRO 2") < texto().indexOf("Has encontrado"));
    }

    @Test
    void victoriaHuidaVictoriaNoRepitenOfertaRechazadaNiPremianLaHuida() {
        Jugador j = jugador();
        Dados dados = new Dados(VICTORIA, new int[]{1, 1, 1, 1, 6, 1}, VICTORIA);
        int[] botin = {0};
        Random azar = new Random(0) {
            @Override
            public int nextInt(int bound) {
                assertEquals(100, bound);
                assertTrue(botin[0]++ < 4);
                return 99;
            }
        };
        assertFalse(assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.jugarAventura(j, new Scanner("3\n1\n2\n3\n3\n3\n1\n2\n0\n"), consola(), dados, azar, soloLobo(), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento())));
        dados.comprobarConsumo();
        assertEquals(4, botin[0]);
        assertEquals(1, veces("Has encontrado una Espada desgastada"));
        assertEquals(2, veces("Victoria."));
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertTrue(j.getInventarioPociones().isEmpty());
        assertEquals(24, j.getVida());
    }

    @Test
    void terminarTrasHuirPermiteAventuraNuevaSinEquipoNiPociones() {
        Dados dados = new Dados(VICTORIA_CON_VIDA_6, new int[]{1, 1, 1, 1, 6, 1},
                new int[]{1, 1, 1, 1, 6, 1});
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> TextoConsola.ejecutarSesiones(new Scanner("1\nAventurero\n3\n1\n1\n3\n3\n0\n1\nAventurero\n3\n3\n1\n2\n0\n0\n0\n"), entradaSesion -> app.ejecutar(entradaSesion, consola(), dados, new Random(0), soloLobo(), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento())));
        dados.comprobarConsumo();
        assertEquals(2, veces("ENCUENTRO 1"));
        assertEquals(1, veces("Has encontrado una Espada desgastada"));
        String nueva = texto().substring(texto().lastIndexOf("ENCUENTRO 1"));
        assertTrue(nueva.contains("PS 48/48"));
        assertTrue(nueva.contains("Sin consumibles"));
        assertTrue(nueva.contains("Sin equipamiento"));
        assertTrue(texto().contains("Vida final: Aventurero 30/48"));
    }

    @Test
    void entradaCerradaTrasFallarHuidaNoGeneraResultadoNiPremios() {
        Jugador j = jugador();
        Dados dados = new Dados(new int[]{1, 1, 1, 1, 1, 6, 4, 2, 1, 6});
        Random sinBotin = new Random(0) {
            @Override
            public int nextInt(int bound) {
                fail("Un combate interrumpido no concede botín");
                return 0;
            }
        };
        assertFalse(assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.jugarAventura(j, new Scanner("3\n3\n"), consola(), dados, sinBotin, soloLobo(), new Random(0),
                org.viajedragos.catalogo.ConfiguracionRepositorios.objetos(),
                null,
                new org.viajedragos.consola.RitmoConsola(org.viajedragos.consola.RitmoConsola.Velocidad.INSTANTANEA),
                TextoConsola.sinBotinEquipamiento(),
                TextoConsola.sinBotinEquipamiento(),
                new org.viajedragos.consola.TiempoAventura(),
                TextoConsola.sinBotinEquipamiento())));
        dados.comprobarConsumo();
        assertEquals(20, j.getVida());
        assertTrue(texto().contains("el combate queda sin terminar"));
        assertFalse(texto().contains("Has escapado."));
        assertFalse(texto().contains("Victoria."));
        assertFalse(texto().contains("Derrota."));
        assertEquals(1, texto().split("EXPLORACIÓN", -1).length - 1);
        assertFalse(texto().contains("Has encontrado"));
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
            assertTrue(usadas < valores.length, "Tirada o encuentro inesperado");
            antes.accept(usadas);
            return valores[usadas++];
        }

        private void comprobarConsumo() {
            assertEquals(valores.length, usadas, "Consumo exacto de dados");
        }
    }
}
