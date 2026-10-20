package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.catalogo.*;
import org.viajedragos.arma.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BotinEquipamientoTest {
    private final RepositorioObjetosMemoria objetos = new RepositorioObjetosMemoria();
    private BotinEquipamiento botin() {
        return new BotinEquipamiento(RepositorioObjetos.validarBotin(objetos.obtenerBotinEquipamiento(), objetos.obtenerArmas(), objetos.obtenerEquipamiento()));
    }
    private static class Tiradas extends Random {
        private final int[] limites, valores;
        private int usadas;
        Tiradas(int[] limites, int... valores) { super(0); this.limites = limites; this.valores = valores; }
        @Override public int nextInt(int limite) {
            assertTrue(usadas < valores.length, "No se permiten sorteos ocultos");
            assertEquals(limites[usadas], limite); int valor = valores[usadas++];
            assertTrue(valor >= 0 && valor < limite); return valor;
        }
        void agotadas() { assertEquals(valores.length, usadas); }
    }

    @Test void veinteResultadosExactosYLimitesDeProbabilidadSinSeleccionTrasFallo() {
        int entregas = 0;
        for (int dado = 0; dado < 100; dado++) {
            Tiradas azar = dado < 20 ? new Tiradas(new int[]{100, 10, 7, 2}, dado, 0, 0, 0)
                    : new Tiradas(new int[]{100}, dado);
            var resultado = botin().sortear(1, 1, azar);
            assertEquals(dado < 20, resultado.isPresent()); if (resultado.isPresent()) entregas++;
            azar.agotadas();
        }
        assertEquals(20, entregas);
    }

    @ParameterizedTest @CsvSource({"1,1,10,7,2,0,1", "2,3,14,7,3,0,2", "3,5,6,8,2,1,2", "4,8,17,8,3,1,3", "5,11,11,7,2,2,3"})
    void rangosDeMejorasExactosSinAplicarlasNiAzarAdicional(int planta, int nivel, int peso, int definiciones,
                                                         int rango, int minimo, int maximo) {
        for (int valor = 0; valor < rango; valor++) {
            var azar = new Tiradas(new int[]{100, peso, definiciones, rango}, 0, 0, 0, valor);
            Equipo equipo = botin().sortear(planta, nivel, azar).orElseThrow(); azar.agotadas();
            assertEquals(minimo + valor, equipo.getMejorasDisponibles());
            assertTrue(equipo.getMejorasDisponibles() <= maximo);
            assertEquals(0, equipo.getMejorasConsumidas()); assertTrue(equipo.getBonificacionesAdicionales().isEmpty());
            assertEquals(equipo.getBonificacionesBase(), equipo.getBonificaciones()); assertTrue(equipo.getHabilidadesOtorgadas().isEmpty());
        }
    }

    @ParameterizedTest @CsvSource({"1,12", "3,14", "4,15", "8,19", "12,23", "99,23"})
    void nivelDelEnemigoPonderaCalidadSinCambiarElVeintePorCiento(int nivel, int total) {
        int mejorados = 0;
        for (int calidad = 0; calidad < total; calidad++) {
            boolean mejorado = calidad >= 10;
            var azar = new Tiradas(new int[]{100, total, mejorado ? 6 : 7, 3}, 19, calidad, 0, 0);
            Equipo pieza = botin().sortear(2, nivel, azar).orElseThrow(); azar.agotadas();
            assertEquals(mejorado ? "espada_hierro" : "espada_desgastada", pieza.getIdDefinicion());
            if (mejorado) mejorados++;
        }
        assertEquals(total - 10, mejorados);
    }

    @Test void nivelesOchoYNueveDeLaMismaPlantaMejoranLaProbabilidadSuperiorSinGarantizarla() {
        int[] superiores = new int[2]; int[] totales = {17, 19};
        for (int caso = 0; caso < 2; caso++) {
            int corte = caso == 0 ? 9 : 10;
            for (int calidad = 0; calidad < totales[caso]; calidad++) {
                var azar = new Tiradas(new int[]{100, totales[caso], calidad < corte ? 8 : 7, 3}, 0, calidad, 0, 0);
                Equipo pieza = botin().sortear(4, 8 + caso, azar).orElseThrow(); azar.agotadas();
                if (pieza.getIdDefinicion().equals("espada_forja")) superiores[caso]++;
            }
        }
        assertEquals(8, superiores[0]); assertEquals(9, superiores[1]);
        assertTrue((double) superiores[1] / totales[1] > (double) superiores[0] / totales[0]);
    }

    @Test void probabilidadSuperiorCreceEnCadaNivelDeLaTablaSinHacerseGarantizada() {
        double anterior = 0;
        for (int nivel = 1; nivel <= 12; nivel++) {
            int total = nivel * 2 + 1, superiores = 0;
            for (int valor = 0; valor < total; valor++) {
                var azar = new Tiradas(new int[]{100, total, valor < nivel + 1 ? 8 : 7, 3}, 0, valor, 0, 0);
                Equipo pieza = botin().sortear(4, nivel, azar).orElseThrow(); azar.agotadas();
                if (pieza.getIdDefinicion().equals("espada_forja")) superiores++;
            }
            double actual = (double) superiores / total;
            assertTrue(actual > anterior && actual < 1); anterior = actual;
        }
    }

    @ParameterizedTest @ValueSource(ints = {1, 2, 3, 4, 5})
    void ningunaSeleccionSaleDeLasDefinicionesPermitidasParaLaPlanta(int planta) {
        var elegibles = objetos.obtenerBotinEquipamiento().stream().filter(d -> d.disponibleEn(planta)).toList();
        assertFalse(elegibles.isEmpty());
        for (int i = 0; i < 100; i++) {
            var resultado = botin().sortear(planta, 99, new Random(i));
            resultado.ifPresent(e -> assertTrue(elegibles.stream().anyMatch(d -> d.equipo().id().equals(e.getIdDefinicion()))));
        }
        if (planta < 4) assertTrue(elegibles.stream().noneMatch(d -> d.calidad() == CalidadEquipo.SUPERIOR));
    }

    @Test void losSieteTiposSonAlcanzablesInclusoConEnemigoDeNivelMuyAltoEnPlantaUno() {
        var tipos = EnumSet.noneOf(TipoEquipo.class);
        for (int i = 0; i < 7; i++) {
            var azar = new Tiradas(new int[]{100, 10, 7, 2}, 0, 9, i, 1);
            var pieza = botin().sortear(1, 99, azar).orElseThrow(); azar.agotadas();
            tipos.add(pieza.getTipoEquipo());
            assertTrue(objetos.obtenerBotinPorPlanta(1, CalidadEquipo.BASICO).stream().anyMatch(d -> d.equipo().id().equals(pieza.getIdDefinicion())));
        }
        assertEquals(EnumSet.allOf(TipoEquipo.class), tipos);
    }

    @Test void mismaDefinicionProducePiezasIndependientesConMejorasExplicitas() {
        var a = botin().sortear(1, 1, new Tiradas(new int[]{100, 10, 7, 2}, 0, 0, 0, 0)).orElseThrow();
        var b = botin().sortear(1, 1, new Tiradas(new int[]{100, 10, 7, 2}, 0, 0, 0, 1)).orElseThrow();
        assertInstanceOf(Arma.class, a); assertInstanceOf(Arma.class, b); assertNotSame(a, b);
        assertEquals(a.getIdDefinicion(), b.getIdDefinicion()); assertEquals(a.getNombre(), b.getNombre());
        assertEquals(0, a.getMejorasDisponibles()); assertEquals(1, b.getMejorasDisponibles());
        Jugador j = new Jugador(); j.agregarEquipo(a); j.agregarEquipo(b); j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertTrue(j.usarPergamino(Atributo.FUERZA, b));
        assertEquals(Map.of(Atributo.FUERZA, 1), a.getBonificaciones());
        assertEquals(Map.of(Atributo.FUERZA, 2), b.getBonificaciones());
        assertEquals(Map.of(Atributo.FUERZA, 1), objetos.obtenerArmas().getFirst().bonificaciones());
    }

    @ParameterizedTest @CsvSource({"1,1,BASICO,10,0,2", "2,3,BASICO,14,0,3", "2,3,MEJORADO,14,10,3",
            "3,5,MEJORADO,6,0,2", "4,8,MEJORADO,17,0,3", "4,8,SUPERIOR,17,9,3", "5,11,SUPERIOR,11,0,2"})
    void todasLasDefinicionesDeCadaPlantaYCalidadSonAlcanzables(int planta, int nivel, CalidadEquipo calidad,
                                                             int peso, int valorCalidad, int rangoMejoras) {
        var elegibles = objetos.obtenerBotinPorPlanta(planta, calidad);
        Set<String> obtenidas = new HashSet<>();
        for (int i = 0; i < elegibles.size(); i++) {
            var azar = new Tiradas(new int[]{100, peso, elegibles.size(), rangoMejoras}, 0, valorCalidad, i, 0);
            var pieza = botin().sortear(planta, nivel, azar).orElseThrow(); azar.agotadas();
            assertEquals(elegibles.get(i).equipo().id(), pieza.getIdDefinicion());
            obtenidas.add(pieza.getIdDefinicion());
        }
        assertEquals(elegibles.size(), obtenidas.size());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void catalogoVacioONoElegibleSoloHaceLaComprobacionInicial(boolean vacio) {
        var catalogo = vacio ? List.<DefinicionBotinEquipo>of() : objetos.obtenerBotinPorPlanta(5, CalidadEquipo.SUPERIOR);
        var selector = new BotinEquipamiento(catalogo); var azar = new Tiradas(new int[]{100}, 0);
        assertFalse(selector.tieneDefiniciones(1)); assertTrue(selector.sortear(1, 99, azar).isEmpty()); azar.agotadas();
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void metadatosInvalidosODuplicadosSinDefinicionCanonicaSeRechazan(int caso) {
        var equipo = objetos.obtenerArmas().getFirst().comoEquipo();
        assertThrows(RuntimeException.class, () -> {
            var entrada = new DefinicionBotinEquipo(equipo, CalidadEquipo.BASICO, 1, 2);
            List<DefinicionBotinEquipo> lista = switch (caso) {
                case 0 -> null;
                case 1 -> Arrays.asList(entrada, null);
                case 2 -> List.of(entrada, entrada);
                case 3 -> List.of(new DefinicionBotinEquipo(equipo, CalidadEquipo.BASICO, 0, 2));
                case 4 -> List.of(new DefinicionBotinEquipo(equipo, CalidadEquipo.BASICO, 2, 1));
                case 5 -> List.of(new DefinicionBotinEquipo(equipo, CalidadEquipo.SUPERIOR, 1, 5));
                case 6 -> List.of(new DefinicionBotinEquipo(new DefinicionEquipo("ajeno", "Ajeno", TipoEquipo.ARMA, Map.of()), CalidadEquipo.BASICO, 1, 1));
                case 7 -> List.of(new DefinicionBotinEquipo(new DefinicionEquipo(equipo.id(), equipo.nombre(), equipo.tipo(), Map.of(Atributo.FUERZA, 99)), CalidadEquipo.BASICO, 1, 1));
                default -> List.of(new DefinicionBotinEquipo(equipo, null, 1, 1));
            };
            RepositorioObjetos.validarBotin(lista, objetos.obtenerArmas(), objetos.obtenerEquipamiento());
        });
    }

    @Test void origenAlternativoYConsultaPorCalidadSinDependerDeIdsDeMemoria() {
        var equipo = new DefinicionEquipo("externo", "Casco externo", TipoEquipo.CASCO, Map.of(Atributo.RESISTENCIA, 7));
        RepositorioObjetos externo = new RepositorioObjetos() {
            public List<DefinicionArma> obtenerArmas() { return List.of(); }
            public List<DefinicionPocion> obtenerPociones() { return List.of(); }
            public List<DefinicionEquipo> obtenerEquipamiento() { return List.of(equipo); }
            public List<DefinicionBotinEquipo> obtenerBotinEquipamiento() { return List.of(new DefinicionBotinEquipo(equipo, CalidadEquipo.SUPERIOR, 4, 5)); }
        };
        assertTrue(externo.obtenerBotinPorPlanta(3, CalidadEquipo.SUPERIOR).isEmpty());
        var lista = RepositorioObjetos.validarBotin(externo.obtenerBotinEquipamiento(), externo.obtenerArmas(), externo.obtenerEquipamiento());
        var azar = new Tiradas(new int[]{100, 8, 1, 3}, 0, 0, 0, 2);
        Equipo pieza = new BotinEquipamiento(lista).sortear(4, 8, azar).orElseThrow(); azar.agotadas();
        assertEquals("externo", pieza.getIdDefinicion()); assertEquals(equipo.bonificaciones(), pieza.getBonificaciones());
        assertEquals(3, pieza.getMejorasDisponibles());
        assertThrows(UnsupportedOperationException.class, lista::clear);
        assertThrows(UnsupportedOperationException.class, () -> externo.obtenerBotinPorPlanta(4, CalidadEquipo.SUPERIOR).clear());
    }
}
