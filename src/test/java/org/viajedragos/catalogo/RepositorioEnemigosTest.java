package org.viajedragos.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.modelo.DefinicionEnemigo;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.habilidad.implementacion.Impetu;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioEnemigosTest {
    @Test
    void catalogoContieneExactamenteLasDefinicionesAutorizadasEnOrdenEstable() {
        var repositorio = new RepositorioEnemigosMemoria();
        var catalogo = repositorio.obtenerTodos();
        assertEquals(List.of(
                new DefinicionEnemigo("lobo", "Lobo", "", 5, 2, 2, 1, 2, 18),
                new DefinicionEnemigo("goblin", "Goblin", "", 3, 1, 2, 2, 3, 12),
                new DefinicionEnemigo("bandido", "Bandido", "", 4, 1, 2, 1, 2, 14),
                new DefinicionEnemigo("esqueleto", "Esqueleto", "", 3, 2, 1, 1, 1, 14)), catalogo);
        assertEquals(4, catalogo.stream().map(DefinicionEnemigo::id).distinct().count());
        assertEquals(catalogo, repositorio.obtenerTodos());
        assertThrows(UnsupportedOperationException.class, catalogo::clear);
        assertThrows(UnsupportedOperationException.class, () -> catalogo.set(0, catalogo.get(1)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void crearEnemigoConservaDatosYNoComparteEstadoMutable(int indice) {
        var d = new RepositorioEnemigosMemoria().obtenerTodos().get(indice);
        var primero = d.crearEnemigo();
        var segundo = d.crearEnemigo();
        assertNotSame(primero, segundo);
        assertEquals(d.nombre(), segundo.getNombre());
        assertEquals(d.descripcion(), segundo.getDescripcion());
        assertEquals(d.fuerza(), segundo.getFuerza());
        assertEquals(d.resistencia(), segundo.getResistencia());
        assertEquals(d.iniciativa(), segundo.getIniciativa());
        assertEquals(d.agilidad(), segundo.getAgilidad());
        assertEquals(d.velocidad(), segundo.getVelocidad());
        assertEquals(d.vidaMaxima(), segundo.getVida());
        assertEquals(d.vidaMaxima(), segundo.getVidaMaxima());
        primero.recibirDanio(primero.getVida());
        primero.setVidaMaxima(100);
        primero.setFuerza(100);
        Impetu habilidad = new Impetu();
        primero.equipar(new Arma("Prueba", TipoEquipo.ARMA,
                Map.of(Atributo.FUERZA, 3), Map.of(habilidad, 2)));
        primero.getHabilidades().get(habilidad).setNivel(5);
        for (var independiente : List.of(segundo, d.crearEnemigo())) {
            assertEquals(d.vidaMaxima(), independiente.getVida());
            assertEquals(d.vidaMaxima(), independiente.getVidaMaxima());
            assertEquals(d.fuerza(), independiente.getFuerza());
            assertTrue(independiente.getEquipoEquipado().isEmpty());
            assertTrue(independiente.getHabilidades().isEmpty());
            assertTrue(independiente.getModificadoresEquipo().values().stream().allMatch(n -> n == 0));
        }
    }

    @ParameterizedTest
    @CsvSource({"-1,1,1,1,1,1", "1,-1,1,1,1,1", "1,1,-1,1,1,1", "1,1,1,-1,1,1",
            "1,1,1,1,-1,1", "1,1,1,1,1,0", "1,1,1,1,1,-1"})
    void rechazaAtributosInvalidos(int fuerza, int resistencia, int iniciativa, int agilidad, int velocidad, int vida) {
        assertThrows(IllegalArgumentException.class, () -> new DefinicionEnemigo("id", "Nombre", "",
                fuerza, resistencia, iniciativa, agilidad, velocidad, vida));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void identificadorYNombreDebenTenerContenido(String vacio) {
        assertThrows(IllegalArgumentException.class, () -> new DefinicionEnemigo(vacio, "Nombre", "", 0,0,0,0,0,1));
        assertThrows(IllegalArgumentException.class, () -> new DefinicionEnemigo("id", vacio, "", 0,0,0,0,0,1));
    }

    @Test
    void rechazaTextosNulosPeroPermiteDescripcionVaciaYAtributosCero() {
        assertThrows(NullPointerException.class, () -> new DefinicionEnemigo(null,"Nombre","",0,0,0,0,0,1));
        assertThrows(NullPointerException.class, () -> new DefinicionEnemigo("id",null,"",0,0,0,0,0,1));
        assertThrows(NullPointerException.class, () -> new DefinicionEnemigo("id","Nombre",null,0,0,0,0,0,1));
        assertEquals(1, new DefinicionEnemigo("id","Nombre","",0,0,0,0,0,1).crearEnemigo().getVida());
    }
}
