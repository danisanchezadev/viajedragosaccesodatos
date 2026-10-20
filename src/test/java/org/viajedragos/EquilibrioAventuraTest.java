package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.viajedragos.catalogo.RepositorioEnemigosMemoria;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.combate.Combate;
import org.viajedragos.combate.ResultadoCombate;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Jugador;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class EquilibrioAventuraTest {
    @Test
    @Timeout(30)
    void cuatroEncuentrosConSemillasFijasYEspadaDesdeElSegundo() {
        // Esta regresión mide los cuatro encuentros iniciales, sin progresión ni pociones.
        var catalogo = new CatalogoCombatePrueba().obtenerTodos().stream()
                .filter(enemigo -> enemigo.nivel() == 1).toList();
        assertEquals(4, catalogo.size());
        for (int vida : new int[]{24, 28, 32, 36, 40, 48}) {
            int exitos = 0, iniciados = 0, turnos = 0, vidaRestante = 0;
            int[] seleccionados = new int[4];
            int[] turnosPorTipo = new int[4];
            Random semillasDados = new Random(20261018L);
            Random semillasEncuentros = new Random(20261019L);
            for (int muestra = 0; muestra < 10000; muestra++) {
                Random dados = new Random(semillasDados.nextLong());
                Random encuentros = new Random(semillasEncuentros.nextLong());
                Jugador j = new Jugador("Aventurero", "",6,2,3,2,2,vida,vida);
                int[] turnosSesion = {0};
                // Observador de pruebas sin efectos sobre las reglas ni los dados.
                j.aprenderHabilidad(new Habilidad("Contador de turnos") {
                    @Override
                    public void alInicioTurno(Combatiente p, Combatiente o, EstadoHabilidad e) {
                        turnosSesion[0]++;
                    }
                }, 1);
                boolean completo = true;
                for (int n = 0; n < 4; n++) {
                    assertEquals(n == 0 ? 6 : 7, j.getFuerza());
                    int indice = encuentros.nextInt(catalogo.size());
                    seleccionados[indice]++;
                    var enemigo = catalogo.get(indice).crearEnemigo();
                    iniciados++;
                    int turnosAntes = turnosSesion[0];
                    var resultado = new Combate(j, enemigo, () -> dados.nextInt(1,7)).combatir();
                    turnosPorTipo[indice] += turnosSesion[0] - turnosAntes;
                    if (resultado != ResultadoCombate.ENEMIGO_MUERE) {
                        completo = false;
                        break;
                    }
                    if (n == 0) {
                        assertTrue(j.equipar(new Arma("Espada desgastada", TipoEquipo.ARMA,
                                Map.of(Atributo.FUERZA,1), Map.of())));
                    }
                }
                turnos += turnosSesion[0];
                assertTrue(j.getInventarioPociones().isEmpty());
                if (completo) { exitos++; vidaRestante += j.getVida(); }
            }
            System.out.printf(Locale.ROOT,
                    "Equilibrio: vida=%d, victorias=%d/10000 (%.2f%%), turnos/combate=%.3f, vida/superviviente=%.2f%n",
                    vida, exitos, exitos/100.0, turnos/(double)iniciados, vidaRestante/(double)exitos);
            for (int cantidad : seleccionados) assertTrue(cantidad > 0, "Participan los cuatro tipos");
            if (vida == 48) {
                assertTrue(exitos >= 9000, "El equilibrio aprobado debe superar el 90 % de la muestra fija");
                assertTrue(turnos / (double) iniciados < 4, "Los combates de referencia deben ser breves");
                for (int indice = 0; indice < catalogo.size(); indice++) {
                    System.out.printf(Locale.ROOT, "  %s: %d encuentros, %.3f turnos/combate%n",
                            catalogo.get(indice).nombre(), seleccionados[indice],
                            turnosPorTipo[indice] / (double) seleccionados[indice]);
                }
            }
        }
    }
}
