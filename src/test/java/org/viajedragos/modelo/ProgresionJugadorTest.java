package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.RepositorioEnemigosMemoria;
import org.viajedragos.habilidad.*;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProgresionJugadorTest {
    private Jugador jugador() { return new Jugador("Prueba", "", 6, 2, 3, 2, 2, 10, 48); }

    @Test
    void nivelUnoSinExperienciaNiPuntosEnAmbosConstructores() {
        for (Jugador j : List.of(jugador(), new Jugador())) {
            assertEquals(1, j.getNivel());
            assertEquals(0, j.getExperiencia());
            assertEquals(20, j.getExperienciaNecesaria());
            assertEquals(0, j.getPuntosPendientes());
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1,1,20,0", "19,1,19,20,0", "20,2,0,30,2", "21,2,1,30,2",
            "49,2,29,30,2", "50,3,0,40,4", "89,3,39,40,4", "90,4,0,50,6", "97,4,7,50,6"})
    void umbralesSobranteYSubidasMultiples(int exp, int nivel, int resto, int siguiente, int puntos) {
        Jugador j = jugador();
        assertEquals(nivel - 1, j.agregarExperiencia(exp));
        assertEquals(nivel, j.getNivel());
        assertEquals(resto, j.getExperiencia());
        assertEquals(siguiente, j.getExperienciaNecesaria());
        assertEquals(puntos, j.getPuntosPendientes());
        assertEquals(48 + 6 * (nivel - 1), j.getVidaMaxima());
        assertEquals(10 + 6 * (nivel - 1), j.getVida());
    }

    @Test
    void recompensasPequenasSeAcumulanSinPerderExperiencia() {
        Jugador j = jugador();
        assertEquals(0, j.agregarExperiencia(5));
        assertEquals(0, j.agregarExperiencia(7));
        assertEquals(1, j.agregarExperiencia(10));
        assertEquals(2, j.getExperiencia());
        assertEquals(1, j.agregarExperiencia(28));
        assertEquals(3, j.getNivel());
        assertEquals(0, j.getExperiencia());
        assertEquals(4, j.getPuntosPendientes());
    }

    @Test
    void mejoraLosCincoAtributosBaseYLaVidaSinDuplicarVidaNiCuracion() {
        Jugador j = jugador();
        var antes = new EnumMap<Atributo, Integer>(Atributo.class);
        for (Atributo a : Atributo.values()) if (a != Atributo.VIDA) antes.put(a, j.getAtributoBase(a));
        j.aprenderHabilidad(new Habilidad("Curación no esperada") {
            @Override public void alCurarse(Combatiente c, EstadoHabilidad e) { fail("Subir nivel no usa curarse"); }
        }, 1);
        j.agregarExperiencia(90);
        antes.forEach((a, valor) -> assertEquals(valor + 3, j.getAtributoBase(a)));
        assertEquals(28, j.getVida());
        assertEquals(66, j.getVidaMaxima());
        assertTrue(j.getModificadoresEquipo().values().stream().allMatch(v -> v == 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 48, 49})
    void vidaActualAumentaSeisSinSuperarElNuevoMaximo(int vida) {
        Jugador j = jugador();
        j.setVida(vida);
        j.agregarExperiencia(20);
        assertEquals(Math.min(vida + 6, 54), j.getVida());
        assertEquals(54, j.getVidaMaxima());
    }

    @ParameterizedTest
    @EnumSource(value = Atributo.class, names = "VIDA", mode = EnumSource.Mode.EXCLUDE)
    void puedeAsignarAmbosPuntosACualquieraDeLosCincoAtributos(Atributo atributo) {
        Jugador j = jugador();
        int antes = j.getAtributoBase(atributo);
        j.agregarExperiencia(20);
        assertTrue(j.asignarPunto(atributo));
        assertEquals(1, j.getPuntosPendientes());
        assertTrue(j.asignarPunto(atributo));
        assertEquals(antes + 3, j.getAtributoBase(atributo));
        assertEquals(0, j.getPuntosPendientes());
        assertFalse(j.asignarPunto(atributo));
        assertEquals(antes + 3, j.getAtributoBase(atributo));
    }

    @Test
    void repartoEntreDosAtributosYAcumulacionDePuntosPendientes() {
        Jugador j = jugador();
        j.agregarExperiencia(20);
        assertTrue(j.asignarPunto(Atributo.FUERZA));
        j.agregarExperiencia(30);
        assertEquals(3, j.getPuntosPendientes());
        assertTrue(j.asignarPunto(Atributo.RESISTENCIA));
        assertTrue(j.asignarPunto(Atributo.AGILIDAD));
        assertTrue(j.asignarPunto(Atributo.AGILIDAD));
        assertEquals(9, j.getFuerza());
        assertEquals(5, j.getResistencia());
        assertEquals(6, j.getAgilidad());
        assertEquals(0, j.getPuntosPendientes());
    }

    @Test
    void opcionesInvalidasNoGastanPuntosNiModificanVida() {
        Jugador j = jugador();
        assertFalse(j.asignarPunto(Atributo.FUERZA));
        j.agregarExperiencia(20);
        assertFalse(j.asignarPunto(null));
        assertFalse(j.asignarPunto(Atributo.VIDA));
        assertEquals(2, j.getPuntosPendientes());
        assertEquals(16, j.getVida());
        assertEquals(54, j.getVidaMaxima());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void experienciaInvalidaNoModificaEstado(int cantidad) {
        Jugador j = jugador();
        assertThrows(IllegalArgumentException.class, () -> j.agregarExperiencia(cantidad));
        assertEquals(1, j.getNivel());
        assertEquals(0, j.getExperiencia());
        assertEquals(0, j.getPuntosPendientes());
        assertEquals(10, j.getVida());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void desbordamientoDeAtributoOVidaNoDejaSubidaParcial(int caso) {
        Jugador j = jugador();
        if (caso == 0) j.setVelocidad(Integer.MAX_VALUE);
        else j.setVidaMaxima(Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> j.agregarExperiencia(20));
        assertEquals(1, j.getNivel());
        assertEquals(0, j.getExperiencia());
        assertEquals(0, j.getPuntosPendientes());
        assertEquals(6, j.getFuerza());
        assertEquals(10, j.getVida());
    }

    @Test
    void atributoSaturadoNoConsumeUnPunto() {
        Jugador j = jugador();
        j.agregarExperiencia(20);
        j.setFuerza(Integer.MAX_VALUE);
        assertFalse(j.asignarPunto(Atributo.FUERZA));
        assertEquals(2, j.getPuntosPendientes());
    }

    @ParameterizedTest
    @ValueSource(ints = {-3, 2})
    void mejorasPermanentesSeConservanAlCambiarYRetirarEquipo(int bono) {
        Jugador j = jugador();
        Equipo arma = new Arma("Prueba", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, bono), Map.of());
        j.agregarEquipo(arma);
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
        j.agregarExperiencia(20);
        j.asignarPunto(Atributo.FUERZA);
        j.asignarPunto(Atributo.FUERZA);
        assertEquals(9, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(9 + bono, j.getFuerza());
        assertTrue(j.desequipar(TipoEquipo.ARMA));
        assertEquals(9, j.getFuerza());
        assertTrue(j.equipar(arma));
        assertEquals(9 + bono, j.getFuerza());
        assertTrue(j.desequipar(TipoEquipo.ARMA));
        assertEquals(9, j.getFuerza());
    }

    @Test
    void limpiarHabilidadTemporalNoRetiraLasMejorasDelNivel() {
        Jugador j = jugador();
        var habilidad = new InstintoSupervivencia();
        j.aprenderHabilidad(habilidad, 1);
        j.curarse(1);
        assertEquals(9, j.getFuerza());
        j.agregarExperiencia(20);
        j.asignarPunto(Atributo.FUERZA);
        j.asignarPunto(Atributo.FUERZA);
        habilidad.retirarEfectosTemporales(j, j.getHabilidades().get(habilidad));
        assertEquals(9, j.getFuerza());
    }

    @Test
    void recompensaEnDatosDelEnemigoYCompatibilidadDeConstructores() {
        var d = new DefinicionEnemigo("externo", "Otro", "", 1, 0, 1, 0, 0, 1, 93);
        assertEquals(93, d.experiencia());
        assertEquals(93, d.crearEnemigo().getExperiencia());
        assertEquals(5, new DefinicionEnemigo("viejo", "Viejo", "", 1, 0, 1, 0, 0, 1).experiencia());
        assertEquals(5, new Enemigo().getExperiencia());
        assertEquals(5, new Enemigo("Viejo", "", 1, 0, 1, 0, 0, 1, 1).getExperiencia());
        assertEquals(List.of(10, 5, 7, 6), new RepositorioEnemigosMemoria().obtenerTodos()
                .stream().map(DefinicionEnemigo::experiencia).toList());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void datosDeEnemigoRechazanExperienciaNoPositiva(int exp) {
        assertThrows(IllegalArgumentException.class, () -> new DefinicionEnemigo("id", "Nombre", "", 1, 0, 1, 0, 0, 1, exp));
        assertThrows(IllegalArgumentException.class, () -> new Enemigo("Nombre", "", 1, 0, 1, 0, 0, 1, 1, exp));
    }
}
