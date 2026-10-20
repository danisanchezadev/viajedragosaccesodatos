package org.viajedragos;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Atributo;
import org.viajedragos.catalogo.RepositorioEnemigosMemoria;
import org.viajedragos.catalogo.RepositorioObjetosMemoria;
import org.viajedragos.combate.Combate;
import org.viajedragos.combate.ResultadoCombate;
import org.viajedragos.modelo.Jugador;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Muestra fija de encuentros aislados: no simula pociones, botín ni supervivencia de una aventura. */
class EquilibrioPlantasTest {
    @Test void rutaMinimaIlustrativaDesbloqueaAbismoANivelCincoConCientoCincuentaYCincoMonedas() {
        var catalogo = new RepositorioEnemigosMemoria().obtenerTodos();
        var j = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 48, 48);
        var progreso = new org.viajedragos.modelo.ProgresoAventura();
        String[][] ruta = {{"rata_cavernaria", "rata_cavernaria", "rata_cavernaria"}, {"capitan_barracones"},
                {"momia", "arana_cripta", "espectro"}, {"salamandra", "herrero_maldito", "centinela_igneo"}};
        int encuentro = 0;
        for (int planta = 0; planta < ruta.length; planta++) {
            for (String id : ruta[planta]) {
                var enemigo = catalogo.stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
                assertTrue(progreso.registrarVictoria(++encuentro, enemigo));
                j.agregarOro(org.viajedragos.modelo.Planta.porNumero(enemigo.planta()).oroVictoria());
                j.agregarExperiencia(enemigo.experiencia());
            }
            assertEquals(new int[]{1, 2, 4, 5}[planta], j.getNivel());
            assertTrue(progreso.bajar());
        }
        assertEquals(5, progreso.plantaActual().numero()); assertEquals(5, j.getNivel());
        assertEquals(34, j.getExperiencia()); assertEquals(155, j.getOro());
        assertEquals(8, j.getPuntosPendientes()); assertEquals(72, j.getVidaMaxima());
    }
    @ParameterizedTest @ValueSource(ints = {1, 2, 3, 4, 5})
    void medirDatosRealesSinEscalado(int planta) {
        int ruta = new int[]{1, 1, 2, 4, 6}[planta - 1];
        int preparado = new int[]{1, 3, 5, 8, 11}[planta - 1];
        medir(planta, ruta, "ruta-sin-equipo", List.of());
        medir(planta, preparado, "entrenado-sin-equipo", List.of());
        var basicos = new RepositorioObjetosMemoria().obtenerBotinEquipamiento().stream()
                .filter(d -> d.calidad() == org.viajedragos.modelo.CalidadEquipo.BASICO)
                .map(org.viajedragos.modelo.DefinicionBotinEquipo::equipo).toList();
        medir(planta, preparado, "entrenado-basico", basicos);
        var mejorados = new RepositorioObjetosMemoria().obtenerBotinEquipamiento().stream()
                .filter(d -> d.calidad() == org.viajedragos.modelo.CalidadEquipo.MEJORADO)
                .map(org.viajedragos.modelo.DefinicionBotinEquipo::equipo)
                .filter(d -> !d.id().equals("espada_hierro")).toList();
        var superiores = new RepositorioObjetosMemoria().obtenerBotinEquipamiento().stream()
                .filter(d -> d.calidad() == org.viajedragos.modelo.CalidadEquipo.SUPERIOR)
                .map(org.viajedragos.modelo.DefinicionBotinEquipo::equipo).toList();
        medir(planta, preparado, "entrenado-mejorado", mejorados);
        medir(planta, preparado, "entrenado-superior", superiores);
    }

    private void medir(int planta, int nivel, String perfil,
                       List<org.viajedragos.modelo.DefinicionEquipo> piezas) {
        var enemigos = new RepositorioEnemigosMemoria().obtenerTodos().stream()
                .filter(d -> d.planta() == planta).toList();
        assertEquals(5, enemigos.size());
        int victorias = 0, danio = 0, tiradas = 0;
        for (var definicion : enemigos) {
            int victoriasAntes = victorias, danioAntes = danio;
            for (int muestra = 0; muestra < 200; muestra++) {
                var j = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 48, 48);
                int experiencia = 0;
                for (int n = 2; n <= nivel; n++) experiencia += n * 10;
                if (experiencia > 0) j.agregarExperiencia(experiencia);
                while (j.getPuntosPendientes() > 0) {
                    assertTrue(j.asignarPunto(j.getPuntosPendientes() % 2 == 0 ? Atributo.FUERZA : Atributo.RESISTENCIA));
                }
                for (var pieza : piezas) assertTrue(j.equipar(pieza.crearEquipo(0)));
                j.setVida(j.getVidaMaxima());
                var e = definicion.crearEnemigo();
                assertEquals(definicion.fuerza(), e.getFuerza(), "El jugador no escala al enemigo");
                assertEquals(definicion.vidaMaxima(), e.getVidaMaxima());
                int[] llamadas = {0};
                var azar = new Random(20261020L + planta * 10000L + enemigos.indexOf(definicion) * 200L + muestra);
                var resultado = new Combate(j, e, () -> {
                    assertTrue(++llamadas[0] < 10000, "El encuentro debe terminar");
                    return azar.nextInt(6) + 1;
                }).combatir();
                if (resultado == ResultadoCombate.ENEMIGO_MUERE) victorias++;
                danio += j.getVidaMaxima() - j.getVida();
                tiradas += llamadas[0];
                assertTrue(j.getVida() >= 0 && j.getVida() <= j.getVidaMaxima());
            }
            if (perfil.equals("ruta-sin-equipo")) System.out.printf(Locale.ROOT,
                    "  Ruta %s: victorias=%d/200; PS perdidos=%.2f%n", definicion.id(),
                    victorias - victoriasAntes, (danio - danioAntes) / 200.0);
        }
        System.out.printf(Locale.ROOT,
                "Plantas: p%d N%d %s: victorias=%d/1000; PS perdidos/encuentro=%.2f; dados/encuentro=%.2f%n",
                planta, nivel, perfil, victorias, danio / 1000.0, tiradas / 1000.0);
    }
}
