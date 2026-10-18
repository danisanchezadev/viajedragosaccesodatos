package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.Equipo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventarioEquipoTest {
    private Equipo pieza(TipoEquipo tipo) {
        return new Arma("Prueba", tipo, Map.of(Atributo.FUERZA, 3),
                Map.of(new InstintoSupervivencia(), 2));
    }

    @Test
    void guardarConservaInstanciaSinAplicarEfectosNiTocarPociones() {
        Jugador j = new Jugador("Prueba", "",6,2,3,2,2,13,24);
        j.agregarPocion(TipoPocion.PEQUENA);
        Equipo pieza = pieza(TipoEquipo.ARMA);
        j.agregarEquipo(pieza);
        assertEquals(1, j.getInventarioEquipo().size());
        assertSame(pieza, j.getInventarioEquipo().getFirst());
        assertEquals(6, j.getFuerza());
        assertEquals(13, j.getVida());
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertTrue(j.getHabilidades().isEmpty());
        assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @Test
    void admiteTodasLasCategoriasYRepeticionesEnOrdenDeInsercion() {
        Jugador j = new Jugador();
        var piezas = new ArrayList<Equipo>();
        for (TipoEquipo tipo : TipoEquipo.values()) {
            Equipo pieza = pieza(tipo);
            piezas.add(pieza);
            j.agregarEquipo(pieza);
        }
        j.agregarEquipo(piezas.getFirst());
        j.agregarEquipo(pieza(TipoEquipo.ARMA));
        assertEquals(TipoEquipo.values().length + 2, j.getInventarioEquipo().size());
        for (int i = 0; i < piezas.size(); i++) assertSame(piezas.get(i), j.getInventarioEquipo().get(i));
        assertSame(piezas.getFirst(), j.getInventarioEquipo().get(piezas.size()));
        assertNotSame(piezas.getFirst(), j.getInventarioEquipo().getLast());
    }

    @ParameterizedTest
    @ValueSource(ints = {1,100,10000})
    void noHayTopeArtificialDeCantidad(int cantidad) {
        Jugador j = new Jugador();
        Equipo pieza = pieza(TipoEquipo.ARMA);
        for (int i = 0; i < cantidad; i++) j.agregarEquipo(pieza);
        assertEquals(cantidad, j.getInventarioEquipo().size());
        assertSame(pieza, j.getInventarioEquipo().getLast());
    }

    @Test
    void coleccionProtegidaSinPerderIdentidadDeLasPiezas() {
        Jugador j = new Jugador();
        Equipo pieza = pieza(TipoEquipo.ARMA);
        j.agregarEquipo(pieza);
        var vista = j.getInventarioEquipo();
        assertThrows(UnsupportedOperationException.class, () -> vista.add(pieza));
        assertThrows(UnsupportedOperationException.class, () -> vista.remove(pieza));
        assertThrows(UnsupportedOperationException.class, vista::clear);
        assertThrows(UnsupportedOperationException.class, () -> vista.set(0, pieza));
        assertThrows(UnsupportedOperationException.class, () -> vista.iterator().remove());
        assertThrows(NullPointerException.class, () -> j.agregarEquipo(null));
        assertEquals(1, vista.size());
        pieza.setNombre("Renombrada");
        assertEquals("Renombrada", vista.getFirst().getNombre());
    }

    @Test
    void inventarioYEquipoEquipadoSonColeccionesIndependientes() {
        Jugador j = new Jugador("Prueba", "",6,2,3,2,2,13,24);
        Equipo equipada = pieza(TipoEquipo.ARMA);
        Equipo guardada = pieza(TipoEquipo.ARMA);
        assertTrue(j.equipar(equipada));
        assertTrue(j.getInventarioEquipo().isEmpty());
        j.agregarEquipo(guardada);
        assertSame(equipada, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(9, j.getFuerza());
        assertTrue(j.desequipar(TipoEquipo.ARMA));
        assertEquals(6, j.getFuerza());
        assertEquals(1, j.getInventarioEquipo().size());
        assertSame(guardada, j.getInventarioEquipo().getFirst());
        assertTrue(j.getHabilidades().isEmpty());
    }

    @Test
    void nuevoJugadorNoComparteInventario() {
        Jugador primero = new Jugador();
        Jugador segundo = new Jugador();
        primero.agregarEquipo(pieza(TipoEquipo.ARMA));
        assertTrue(segundo.getInventarioEquipo().isEmpty());
    }
}
