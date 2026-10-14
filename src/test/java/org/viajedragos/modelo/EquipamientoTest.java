package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.Equipo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.implementacion.GolpeCertero;
import org.viajedragos.habilidad.implementacion.GuardiaFerrea;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EquipamientoTest {

    private Jugador jugador() {
        return new Jugador("Jugador", "", 10, 10, 10, 10, 10, 15, 30);
    }

    private Equipo pieza(TipoEquipo tipo, Map<Atributo, Integer> atributos,
                         Map<Habilidad, Integer> habilidades) {
        return new Arma("Pieza de prueba", tipo, atributos, habilidades);
    }

    @ParameterizedTest
    @EnumSource(value = Atributo.class, names = "VIDA", mode = EnumSource.Mode.EXCLUDE)
    void equiparYDesequiparConservaBaseYRevierteCadaAtributo(Atributo atributo) {
        Jugador jugador = jugador();
        Equipo equipo = pieza(TipoEquipo.ARMA, Map.of(atributo, 4), Map.of());

        assertTrue(jugador.equipar(equipo));
        assertEquals(10, jugador.getAtributoBase(atributo));
        assertEquals(14, jugador.getAtributo(atributo));
        assertTrue(jugador.desequipar(TipoEquipo.ARMA));
        assertEquals(10, jugador.getAtributoBase(atributo));
        assertEquals(10, jugador.getAtributo(atributo));
        assertEquals(0, jugador.getModificadoresEquipo().get(atributo));
    }

    @ParameterizedTest
    @EnumSource(value = Atributo.class, names = "VIDA", mode = EnumSource.Mode.EXCLUDE)
    void penalizacionMayorQueBaseSeConservaYSeRetiraSinPerdidas(Atributo atributo) {
        Jugador jugador = jugador();
        Equipo equipo = pieza(TipoEquipo.ARMA, Map.of(atributo, -25), Map.of());

        jugador.equipar(equipo);
        assertEquals(0, jugador.getAtributo(atributo));
        assertEquals(-25, jugador.getModificadoresEquipo().get(atributo));
        assertEquals(10, jugador.getAtributoBase(atributo));
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(10, jugador.getAtributo(atributo));
    }

    @ParameterizedTest
    @EnumSource(TipoEquipo.class)
    void cadaRanuraRechazaOtraPiezaSinAplicarNingunoDeSusEfectos(TipoEquipo tipo) {
        Jugador jugador = jugador();
        Equipo actual = pieza(tipo, Map.of(Atributo.FUERZA, 3), Map.of(new GolpeCertero(), 2));
        Equipo nuevo = pieza(tipo, Map.of(Atributo.FUERZA, 20), Map.of(new GuardiaFerrea(), 9));

        assertTrue(jugador.equipar(actual));
        assertFalse(jugador.equipar(nuevo));
        assertSame(actual, jugador.getEquipoEquipado().get(tipo));
        assertEquals(13, jugador.getFuerza());
        assertEquals(1, jugador.getHabilidades().size());
        assertEquals(2, jugador.getHabilidades().get(new GolpeCertero()).getNivel());
        assertFalse(jugador.getHabilidades().containsKey(new GuardiaFerrea()));
    }

    @Test
    void sustituirExigeRetirarAntesYNoAcumulaEfectosDeLaPiezaAnterior() {
        Jugador jugador = jugador();
        Equipo actual = pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 5, Atributo.RESISTENCIA, -4),
                Map.of(new GolpeCertero(), 2));
        Equipo nuevo = pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, -3, Atributo.AGILIDAD, 2),
                Map.of(new GuardiaFerrea(), 4));

        jugador.equipar(actual);
        assertFalse(jugador.equipar(nuevo));
        assertTrue(jugador.desequipar(TipoEquipo.ARMA));
        assertTrue(jugador.equipar(nuevo));
        assertEquals(7, jugador.getFuerza());
        assertEquals(10, jugador.getResistencia());
        assertEquals(12, jugador.getAgilidad());
        assertFalse(jugador.getHabilidades().containsKey(new GolpeCertero()));
        assertEquals(4, jugador.getHabilidades().get(new GuardiaFerrea()).getNivel());
    }

    @Test
    void quitarUnaPiezaRespetaBonificacionesPositivasYNegativasDeLasOtras() {
        Jugador jugador = jugador();
        jugador.equipar(pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 7, Atributo.RESISTENCIA, -20), Map.of()));
        jugador.equipar(pieza(TipoEquipo.ARMADURA, Map.of(Atributo.FUERZA, -3, Atributo.RESISTENCIA, 4), Map.of()));

        assertEquals(14, jugador.getFuerza());
        assertEquals(0, jugador.getResistencia());
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(7, jugador.getFuerza());
        assertEquals(14, jugador.getResistencia());
        assertEquals(-3, jugador.getModificadoresEquipo().get(Atributo.FUERZA));
        jugador.desequipar(TipoEquipo.ARMADURA);
        assertEquals(10, jugador.getFuerza());
        assertEquals(10, jugador.getResistencia());
    }

    @Test
    void aumentosYDisminucionesPermanentesSobrevivenAlDesequipar() {
        Jugador jugador = jugador();
        jugador.equipar(pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 5, Atributo.AGILIDAD, -20), Map.of()));
        jugador.aumentarAtributo(Atributo.FUERZA, 3);
        jugador.disminuirAtributo(Atributo.AGILIDAD, 2);

        assertEquals(13, jugador.getAtributoBase(Atributo.FUERZA));
        assertEquals(18, jugador.getFuerza());
        assertEquals(8, jugador.getAtributoBase(Atributo.AGILIDAD));
        assertEquals(0, jugador.getAgilidad());
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(13, jugador.getFuerza());
        assertEquals(8, jugador.getAgilidad());
    }

    @Test
    void settersEscribenSoloBaseConEquipoActivo() {
        Jugador jugador = jugador();
        jugador.equipar(pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 5, Atributo.AGILIDAD, -20), Map.of()));
        jugador.setFuerza(12);
        jugador.setAgilidad(6);
        assertEquals(17, jugador.getFuerza());
        assertEquals(0, jugador.getAgilidad());
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(12, jugador.getFuerza());
        assertEquals(6, jugador.getAgilidad());
    }

    @Test
    void dosPiezasConLaMismaHabilidadDescuentanSoloSusNiveles() {
        Jugador jugador = jugador();
        jugador.equipar(pieza(TipoEquipo.ARMA, Map.of(), Map.of(new GolpeCertero(), 2)));
        jugador.equipar(pieza(TipoEquipo.ANILLO, Map.of(), Map.of(new GolpeCertero(), 5)));
        EstadoHabilidad estado = jugador.getHabilidades().get(new GolpeCertero());
        assertEquals(7, estado.getNivel());
        assertEquals(2, estado.getVecesEquipada());
        assertFalse(estado.isAprendida());

        jugador.desequipar(TipoEquipo.ARMA);
        assertSame(estado, jugador.getHabilidades().get(new GolpeCertero()));
        assertEquals(5, estado.getNivel());
        assertEquals(1, estado.getVecesEquipada());
        jugador.desequipar(TipoEquipo.ANILLO);
        assertFalse(jugador.getHabilidades().containsKey(new GolpeCertero()));
    }

    @Test
    void habilidadAprendidaConservaSusNivelesTrasRetirarTodoElEquipo() {
        Jugador jugador = jugador();
        jugador.aprenderHabilidad(new GolpeCertero(), 3);
        EstadoHabilidad estado = jugador.getHabilidades().get(new GolpeCertero());
        jugador.equipar(pieza(TipoEquipo.ARMA, Map.of(), Map.of(new GolpeCertero(), 2)));
        jugador.equipar(pieza(TipoEquipo.ANILLO, Map.of(), Map.of(new GolpeCertero(), 5)));
        jugador.aprenderHabilidad(new GolpeCertero(), 4);
        assertEquals(14, estado.getNivel());

        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(12, estado.getNivel());
        jugador.desequipar(TipoEquipo.ANILLO);
        assertSame(estado, jugador.getHabilidades().get(new GolpeCertero()));
        assertEquals(7, estado.getNivel());
        assertEquals(0, estado.getVecesEquipada());
        assertTrue(estado.isAprendida());
    }

    @Test
    void aprenderUnaHabilidadMientrasEstaEquipadaLaConservaAlRetirarLaPieza() {
        Jugador jugador = jugador();
        jugador.equipar(pieza(TipoEquipo.ARMA, Map.of(), Map.of(new GolpeCertero(), 2)));
        jugador.aprenderHabilidad(new GolpeCertero(), 3);
        jugador.desequipar(TipoEquipo.ARMA);
        EstadoHabilidad estado = jugador.getHabilidades().get(new GolpeCertero());
        assertTrue(estado.isAprendida());
        assertEquals(3, estado.getNivel());
        assertEquals(0, estado.getVecesEquipada());
    }

    @Test
    void operacionesRepetidasDeEquiparODesequiparNoDuplicanEfectos() {
        Jugador jugador = jugador();
        Equipo equipo = pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 5), Map.of(new GolpeCertero(), 2));
        jugador.setEquipo(equipo);
        assertFalse(jugador.equipar(equipo));
        assertEquals(15, jugador.getFuerza());
        assertEquals(1, jugador.getHabilidades().get(new GolpeCertero()).getVecesEquipada());
        assertTrue(jugador.desequipar(TipoEquipo.ARMA));
        assertFalse(jugador.desequipar(TipoEquipo.ARMA));
        assertEquals(10, jugador.getFuerza());
        assertTrue(jugador.getHabilidades().isEmpty());
    }

    @Test
    void aplicarYRetirarEfectosDirectamenteSonOperacionesInversas() {
        Jugador jugador = jugador();
        jugador.aprenderHabilidad(new GolpeCertero(), 3);
        Equipo equipo = pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, -15), Map.of(new GolpeCertero(), 2));
        equipo.aplicarEfectos(jugador);
        assertEquals(0, jugador.getFuerza());
        assertEquals(5, jugador.getHabilidades().get(new GolpeCertero()).getNivel());
        equipo.retirarEfectos(jugador);
        assertEquals(10, jugador.getFuerza());
        assertEquals(3, jugador.getHabilidades().get(new GolpeCertero()).getNivel());
        assertTrue(jugador.getEquipoEquipado().isEmpty());
    }

    @Test
    void equipoCopiaLosMapasYNoPermiteAlterarSusContribuciones() {
        Jugador jugador = jugador();
        Map<Atributo, Integer> atributos = new HashMap<>(Map.of(Atributo.FUERZA, 4));
        Map<Habilidad, Integer> habilidades = new HashMap<>(Map.of(new GolpeCertero(), 2));
        Equipo equipo = pieza(TipoEquipo.ARMA, atributos, habilidades);
        jugador.equipar(equipo);
        atributos.put(Atributo.FUERZA, 100);
        habilidades.put(new GolpeCertero(), 100);

        assertThrows(UnsupportedOperationException.class, () -> equipo.getBonificaciones().clear());
        assertThrows(UnsupportedOperationException.class, () -> equipo.getHabilidadesOtorgadas().clear());
        assertEquals(14, jugador.getFuerza());
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(10, jugador.getFuerza());
        assertTrue(jugador.getHabilidades().isEmpty());
    }

    @Test
    void mapasDelCombatienteNoPermitenSaltarseEquiparYDesequipar() {
        Jugador jugador = jugador();
        assertThrows(UnsupportedOperationException.class,
                () -> jugador.getModificadoresEquipo().put(Atributo.FUERZA, 20));
        assertThrows(UnsupportedOperationException.class,
                () -> jugador.getEquipoEquipado().put(TipoEquipo.ARMA, pieza(TipoEquipo.ARMA, Map.of(), Map.of())));
    }

    @Test
    void retirarHabilidadSinContribucionNoModificaLaHabilidadAprendida() {
        Jugador jugador = jugador();
        jugador.aprenderHabilidad(new GolpeCertero(), 3);
        assertThrows(IllegalStateException.class, () -> jugador.quitarHabilidadEquipo(new GolpeCertero(), 2));
        assertEquals(3, jugador.getHabilidades().get(new GolpeCertero()).getNivel());
        assertEquals(0, jugador.getHabilidades().get(new GolpeCertero()).getVecesEquipada());
    }

    @Test
    void nivelesInvalidosNoAlteranElEstadoExistente() {
        Jugador jugador = jugador();
        jugador.aprenderHabilidad(new GolpeCertero(), 3);
        assertThrows(IllegalArgumentException.class, () -> jugador.agregarHabilidadEquipo(new GolpeCertero(), -1));
        assertThrows(IllegalArgumentException.class,
                () -> pieza(TipoEquipo.ARMA, Map.of(), Map.of(new GolpeCertero(), -1)));
        assertThrows(IllegalArgumentException.class, () -> jugador.quitarHabilidadEquipo(new GolpeCertero(), -1));
        assertEquals(3, jugador.getHabilidades().get(new GolpeCertero()).getNivel());
        assertEquals(0, jugador.getHabilidades().get(new GolpeCertero()).getVecesEquipada());
    }

    @Test
    void laMismaPiezaPuedeAplicarseYRetirarseEnCombatientesIndependientes() {
        Jugador primero = jugador();
        Jugador segundo = jugador();
        Equipo equipo = pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 4), Map.of(new GolpeCertero(), 2));
        primero.equipar(equipo);
        segundo.equipar(equipo);
        assertNotSame(primero.getHabilidades().get(new GolpeCertero()), segundo.getHabilidades().get(new GolpeCertero()));
        primero.desequipar(TipoEquipo.ARMA);
        assertEquals(10, primero.getFuerza());
        assertTrue(primero.getHabilidades().isEmpty());
        assertEquals(14, segundo.getFuerza());
        assertEquals(2, segundo.getHabilidades().get(new GolpeCertero()).getNivel());
    }

    @Test
    void retiradaInvalidaNoRetiraParcialmenteAtributosNiOtrasHabilidades() {
        Jugador jugador = jugador();
        Equipo equipo = pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 4),
                Map.of(new GolpeCertero(), 2, new GuardiaFerrea(), 3));
        jugador.equipar(equipo);
        jugador.eliminarHabilidad(new GuardiaFerrea());

        assertThrows(IllegalStateException.class, () -> jugador.desequipar(TipoEquipo.ARMA));
        assertSame(equipo, jugador.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(14, jugador.getFuerza());
        assertEquals(2, jugador.getHabilidades().get(new GolpeCertero()).getNivel());
    }

    @Test
    void vidaNoSeModificaAlEquiparAtributosOrdinarios() {
        Jugador jugador = jugador();
        jugador.equipar(pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, -25), Map.of()));
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(15, jugador.getVida());
        assertEquals(30, jugador.getVidaMaxima());
    }

    @Test
    void vidaRequiereUnaReglaSeparadaYNoEntraEnLosMapasDeAtributos() {
        Jugador jugador = jugador();
        assertThrows(IllegalArgumentException.class, () -> jugador.getAtributo(Atributo.VIDA));
        assertThrows(IllegalArgumentException.class, () -> jugador.getAtributoBase(Atributo.VIDA));
        assertThrows(IllegalArgumentException.class, () -> jugador.modificarAtributo(Atributo.VIDA, 5));
        assertThrows(IllegalArgumentException.class, () -> jugador.aumentarAtributo(Atributo.VIDA, 5));
        assertThrows(IllegalArgumentException.class, () -> jugador.disminuirAtributo(Atributo.VIDA, 5));
        assertThrows(IllegalArgumentException.class, () -> pieza(TipoEquipo.ARMA, Map.of(Atributo.VIDA, 5), Map.of()));
        assertEquals(15, jugador.getVida());
        assertEquals(30, jugador.getVidaMaxima());
    }

    @Test
    void comparadorMantieneUnionAusenciasYSignoSinEquipar() {
        Equipo actual = pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 5, Atributo.RESISTENCIA, -4), Map.of());
        Equipo nuevo = pieza(TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 8, Atributo.AGILIDAD, -2), Map.of());
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PrintStream consolaOriginal = System.out;
        try (PrintStream captura = new PrintStream(salida, true, StandardCharsets.UTF_8)) {
            System.setOut(captura);
            actual.compararCon(nuevo);
        } finally {
            System.setOut(consolaOriginal);
        }
        String texto = salida.toString(StandardCharsets.UTF_8);
        assertTrue(texto.lines().anyMatch(l -> l.trim().matches("FUERZA\\s+5\\s+8\\s+\\+3")));
        assertTrue(texto.lines().anyMatch(l -> l.trim().matches("RESISTENCIA\\s+-4\\s+--\\s+\\+4")));
        assertTrue(texto.lines().anyMatch(l -> l.trim().matches("AGILIDAD\\s+--\\s+-2\\s+-2")));
        assertEquals(Map.of(Atributo.FUERZA, 5, Atributo.RESISTENCIA, -4), actual.getBonificaciones());
        assertEquals(Map.of(Atributo.FUERZA, 8, Atributo.AGILIDAD, -2), nuevo.getBonificaciones());
    }
}
