package org.viajedragos;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.RepositorioEnemigosMemoria;
import org.viajedragos.combate.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.modelo.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EquilibrioVariantesTest {
    @ParameterizedTest
    @ValueSource(ints = {4, 5, 6})
    @Timeout(20)
    void asequiblesYCortosAlNivelDeDesbloqueoSinPocionesInclusoSinArma(int indice) {
        var definicion = new RepositorioEnemigosMemoria().obtenerTodos().get(indice);
        // Tres repartos extremos, con/sin arma. Vida completa al inicio de cada muestra.
        for (Atributo reparto : List.of(Atributo.FUERZA, Atributo.RESISTENCIA, Atributo.VELOCIDAD)) {
            for (int bono : new int[]{0, 2}) {
                int victorias = 0, turnosTotales = 0, vidaRestante = 0;
                for (int muestra = 0; muestra < 1000; muestra++) {
                    Jugador j = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 48, 48);
                    while (j.getNivel() < definicion.nivel()) j.agregarExperiencia((int) j.getExperienciaNecesaria());
                    while (j.getPuntosPendientes() > 0) assertTrue(j.asignarPunto(reparto));
                    if (bono != 0) assertTrue(j.equipar(new Arma("Arma de prueba", TipoEquipo.ARMA,
                            Map.of(Atributo.FUERZA, bono), Map.of())));
                    int[] turnos = {0}, tiradas = {0};
                    j.aprenderHabilidad(new Habilidad("Observación de turnos") {
                        public void alInicioTurno(Combatiente p, Combatiente o, EstadoHabilidad estado) { turnos[0]++; }
                    }, 1);
                    Random dados = new Random(20261018L + muestra);
                    ResultadoCombate resultado = new Combate(j, definicion.crearEnemigo(), () -> {
                        assertTrue(++tiradas[0] <= 500, "Combate de muestra demasiado largo");
                        return dados.nextInt(1, 7);
                    }).combatir();
                    turnosTotales += turnos[0];
                    if (resultado == ResultadoCombate.ENEMIGO_MUERE) { victorias++; vidaRestante += j.getVida(); }
                    assertTrue(j.getInventarioPociones().isEmpty());
                }
                System.out.printf(Locale.ROOT,
                        "Variante: %s, nivel=%d, reparto=%s, arma=+%d, victorias=%d/1000, turnos=%.3f, vida=%.2f%n",
                        definicion.nombre(), definicion.nivel(), reparto, bono, victorias,
                        turnosTotales / 1000.0, vidaRestante / (double) victorias);
                assertTrue(victorias >= 950, "Al menos 95 % con vida completa y sin consumibles");
                assertTrue(turnosTotales / 1000.0 < 5, "Media inferior a cinco turnos");
            }
        }
    }
}
