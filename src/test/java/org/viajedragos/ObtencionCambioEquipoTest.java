package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.RepositorioEnemigos;
import org.viajedragos.combate.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.modelo.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ObtencionCambioEquipoTest {
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private PrintStream consola() { return new PrintStream(salida,true,StandardCharsets.UTF_8); }
    private String texto() { return org.viajedragos.TextoConsola.contenido(salida.toString(StandardCharsets.UTF_8)); }
    private Jugador jugador() { return new Jugador("Prueba", "",6,2,3,2,2,48,48); }
    private Equipo pieza(String nombre, TipoEquipo tipo, int fuerza) {
        return new Arma(nombre,tipo,Map.of(Atributo.FUERZA,fuerza),Map.of());
    }
    private Jugador preparado() {
        Jugador j = jugador();
        j.equipar(pieza("Actual",TipoEquipo.ARMA,1));
        j.agregarEquipo(pieza("Nueva",TipoEquipo.ARMA,2));
        return j;
    }
    private final RepositorioEnemigos lobos = () -> List.of(new DefinicionEnemigo("lobo","Lobo","",5,2,2,1,2,18));

    private boolean aventura(Jugador j, String entrada, int... valores) {
        int[] usadas = {0};
        Random botin = new Random(0) {
            @Override public int nextInt(int limite) { assertEquals(100,limite); return 99; }
        };
        boolean resultado = app.jugarAventura(j,new Scanner(entrada),consola(),() -> {
            assertTrue(usadas[0] < valores.length,"No se permiten dados adicionales");
            return valores[usadas[0]++];
        },botin,lobos,new Random(0));
        assertEquals(valores.length,usadas[0]);
        return resultado;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void segundaVictoriaOfreceUnaVezYGuardarNoEquipa(boolean guardar) {
        Jugador j = jugador();
        assertFalse(aventura(j,"1\n1\n3\n1\nx\n" + (guardar ? "1" : "2") + "\n3\n1\n0\n",
                1,1,1,1,6,1,6, 1,1,1,1,6,1,6, 1,1,1,1,6,1,6));
        assertEquals(3,texto().split("Victoria\\.",-1).length-1);
        assertEquals(1,texto().split("Has encontrado una Espada de hierro",-1).length-1);
        assertEquals(1,texto().split("Has encontrado una Espada desgastada",-1).length-1);
        assertTrue(texto().indexOf("Has encontrado una Espada de hierro") > texto().indexOf("ENCUENTRO 2"));
        assertTrue(texto().indexOf("Has encontrado una Espada de hierro") < texto().indexOf("ENCUENTRO 3"));
        assertEquals(guardar ? 1 : 0,j.getInventarioEquipo().size());
        if (guardar) {
            assertEquals("Espada de hierro",j.getInventarioEquipo().getFirst().getNombre());
            assertTrue(j.getInventarioEquipo().getFirst().getHabilidadesOtorgadas().isEmpty());
        }
        assertEquals("Espada desgastada",j.getEquipoEquipado().get(TipoEquipo.ARMA).getNombre());
        assertEquals(7,j.getFuerza());
    }

    @Test
    void huidaAnteriorNoCuentaComoVictoria() {
        Jugador j = jugador();
        assertFalse(aventura(j,"3\n3\n1\n2\n3\n1\n1\n0\n",
                1,1,1,1,6,1, 1,1,1,1,6,1,6, 1,1,1,1,6,1,6));
        assertTrue(texto().contains("Has escapado"));
        assertTrue(texto().indexOf("Has encontrado una Espada de hierro") > texto().indexOf("ENCUENTRO 3"));
        assertEquals(1,j.getInventarioEquipo().size());
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertEquals(6,j.getFuerza());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void muerteTrasPrimeraVictoriaNoConcedeHierro(boolean simultanea) {
        Jugador j = jugador();
        j.aprenderHabilidad(new Habilidad("Muerte de prueba") {
            int inicios;
            @Override public void alInicioCombate(Combatiente p,Combatiente o,EstadoHabilidad e) {
                if (++inicios == 2) {
                    p.setVida(0);
                    if (simultanea) o.setVida(0);
                }
            }
        },1);
        assertTrue(aventura(j,"1\n2\n3\n",1,1,1,1,6,1,6));
        assertFalse(texto().contains("Espada de hierro"));
        assertTrue(j.getInventarioEquipo().isEmpty());
    }

    @Test
    void cierreEnOfertaNoAlmacenaNiConsumeDadosAdicionales() {
        Jugador j = jugador();
        assertFalse(aventura(j,"1\n2\n3\n1\n",1,1,1,1,6,1,6, 1,1,1,1,6,1,6));
        assertTrue(texto().contains("Espada de hierro"));
        assertTrue(texto().contains("Entrada finalizada"));
        assertTrue(j.getInventarioEquipo().isEmpty());
    }

    @Test
    void guardarCompararCambiarYCombatirConFuerzaCorrecta() {
        Jugador j = jugador();
        int[] inicios = {0};
        j.aprenderHabilidad(new Habilidad("Observar equipo") {
            @Override public void alInicioCombate(Combatiente p,Combatiente o,EstadoHabilidad e) {
                inicios[0]++;
                if (inicios[0] == 3) {
                    assertEquals(8,j.getFuerza());
                    assertEquals("Espada de hierro",j.getEquipoEquipado().get(TipoEquipo.ARMA).getNombre());
                    assertEquals("Espada desgastada",j.getInventarioEquipo().getFirst().getNombre());
                }
            }
        },1);
        assertFalse(aventura(j,"1\n1\n3\n1\n1\n1\n2\n1\n1\n1\n0\n0\n2\n1\n1\n1\n0\n0\n3\n1\n0\n",
                1,1,1,1,6,1,6, 1,1,1,1,6,1,6, 1,1,1,1,6,1,6));
        assertEquals(3,inicios[0]);
        assertTrue(texto().contains("Actual: Espada desgastada | Nuevo: Espada de hierro"));
        assertEquals(1,j.getInventarioEquipo().size());
        assertEquals(8,j.getFuerza());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2\n0\n0\n", "2\n1\n0\n0\n0\n", "2\n1\n1\n0\n0\n0\n"})
    void cancelarAntesDeConfirmarConservaTodo(String entrada) {
        Jugador j = preparado();
        var equipo = Map.copyOf(j.getEquipoEquipado());
        var inventario = List.copyOf(j.getInventarioEquipo());
        assertTrue(app.consultarInventario(j,new Scanner(entrada),consola()));
        assertEquals(equipo,j.getEquipoEquipado());
        assertEquals(inventario,j.getInventarioEquipo());
        assertEquals(7,j.getFuerza());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2\n", "2\n1\n", "2\n1\n1\n"})
    void eofAntesDeConfirmarConservaTodo(String entrada) {
        Jugador j = preparado();
        var equipo = Map.copyOf(j.getEquipoEquipado());
        var inventario = List.copyOf(j.getInventarioEquipo());
        assertFalse(app.consultarInventario(j,new Scanner(entrada),consola()));
        assertEquals(equipo,j.getEquipoEquipado());
        assertEquals(inventario,j.getInventarioEquipo());
        assertEquals(7,j.getFuerza());
    }

    @Test
    void ranuraVaciaSePuedeEquiparYLosInvalidosNoConfirman() {
        Jugador j = jugador();
        Equipo pieza = pieza("Guardada",TipoEquipo.ARMA,2);
        j.agregarEquipo(pieza);
        assertTrue(app.consultarInventario(j,new Scanner("2\nx\n99\n1\nx\n2\n1\nx\n2\n1\n0\n"),consola()));
        assertSame(pieza,j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertTrue(j.getInventarioEquipo().isEmpty());
        assertEquals(8,j.getFuerza());
        assertEquals(6,texto().split("Entrada inválida",-1).length-1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void vaciosEIncompatiblesPermitenVolver(boolean vacio) {
        Jugador j = jugador();
        if (!vacio) j.agregarEquipo(pieza("Botas",TipoEquipo.BOTAS,1));
        assertTrue(app.consultarInventario(j,new Scanner(vacio ? "2\n0\n" : "2\n1\n0\n0\n"),consola()));
        assertTrue(texto().contains(vacio ? "Sin equipamiento" : "No tienes piezas guardadas compatibles"));
        assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @Test
    void falloDeEquipamientoSeInformaSinAnunciarExito() {
        Jugador j = preparado();
        j.agregarEquipo(new Arma("Falla",TipoEquipo.ARMA,Map.of(),Map.of()) {
            @Override public void aplicarEfectos(Combatiente c) { throw new IllegalStateException("Fallo"); }
        });
        Equipo actual = j.getEquipoEquipado().get(TipoEquipo.ARMA);
        assertTrue(app.consultarInventario(j,new Scanner("2\n1\n2\n1\n0\n0\n"),consola()));
        assertSame(actual,j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(2,j.getInventarioEquipo().size());
        assertEquals(7,j.getFuerza());
        assertTrue(texto().contains("Se conserva el estado anterior"));
        assertFalse(texto().contains("Falla equipada"));
    }

    @Test
    void aventuraNuevaTrasGuardarEmpiezaSinLaPiezaAnterior() {
        int[] inicios = {0};
        int[] consultasObjetos = {0};
        var memoria = new org.viajedragos.catalogo.RepositorioObjetosMemoria();
        var repo = new org.viajedragos.catalogo.RepositorioObjetos() {
            public List<DefinicionArma> obtenerArmas() { consultasObjetos[0]++; return memoria.obtenerArmas(); }
            public List<DefinicionPocion> obtenerPociones() { return memoria.obtenerPociones(); }
        };
        int[] tiradas = {1,1,1,1,6,1,6, 1,1,1,1,6,1,6, 1,1,1,1,6,1,6};
        app.ejecutar(new Scanner("1\nAventurero\n1\n2\n3\n1\n1\n4\n1\nAventurero\n1\n2\n1\n2\n0\n0\n0\n"),consola(),
                () -> tiradas[inicios[0]++],new Random(0),lobos,new Random(0),repo);
        assertEquals(tiradas.length,inicios[0]);
        assertEquals(2,consultasObjetos[0]);
        String segunda = texto().substring(texto().lastIndexOf("ENCUENTRO 1"));
        assertTrue(segunda.contains("Sin equipamiento"));
        assertTrue(segunda.contains("Sin equipamiento"));
        assertTrue(segunda.contains("48/48"));
    }
}
