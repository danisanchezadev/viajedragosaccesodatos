package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.catalogo.RepositorioEnemigos;
import org.viajedragos.catalogo.RepositorioObjetos;
import org.viajedragos.catalogo.RepositorioObjetosMemoria;
import org.viajedragos.combate.Combate;
import org.viajedragos.combate.ResultadoCombate;
import org.viajedragos.combate.TiradorDado;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CatalogoObjetosIntegracionTest {
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private PrintStream consola() { return new PrintStream(salida, true, StandardCharsets.UTF_8); }
    private String texto() { return salida.toString(StandardCharsets.UTF_8); }
    private final RepositorioEnemigos enemigos = () -> List.of(
            new DefinicionEnemigo("lobo", "Lobo", "", 5, 2, 2, 1, 2, 18));

    private List<DefinicionPocion> pociones() {
        return List.of(new DefinicionPocion("pequena", TipoPocion.PEQUENA, "Pequeña del catálogo", 10),
                new DefinicionPocion("grande", TipoPocion.GRANDE, "Grande del catálogo", 20));
    }
    private List<DefinicionArma> armas() {
        return List.of(new DefinicionArma(DefinicionArma.ID_ESPADA_DESGASTADA, "Espada del catálogo",
                Map.of(Atributo.FUERZA, 3, Atributo.VELOCIDAD, -1)));
    }
    private RepositorioObjetos repositorio(List<DefinicionArma> armas, List<DefinicionPocion> pociones) {
        return new RepositorioObjetos() {
            public List<DefinicionArma> obtenerArmas() { return armas; }
            public List<DefinicionPocion> obtenerPociones() { return pociones; }
        };
    }
    private Jugador jugador(int vida) {
        return new Jugador("Jugador", "", 10, 0, 3, 0, 0, vida, 30, pociones());
    }
    private TiradorDado dados(int... valores) {
        int[] usadas = {0};
        return () -> {
            assertTrue(usadas[0] < valores.length, "No se deben consumir dados adicionales");
            return valores[usadas[0]++];
        };
    }
    private Random sinAzar() {
        return new Random(0) {
            @Override public int nextInt(int limite) { fail("No debe consultarse el azar"); return 0; }
        };
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16})
    void catalogoInvalidoSeRechazaSinIniciarNiCambiarAlJugador(int caso) {
        var inicial = new RepositorioObjetosMemoria();
        List<DefinicionArma> armas = inicial.obtenerArmas();
        List<DefinicionPocion> pociones = inicial.obtenerPociones();
        switch (caso) {
            case 0 -> armas = null;
            case 1 -> pociones = null;
            case 2 -> armas = Arrays.asList((DefinicionArma) null);
            case 3 -> pociones = Arrays.asList(pociones.getFirst(), null);
            case 4 -> armas = List.of();
            case 5 -> pociones = List.of(pociones.getFirst());
            case 6 -> pociones = List.of(pociones.get(1));
            case 7 -> armas = List.of(armas.getFirst(), armas.getFirst());
            case 8 -> pociones = List.of(pociones.getFirst(), pociones.getFirst(), pociones.get(1));
            case 9 -> pociones = List.of(pociones.getFirst(), new DefinicionPocion("otra", TipoPocion.PEQUENA, "Otra", 8), pociones.get(1));
            case 10 -> pociones = List.of(new DefinicionPocion(DefinicionArma.ID_ESPADA_DESGASTADA,
                    TipoPocion.PEQUENA, "Poción", 8), pociones.get(1));
            case 11 -> pociones = List.of(new DefinicionPocion("repetida", TipoPocion.PEQUENA, "Poción", 8),
                    new DefinicionPocion("repetida", TipoPocion.GRANDE, "Poción", 16));
            case 12 -> armas = List.of(new DefinicionArma("otro_id", "Espada desgastada", Map.of(Atributo.FUERZA, 1)));
        }
        RepositorioObjetos objetos = repositorio(armas, pociones);
        if (caso == 13) objetos = null;
        if (caso == 14) objetos = new RepositorioObjetos() {
            public List<DefinicionArma> obtenerArmas() {
                return List.of(new DefinicionArma("", "Inválida", Map.of()));
            }
            public List<DefinicionPocion> obtenerPociones() { fail("No debe continuar"); return null; }
        };
        if (caso == 15 || caso == 16) objetos = new RepositorioObjetos() {
            public List<DefinicionArma> obtenerArmas() { return inicial.obtenerArmas(); }
            public List<DefinicionPocion> obtenerPociones() {
                return List.of(new DefinicionPocion("invalida", TipoPocion.PEQUENA, "Inválida",
                        caso == 15 ? 0 : -1));
            }
        };
        Jugador j = jugador(10);
        var anterior = j.getDefinicionPocion(TipoPocion.PEQUENA);
        j.agregarPocion(TipoPocion.PEQUENA);
        assertTrue(app.jugarAventura(j, new Scanner(""), consola(), () -> {
            fail("No debe empezar el combate"); return 1;
        }, sinAzar(), enemigos, sinAzar(), objetos));
        assertTrue(texto().contains("catálogo de objetos no es válido"));
        assertFalse(texto().contains("Encuentro 1."));
        assertEquals(10, j.getVida());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertSame(anterior, j.getDefinicionPocion(TipoPocion.PEQUENA));
        assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @Test
    void catalogoInvalidoRegresaAlMenuInicial() {
        app.ejecutar(new Scanner("1\n0\n"), consola(), () -> {
            fail("Sin combate"); return 1;
        }, sinAzar(), enemigos, sinAzar(), repositorio(List.of(), pociones()));
        assertEquals(2, texto().split("1. Nueva aventura", -1).length - 1);
        assertTrue(texto().contains("Hasta pronto."));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void espadaAlternativaSeOfreceUnaVezYSeConservaSinDuplicar(boolean aceptar) {
        Jugador j = new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 48, 48);
        var armasOriginales = new ArrayList<>(armas());
        var pocionesOriginales = new ArrayList<>(pociones());
        int[] consultas = {0,0};
        RepositorioObjetos repo = new RepositorioObjetos() {
            public List<DefinicionArma> obtenerArmas() { consultas[0]++; return armasOriginales; }
            public List<DefinicionPocion> obtenerPociones() { consultas[1]++; return pocionesOriginales; }
        };
        int[] encuentros = {0};
        j.aprenderHabilidad(new Habilidad("Observación") {
            @Override public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
                assertSame(j, portador);
                if (encuentros[0]++ > 0) {
                    assertEquals(aceptar ? 9 : 6, j.getFuerza());
                    assertEquals(aceptar ? 1 : 2, j.getVelocidad());
                }
                armasOriginales.clear();
                pocionesOriginales.clear();
            }
        }, 1);
        Random botin = new Random(0) {
            int llamadas;
            @Override public int nextInt(int limite) {
                assertEquals(100, limite);
                assertTrue(llamadas++ < 4);
                return 0;
            }
        };
        assertFalse(app.jugarAventura(j, new Scanner("1\n" + (aceptar ? "1" : "2") + "\n3\n1\n0\n"), consola(),
                dados(1,1,1,1,6,1,6, 1,1,1,1,6,1,6), botin, enemigos, new Random(0), repo));
        assertArrayEquals(new int[]{1,1}, consultas);
        assertEquals(2, encuentros[0]);
        assertEquals(1, texto().split("Has encontrado una Espada del catálogo", -1).length - 1);
        assertTrue(texto().contains("Fuerza +3"));
        assertTrue(texto().contains("Velocidad -1"));
        assertTrue(texto().contains("Botín: Pequeña del catálogo (+1)."));
        assertEquals(2, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(2, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(aceptar ? 9 : 6, j.getFuerza());
        assertEquals(aceptar ? 1 : 0, j.getEquipoEquipado().size());
    }

    @Test
    void configuracionEsIndependienteYUnRechazoNoReemplazaLaAnterior() {
        var original = new ArrayList<>(pociones());
        Jugador j = new Jugador("Prueba", "", 6,2,3,2,2,10,48,original);
        original.clear();
        j.agregarPocion(TipoPocion.PEQUENA);
        var anterior = j.getDefinicionPocion(TipoPocion.PEQUENA);
        assertThrows(IllegalArgumentException.class, () -> j.configurarPociones(List.of(anterior)));
        assertSame(anterior, j.getDefinicionPocion(TipoPocion.PEQUENA));
        assertEquals(10, j.usarPocion(TipoPocion.PEQUENA));
        assertEquals(20, j.getVida());
        assertEquals(0, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(8, new Jugador().getDefinicionPocion(TipoPocion.PEQUENA).recuperacionPS());
        assertEquals(16, new Jugador().getDefinicionPocion(TipoPocion.GRANDE).recuperacionPS());
        assertEquals(8, TipoPocion.PEQUENA.getRecuperacionPS());
        assertEquals(16, TipoPocion.GRANDE.getRecuperacionPS());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void exploracionMuestraYConsumeLaDefinicionEfectiva(boolean parcial) {
        Jugador j = jugador(parcial ? 28 : 10);
        j.agregarPocion(TipoPocion.PEQUENA);
        int[] eventos = {0};
        j.aprenderHabilidad(new Habilidad("Curación") {
            @Override public void alCurarse(Combatiente c, EstadoHabilidad estado) {
                assertSame(j, c);
                assertSame(estado, j.getHabilidades().get(this));
                assertEquals(2, estado.getNivel());
                eventos[0]++;
            }
        }, 2);
        assertTrue(app.consultarInventario(j, new Scanner("0\n"), consola()));
        assertTrue(texto().contains("Pequeña del catálogo"));
        assertTrue(texto().contains("Recupera 10 PS"));
        assertTrue(app.usarObjeto(j, new Scanner("1\n"), consola()));
        assertEquals(parcial ? 30 : 20, j.getVida());
        assertEquals(0, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(1, eventos[0]);
        assertTrue(texto().contains("1. Pequeña del catálogo (x1) +10 PS"));
        assertTrue(texto().contains("Vida recuperada: +" + (parcial ? 2 : 10) + " PS"));
        assertFalse(texto().contains("Poción pequeña"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void combateMuestraYAplicaCatalogoSinCambiarAccionesNiDados(boolean grande) {
        Jugador j = jugador(10);
        TipoPocion tipo = grande ? TipoPocion.GRANDE : TipoPocion.PEQUENA;
        j.agregarPocion(tipo);
        var enemigo = new Enemigo("Enemigo", "", 6,0,1,0,0,10,30);
        int[] ataques = {0};
        j.aprenderHabilidad(new Habilidad("Ataques") {
            @Override public void antesDeAtacar(org.viajedragos.combate.ContextoAtaque c, EstadoHabilidad e) {
                ataques[0]++;
            }
        }, 1);
        int[] valores = {1,1,1,1,4,2,1,6,4,2};
        int[] usadas = {0};
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(j, enemigo, () -> {
            assertTrue(usadas[0] < valores.length);
            return valores[usadas[0]++];
        }).combatir(new Scanner("2\n" + (grande ? "2" : "1") + "\n1\n"), consola()));
        assertEquals(valores.length, usadas[0]);
        assertEquals(grande ? 22 : 12, j.getVida());
        assertEquals(0, j.getCantidadPociones(tipo));
        assertEquals(1, ataques[0]);
        assertTrue(texto().contains((grande ? "Grande" : "Pequeña") + " del catálogo (x1) +" + (grande ? 20 : 10) + " PS"));
        assertTrue(texto().contains("Vida recuperada: +" + (grande ? 20 : 10) + " PS"));
    }

    @Test
    void vidaCompletaNoConsumeNiDisparaCuracionConCatalogoAlternativo() {
        Jugador j = jugador(30);
        j.agregarPocion(TipoPocion.PEQUENA);
        j.aprenderHabilidad(new Habilidad("No curar") {
            @Override public void alCurarse(Combatiente c, EstadoHabilidad e) { fail("Sin curación"); }
        },1);
        assertTrue(app.usarObjeto(j, new Scanner("1\n0\n"), consola()));
        assertEquals(30, j.getVida());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertTrue(texto().contains("La vida está completa"));
    }

    @Test
    void nuevaAventuraRecargaElCatalogoYNoArrastraInventarioNiEquipo() {
        int[] consultas = {0,0};
        RepositorioObjetos repo = new RepositorioObjetos() {
            public List<DefinicionArma> obtenerArmas() { consultas[0]++; return armas(); }
            public List<DefinicionPocion> obtenerPociones() { consultas[1]++; return pociones(); }
        };
        Random botin = new Random(0) {
            @Override public int nextInt(int limite) { return 0; }
        };
        app.ejecutar(new Scanner("1\n1\n1\n4\n1\n1\n2\n1\n0\n0\n"), consola(),
                dados(1,1,1,1,6,1,6, 1,1,1,1,6,1,6), botin, enemigos, new Random(0), repo);
        assertArrayEquals(new int[]{2,2}, consultas);
        assertEquals(2, texto().split("Aventurero: 48/48", -1).length - 1);
        assertTrue(texto().contains("No tienes equipo equipado"));
        assertTrue(texto().contains("Pequeña del catálogo     x1"));
        assertFalse(texto().contains("x2"));
    }

    @Test
    void salirSinAventuraNoConsultaObjetos() {
        RepositorioObjetos repo = new RepositorioObjetos() {
            public List<DefinicionArma> obtenerArmas() { fail("Sin consulta"); return null; }
            public List<DefinicionPocion> obtenerPociones() { fail("Sin consulta"); return null; }
        };
        app.ejecutar(new Scanner("0\n"), consola(), () -> { fail("Sin dados"); return 1; },
                sinAzar(), enemigos, sinAzar(), repo);
        assertTrue(texto().contains("Hasta pronto."));
    }

    @Test
    void combateConCuracionParcialRespetaMaximoYDisparaUnaVezElEstadoPropio() {
        Jugador j = jugador(28);
        j.agregarPocion(TipoPocion.GRANDE);
        int[] eventos = {0};
        j.aprenderHabilidad(new Habilidad("Curación de combate") {
            @Override public void alCurarse(Combatiente c, EstadoHabilidad estado) {
                assertSame(j, c);
                assertSame(estado, j.getHabilidades().get(this));
                assertEquals(3, estado.getNivel());
                assertEquals(30, j.getVida());
                eventos[0]++;
            }
        }, 3);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(j,
                new Enemigo("Enemigo", "",6,0,1,0,0,10,30),
                dados(1,1,1,1,4,2,1,6,4,2)).combatir(new Scanner("2\n2\n1\n"), consola()));
        assertEquals(1, eventos[0]);
        assertEquals(22, j.getVida());
        assertEquals(0, j.getCantidadPociones(TipoPocion.GRANDE));
        assertTrue(texto().contains("Grande del catálogo (x1) +20 PS"));
        assertTrue(texto().contains("Vida recuperada: +2 PS"));
    }

    @Test
    void modoAutomaticoNoUsaPocionesDelCatalogo() {
        Jugador j = jugador(10);
        j.agregarPocion(TipoPocion.PEQUENA);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(j,
                new Enemigo("Enemigo", "",6,0,1,0,0,10,30), dados(1,1,1,1,4,2)).combatir());
        assertEquals(10, j.getVida());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
    }

    @Test
    void habilidadRealDeCuracionRestauraSuEfectoSinRetirarElArmaDelCatalogo() {
        Jugador j = new Jugador("Prueba", "",6,2,3,2,2,5,24,pociones());
        var instinto = new org.viajedragos.habilidad.implementacion.InstintoSupervivencia();
        j.aprenderHabilidad(instinto, 1);
        var estado = j.getHabilidades().get(instinto);
        var arma = armas().getFirst().crearArma();
        assertTrue(j.equipar(arma));
        instinto.alInicioCombate(j, new Enemigo(), estado);
        assertEquals(12, j.getFuerza());
        j.agregarPocion(TipoPocion.PEQUENA);
        assertEquals(10, j.usarPocion(TipoPocion.PEQUENA));
        assertEquals(15, j.getVida());
        assertEquals(9, j.getFuerza());
        instinto.alFinalCombate(j, new Enemigo(), estado);
        assertEquals(9, j.getFuerza());
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertSame(arma, j.getEquipoEquipado().get(TipoEquipo.ARMA));
    }
}
