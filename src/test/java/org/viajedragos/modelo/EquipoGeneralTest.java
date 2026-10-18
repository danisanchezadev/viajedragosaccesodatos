package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.combate.*;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EquipoGeneralTest {
    private Jugador jugador() { return new Jugador("Prueba", "", 6, 2, 3, 2, 2, 20, 30); }
    private DefinicionEquipo definicion(TipoEquipo tipo, Map<Atributo, Integer> bonos) {
        return new DefinicionEquipo("id_" + tipo, "Modelo " + tipo, tipo, bonos);
    }

    @ParameterizedTest
    @EnumSource(TipoEquipo.class)
    void todasLasRanurasCreanEjemplaresIndependientesConIdentificador(TipoEquipo tipo) {
        var d = definicion(tipo, Map.of(Atributo.FUERZA, 2, Atributo.VIDA, 5));
        Equipo uno = d.crearEquipo(), dos = d.crearEquipo();
        assertNotSame(uno, dos);
        assertNotEquals(uno, dos);
        assertEquals(d.id(), uno.getIdDefinicion());
        assertEquals(d.id(), dos.getIdDefinicion());
        assertEquals(tipo, uno.getTipoEquipo());
        Jugador j = jugador();
        j.agregarEquipo(uno); j.agregarEquipo(dos);
        assertTrue(j.equiparDesdeInventario(tipo, 1));
        assertSame(dos, j.getEquipoEquipado().get(tipo));
        assertEquals(List.of(uno), j.getInventarioEquipo());
        assertFalse(j.equipar(uno));
        assertEquals(8, j.getFuerza());
        assertEquals(35, j.getVidaMaxima());
        assertEquals(20, j.getVida());
        assertTrue(j.equiparDesdeInventario(tipo, 0));
        assertSame(uno, j.getEquipoEquipado().get(tipo));
        assertEquals(List.of(dos), j.getInventarioEquipo());
        assertTrue(j.desequiparAlInventario(tipo));
        assertEquals(List.of(dos, uno), j.getInventarioEquipo());
        assertEquals(6, j.getFuerza());
        assertEquals(30, j.getVidaMaxima());
        assertFalse(j.desequiparAlInventario(tipo));
    }

    @Test
    void sieteRanurasSeAcumulanSinDuplicarNiPerderPiezas() {
        Jugador j = jugador();
        for (TipoEquipo tipo : TipoEquipo.values()) {
            Equipo e = definicion(tipo, Map.of(Atributo.FUERZA, 1, Atributo.RESISTENCIA, 2)).crearEquipo();
            j.agregarEquipo(e);
            assertTrue(j.equiparDesdeInventario(tipo, 0));
        }
        assertEquals(7, j.getEquipoEquipado().size());
        assertEquals(13, j.getFuerza());
        assertEquals(16, j.getResistencia());
        for (TipoEquipo tipo : TipoEquipo.values()) assertTrue(j.desequiparAlInventario(tipo));
        assertEquals(7, j.getInventarioEquipo().size());
        assertEquals(6, j.getFuerza());
        assertEquals(2, j.getResistencia());
    }

    @Test
    void ejemploDeVidaMaximaConDosRanurasNoCuraYRetiraSoloSuContribucion() {
        Jugador j = jugador();
        Equipo armadura = definicion(TipoEquipo.ARMADURA, Map.of(Atributo.VIDA, 5)).crearEquipo();
        Equipo amuleto = definicion(TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 3)).crearEquipo();
        assertTrue(j.equipar(armadura));
        assertEquals(35, j.getVidaMaxima()); assertEquals(20, j.getVida());
        assertTrue(j.equipar(amuleto));
        assertEquals(38, j.getVidaMaxima()); assertEquals(20, j.getVida());
        assertTrue(j.desequipar(TipoEquipo.ARMADURA));
        assertEquals(33, j.getVidaMaxima()); assertEquals(20, j.getVida());
        j.curarse(100);
        assertEquals(33, j.getVida());
        assertTrue(j.desequipar(TipoEquipo.AMULETO));
        assertEquals(30, j.getVidaMaxima()); assertEquals(30, j.getVida());
        assertEquals(30, j.getVidaMaximaBase());
    }

    @Test
    void cambiarAMayorVidaMaximaNoPierdeVidaPorElPasoIntermedio() {
        Jugador j = jugador();
        var anterior = definicion(TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 5)).crearEquipo();
        var nuevo = definicion(TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 10)).crearEquipo();
        j.equipar(anterior); j.setVida(35); j.agregarEquipo(nuevo);
        assertTrue(j.equiparDesdeInventario(TipoEquipo.AMULETO, 0));
        assertEquals(35, j.getVida()); assertEquals(40, j.getVidaMaxima());
        assertEquals(List.of(anterior), j.getInventarioEquipo());
        assertTrue(j.equiparDesdeInventario(TipoEquipo.AMULETO, 0));
        assertEquals(35, j.getVida()); assertEquals(35, j.getVidaMaxima());
    }

    @Test
    void nivelMantieneSuIncrementoDeSeisConEquipoYTrasRetirarlo() {
        Jugador j = jugador();
        var equipo = definicion(TipoEquipo.ARMADURA, Map.of(Atributo.VIDA, 5, Atributo.FUERZA, 2)).crearEquipo();
        j.equipar(equipo);
        j.agregarExperiencia(20);
        j.asignarPunto(Atributo.FUERZA); j.asignarPunto(Atributo.FUERZA);
        assertEquals(36, j.getVidaMaximaBase());
        assertEquals(41, j.getVidaMaxima()); assertEquals(26, j.getVida());
        assertEquals(11, j.getFuerza());
        j.desequiparAlInventario(TipoEquipo.ARMADURA);
        assertEquals(36, j.getVidaMaxima()); assertEquals(26, j.getVida());
        assertEquals(9, j.getFuerza());
    }

    @Test
    void pocionesUsanMaximoEfectivoSinConsumirsePorEquipar() {
        Jugador j = jugador(); j.setVida(30); j.agregarPocion(TipoPocion.PEQUENA);
        j.equipar(definicion(TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 5)).crearEquipo());
        assertEquals(30, j.getVida());
        assertEquals(5, j.usarPocion(TipoPocion.PEQUENA));
        assertEquals(35, j.getVida());
        assertEquals(0, j.getCantidadPociones(TipoPocion.PEQUENA));
        j.desequipar(TipoEquipo.AMULETO);
        assertEquals(30, j.getVida());
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 2, 20})
    void ciclosNoAcumulanBonificacionesDeVidaNiDeOtrosAtributos(int bono) {
        Jugador j = jugador();
        var equipo = definicion(TipoEquipo.ANILLO, Map.of(Atributo.VIDA, bono, Atributo.FUERZA, -2)).crearEquipo();
        j.agregarEquipo(equipo);
        for (int ciclo = 0; ciclo < 20; ciclo++) {
            assertTrue(j.equiparDesdeInventario(TipoEquipo.ANILLO, 0));
            assertEquals(4, j.getFuerza()); assertEquals(30 + bono, j.getVidaMaxima());
            assertTrue(j.desequiparAlInventario(TipoEquipo.ANILLO));
            assertEquals(6, j.getFuerza()); assertEquals(30, j.getVidaMaxima());
            assertEquals(List.of(equipo), j.getInventarioEquipo());
        }
    }

    @Test
    void habilidadesDeArmaConservanSuEstadoConOtrasPiezas() {
        Jugador j = jugador(); j.setVida(1);
        var instinto = new InstintoSupervivencia();
        var arma = new Arma("Arma con habilidad", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of(instinto, 1));
        j.equipar(arma); j.curarse(1);
        var estado = j.getHabilidades().get(instinto);
        assertEquals(10, j.getFuerza());
        j.agregarEquipo(definicion(TipoEquipo.GUANTES, Map.of(Atributo.FUERZA, 1)).crearEquipo());
        assertTrue(j.equiparDesdeInventario(TipoEquipo.GUANTES, 0));
        assertEquals(11, j.getFuerza());
        assertSame(estado, j.getHabilidades().get(instinto));
        assertTrue(j.desequiparAlInventario(TipoEquipo.GUANTES));
        assertEquals(10, j.getFuerza());
        assertTrue(j.desequiparAlInventario(TipoEquipo.ARMA));
        assertTrue(j.getHabilidades().isEmpty());
        assertEquals(6, j.getFuerza());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void falloAlVenderODesequiparRestauraVidaAtributosYEstados(boolean venta) {
        Jugador j = jugador();
        var d = definicion(TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 5, Atributo.FUERZA, 2));
        var malo = new Equipo(d.id(), d.nombre(), d.tipo(), d.bonificaciones(), Map.of()) {
            public void retirarEfectos(Combatiente c) {
                super.retirarEfectos(c); c.setVida(1); c.setFuerza(100);
                throw new IllegalStateException("Retirada parcial");
            }
        };
        j.equipar(malo); j.setVida(35);
        RepositorioTienda tienda = () -> List.of(new ProductoTienda(d, 10));
        assertFalse(venta ? j.venderEquipo(malo, true, tienda) : j.desequiparAlInventario(d.tipo()));
        assertSame(malo, j.getEquipoEquipado().get(d.tipo()));
        assertEquals(35, j.getVida()); assertEquals(35, j.getVidaMaxima());
        assertEquals(30, j.getVidaMaximaBase()); assertEquals(8, j.getFuerza());
        assertEquals(15, j.getOro()); assertTrue(j.getInventarioEquipo().isEmpty());
    }

    @Test
    void falloAlAplicarNuevaPiezaRecuperaLaAnteriorYAmbosInventarios() {
        Jugador j = jugador();
        Equipo anterior = definicion(TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 5)).crearEquipo();
        Equipo nuevo = new Equipo("nuevo", "Fallo", TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 10), Map.of()) {
            public void aplicarEfectos(Combatiente c) { super.aplicarEfectos(c); throw new IllegalStateException(); }
        };
        j.equipar(anterior); j.setVida(35); j.agregarEquipo(nuevo);
        assertFalse(j.equiparDesdeInventario(TipoEquipo.AMULETO, 0));
        assertEquals(35, j.getVida()); assertEquals(35, j.getVidaMaxima());
        assertEquals(List.of(nuevo), j.getInventarioEquipo());
        assertSame(anterior, j.getEquipoEquipado().get(TipoEquipo.AMULETO));
    }

    @Test
    void idDeCatalogoDistingueModelosConNombreYBonosIguales() {
        Jugador j = jugador();
        var a = new DefinicionEquipo("a", "Anillo", TipoEquipo.ANILLO, Map.of(Atributo.FUERZA, 1));
        var b = new DefinicionEquipo("b", "Anillo", TipoEquipo.ANILLO, Map.of(Atributo.FUERZA, 1));
        Equipo uno = a.crearEquipo(), dos = b.crearEquipo();
        j.agregarEquipo(uno); j.agregarEquipo(dos);
        RepositorioTienda tienda = () -> List.of(new ProductoTienda(a, 10), new ProductoTienda(b, 20));
        assertTrue(j.venderEquipo(dos, false, tienda));
        assertEquals(25, j.getOro()); assertEquals(List.of(uno), j.getInventarioEquipo());
        assertFalse(j.venderEquipo(dos, false, tienda));
        assertTrue(j.venderEquipo(uno, false, tienda));
        assertEquals(30, j.getOro());
    }

    @Test
    void combateConsumeLosAtributosEfectivosDeVariasRanurasSinCambiarFormulas() {
        Jugador j = jugador();
        j.equipar(definicion(TipoEquipo.GUANTES, Map.of(Atributo.FUERZA, 1)).crearEquipo());
        j.equipar(definicion(TipoEquipo.BOTAS, Map.of(Atributo.AGILIDAD, 2)).crearEquipo());
        j.equipar(definicion(TipoEquipo.ARMADURA, Map.of(Atributo.RESISTENCIA, 3)).crearEquipo());
        Enemigo e = new Enemigo("Prueba", "", 3, 1, 1, 1, 1, 40, 40);
        new Combate(j, e, () -> 4).ataque(j, e);
        assertEquals(31, e.getVida()); // (7 fuerza + 4 agilidad + 4) - (1 + 1 + 4) = 9.
        assertEquals(5, j.getResistencia());
    }

    @Test
    void venderAmuletoAjustaVidaSinDejarNiObjetoNiBonificacion() {
        Jugador j = jugador();
        var d = definicion(TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 5));
        Equipo pieza = d.crearEquipo();
        j.equipar(pieza); j.setVida(35);
        RepositorioTienda tienda = () -> List.of(new ProductoTienda(d, 10));
        assertTrue(j.venderEquipo(pieza, true, tienda));
        assertEquals(30, j.getVida()); assertEquals(30, j.getVidaMaxima());
        assertEquals(20, j.getOro()); assertTrue(j.getEquipoEquipado().isEmpty());
        assertFalse(j.venderEquipo(pieza, true, tienda));
        assertFalse(j.venderEquipo(pieza, false, tienda));
    }

    @Test
    void comparacionDeVidaMaximaRespetaDiferenciaYSoloConsulta() {
        Jugador j = jugador();
        Equipo actual = definicion(TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 2)).crearEquipo();
        Equipo nuevo = definicion(TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 7)).crearEquipo();
        j.equipar(actual); j.agregarEquipo(nuevo);
        var bytes = new java.io.ByteArrayOutputStream();
        actual.compararCon(nuevo, new java.io.PrintStream(bytes, true, java.nio.charset.StandardCharsets.UTF_8), false);
        String texto = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(texto.contains("VIDA MÁX.")); assertTrue(texto.contains("+5"));
        assertEquals(32, j.getVidaMaxima()); assertEquals(20, j.getVida());
        assertSame(actual, j.getEquipoEquipado().get(TipoEquipo.AMULETO));
        assertEquals(List.of(nuevo), j.getInventarioEquipo());
    }

    @Test
    void vidaBaseSePrevalidaAntesDeAplicarUnaSubidaConPenalizacionDeEquipo() {
        Jugador j = jugador(); j.setVidaMaxima(Integer.MAX_VALUE - 3);
        j.equipar(definicion(TipoEquipo.AMULETO, Map.of(Atributo.VIDA, -100)).crearEquipo());
        assertThrows(ArithmeticException.class, () -> j.agregarExperiencia(20));
        assertEquals(1, j.getNivel()); assertEquals(0, j.getExperiencia());
        assertEquals(6, j.getFuerza()); assertEquals(20, j.getVida());
        assertEquals(Integer.MAX_VALUE - 3, j.getVidaMaximaBase());
    }

    @Test
    void piezaEnRanuraIncorrectaNoSeEquipaNiSeRetiraDelInventario() {
        Jugador j = jugador();
        Equipo anillo = definicion(TipoEquipo.ANILLO, Map.of(Atributo.FUERZA, 1)).crearEquipo();
        j.agregarEquipo(anillo);
        assertFalse(j.equiparDesdeInventario(TipoEquipo.AMULETO, 0));
        assertEquals(List.of(anillo), j.getInventarioEquipo());
        assertTrue(j.getEquipoEquipado().isEmpty()); assertEquals(6, j.getFuerza());
    }
}
