package org.viajedragos.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.modelo.TipoPocion;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioObjetosTest {
    @Test
    void catalogoInicialExactoSinEstadoDeJugadores() {
        var repo = new RepositorioObjetosMemoria();
        assertEquals(1, repo.obtenerArmas().size());
        var espada = repo.obtenerArmas().getFirst();
        assertEquals("espada_desgastada", espada.id());
        assertEquals("Espada desgastada", espada.nombre());
        assertEquals(Map.of(Atributo.FUERZA, 1), espada.bonificaciones());
        assertEquals(TipoEquipo.ARMA, espada.crearArma().getTipoEquipo());
        assertTrue(espada.crearArma().getHabilidadesOtorgadas().isEmpty());
        assertEquals(2, repo.obtenerPociones().size());
        assertEquals(new DefinicionPocion("pocion_pequena", TipoPocion.PEQUENA, "Poción pequeña", 8),
                repo.obtenerPociones().get(0));
        assertEquals(new DefinicionPocion("pocion_grande", TipoPocion.GRANDE, "Poción grande", 16),
                repo.obtenerPociones().get(1));
        var ids = new HashSet<String>();
        repo.obtenerArmas().forEach(d -> assertTrue(ids.add(d.id())));
        repo.obtenerPociones().forEach(d -> assertTrue(ids.add(d.id())));
        assertThrows(UnsupportedOperationException.class, () -> repo.obtenerArmas().clear());
        assertThrows(UnsupportedOperationException.class, () -> repo.obtenerPociones().clear());
    }

    @Test
    void copiaDefensivaYArmasIndependientes() {
        var original = new EnumMap<Atributo, Integer>(Atributo.class);
        original.put(Atributo.FUERZA, 1);
        original.put(Atributo.VELOCIDAD, -2);
        var definicion = new DefinicionArma("prueba", "Prueba", original);
        original.clear();
        assertEquals(2, definicion.bonificaciones().size());
        assertThrows(UnsupportedOperationException.class, () -> definicion.bonificaciones().clear());
        var primera = definicion.crearArma();
        var segunda = definicion.crearArma();
        assertNotSame(primera, segunda);
        assertNotSame(primera.getBonificaciones(), segunda.getBonificaciones());
        primera.setNombre("Renombrada");
        assertEquals("Prueba", segunda.getNombre());
        assertEquals("Prueba", definicion.nombre());
        var jugador = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 48, 48);
        assertTrue(jugador.equipar(primera));
        assertFalse(jugador.equipar(segunda));
        assertEquals(7, jugador.getFuerza());
        assertEquals(0, jugador.getVelocidad());
        assertEquals(6, jugador.getAtributoBase(Atributo.FUERZA));
        assertTrue(jugador.desequipar(TipoEquipo.ARMA));
        assertEquals(6, jugador.getFuerza());
        assertEquals(2, jugador.getVelocidad());
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5,6,7,8})
    void armaRechazaDatosInvalidos(int caso) {
        Class<? extends RuntimeException> error = switch (caso) {
            case 1, 3, 5, 6 -> IllegalArgumentException.class;
            default -> NullPointerException.class;
        };
        assertThrows(error, () -> {
            switch (caso) {
                case 0 -> new DefinicionArma(null, "Arma", Map.of());
                case 1 -> new DefinicionArma(" ", "Arma", Map.of());
                case 2 -> new DefinicionArma("arma", null, Map.of());
                case 3 -> new DefinicionArma("arma", "\t", Map.of());
                case 4 -> new DefinicionArma("arma", "Arma", null);
                case 5 -> new DefinicionArma("arma", "Arma", Map.of(Atributo.VIDA, 1));
                case 6 -> new DefinicionArma("arma", "Arma", Map.of(Atributo.VIDA, -1));
                case 7 -> {
                    var mapa = new java.util.HashMap<Atributo, Integer>();
                    mapa.put(null, 1);
                    new DefinicionArma("arma", "Arma", mapa);
                }
                case 8 -> {
                    var mapa = new EnumMap<Atributo, Integer>(Atributo.class);
                    mapa.put(Atributo.FUERZA, null);
                    new DefinicionArma("arma", "Arma", mapa);
                }
            }
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5,6})
    void pocionRechazaDatosInvalidos(int caso) {
        Class<? extends RuntimeException> error = switch (caso) {
            case 1, 4, 5, 6 -> IllegalArgumentException.class;
            default -> NullPointerException.class;
        };
        assertThrows(error, () -> {
            switch (caso) {
                case 0 -> new DefinicionPocion(null, TipoPocion.PEQUENA, "Poción", 8);
                case 1 -> new DefinicionPocion(" ", TipoPocion.PEQUENA, "Poción", 8);
                case 2 -> new DefinicionPocion("pocion", null, "Poción", 8);
                case 3 -> new DefinicionPocion("pocion", TipoPocion.PEQUENA, null, 8);
                case 4 -> new DefinicionPocion("pocion", TipoPocion.PEQUENA, " ", 8);
                case 5 -> new DefinicionPocion("pocion", TipoPocion.PEQUENA, "Poción", 0);
                case 6 -> new DefinicionPocion("pocion", TipoPocion.PEQUENA, "Poción", -1);
            }
        });
    }
}
