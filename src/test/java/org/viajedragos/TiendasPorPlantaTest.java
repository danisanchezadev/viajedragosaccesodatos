package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.catalogo.*;
import org.viajedragos.modelo.*;
import org.viajedragos.arma.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TiendasPorPlantaTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private PrintStream salida() { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private String texto() { return TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8)); }
    private Jugador jugador() { var j = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 48, 48); j.agregarOro(200); return j; }

    @ParameterizedTest @ValueSource(ints = {1, 2, 3, 4, 5})
    void productosPorPlantaSonReferenciasDelCatalogoConPreciosUnicos(int planta) {
        var tienda = new RepositorioTiendaMemoria();
        var ofertas = tienda.obtenerPorPlanta(planta); assertFalse(ofertas.isEmpty());
        assertEquals(ofertas.size(), ofertas.stream().map(ProductoTienda::id).distinct().count());
        for (var p : ofertas) {
            assertSame(p, tienda.buscarPorId(p.id()).orElseThrow()); assertEquals(p.precio() / 2, p.precioVenta());
        }
        assertThrows(UnsupportedOperationException.class, ofertas::clear);
        Jugador j = jugador();
        assertTrue(app.consultarTienda(j, new Scanner("1\n0\n0\n"), salida(), tienda, planta));
        for (var p : ofertas) assertTrue(texto().contains(p.nombre() + " - " + p.precio() + " monedas"));
        TextoConsola.comprobarPaneles(bytes.toString(StandardCharsets.UTF_8));
    }

    @Test void cincoCatalogosDistintosCubrenSieteRanurasYProgresionDeCompra() {
        var t = new RepositorioTiendaMemoria(); var catalogos = new HashSet<List<String>>();
        for (int i = 1; i <= 5; i++) catalogos.add(t.obtenerPorPlanta(i).stream().map(ProductoTienda::id).toList());
        assertEquals(5, catalogos.size());
        assertEquals(1, t.obtenerPorPlanta(1).stream().filter(p -> p.pocion() != null).count());
        assertEquals(2, t.obtenerPorPlanta(5).stream().filter(p -> p.pocion() != null).count());
        assertEquals(Set.of(TipoEquipo.values()), t.obtenerPorPlanta(5).stream().filter(p -> p.equipo() != null)
                .map(p -> p.equipo().tipo()).collect(java.util.stream.Collectors.toSet()));
        for (int i = 1; i <= 5; i++) {
            int fuerza = t.obtenerPorPlanta(i).stream().filter(p -> p.equipo() != null)
                    .mapToInt(p -> p.equipo().bonificaciones().getOrDefault(Atributo.FUERZA, 0)).max().orElseThrow();
            assertEquals(new int[]{1, 2, 3, 3, 4}[i - 1], fuerza);
        }
    }

    @Test void comprarFueraDePlantaSeRechazaYVolverRecuperaLaOferta() {
        var tienda = new RepositorioTiendaMemoria(); Jugador j = jugador(); int oro = j.getOro();
        assertEquals(ResultadoCompra.PRODUCTO_INEXISTENTE, j.comprar("espada_forja", tienda.paraPlanta(1)));
        assertEquals(oro, j.getOro()); assertTrue(j.getInventarioEquipo().isEmpty());
        assertEquals(ResultadoCompra.COMPRADA, j.comprar("espada_forja", tienda.paraPlanta(5)));
        assertEquals(oro - 50, j.getOro());
        assertEquals(ResultadoCompra.COMPRADA, j.comprar("espada_desgastada", tienda.paraPlanta(1)));
        assertEquals(oro - 62, j.getOro());
        assertNotSame(j.getInventarioEquipo().get(0), j.getInventarioEquipo().get(1));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void venderEquipoDeOtraPlantaUsaPrecioGlobalInclusoEquipado(boolean equipado) {
        var tienda = new RepositorioTiendaMemoria(); Jugador j = jugador();
        assertEquals(ResultadoCompra.COMPRADA, j.comprar("espada_forja", tienda.paraPlanta(5)));
        Equipo espada = j.getInventarioEquipo().getFirst();
        if (equipado) assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA, 0));
        assertTrue(app.consultarTienda(j, new Scanner("2\n1\n1\n0\n0\n"), salida(), tienda, 1));
        assertEquals(190, j.getOro()); assertTrue(j.getInventarioEquipo().isEmpty());
        assertTrue(j.getEquipoEquipado().isEmpty()); assertEquals(6, j.getFuerza());
        assertTrue(texto().contains("Has vendido Espada de la forja por 25 monedas"));
        assertFalse(j.venderEquipo(espada, equipado, tienda)); assertEquals(190, j.getOro());
    }

    @Test void pocionesDeOtrasPlantasSeVendenAunqueNoSeComprenEnLaActual() {
        var tienda = new RepositorioTiendaMemoria(); Jugador j = jugador();
        assertEquals(ResultadoCompra.COMPRADA, j.comprar("pocion_grande", tienda.paraPlanta(2)));
        assertTrue(app.consultarTienda(j, new Scanner("2\n1\n1\n0\n0\n"), salida(), tienda, 1));
        assertEquals(210, j.getOro()); assertEquals(0, j.getCantidadPociones(TipoPocion.GRANDE));
    }

    @Test void botinExclusivoSeVendeEnOtraPlantaSinAparecerEnCompras() {
        var objetos = new RepositorioObjetosMemoria(); var tienda = new RepositorioTiendaMemoria(objetos);
        int[] valores = {0, 10, 2, 1}, limites = {100, 14, 6, 3}, indice = {0};
        Random azar = new Random(0) {
            public int nextInt(int limite) { assertEquals(limites[indice[0]], limite); return valores[indice[0]++]; }
        };
        Equipo pieza = new BotinEquipamiento(objetos.obtenerBotinEquipamiento()).sortear(2, 3, azar).orElseThrow();
        assertEquals("guantes_reforzados", pieza.getIdDefinicion()); assertEquals(4, indice[0]);
        Jugador j = jugador(); int oro = j.getOro(); j.agregarEquipo(pieza);
        assertTrue(app.consultarTienda(j, new Scanner("1\n0\n2\n1\n1\n0\n0\n"), salida(), tienda, 1));
        assertEquals(oro + 12, j.getOro()); assertTrue(j.getInventarioEquipo().isEmpty());
        assertEquals(6, j.getFuerza()); assertTrue(j.getEquipoEquipado().isEmpty());
        assertTrue(texto().contains("Has vendido Guantes reforzados por 12 monedas"));
        assertEquals(1, texto().split("Guantes reforzados - Cantidad:", -1).length - 1);
        TextoConsola.comprobarPaneles(bytes.toString(StandardCharsets.UTF_8));
    }

    @Test void repositorioAlternativoAsociaIdsSinDuplicarDefinicionesYAdmiteTiendaVacia() {
        var objetos = new RepositorioObjetosMemoria();
        var p = new ProductoTienda(objetos.obtenerArmas().getFirst(), 7);
        var ids = new ArrayList<>(List.of(p.id())); var asociaciones = new HashMap<Integer, List<String>>(); asociaciones.put(3, ids);
        var tienda = new RepositorioTiendaMemoria(List.of(p), objetos.obtenerArmas(), objetos.obtenerPociones(), objetos.obtenerEquipamiento(), asociaciones);
        ids.clear(); asociaciones.clear();
        assertTrue(tienda.obtenerPorPlanta(1).isEmpty()); assertEquals(List.of(p), tienda.obtenerPorPlanta(3));
        assertEquals(ResultadoCompra.COMPRADA, jugador().comprar(p.id(), tienda.paraPlanta(3)));
        assertTrue(app.consultarTienda(jugador(), new Scanner("1\n0\n0\n"), salida(), tienda, 1));
        assertTrue(texto().contains("No hay productos disponibles"));
        RepositorioTienda anterior = () -> List.of(p);
        assertEquals(List.of(p), anterior.obtenerPorPlanta(5));
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4})
    void asociacionesInvalidasNoCreanUnRepositorio(int caso) {
        var o = new RepositorioObjetosMemoria(); var p = new ProductoTienda(o.obtenerArmas().getFirst(), 7);
        Map<Integer, List<String>> mapa = switch (caso) {
            case 0 -> Map.of(0, List.of(p.id()));
            case 1 -> Map.of(6, List.of(p.id()));
            case 2 -> Map.of(1, List.of("desconocido"));
            case 3 -> Map.of(1, List.of(p.id(), p.id()));
            default -> Collections.singletonMap(1, null);
        };
        assertThrows(RuntimeException.class, () -> new RepositorioTiendaMemoria(List.of(p), o.obtenerArmas(), o.obtenerPociones(), o.obtenerEquipamiento(), mapa));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void seleccionExternaAjenaODuplicadaNoPermiteComprar(boolean duplicado) {
        var o = new RepositorioObjetosMemoria(); var p = new ProductoTienda(o.obtenerArmas().getFirst(), 7);
        RepositorioTienda externo = new RepositorioTienda() {
            public List<ProductoTienda> obtenerTodos() { return List.of(p); }
            public List<ProductoTienda> obtenerPorPlanta(int planta) { return duplicado ? List.of(p, p) : List.of(new ProductoTienda(p.arma(), 99)); }
        };
        Jugador j = jugador(); assertTrue(app.consultarTienda(j, new Scanner("1\n1\n"), salida(), externo, 1));
        assertEquals(215, j.getOro()); assertTrue(j.getInventarioEquipo().isEmpty()); assertTrue(texto().contains("no es válido"));
    }
}
