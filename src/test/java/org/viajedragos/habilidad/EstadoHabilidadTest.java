package org.viajedragos.habilidad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.combate.Combate;
import org.viajedragos.combate.CombateTest;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.implementacion.GolpeCertero;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EstadoHabilidadTest {

    private Jugador jugador() {
        return new Jugador("Jugador", "", 10, 0, 1, 3, 1, 100, 100);
    }

    @Test
    void constructorVacioProduceUnEstadoInactivoConNivelPositivo() {
        EstadoHabilidad estado = new EstadoHabilidad();
        assertEquals(1, estado.getNivel());
        assertFalse(estado.isActiva());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void constructorYSetterRechazanNivelesNoPositivos(int nivel) {
        assertThrows(IllegalArgumentException.class, () -> new EstadoHabilidad(true, 0, nivel));
        EstadoHabilidad estado = new EstadoHabilidad(true, 0, 5);
        assertThrows(IllegalArgumentException.class, () -> estado.setNivel(nivel));
        assertEquals(5, estado.getNivel());
    }

    @ParameterizedTest
    @CsvSource({"true,0,true", "false,1,true", "true,2,true", "false,0,false"})
    void actividadDependeDeAprendizajeOEquipo(boolean aprendida, int equipos, boolean activa) {
        assertEquals(activa, new EstadoHabilidad(aprendida, equipos, 3).isActiva());
    }

    @Test
    void noSePermitenContadoresDeEquipoNegativos() {
        assertThrows(IllegalArgumentException.class, () -> new EstadoHabilidad(false, -1, 1));
        EstadoHabilidad estado = new EstadoHabilidad(false, 1, 2);
        assertThrows(IllegalArgumentException.class, () -> estado.setVecesEquipada(-1));
        assertEquals(1, estado.getVecesEquipada());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void aprendizajeYEquipoRechazanAportacionesNoPositivasSinModificarEstado(int niveles) {
        Jugador jugador = jugador();
        jugador.aprenderHabilidad(new GolpeCertero(), 3);
        EstadoHabilidad estado = jugador.getHabilidades().get(new GolpeCertero());
        assertThrows(IllegalArgumentException.class, () -> jugador.aprenderHabilidad(new GolpeCertero(), niveles));
        assertThrows(IllegalArgumentException.class, () -> jugador.agregarHabilidadEquipo(new GolpeCertero(), niveles));
        assertThrows(IllegalArgumentException.class,
                () -> new Arma("Arma", TipoEquipo.ARMA, Map.of(), Map.of(new GolpeCertero(), niveles)));
        assertEquals(3, estado.getNivel());
        assertEquals(0, estado.getVecesEquipada());
        assertTrue(estado.isAprendida());
    }

    @Test
    void ultimaContribucionSeEliminaSinAsignarNivelCero() {
        Jugador jugador = jugador();
        jugador.agregarHabilidadEquipo(new GolpeCertero(), 2);
        EstadoHabilidad estado = jugador.getHabilidades().get(new GolpeCertero());
        jugador.quitarHabilidadEquipo(new GolpeCertero(), 2);
        assertFalse(jugador.getHabilidades().containsKey(new GolpeCertero()));
        assertFalse(estado.isActiva());
        assertTrue(estado.getNivel() > 0);
    }

    @Test
    void noSePuedeDejarUnaHabilidadAprendidaConNivelCero() {
        Jugador jugador = jugador();
        EstadoHabilidad estado = new EstadoHabilidad(true, 1, 2);
        jugador.getHabilidades().put(new GolpeCertero(), estado);
        assertThrows(IllegalStateException.class, () -> jugador.quitarHabilidadEquipo(new GolpeCertero(), 2));
        assertEquals(2, estado.getNivel());
        assertEquals(1, estado.getVecesEquipada());
    }

    @Test
    void curarseSoloDisparaHabilidadesActivasConSuEstado() {
        Jugador jugador = jugador();
        Registro registro = new Registro();
        EstadoHabilidad estado = new EstadoHabilidad(false, 0, 2);
        jugador.getHabilidades().put(registro, estado);
        jugador.curarse(1);
        assertEquals(0, registro.curaciones);
        estado.setAprendida(true);
        jugador.curarse(1);
        assertEquals(1, registro.curaciones);
        assertSame(estado, registro.recibido);
    }

    @Test
    void ataqueNoEjecutaEntradasInactivasYLasEquipadaSi() {
        Jugador jugador = jugador();
        Enemigo enemigo = new Enemigo("Enemigo", "", 5, 4, 1, 2, 1, 100, 100);
        Registro registro = new Registro();
        EstadoHabilidad estado = new EstadoHabilidad(false, 0, 2);
        jugador.getHabilidades().put(registro, estado);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);
        assertEquals(91, enemigo.getVida());
        assertEquals(0, registro.ataques);
        estado.setVecesEquipada(1);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);
        assertEquals(61, enemigo.getVida());
        assertEquals(1, registro.ataques);
        assertSame(estado, registro.recibido);
    }

    private static class Registro extends Habilidad {
        private int curaciones;
        private int ataques;
        private EstadoHabilidad recibido;

        private Registro() {
            super("Registro de prueba");
        }

        @Override
        public void alCurarse(Combatiente combatiente, EstadoHabilidad estado) {
            curaciones++;
            recibido = estado;
        }

        @Override
        public void antesDeAplicarDanio(ContextoAtaque contexto, EstadoHabilidad estado) {
            ataques++;
            recibido = estado;
            contexto.setDanioFinal(30);
        }
    }
}
