package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.combate.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.habilidad.implementacion.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PergaminosHabilidadesTest {
    private Jugador jugador() { return new Jugador("Prueba", "", 6, 2, 3, 2, 2, 1, 100); }
    private Equipo pieza(TipoEquipo tipo, Habilidad h) {
        return new Equipo(null, "Equipo", tipo, Map.of(Atributo.FUERZA, 1), Map.of(h, 2), 0);
    }
    private void niveles(Jugador j, Habilidad h, int permanente, int equipo, int fuentes) {
        var e = j.getHabilidades().get(h);
        assertNotNull(e);
        assertTrue(e.isAprendida());
        assertEquals(permanente, e.getNivelPermanente());
        assertEquals(equipo, e.getNivelEquipo());
        assertEquals(permanente + equipo, e.getNivel());
        assertEquals(fuentes, e.getVecesEquipada());
    }

    @ParameterizedTest @EnumSource(HabilidadDisponible.class)
    void aprendizajeYMejoraSinEquipoCasosAyB(HabilidadDisponible tipo) {
        Jugador j = jugador(); Habilidad h = tipo.crearHabilidad();
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(tipo));
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(tipo));
        assertTrue(j.usarPergaminoHabilidad(tipo));
        niveles(j, h, 1, 0, 0);
        assertEquals(1, j.getCantidadPergaminosHabilidad(tipo));
        j.aprenderHabilidad(h, 1);
        assertTrue(j.usarPergaminoHabilidad(tipo));
        niveles(j, h, 3, 0, 0);
        assertEquals(0, j.getCantidadPergaminosHabilidad(tipo));
        assertFalse(j.usarPergaminoHabilidad(tipo));
        assertEquals(6, j.getFuerza()); assertEquals(1, j.getVida());
        assertEquals(100, j.getVidaMaxima()); assertEquals(15, j.getOro());
        assertEquals(0, j.getExperiencia()); assertEquals(1, j.getNivel());
        assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @ParameterizedTest @EnumSource(HabilidadDisponible.class)
    void aprendizajeConEquipoCasosCyDYCiclosSinDuplicacion(HabilidadDisponible tipo) {
        for (int permanente : new int[]{0, 2}) {
            Jugador j = jugador(); Habilidad h = tipo.crearHabilidad();
            if (permanente > 0) j.aprenderHabilidad(h, permanente);
            Equipo pieza = pieza(TipoEquipo.ARMA, h);
            j.agregarEquipo(pieza);
            assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
            var estado = j.getHabilidades().get(h);
            j.agregarPergaminoHabilidad(new PergaminoHabilidad(tipo));
            assertTrue(j.usarPergaminoHabilidad(tipo));
            assertSame(estado, j.getHabilidades().get(h));
            niveles(j, h, permanente + 1, 2, 1);
            for (int i = 0; i < 3; i++) {
                assertTrue(j.desequiparAlInventario(TipoEquipo.ARMA));
                niveles(j, h, permanente + 1, 0, 0);
                assertEquals(6, j.getFuerza());
                assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
                niveles(j, h, permanente + 1, 2, 1);
                assertEquals(7, j.getFuerza());
            }
        }
    }

    @ParameterizedTest @EnumSource(HabilidadDisponible.class)
    void maestriaPermiteElegirCualquierHabilidadYConsumeUnaUnidad(HabilidadDisponible tipo) {
        Jugador j = jugador(); j.agregarPergaminoMaestria(); j.agregarPergaminoMaestria();
        assertTrue(j.usarPergaminoMaestria(tipo));
        niveles(j, tipo.crearHabilidad(), 1, 0, 0);
        assertEquals(1, j.getCantidadPergaminosMaestria());
        assertTrue(j.usarPergaminoMaestria(tipo));
        niveles(j, tipo.crearHabilidad(), 2, 0, 0);
        assertEquals(0, j.getCantidadPergaminosMaestria());
        assertFalse(j.usarPergaminoMaestria(tipo));
    }

    @Test void catalogoContieneExactamenteLasTreceImplementacionesJugablesSinDuplicados() {
        var clases = Arrays.stream(HabilidadDisponible.values()).map(h -> h.crearHabilidad().getClass()).toList();
        assertEquals(Set.of(Caparazon.class, CastigoAlTorpe.class, DebilidadEnemiga.class,
                DefensaPerfecta.class, FuriaContenida.class, GolpeCertero.class, GuardiaFerrea.class,
                Impetu.class, InstintoSupervivencia.class, PrimerImpulso.class, Rabia.class,
                SedDeBatalla.class, SegundoAliento.class), new HashSet<>(clases));
        assertEquals(13, clases.size());
        for (var tipo : HabilidadDisponible.values()) {
            assertNotSame(tipo.crearHabilidad(), tipo.crearHabilidad());
            assertEquals(tipo.crearHabilidad(), tipo.crearHabilidad());
            assertEquals(tipo.getNombre(), tipo.crearHabilidad().getNombre());
        }
        assertEquals(HabilidadDisponible.CAPARAZON, HabilidadDisponible.values()[0]);
        assertEquals(HabilidadDisponible.SEGUNDO_ALIENTO, HabilidadDisponible.values()[12]);
    }

    @Test void inventarioProtegidoContenidoFijoYCantidadesIndependientes() {
        Jugador j = jugador(); var p = new PergaminoHabilidad(HabilidadDisponible.RABIA);
        j.agregarPergaminoHabilidad(p); j.agregarPergaminoHabilidad(p);
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(HabilidadDisponible.IMPETU));
        var vista = j.getPergaminosHabilidad();
        assertThrows(UnsupportedOperationException.class, () -> vista.clear());
        assertEquals(HabilidadDisponible.RABIA, p.habilidad());
        assertTrue(p.nombre().contains("Rabia"));
        assertTrue(j.usarPergaminoHabilidad(p.habilidad()));
        assertEquals(1, vista.get(HabilidadDisponible.RABIA));
        assertEquals(1, vista.get(HabilidadDisponible.IMPETU));
        assertFalse(j.usarPergaminoHabilidad(null)); assertFalse(j.usarPergaminoMaestria(null));
        assertThrows(NullPointerException.class, () -> new PergaminoHabilidad(null));
        assertTrue(new Jugador().getPergaminosHabilidad().isEmpty());
        assertEquals(0, new Jugador().getCantidadPergaminosMaestria());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void desbordamientoNoConsumeNiModificaEstado(boolean equipo) {
        Jugador j = jugador(); var tipo = HabilidadDisponible.INSTINTO_SUPERVIVENCIA;
        var h = tipo.crearHabilidad();
        if (equipo) j.agregarHabilidadEquipo(h, Integer.MAX_VALUE);
        else j.aprenderHabilidad(h, Integer.MAX_VALUE);
        var e = j.getHabilidades().get(h); e.setEfectoTemporalActivo(true);
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(tipo)); j.agregarPergaminoMaestria();
        assertFalse(j.usarPergaminoHabilidad(tipo)); assertFalse(j.usarPergaminoMaestria(tipo));
        assertSame(e, j.getHabilidades().get(h)); assertEquals(Integer.MAX_VALUE, e.getNivel());
        assertEquals(equipo ? 0 : Integer.MAX_VALUE, e.getNivelPermanente());
        assertTrue(e.isEfectoTemporalActivo()); assertEquals(!equipo, e.isAprendida());
        assertEquals(1, j.getCantidadPergaminosHabilidad(tipo)); assertEquals(1, j.getCantidadPergaminosMaestria());
    }

    @Test void desbordamientosDeCantidadesNoSeAcumulan() throws Exception {
        Jugador j = jugador(); var campo = Jugador.class.getDeclaredField("pergaminosHabilidad"); campo.setAccessible(true);
        @SuppressWarnings("unchecked") var mapa = (Map<HabilidadDisponible, Integer>) campo.get(j);
        mapa.put(HabilidadDisponible.RABIA, Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> j.agregarPergaminoHabilidad(new PergaminoHabilidad(HabilidadDisponible.RABIA)));
        assertEquals(Integer.MAX_VALUE, j.getCantidadPergaminosHabilidad(HabilidadDisponible.RABIA));
        var cantidad = Jugador.class.getDeclaredField("pergaminosMaestria"); cantidad.setAccessible(true);
        cantidad.setInt(j, Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, j::agregarPergaminoMaestria);
        assertEquals(Integer.MAX_VALUE, j.getCantidadPergaminosMaestria());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void falloTrasAprenderRestauraInstanciaNivelYDatosTemporales(boolean maestria) {
        Jugador j = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 1, 100) {
            @Override public void aprenderHabilidad(Habilidad h, int niveles) {
                super.aprenderHabilidad(h, niveles);
                setVida(50); setFuerza(99); getHabilidades().get(h).setEfectoTemporalActivo(false);
                throw new IllegalStateException("Fallo después de aprender");
            }
        };
        var tipo = HabilidadDisponible.INSTINTO_SUPERVIVENCIA; var h = tipo.crearHabilidad();
        var pieza = pieza(TipoEquipo.ARMA, h); j.equipar(pieza);
        j.curarse(8); var e = j.getHabilidades().get(h);
        assertEquals(10, j.getFuerza()); assertTrue(e.isEfectoTemporalActivo());
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(tipo)); j.agregarPergaminoMaestria();
        assertFalse(maestria ? j.usarPergaminoMaestria(tipo) : j.usarPergaminoHabilidad(tipo));
        assertSame(e, j.getHabilidades().get(h)); assertFalse(e.isAprendida());
        assertEquals(0, e.getNivelPermanente()); assertEquals(2, e.getNivel());
        assertTrue(e.isEfectoTemporalActivo()); assertEquals(10, j.getFuerza()); assertEquals(9, j.getVida());
        assertSame(pieza, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(1, j.getCantidadPergaminosHabilidad(tipo)); assertEquals(1, j.getCantidadPergaminosMaestria());
    }

    @ParameterizedTest @EnumSource(value = HabilidadDisponible.class,
            names = {"INSTINTO_SUPERVIVENCIA", "IMPETU", "PRIMER_IMPULSO", "FURIA_CONTENIDA", "RABIA"})
    void aprenderConEfectoActivoConservaEstadoYLaRestauracionDelIntercambio(HabilidadDisponible tipo) {
        Jugador j = jugador(); Habilidad h = tipo.crearHabilidad();
        var arma = pieza(TipoEquipo.ARMA, h); j.equipar(arma);
        var e = j.getHabilidades().get(h);
        var enemigo = new Enemigo();
        h.alInicioCombate(j, enemigo, e);
        var contexto = new ContextoAtaque(j, j); contexto.setDanioFinal(1);
        h.alHacerDanio(contexto, e); h.alRecibirDanio(contexto, e);
        assertTrue(e.isEfectoTemporalActivo() || e.getAcumulacionesTemporales() > 0);
        int fuerza = j.getFuerza(), agilidad = j.getAgilidad(), iniciativa = j.getIniciativa(), resistencia = j.getResistencia();
        j.agregarPergaminoMaestria(); assertTrue(j.usarPergaminoMaestria(tipo));
        niveles(j, h, 1, 2, 1); assertSame(e, j.getHabilidades().get(h));
        var fallida = new Equipo("Fallida", TipoEquipo.ARMA, Map.of(), Map.of()) {
            @Override public void aplicarEfectos(Combatiente c) { super.aplicarEfectos(c); throw new IllegalStateException(); }
        };
        j.agregarEquipo(fallida);
        assertFalse(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
        assertSame(arma, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(List.of(fallida), j.getInventarioEquipo());
        niveles(j, h, 1, 2, 1); assertSame(e, j.getHabilidades().get(h));
        assertEquals(fuerza, j.getFuerza()); assertEquals(agilidad, j.getAgilidad());
        assertEquals(iniciativa, j.getIniciativa()); assertEquals(resistencia, j.getResistencia());
        assertTrue(e.isEfectoTemporalActivo() || e.getAcumulacionesTemporales() > 0);
        assertTrue(j.desequiparAlInventario(TipoEquipo.ARMA)); niveles(j, h, 1, 0, 0);
        h.alFinalCombate(j, enemigo, e); h.alFinalCombate(j, enemigo, e);
        assertEquals(6, j.getFuerza()); assertEquals(2, j.getAgilidad());
        assertEquals(3, j.getIniciativa()); assertEquals(2, j.getResistencia());
        assertFalse(e.isEfectoTemporalActivo()); assertEquals(0, e.getAcumulacionesTemporales());
    }

    @Test void variasPiezasSiguenSiendoUnaHabilidadConNivelPermanenteIndependiente() {
        Jugador j = jugador(); var h = new InstintoSupervivencia();
        j.equipar(pieza(TipoEquipo.ARMA, h)); j.equipar(pieza(TipoEquipo.ANILLO, h));
        j.curarse(8); var e = j.getHabilidades().get(h);
        j.agregarPergaminoMaestria(); assertTrue(j.usarPergaminoMaestria(HabilidadDisponible.INSTINTO_SUPERVIVENCIA));
        niveles(j, h, 1, 4, 2); assertEquals(1, j.getHabilidades().size());
        assertTrue(j.desequiparAlInventario(TipoEquipo.ARMA)); niveles(j, h, 1, 2, 1);
        assertTrue(j.desequiparAlInventario(TipoEquipo.ANILLO)); niveles(j, h, 1, 0, 0);
        assertTrue(e.isEfectoTemporalActivo()); assertEquals(9, j.getFuerza());
        h.alFinalCombate(j, new Enemigo(), e); assertEquals(6, j.getFuerza());
    }

    @Test void copiaYRestauracionSeparanNivelesInclusoAlRevertirUnAprendizaje() {
        var estado = new EstadoHabilidad(false, 1, 2); estado.aprender(1);
        estado.setEfectoTemporalActivo(true); estado.setAcumulacionesTemporales(2);
        var copia = new EstadoHabilidad(estado);
        estado.aprender(3); estado.agregarEquipo(2); estado.setEfectoTemporalActivo(false);
        estado.restaurar(copia);
        assertEquals(1, estado.getNivelPermanente()); assertEquals(2, estado.getNivelEquipo());
        assertEquals(3, estado.getNivel()); assertEquals(1, estado.getVecesEquipada());
        assertTrue(estado.isEfectoTemporalActivo()); assertEquals(2, estado.getAcumulacionesTemporales());
        copia.aprender(1); assertEquals(1, estado.getNivelPermanente());
    }

    @Test void nivelAprendidoPorPergaminosAfectaCaparazonSinCambiarFormula() {
        Jugador j = jugador(); var tipo = HabilidadDisponible.CAPARAZON;
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(tipo)); assertTrue(j.usarPergaminoHabilidad(tipo));
        var enemigo = new Enemigo("Enemigo", "", 96, 0, 1, 0, 1, 100, 100);
        j.setVida(100);
        int[] dados = {4, 1}; int[] usados = {0};
        new Combate(j, enemigo, () -> dados[usados[0]++]).ataque(enemigo, j);
        assertEquals(1, j.getVida()); assertEquals(2, usados[0]);
        j.agregarPergaminoMaestria(); assertTrue(j.usarPergaminoMaestria(tipo)); j.setVida(100); usados[0] = 0;
        new Combate(j, enemigo, () -> dados[usados[0]++]).ataque(enemigo, j);
        assertEquals(2, j.getVida()); assertEquals(2, usados[0]);
    }

    @Test void combateCompletoUsaYLimpiaHabilidadAprendidaPorPergamino() {
        Jugador j = jugador(); j.setVida(100); j.agregarPergaminoMaestria();
        assertTrue(j.usarPergaminoMaestria(HabilidadDisponible.RABIA));
        var enemigo = new Enemigo("Enemigo", "", 1, 0, 1, 0, 1, 1, 1);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(j, enemigo, () -> 4).combatir());
        assertEquals(6, j.getFuerza()); assertEquals(2, j.getResistencia()); assertEquals(2, j.getAgilidad());
        niveles(j, new Rabia(), 1, 0, 0);
        assertFalse(j.getHabilidades().get(new Rabia()).isEfectoTemporalActivo());
    }

    @Test void entradaInactivaNoAportaNivelesFicticiosAlAprender() {
        Jugador j = jugador(); var h = HabilidadDisponible.RABIA;
        j.getHabilidades().put(h.crearHabilidad(), new EstadoHabilidad());
        j.agregarPergaminoMaestria(); assertTrue(j.usarPergaminoMaestria(h));
        niveles(j, h.crearHabilidad(), 1, 0, 0);
    }

    @Test void constructorMixtoExplicitoYSetterConservanLaContribucionDeEquipo() {
        var e = new EstadoHabilidad(true, 2, 7, 3);
        assertEquals(3, e.getNivelPermanente()); assertEquals(4, e.getNivelEquipo());
        e.setNivel(8); assertEquals(4, e.getNivelPermanente()); assertEquals(4, e.getNivelEquipo());
        assertThrows(IllegalArgumentException.class, () -> new EstadoHabilidad(true, 1, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> new EstadoHabilidad(false, 1, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> new EstadoHabilidad(true, 1, 2, 3));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void aprendizajeNuevoFallidoNoDejaUnaHabilidadSinConsumir(boolean aplicarAntesDelFallo) {
        Jugador j = new Jugador() {
            @Override public void aprenderHabilidad(Habilidad h, int niveles) {
                if (aplicarAntesDelFallo) super.aprenderHabilidad(h, niveles);
                throw new IllegalStateException("Fallo controlado");
            }
        };
        j.agregarPergaminoHabilidad(new PergaminoHabilidad(HabilidadDisponible.RABIA));
        assertFalse(j.usarPergaminoHabilidad(HabilidadDisponible.RABIA));
        assertTrue(j.getHabilidades().isEmpty());
        assertEquals(1, j.getCantidadPergaminosHabilidad(HabilidadDisponible.RABIA));
    }
}
