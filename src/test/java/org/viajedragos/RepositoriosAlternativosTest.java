package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.combate.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RepositoriosAlternativosTest {
    // Dobles solo de prueba: no acceden a ficheros, red ni bases de datos.
    private static final class ObjetosPrueba implements RepositorioObjetos {
        int lecturasArmas, lecturasPociones;
        final List<DefinicionArma> armas = List.of(
                new DefinicionArma("daga_prueba", "Daga alternativa", Map.of(Atributo.FUERZA, 3)),
                new DefinicionArma(DefinicionArma.ID_ESPADA_HIERRO, "Hierro alternativo", Map.of(Atributo.FUERZA, 4)),
                new DefinicionArma(DefinicionArma.ID_ESPADA_DESGASTADA, "Desgastada alternativa", Map.of(Atributo.FUERZA, 2)));
        final List<DefinicionPocion> pociones = List.of(
                new DefinicionPocion("tonico", TipoPocion.PEQUENA, "Tónico alternativo", 11),
                new DefinicionPocion("elixir", TipoPocion.GRANDE, "Elixir alternativo", 21));
        public List<DefinicionArma> obtenerArmas() { lecturasArmas++; return armas; }
        public List<DefinicionPocion> obtenerPociones() { lecturasPociones++; return pociones; }
    }
    private static final class EnemigosPrueba implements RepositorioEnemigos {
        int lecturas;
        public List<DefinicionEnemigo> obtenerTodos() {
            lecturas++;
            return List.of(new DefinicionEnemigo("rata", "Rata de prueba", "", 1, 0, 1, 0, 0, 1),
                    new DefinicionEnemigo("cuervo", "Cuervo de prueba", "", 2, 0, 1, 0, 0, 1));
        }
    }
    private static final class TiendaPrueba implements RepositorioTienda {
        private final List<ProductoTienda> productos;
        TiendaPrueba(ObjetosPrueba objetos) {
            productos = List.of(new ProductoTienda(objetos.pociones.getFirst(), 3),
                    new ProductoTienda(objetos.armas.getFirst(), 7));
        }
        public List<ProductoTienda> obtenerTodos() { return productos; }
    }
    private final ObjetosPrueba objetos = new ObjetosPrueba();
    private final EnemigosPrueba enemigos = new EnemigosPrueba();
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private Jugador jugador() { return new Jugador("Prueba", "", 20, 2, 3, 2, 2, 48, 48); }
    private Random sinBotin() {
        return new Random(0) {
            public int nextInt(int limite) { assertEquals(100, limite); return 99; }
        };
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void aventuraCombateCompraYVentaConTresRepositoriosAlternativos(int indice) {
        Jugador jugador = jugador();
        int[] tiradas = {0};
        var tienda = new TiendaPrueba(objetos);
        Random seleccion = new Random(0) {
            public int nextInt(int limite) { assertEquals(2, limite); return indice; }
        };
        assertFalse(app.jugarAventura(jugador,
                new Scanner("1\n2\n5\n1\n1\n2\n0\n2\n1\n1\n1\n1\n0\n0\n0\n"), salida(),
                () -> { assertTrue(++tiradas[0] <= 6); return 4; }, sinBotin(), enemigos,
                seleccion, objetos, tienda));
        assertEquals(6, tiradas[0]);
        assertEquals(1, enemigos.lecturas);
        assertEquals(1, objetos.lecturasArmas);
        assertEquals(1, objetos.lecturasPociones);
        assertEquals(14, jugador.getOro()); // 15 + 5 - 3 - 7 + 1 + 3
        assertEquals(48, jugador.getVida());
        assertTrue(jugador.getInventarioEquipo().isEmpty());
        assertTrue(jugador.getInventarioPociones().isEmpty());
        assertTrue(jugador.getEquipoEquipado().isEmpty());
        assertSame(objetos.pociones.getFirst(), jugador.getDefinicionPocion(TipoPocion.PEQUENA));
        String texto = TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8));
        assertTrue(texto.contains(indice == 0 ? "Rata de prueba" : "Cuervo de prueba"));
        assertTrue(texto.contains("Victoria."));
        assertTrue(texto.contains("Tónico alternativo - 3 monedas"));
        assertTrue(texto.contains("Daga alternativa - 7 monedas"));
        assertTrue(texto.contains("Has vendido Tónico alternativo por 1 monedas"));
        assertTrue(texto.contains("Has vendido Daga alternativa por 3 monedas"));
        assertFalse(texto.contains("Poción pequeña - 5 monedas"));
    }

    @Test
    void nuevaArmaSeCompraEquipaCombateYVendeSinCambiarElMotor() {
        Jugador j = jugador();
        var tienda = new TiendaPrueba(objetos);
        j.configurarPociones(objetos.pociones);
        assertEquals(ResultadoCompra.COMPRADA, j.comprar("daga_prueba", tienda));
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
        assertEquals(23, j.getFuerza());
        Equipo daga = j.getEquipoEquipado().get(TipoEquipo.ARMA);
        Enemigo enemigo = enemigos.obtenerTodos().getFirst().crearEnemigo();
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(j, enemigo, () -> 4).combatir());
        assertTrue(j.venderEquipo(daga, true, tienda));
        assertEquals(20, j.getFuerza());
        assertEquals(11, j.getOro());
        assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void catalogoComercialIncoherenteImpideEmpezarSinConsumirEntrada(int caso) {
        ProductoTienda valido = new TiendaPrueba(objetos).obtenerTodos().getFirst();
        RepositorioTienda tienda = switch (caso) {
            case 0 -> () -> List.of(new ProductoTienda(new DefinicionArma("ausente", "Ausente", Map.of()), 1));
            case 1 -> () -> List.of(new ProductoTienda(new DefinicionPocion("tonico", TipoPocion.PEQUENA, "Otra", 11), 3));
            case 2 -> () -> List.of(valido, valido);
            case 3 -> () -> null;
            case 4 -> () -> { throw new IllegalStateException("Error del origen"); };
            default -> new RepositorioTienda() {
                public List<ProductoTienda> obtenerTodos() { return List.of(valido); }
                public Optional<ProductoTienda> buscarPorId(String id) { return Optional.empty(); }
            };
        };
        Scanner entrada = new Scanner("pendiente\n");
        Jugador j = jugador();
        var definicionAnterior = j.getDefinicionPocion(TipoPocion.PEQUENA);
        assertTrue(app.jugarAventura(j, entrada, salida(), () -> { fail("No hay combate"); return 1; },
                sinBotin(), enemigos, new Random(0), objetos, tienda));
        assertEquals("pendiente", entrada.nextLine());
        assertSame(definicionAnterior, j.getDefinicionPocion(TipoPocion.PEQUENA));
        assertEquals(15, j.getOro());
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("no es válido"));
    }

    @Test
    void tiendaVaciaEsValidaYPermiteCombatir() {
        Jugador j = jugador();
        assertFalse(app.jugarAventura(j, new Scanner("1\n2\n5\n1\n0\n0\n0\n"), salida(), () -> 4,
                sinBotin(), enemigos, new Random(0), objetos, List::of));
        assertEquals(20, j.getOro());
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("No hay productos disponibles."));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void errorDelOrigenDeEnemigosUObjetosNoIniciaCombate(boolean errorEnemigos) {
        RepositorioEnemigos repoEnemigos = errorEnemigos
                ? () -> { throw new IllegalStateException("Origen no disponible"); } : enemigos;
        RepositorioObjetos repoObjetos = errorEnemigos ? objetos : new RepositorioObjetos() {
            public List<DefinicionArma> obtenerArmas() { throw new IllegalStateException("Origen no disponible"); }
            public List<DefinicionPocion> obtenerPociones() { return objetos.pociones; }
        };
        Scanner entrada = new Scanner("pendiente\n");
        assertTrue(app.jugarAventura(jugador(), entrada, salida(), () -> { fail("No hay combate"); return 1; },
                sinBotin(), repoEnemigos, new Random(0), repoObjetos));
        assertEquals("pendiente", entrada.nextLine());
    }
}
