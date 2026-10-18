package org.viajedragos.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.*;
import org.viajedragos.combate.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.habilidad.implementacion.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AislamientoHabilidadesTest {
    private Jugador jugador() {
        return new Jugador("Prueba", "", 6, 2, 3, 2, 2, 1, 100);
    }

    private Equipo pieza(TipoEquipo tipo, int fuerza, Habilidad habilidad, int niveles) {
        return new Arma("Prueba", tipo, Map.of(Atributo.FUERZA, fuerza), Map.of(habilidad, niveles));
    }

    private Habilidad habilidad(String nombre) {
        return switch (nombre) {
            case "instinto" -> new InstintoSupervivencia();
            case "impetu" -> new Impetu();
            case "primer" -> new PrimerImpulso(7);
            case "furia" -> new FuriaContenida();
            case "rabia" -> new Rabia();
            default -> throw new IllegalArgumentException(nombre);
        };
    }

    private Map<Atributo, Integer> base(Combatiente personaje) {
        var valores = new EnumMap<Atributo, Integer>(Atributo.class);
        for (Atributo atributo : Atributo.values()) {
            if (atributo != Atributo.VIDA) valores.put(atributo, personaje.getAtributoBase(atributo));
        }
        return valores;
    }

    private void activar(Habilidad habilidad, Jugador personaje) {
        EstadoHabilidad estado = personaje.getHabilidades().get(habilidad);
        Enemigo otro = new Enemigo();
        habilidad.alInicioCombate(personaje, otro, estado);
        ContextoAtaque ataque = new ContextoAtaque(personaje, otro);
        ataque.setDanioFinal(1);
        habilidad.alHacerDanio(ataque, estado);
        ContextoAtaque defensa = new ContextoAtaque(otro, personaje);
        defensa.setDanioFinal(1);
        habilidad.alRecibirDanio(defensa, estado);
    }

    private void finalizar(Habilidad habilidad, Jugador personaje) {
        habilidad.alFinalCombate(personaje, new Enemigo(), personaje.getHabilidades().get(habilidad));
    }

    @Test
    void dosArmasConLaMismaInstanciaDeInstintoActivanYRetiranIndependientemente() {
        Jugador a = jugador();
        Jugador b = jugador();
        Habilidad compartida = new InstintoSupervivencia();
        Equipo armaA = pieza(TipoEquipo.ARMA, 0, compartida, 1);
        Equipo armaB = pieza(TipoEquipo.ARMA, 0, compartida, 1);
        assertNotSame(armaA, armaB);
        assertTrue(a.equipar(armaA));
        assertTrue(b.equipar(armaB));
        assertNotSame(a.getHabilidades().get(compartida), b.getHabilidades().get(compartida));
        // La referencia de Habilidad puede compartirse: sus datos temporales ya no viven en ella.
        assertSame(a.getHabilidades().keySet().iterator().next(), b.getHabilidades().keySet().iterator().next());
        a.curarse(8);
        b.curarse(8);
        assertEquals(9, a.getFuerza());
        assertEquals(9, b.getFuerza());
        assertTrue(b.desequipar(TipoEquipo.ARMA));
        assertEquals(6, b.getFuerza());
        assertEquals(9, a.getFuerza());
        assertTrue(b.getHabilidades().isEmpty());
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(a,
                new Enemigo("Prueba", "", 1, 0, 1, 0, 0, 1, 1), () -> 4).combatir());
        assertEquals(6, a.getFuerza());
        assertFalse(a.getHabilidades().get(compartida).isEfectoTemporalActivo());
        finalizar(compartida, a);
        assertEquals(6, a.getFuerza());
    }

    @Test
    void retirarDelPersonajeInactivoNoDescuentaElEfectoDelOtro() {
        Jugador a = jugador();
        Jugador b = jugador();
        Habilidad compartida = new InstintoSupervivencia();
        a.equipar(pieza(TipoEquipo.ARMA, 1, compartida, 1));
        b.equipar(pieza(TipoEquipo.ARMA, 2, compartida, 1));
        a.curarse(8);
        assertTrue(b.desequipar(TipoEquipo.ARMA));
        assertEquals(6, b.getFuerza());
        assertEquals(10, a.getFuerza());
        finalizar(compartida, a);
        assertEquals(7, a.getFuerza());
    }

    @ParameterizedTest
    @ValueSource(strings = {"instinto", "impetu", "primer", "furia", "rabia"})
    void aprendidaYConcedidaPorEquipoAislanTodosLosEfectos(String nombre) {
        Jugador a = jugador();
        Jugador b = jugador();
        var inicial = base(a);
        Habilidad compartida = habilidad(nombre);
        a.aprenderHabilidad(compartida, 3);
        b.equipar(pieza(TipoEquipo.ARMA, 2, compartida, 4));
        activar(compartida, a);
        var activa = base(a);
        assertNotEquals(inicial, activa);
        assertEquals(inicial, base(b), "Activar A no modifica ni activa B");
        activar(compartida, b);
        assertEquals(activa, base(b));
        assertTrue(b.desequipar(TipoEquipo.ARMA));
        assertEquals(inicial, base(b));
        assertEquals(activa, base(a));
        assertTrue(a.getHabilidades().get(compartida).isAprendida());
        assertEquals(3, a.getHabilidades().get(compartida).getNivel());
        finalizar(compartida, a);
        finalizar(compartida, a);
        assertEquals(inicial, base(a));
    }

    @Test
    void dosPiezasYAprendizajeMantienenUnaEntradaSinDuplicarElEfecto() {
        Jugador a = jugador();
        Jugador b = jugador();
        Habilidad compartida = new InstintoSupervivencia();
        a.aprenderHabilidad(compartida, 3);
        a.equipar(pieza(TipoEquipo.ARMA, 1, compartida, 2));
        a.equipar(pieza(TipoEquipo.BOTAS, 0, new InstintoSupervivencia(), 4));
        b.aprenderHabilidad(compartida, 5);
        EstadoHabilidad estadoA = a.getHabilidades().get(compartida);
        EstadoHabilidad estadoB = b.getHabilidades().get(compartida);
        a.curarse(8);
        b.curarse(8);
        assertEquals(1, a.getHabilidades().size());
        assertEquals(9, estadoA.getNivel());
        assertEquals(2, estadoA.getVecesEquipada());
        assertEquals(10, a.getFuerza());
        assertEquals(9, b.getFuerza());
        a.desequipar(TipoEquipo.ARMA);
        assertSame(estadoA, a.getHabilidades().get(compartida));
        assertEquals(7, estadoA.getNivel());
        assertEquals(1, estadoA.getVecesEquipada());
        a.desequipar(TipoEquipo.BOTAS);
        assertEquals(3, estadoA.getNivel());
        assertEquals(0, estadoA.getVecesEquipada());
        assertEquals(9, a.getFuerza());
        assertEquals(5, estadoB.getNivel());
        finalizar(compartida, a);
        assertEquals(6, a.getFuerza());
        assertEquals(9, b.getFuerza());
        finalizar(compartida, b);
        assertEquals(6, b.getFuerza());
    }

    @Test
    void variasPiezasSinAprendizajeLimpianSoloAlRetirarLaUltima() {
        Jugador a = jugador();
        Jugador b = jugador();
        Habilidad compartida = new InstintoSupervivencia();
        a.equipar(pieza(TipoEquipo.ARMA, 0, compartida, 2));
        a.equipar(pieza(TipoEquipo.BOTAS, 0, compartida, 4));
        b.equipar(pieza(TipoEquipo.ARMA, 0, compartida, 1));
        a.curarse(8);
        b.curarse(8);
        EstadoHabilidad estado = a.getHabilidades().get(compartida);
        a.desequipar(TipoEquipo.ARMA);
        assertSame(estado, a.getHabilidades().get(compartida));
        assertEquals(4, estado.getNivel());
        assertEquals(1, estado.getVecesEquipada());
        assertEquals(9, a.getFuerza());
        a.desequipar(TipoEquipo.BOTAS);
        assertEquals(6, a.getFuerza());
        assertTrue(a.getHabilidades().isEmpty());
        assertEquals(9, b.getFuerza());
        assertTrue(estado.getVecesEquipada() == 0 && !estado.isEfectoTemporalActivo());
    }

    @ParameterizedTest
    @CsvSource({"instinto,true", "instinto,false", "impetu,true", "impetu,false",
            "primer,true", "primer,false", "furia,true", "furia,false", "rabia,true", "rabia,false"})
    void intercambioFallidoRestauraElEstadoPropioSinAfectarAlOtro(String nombre, boolean fallaRetirada) {
        Jugador a = jugador();
        Jugador b = jugador();
        var inicial = base(b);
        Habilidad compartida = habilidad(nombre);
        a.aprenderHabilidad(compartida, 3);
        Equipo vieja = new Arma("Vieja", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 1), Map.of(compartida, 2)) {
            @Override public void retirarEfectos(Combatiente c) {
                super.retirarEfectos(c);
                if (fallaRetirada) throw new IllegalStateException("Fallo tras retirar");
            }
        };
        Equipo nueva = new Arma("Nueva", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 2), Map.of()) {
            @Override public void aplicarEfectos(Combatiente c) {
                super.aplicarEfectos(c);
                c.setVida(50);
                c.setVidaMaxima(200);
                throw new IllegalStateException("Fallo tras aplicar");
            }
        };
        b.equipar(vieja);
        activar(compartida, a);
        activar(compartida, b);
        var activa = base(b);
        var mapa = b.getHabilidades();
        var estado = mapa.get(compartida);
        boolean indicador = estado.isEfectoTemporalActivo();
        int contador = estado.getAcumulacionesTemporales();
        b.agregarEquipo(nueva);
        assertFalse(b.equiparDesdeInventario(TipoEquipo.ARMA, 0));
        assertSame(vieja, b.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(List.of(nueva), b.getInventarioEquipo());
        assertSame(mapa, b.getHabilidades());
        assertSame(estado, mapa.get(compartida));
        assertSame(compartida, mapa.keySet().iterator().next());
        assertEquals(2, estado.getNivel());
        assertEquals(1, estado.getVecesEquipada());
        assertEquals(indicador, estado.isEfectoTemporalActivo());
        assertEquals(contador, estado.getAcumulacionesTemporales());
        assertEquals(1, b.getVida());
        assertEquals(100, b.getVidaMaxima());
        assertEquals(activa, base(b));
        assertEquals(activa, base(a));
        finalizar(compartida, b);
        finalizar(compartida, b);
        assertEquals(inicial, base(b));
        assertEquals(7, b.getFuerza());
        assertEquals(activa, base(a));
        finalizar(compartida, a);
        assertEquals(inicial, base(a));
    }

    @Test
    void intercambiosExitososRepetidosNoAcumulanNiRetiranEfectosDelOtro() {
        Jugador a = jugador();
        Jugador b = jugador();
        Habilidad compartida = new InstintoSupervivencia();
        a.aprenderHabilidad(compartida, 1);
        b.equipar(pieza(TipoEquipo.ARMA, 1, compartida, 2));
        b.agregarEquipo(new Arma("Sin habilidad", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 2), Map.of()));
        a.curarse(8);
        for (int i = 0; i < 10; i++) {
            b.setVida(1);
            b.curarse(8);
            assertEquals(10, b.getFuerza());
            assertTrue(b.equiparDesdeInventario(TipoEquipo.ARMA, 0));
            assertEquals(8, b.getFuerza());
            assertEquals(6, b.getAtributoBase(Atributo.FUERZA));
            assertTrue(b.getHabilidades().isEmpty());
            assertEquals(9, a.getFuerza());
            assertTrue(b.equiparDesdeInventario(TipoEquipo.ARMA, 0));
            assertEquals(7, b.getFuerza());
            assertEquals(2, b.getHabilidades().get(compartida).getNivel());
            assertEquals(1, b.getInventarioEquipo().size());
        }
        finalizar(compartida, a);
        assertEquals(6, a.getFuerza());
    }

    @Test
    void ataquesRealesAcumulanImpetuPorAtacanteSinActivarAlOponente() {
        Jugador a = jugador();
        Jugador b = jugador();
        a.setVida(100);
        b.setVida(100);
        Habilidad compartida = new Impetu();
        a.aprenderHabilidad(compartida, 1);
        b.aprenderHabilidad(compartida, 1);
        Combate combate = new Combate(a, b, () -> 4);
        for (int i = 0; i < 3; i++) combate.ataque(a, b);
        assertEquals(5, a.getAgilidad());
        assertEquals(2, b.getAgilidad());
        assertEquals(3, a.getHabilidades().get(compartida).getAcumulacionesTemporales());
        assertEquals(0, b.getHabilidades().get(compartida).getAcumulacionesTemporales());
        // Defensa fallida para garantizar daño pese a la agilidad acumulada de A.
        int[] dados = {4, 1};
        int[] usados = {0};
        new Combate(a, b, () -> dados[usados[0]++]).ataque(b, a);
        assertEquals(2, usados[0]);
        assertEquals(3, b.getAgilidad());
        finalizar(compartida, b);
        assertEquals(2, b.getAgilidad());
        assertEquals(5, a.getAgilidad());
        finalizar(compartida, a);
        assertEquals(2, a.getAgilidad());
    }

    @Test
    void habilidadesSinEfectosTemporalesMantienenSusReferenciasYNiveles() {
        Jugador a = jugador();
        Jugador b = jugador();
        Habilidad compartida = new GolpeCertero();
        a.aprenderHabilidad(compartida, 3);
        b.aprenderHabilidad(compartida, 5);
        assertSame(compartida, a.getHabilidades().keySet().iterator().next());
        assertSame(compartida, b.getHabilidades().keySet().iterator().next());
        ContextoAtaque contexto = new ContextoAtaque(a, b);
        contexto.setDanioFinal(4);
        compartida.antesDeAplicarDanio(contexto, a.getHabilidades().get(compartida));
        assertEquals(6, contexto.getDanioFinal());
        compartida.antesDeAplicarDanio(contexto, b.getHabilidades().get(compartida));
        assertEquals(6, contexto.getDanioFinal());
        assertEquals(3, a.getHabilidades().get(compartida).getNivel());
        assertEquals(5, b.getHabilidades().get(compartida).getNivel());
    }

    @Test
    void copiaDeEstadoConservaLosDatosTemporalesSinCompartirlos() {
        EstadoHabilidad original = new EstadoHabilidad(true, 2, 7);
        original.setEfectoTemporalActivo(true);
        original.setAcumulacionesTemporales(3);
        EstadoHabilidad copia = new EstadoHabilidad(original);
        assertTrue(copia.isAprendida());
        assertEquals(2, copia.getVecesEquipada());
        assertEquals(7, copia.getNivel());
        assertTrue(copia.isEfectoTemporalActivo());
        assertEquals(3, copia.getAcumulacionesTemporales());
        copia.setEfectoTemporalActivo(false);
        copia.setAcumulacionesTemporales(0);
        assertTrue(original.isEfectoTemporalActivo());
        assertEquals(3, original.getAcumulacionesTemporales());
        assertThrows(IllegalArgumentException.class, () -> copia.setAcumulacionesTemporales(-1));
    }
}
