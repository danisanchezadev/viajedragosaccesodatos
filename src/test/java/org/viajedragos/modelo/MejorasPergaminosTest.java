package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MejorasPergaminosTest {
    private Jugador jugador() { return new Jugador("Prueba", "", 6, 2, 3, 2, 2, 20, 30); }
    private DefinicionEquipo definicion(TipoEquipo tipo) {
        return new DefinicionEquipo("id_" + tipo, "Modelo", tipo, Map.of(Atributo.AGILIDAD, 1));
    }

    @ParameterizedTest
    @EnumSource(TipoEquipo.class)
    void cadaCategoriaAdmiteLosCuatroResultadosUniformesUnaSolaVez(TipoEquipo tipo) {
        var d = definicion(tipo);
        int[] tiradas = {0};
        Random azar = new Random(0) {
            public int nextInt(int limite) { assertEquals(4, limite); return tiradas[0]++; }
        };
        for (int puntos = 0; puntos < 4; puntos++) {
            Equipo equipo = d.crearEquipo(azar);
            assertEquals(puntos, equipo.getMejorasDisponibles());
            assertEquals(0, equipo.getMejorasConsumidas());
            Jugador j = jugador(); j.agregarEquipo(equipo);
            assertTrue(j.equiparDesdeInventario(tipo, 0));
            equipo.getBonificaciones(); equipo.getBonificacionesBase(); equipo.getMejorasDisponibles();
            assertTrue(j.desequiparAlInventario(tipo));
            assertEquals(puntos, equipo.getMejorasDisponibles());
        }
        assertEquals(4, tiradas[0], "Solo al crear, nunca al consultar/equipar/desequipar");
        assertEquals(Map.of(Atributo.AGILIDAD, 1), d.bonificaciones());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 4, Integer.MAX_VALUE})
    void rechazaPuntosFueraDelRango(int puntos) {
        assertThrows(IllegalArgumentException.class, () -> definicion(TipoEquipo.ARMA).crearEquipo(puntos));
        assertThrows(IllegalArgumentException.class, () -> new DefinicionArma("a", "Arma", Map.of()).crearArma(puntos));
    }

    @ParameterizedTest
    @EnumSource(Atributo.class)
    void pergaminosSiempreSonDeUnPuntoYSeAgrupanSinLimiteArtificial(Atributo atributo) {
        Pergamino pergamino = new Pergamino(atributo);
        assertEquals(atributo, pergamino.atributo()); assertEquals(1, pergamino.cantidad());
        assertTrue(pergamino.nombre().startsWith("Pergamino de "));
        if (atributo == Atributo.VIDA) assertEquals("Pergamino de vida máxima", pergamino.nombre());
        Jugador j = jugador();
        for (int i = 0; i < 10000; i++) j.agregarPergamino(pergamino);
        assertEquals(10000, j.getCantidadPergaminos(atributo));
        assertThrows(UnsupportedOperationException.class, () -> j.getPergaminos().clear());
        assertThrows(IllegalArgumentException.class, () -> new Pergamino(atributo, 0));
        assertThrows(IllegalArgumentException.class, () -> new Pergamino(atributo, 2));
    }

    @ParameterizedTest
    @EnumSource(TipoEquipo.class)
    void mejoraAtributoNuevoYExistenteEnTodasLasCategorias(TipoEquipo tipo) {
        Jugador j = jugador(); Equipo pieza = definicion(tipo).crearEquipo(3); j.agregarEquipo(pieza);
        j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        j.agregarPergamino(new Pergamino(Atributo.AGILIDAD));
        assertTrue(j.usarPergamino(Atributo.FUERZA, pieza));
        assertTrue(j.usarPergamino(Atributo.AGILIDAD, pieza));
        assertEquals(1, pieza.getBonificaciones().get(Atributo.FUERZA));
        assertEquals(2, pieza.getBonificaciones().get(Atributo.AGILIDAD));
        assertEquals(1, pieza.getMejorasDisponibles()); assertEquals(2, pieza.getMejorasConsumidas());
        assertEquals(6, j.getFuerza()); assertEquals(2, j.getAgilidad());
        assertTrue(j.getPergaminos().isEmpty());
        assertEquals(Map.of(Atributo.AGILIDAD, 1), pieza.getBonificacionesBase());
        assertTrue(j.equiparDesdeInventario(tipo, 0));
        assertEquals(7, j.getFuerza()); assertEquals(4, j.getAgilidad());
        assertTrue(j.desequiparAlInventario(tipo));
        assertEquals(6, j.getFuerza()); assertEquals(2, j.getAgilidad());
    }

    @ParameterizedTest
    @EnumSource(Atributo.class)
    void mejoraEquipadaActualizaInmediatamenteYSinDuplicar(Atributo atributo) {
        Jugador j = jugador(); Equipo pieza = definicion(TipoEquipo.ANILLO).crearEquipo(3);
        j.equipar(pieza); j.agregarPergamino(new Pergamino(atributo)); j.agregarPergamino(new Pergamino(atributo));
        int antes = atributo == Atributo.VIDA ? j.getVidaMaxima() : j.getAtributo(atributo);
        assertTrue(j.usarPergamino(atributo, pieza));
        assertEquals(antes + 1, atributo == Atributo.VIDA ? j.getVidaMaxima() : j.getAtributo(atributo));
        assertEquals(1, j.getCantidadPergaminos(atributo));
        assertEquals(2, pieza.getMejorasDisponibles());
        assertEquals(20, j.getVida());
        assertTrue(j.desequiparAlInventario(TipoEquipo.ANILLO));
        for (int ciclo = 0; ciclo < 5; ciclo++) {
            assertTrue(j.equiparDesdeInventario(TipoEquipo.ANILLO, 0));
            assertEquals(antes + 1, atributo == Atributo.VIDA ? j.getVidaMaxima() : j.getAtributo(atributo));
            assertTrue(j.desequiparAlInventario(TipoEquipo.ANILLO));
        }
        assertEquals(2, pieza.getMejorasDisponibles());
    }

    @Test
    void rechazosNoConsumenPergaminoNiModificanObjetos() {
        Jugador j = jugador(); var cero = definicion(TipoEquipo.ARMA).crearEquipo(0);
        var ajena = definicion(TipoEquipo.ARMA).crearEquipo(3);
        j.agregarEquipo(cero); j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertFalse(j.usarPergamino(Atributo.FUERZA, cero));
        assertFalse(j.usarPergamino(Atributo.FUERZA, ajena));
        assertFalse(j.usarPergamino(null, cero)); assertFalse(j.usarPergamino(Atributo.FUERZA, null));
        assertFalse(j.usarPergamino(Atributo.VIDA, cero));
        assertEquals(1, j.getCantidadPergaminos(Atributo.FUERZA));
        assertEquals(0, cero.getMejorasDisponibles()); assertEquals(3, ajena.getMejorasDisponibles());
        assertEquals(Map.of(Atributo.AGILIDAD, 1), cero.getBonificaciones());
        assertFalse(ajena.aplicarMejora(Atributo.FUERZA, j));
    }

    @Test
    void dosEjemplaresNoCompartenPuntosNiBonosYMapasNoSePuedenModificar() {
        Jugador j = jugador(); var d = definicion(TipoEquipo.ANILLO);
        Equipo uno = d.crearEquipo(1), dos = d.crearEquipo(3);
        j.agregarEquipo(uno); j.agregarEquipo(dos); j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        var vista = uno.getBonificaciones();
        assertTrue(j.usarPergamino(Atributo.FUERZA, uno));
        assertEquals(0, uno.getMejorasDisponibles()); assertEquals(3, dos.getMejorasDisponibles());
        assertEquals(1, vista.get(Atributo.FUERZA)); assertFalse(dos.getBonificaciones().containsKey(Atributo.FUERZA));
        assertEquals(d.id(), uno.getIdDefinicion()); assertEquals(d.id(), dos.getIdDefinicion());
        assertThrows(UnsupportedOperationException.class, () -> vista.clear());
        assertThrows(UnsupportedOperationException.class, () -> uno.getBonificacionesBase().clear());
        assertThrows(UnsupportedOperationException.class, () -> uno.getBonificacionesAdicionales().clear());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void falloDespuesDeActualizarPortadorRestauraTodo(boolean vida) {
        Jugador j = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 20, 30) {
            public void modificarAtributo(Atributo a, int cantidad) {
                super.modificarAtributo(a, cantidad);
                if (!vida && a == Atributo.FUERZA && cantidad == 1) throw new IllegalStateException("Fallo parcial");
            }
            public void modificarVidaMaximaEquipo(int cantidad) {
                super.modificarVidaMaximaEquipo(cantidad);
                if (vida && cantidad == 1) throw new IllegalStateException("Fallo parcial");
            }
        };
        Equipo pieza = definicion(TipoEquipo.AMULETO).crearEquipo(3); j.equipar(pieza);
        Atributo atributo = vida ? Atributo.VIDA : Atributo.FUERZA;
        j.agregarPergamino(new Pergamino(atributo));
        var modificadores = Map.copyOf(j.getModificadoresEquipo());
        assertFalse(j.usarPergamino(atributo, pieza));
        assertEquals(modificadores, j.getModificadoresEquipo());
        assertEquals(30, j.getVidaMaxima()); assertEquals(20, j.getVida()); assertEquals(6, j.getFuerza());
        assertEquals(3, pieza.getMejorasDisponibles()); assertEquals(0, pieza.getMejorasConsumidas());
        assertEquals(Map.of(Atributo.AGILIDAD, 1), pieza.getBonificaciones());
        assertEquals(1, j.getCantidadPergaminos(atributo));
        assertSame(pieza, j.getEquipoEquipado().get(TipoEquipo.AMULETO));
    }

    @Test
    void desbordamientoNoConsumeNada() {
        Jugador j = jugador();
        var d = new DefinicionEquipo("max", "Máximo", TipoEquipo.ANILLO, Map.of(Atributo.FUERZA, Integer.MAX_VALUE));
        Equipo pieza = d.crearEquipo(1); j.agregarEquipo(pieza); j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertFalse(j.usarPergamino(Atributo.FUERZA, pieza));
        assertEquals(Integer.MAX_VALUE, pieza.getBonificaciones().get(Atributo.FUERZA));
        assertEquals(1, pieza.getMejorasDisponibles()); assertEquals(1, j.getCantidadPergaminos(Atributo.FUERZA));
    }

    @Test
    void vidaMaximaMejoradaNoCuraYRespetaNivelYCambioDePieza() {
        Jugador j = jugador();
        var d = new DefinicionEquipo("amuleto", "Amuleto", TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 2));
        Equipo pieza = d.crearEquipo(3); j.equipar(pieza);
        assertEquals(32, j.getVidaMaxima());
        j.agregarPergamino(new Pergamino(Atributo.VIDA)); assertTrue(j.usarPergamino(Atributo.VIDA, pieza));
        assertEquals(33, j.getVidaMaxima()); assertEquals(20, j.getVida());
        j.agregarExperiencia(20);
        assertEquals(39, j.getVidaMaxima()); assertEquals(36, j.getVidaMaximaBase()); assertEquals(26, j.getVida());
        j.curarse(100); assertEquals(39, j.getVida());
        Equipo otra = d.crearEquipo(0); j.agregarEquipo(otra);
        assertTrue(j.equiparDesdeInventario(TipoEquipo.AMULETO, 0));
        assertEquals(38, j.getVidaMaxima()); assertEquals(38, j.getVida());
        assertEquals(List.of(pieza), j.getInventarioEquipo());
        j.desequiparAlInventario(TipoEquipo.AMULETO);
        assertEquals(36, j.getVidaMaxima()); assertEquals(36, j.getVida());
    }

    @Test
    void habilidadesDeArmaNoSeReaplicanNiPierdenSuEstado() {
        Jugador j = jugador(); j.setVida(1);
        var habilidad = new InstintoSupervivencia();
        Equipo arma = new Arma(null, "Arma", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of(habilidad, 1), 3);
        j.equipar(arma); j.curarse(1); var estado = j.getHabilidades().get(habilidad);
        assertEquals(10, j.getFuerza());
        j.agregarPergamino(new Pergamino(Atributo.FUERZA)); assertTrue(j.usarPergamino(Atributo.FUERZA, arma));
        assertSame(estado, j.getHabilidades().get(habilidad));
        assertEquals(1, estado.getNivel()); assertEquals(1, estado.getVecesEquipada());
        assertTrue(estado.isEfectoTemporalActivo()); assertEquals(11, j.getFuerza());
        j.desequiparAlInventario(TipoEquipo.ARMA);
        assertTrue(j.getHabilidades().isEmpty()); assertEquals(6, j.getFuerza());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ventaMejoradaConservaPrecioOriginalYRestaTodasSusBonificaciones(boolean equipada) {
        Jugador j = jugador(); var d = definicion(TipoEquipo.ANILLO);
        Equipo uno = d.crearEquipo(3), dos = d.crearEquipo(0);
        j.agregarEquipo(uno); j.agregarEquipo(dos);
        j.agregarPergamino(new Pergamino(Atributo.FUERZA)); assertTrue(j.usarPergamino(Atributo.FUERZA, uno));
        if (equipada) assertTrue(j.equiparDesdeInventario(TipoEquipo.ANILLO, 0));
        RepositorioTienda tienda = () -> List.of(new ProductoTienda(d, 11));
        assertTrue(j.venderEquipo(uno, equipada, tienda));
        assertEquals(20, j.getOro()); assertEquals(List.of(dos), j.getInventarioEquipo());
        assertEquals(6, j.getFuerza()); assertEquals(2, j.getAgilidad());
        assertFalse(j.venderEquipo(uno, equipada, tienda));
        assertEquals(0, dos.getMejorasDisponibles());
    }

    @Test
    void compraGeneraUnaVezYNoConsultaAzarParaOfertasOPociones() {
        Jugador j = jugador(); j.agregarOro(100);
        var repo = new RepositorioTiendaMemoria(); int[] tiradas = {0};
        Random azar = new Random(0) { public int nextInt(int limite) { assertEquals(4, limite); return tiradas[0]++; } };
        repo.obtenerTodos(); repo.buscarPorId("amuleto_sencillo"); assertEquals(0, tiradas[0]);
        assertEquals(ResultadoCompra.COMPRADA, j.comprar("pocion_pequena", repo, azar));
        assertEquals(0, tiradas[0]);
        for (int i = 0; i < 4; i++) assertEquals(ResultadoCompra.COMPRADA, j.comprar("amuleto_sencillo", repo, azar));
        assertEquals(List.of(0, 1, 2, 3), j.getInventarioEquipo().stream().map(Equipo::getMejorasDisponibles).toList());
        assertEquals(4, tiradas[0]);
    }

    @ParameterizedTest
    @EnumSource(TipoEquipo.class)
    void constructoresDeEquipoInicialGeneranPuntosSinReiniciarlos(TipoEquipo tipo) {
        Equipo pieza = new Equipo("Inicial", tipo, Map.of(), Map.of());
        assertTrue(pieza.getMejorasDisponibles() >= 0 && pieza.getMejorasDisponibles() <= 3);
        int puntos = pieza.getMejorasDisponibles();
        Jugador j = jugador(); j.equipar(pieza); j.desequiparAlInventario(tipo);
        assertEquals(puntos, pieza.getMejorasDisponibles());
        assertEquals(0, pieza.getMejorasConsumidas());
    }

    @Test
    void tresMejorasAgotanLaPiezaYLaCuartaNoConsumePergamino() {
        Jugador j = jugador(); Equipo pieza = definicion(TipoEquipo.ARMA).crearEquipo(3); j.agregarEquipo(pieza);
        for (int i = 0; i < 4; i++) j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        for (int i = 0; i < 3; i++) assertTrue(j.usarPergamino(Atributo.FUERZA, pieza));
        assertFalse(j.usarPergamino(Atributo.FUERZA, pieza));
        assertEquals(0, pieza.getMejorasDisponibles()); assertEquals(3, pieza.getMejorasConsumidas());
        assertEquals(3, pieza.getBonificaciones().get(Atributo.FUERZA));
        assertEquals(1, j.getCantidadPergaminos(Atributo.FUERZA));
    }

    @Test
    void compraFallidaNoGeneraNiGuardaObjetosYUnaGeneracionInvalidaRestauraSaldo() {
        Jugador j = jugador(); var tienda = new RepositorioTiendaMemoria();
        Random prohibido = new Random(0) { public int nextInt(int limite) { fail("Compra rechazada antes de crear"); return 0; } };
        assertEquals(ResultadoCompra.PRODUCTO_INEXISTENTE, j.comprar("ausente", tienda, prohibido));
        assertEquals(ResultadoCompra.SALDO_INSUFICIENTE, j.comprar("espada_hierro", tienda, prohibido));
        Random invalido = new Random(0) { public int nextInt(int limite) { assertEquals(4, limite); return 4; } };
        assertEquals(ResultadoCompra.INCORPORACION_FALLIDA, j.comprar("anillo_sencillo", tienda, invalido));
        assertEquals(15, j.getOro()); assertTrue(j.getInventarioEquipo().isEmpty());
    }

    @Test
    void incrementoNoAplicadoNoDejaEquipoMejoradoNiConsumePergamino() {
        Jugador j = new Jugador("Prueba", "", 6, 2, 3, 2, 2, 20, 30) {
            public void modificarAtributo(Atributo a, int cantidad) {
                if (a != Atributo.FUERZA) super.modificarAtributo(a, cantidad);
            }
        };
        Equipo pieza = definicion(TipoEquipo.ANILLO).crearEquipo(3); j.equipar(pieza);
        j.agregarPergamino(new Pergamino(Atributo.FUERZA));
        assertFalse(j.usarPergamino(Atributo.FUERZA, pieza));
        assertEquals(6, j.getFuerza()); assertEquals(3, pieza.getMejorasDisponibles());
        assertFalse(pieza.getBonificaciones().containsKey(Atributo.FUERZA));
        assertEquals(1, j.getCantidadPergaminos(Atributo.FUERZA));
    }
}
