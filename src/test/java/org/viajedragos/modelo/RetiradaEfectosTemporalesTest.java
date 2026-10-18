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

class RetiradaEfectosTemporalesTest {
    private Jugador jugador() { return new Jugador("Prueba", "",6,2,3,2,2,1,100); }
    private Equipo arma(String nombre, int fuerza, Map<Habilidad,Integer> habilidades) {
        return new Arma(nombre,TipoEquipo.ARMA,Map.of(Atributo.FUERZA,fuerza),habilidades);
    }
    private Habilidad habilidad(String nombre) {
        return switch (nombre) {
            case "instinto" -> new InstintoSupervivencia();
            case "impetu" -> new Impetu();
            case "primer" -> new PrimerImpulso(3);
            case "furia" -> new FuriaContenida();
            case "rabia" -> new Rabia();
            default -> throw new IllegalArgumentException(nombre);
        };
    }
    private Map<Atributo,Integer> base(Combatiente c) {
        var valores = new EnumMap<Atributo,Integer>(Atributo.class);
        for (Atributo atributo : Atributo.values()) {
            if (atributo != Atributo.VIDA) valores.put(atributo,c.getAtributoBase(atributo));
        }
        return valores;
    }
    private void activar(Habilidad h, Jugador j) {
        var estado = j.getHabilidades().get(h);
        Enemigo oponente = new Enemigo();
        h.alInicioCombate(j,oponente,estado);
        var ataque = new ContextoAtaque(j,oponente);
        ataque.setDanioFinal(1);
        h.alHacerDanio(ataque,estado);
        var defensa = new ContextoAtaque(oponente,j);
        defensa.setDanioFinal(1);
        h.alRecibirDanio(defensa,estado);
    }

    @Test
    void reproduceCasoExactoDePocionIntercambioYSiguienteCombate() {
        Jugador j = jugador();
        var instinto = new InstintoSupervivencia();
        Equipo vieja = arma("Vieja",1,Map.of(instinto,1));
        Equipo nueva = arma("Nueva",2,Map.of());
        assertTrue(j.equipar(vieja));
        j.agregarPocion(TipoPocion.PEQUENA);
        assertEquals(8,j.usarPocion(TipoPocion.PEQUENA));
        assertEquals(9,j.getVida());
        assertEquals(9,j.getAtributoBase(Atributo.FUERZA));
        assertEquals(10,j.getFuerza());
        j.agregarEquipo(nueva);
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA,0));
        assertTrue(j.getHabilidades().isEmpty());
        assertEquals(6,j.getAtributoBase(Atributo.FUERZA));
        assertEquals(8,j.getFuerza());
        assertSame(nueva,j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(List.of(vieja),j.getInventarioEquipo());
        assertEquals(ResultadoCombate.ENEMIGO_MUERE,new Combate(j,
                new Enemigo("Enemigo", "",1,0,1,0,0,1,1),() -> 4).combatir());
        assertEquals(6,j.getAtributoBase(Atributo.FUERZA));
        assertEquals(8,j.getFuerza());
        assertEquals(9,j.getVida());
        assertEquals(0,j.getCantidadPociones(TipoPocion.PEQUENA));
    }

    @ParameterizedTest
    @CsvSource({"instinto,true","instinto,false","impetu,true","impetu,false","primer,true","primer,false",
            "furia,true","furia,false","rabia,true","rabia,false"})
    void ultimaFuenteLimpiaSoloElEfectoExistente(String nombre, boolean activo) {
        Jugador j = jugador();
        var inicial = base(j);
        Habilidad h = habilidad(nombre);
        Equipo vieja = arma("Vieja",1,Map.of(h,2));
        j.equipar(vieja);
        if (activo) activar(h,j);
        Equipo nueva = arma("Nueva",2,Map.of());
        j.agregarEquipo(nueva);
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA,0));
        assertEquals(inicial,base(j));
        assertEquals(8,j.getFuerza());
        assertTrue(j.getHabilidades().isEmpty());
        assertEquals(List.of(vieja),j.getInventarioEquipo());
        // La limpieza repetida es inocua y no descuenta otra vez el efecto.
        h.retirarEfectosTemporales(j,new EstadoHabilidad());
        assertEquals(inicial,base(j));
    }

    @ParameterizedTest
    @CsvSource({"instinto,true","instinto,false","impetu,true","impetu,false","primer,true","primer,false",
            "furia,true","furia,false","rabia,true","rabia,false"})
    void falloTrasLimpiezaRestauraTambienElIndicadorInterno(String nombre, boolean fallaRetirada) {
        Jugador j = jugador();
        var inicial = base(j);
        Habilidad h = habilidad(nombre);
        Equipo vieja = new Arma("Vieja",TipoEquipo.ARMA,Map.of(Atributo.FUERZA,1),Map.of(h,2)) {
            @Override public void retirarEfectos(Combatiente c) {
                super.retirarEfectos(c);
                if (fallaRetirada) throw new IllegalStateException("Fallo después de limpiar");
            }
        };
        Equipo nueva = new Arma("Nueva",TipoEquipo.ARMA,Map.of(Atributo.FUERZA,2),Map.of()) {
            @Override public void aplicarEfectos(Combatiente c) {
                super.aplicarEfectos(c);
                c.setVida(50);
                c.setVidaMaxima(200);
                throw new IllegalStateException("Fallo después de aplicar");
            }
        };
        j.equipar(vieja);
        activar(h,j);
        var activa = base(j);
        var estado = j.getHabilidades().get(h);
        var mapa = j.getHabilidades();
        j.agregarEquipo(nueva);
        assertFalse(j.equiparDesdeInventario(TipoEquipo.ARMA,0));
        assertEquals(activa,base(j));
        assertEquals(1,j.getVida());
        assertEquals(100,j.getVidaMaxima());
        assertSame(mapa,j.getHabilidades());
        assertSame(estado,j.getHabilidades().get(h));
        assertSame(h,j.getHabilidades().keySet().iterator().next());
        assertEquals(2,estado.getNivel());
        assertEquals(1,estado.getVecesEquipada());
        assertFalse(estado.isAprendida());
        assertSame(vieja,j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(List.of(nueva),j.getInventarioEquipo());
        h.alFinalCombate(j,new Enemigo(),estado);
        assertEquals(inicial,base(j),"El indicador restaurado debe permitir limpiar el efecto exactamente una vez");
        assertEquals(7,j.getFuerza());
        h.alFinalCombate(j,new Enemigo(),estado);
        assertEquals(inicial,base(j));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void aprendidaOCompartidaConservaEfectoEstadoYNiveles(boolean aprendida) {
        Jugador j = jugador();
        var h = new InstintoSupervivencia();
        if (aprendida) j.aprenderHabilidad(h,3);
        j.equipar(arma("Vieja",1,Map.of(h,2)));
        if (!aprendida) j.equipar(new Arma("Botas",TipoEquipo.BOTAS,Map.of(),Map.of(new InstintoSupervivencia(),4)));
        j.agregarPocion(TipoPocion.PEQUENA);
        assertEquals(8,j.usarPocion(TipoPocion.PEQUENA));
        var estado = j.getHabilidades().get(h);
        j.agregarEquipo(arma("Nueva",2,Map.of()));
        assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA,0));
        assertSame(estado,j.getHabilidades().get(h));
        assertSame(h,j.getHabilidades().keySet().iterator().next());
        assertEquals(aprendida ? 3 : 4,estado.getNivel());
        assertEquals(aprendida ? 0 : 1,estado.getVecesEquipada());
        assertEquals(aprendida,estado.isAprendida());
        assertEquals(9,j.getAtributoBase(Atributo.FUERZA));
        assertEquals(11,j.getFuerza());
        h.alFinalCombate(j,new Enemigo(),estado);
        assertEquals(6,j.getAtributoBase(Atributo.FUERZA));
        assertEquals(8,j.getFuerza());
    }

    @Test
    void quitarConInstanciaEquivalenteLimpiaLaInstanciaPropietaria() {
        Jugador j = jugador();
        Habilidad activa = new InstintoSupervivencia();
        j.agregarHabilidadEquipo(activa,1);
        activar(activa,j);
        j.quitarHabilidadEquipo(new InstintoSupervivencia(),1);
        assertEquals(6,j.getFuerza());
        assertTrue(j.getHabilidades().isEmpty());
    }

    @Test
    void repetirEquipamientosNoAcumulaEfectosTemporales() {
        Jugador j = jugador();
        var h = new InstintoSupervivencia();
        Equipo vieja = arma("Vieja",1,Map.of(h,1));
        j.equipar(vieja);
        j.agregarEquipo(arma("Nueva",2,Map.of()));
        for (int i = 0; i < 10; i++) {
            activar(h,j);
            assertEquals(10,j.getFuerza());
            assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA,0));
            assertEquals(8,j.getFuerza());
            assertEquals(6,j.getAtributoBase(Atributo.FUERZA));
            assertTrue(j.equiparDesdeInventario(TipoEquipo.ARMA,0));
            assertEquals(7,j.getFuerza());
            assertEquals(1,j.getInventarioEquipo().size());
        }
    }

    @Test
    void retornoFalseTrasDesequiparRestauraInstintoActivo() {
        Equipo nueva = arma("Nueva",2,Map.of());
        Jugador j = new Jugador("Prueba", "",6,2,3,2,2,1,100) {
            @Override public boolean equipar(Equipo equipo) {
                return equipo != nueva && super.equipar(equipo);
            }
        };
        var h = new InstintoSupervivencia();
        Equipo vieja = arma("Vieja",1,Map.of(h,1));
        j.equipar(vieja);
        j.agregarPocion(TipoPocion.PEQUENA);
        j.usarPocion(TipoPocion.PEQUENA);
        var estado = j.getHabilidades().get(h);
        j.agregarEquipo(nueva);
        assertFalse(j.equiparDesdeInventario(TipoEquipo.ARMA,0));
        assertSame(vieja,j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(List.of(nueva),j.getInventarioEquipo());
        assertSame(estado,j.getHabilidades().get(h));
        assertEquals(10,j.getFuerza());
        j.curarse(30);
        assertEquals(6,j.getAtributoBase(Atributo.FUERZA));
        assertEquals(7,j.getFuerza());
    }
}
