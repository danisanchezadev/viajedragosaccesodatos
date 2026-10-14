package org.viajedragos.habilidad.implementacion;

import org.junit.jupiter.api.Test;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.modelo.Jugador;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HabilidadesConEquipoTest {

    private Jugador jugador() {
        return new Jugador("Jugador", "", 10, 10, 10, 10, 10, 15, 30);
    }

    private EstadoHabilidad estado() {
        return new EstadoHabilidad(true, 0, 2);
    }

    private void equipar(Jugador jugador, Map<Atributo, Integer> atributos) {
        assertTrue(jugador.equipar(new Arma("Arma de prueba", TipoEquipo.ARMA, atributos, Map.of())));
    }

    @Test
    void impetuRevierteSoloSuAumentoAunqueCambieElEquipo() {
        Jugador jugador = jugador();
        Jugador enemigo = jugador();
        equipar(jugador, Map.of(Atributo.AGILIDAD, 5));
        Impetu habilidad = new Impetu();
        EstadoHabilidad estado = estado();
        ContextoAtaque contexto = new ContextoAtaque(jugador, enemigo);
        contexto.setDanioFinal(1);

        for (int i = 0; i < 5; i++) {
            habilidad.alHacerDanio(contexto, estado);
        }
        assertEquals(13, jugador.getAtributoBase(Atributo.AGILIDAD));
        assertEquals(18, jugador.getAgilidad());
        jugador.desequipar(TipoEquipo.ARMA);
        equipar(jugador, Map.of(Atributo.AGILIDAD, -20));
        assertEquals(0, jugador.getAgilidad());
        jugador.aumentarAtributo(Atributo.AGILIDAD, 2);
        habilidad.alFinalCombate(jugador, enemigo, estado);
        assertEquals(12, jugador.getAtributoBase(Atributo.AGILIDAD));
        assertEquals(-20, jugador.getModificadoresEquipo().get(Atributo.AGILIDAD));
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(12, jugador.getAgilidad());
    }

    @Test
    void primerImpulsoRestaSuNivelSinArrastrarBonificacionesNiPenalizaciones() {
        Jugador jugador = jugador();
        Jugador enemigo = jugador();
        equipar(jugador, Map.of(Atributo.INICIATIVA, -20));
        PrimerImpulso habilidad = new PrimerImpulso(3);
        EstadoHabilidad estado = new EstadoHabilidad(true, 0, 3);
        habilidad.alInicioCombate(jugador, enemigo, estado);
        assertEquals(13, jugador.getAtributoBase(Atributo.INICIATIVA));
        assertEquals(0, jugador.getIniciativa());
        jugador.desequipar(TipoEquipo.ARMA);
        equipar(jugador, Map.of(Atributo.INICIATIVA, 5));
        jugador.aumentarAtributo(Atributo.INICIATIVA, 2);
        habilidad.alFinalCombate(jugador, enemigo, estado);
        assertEquals(12, jugador.getAtributoBase(Atributo.INICIATIVA));
        assertEquals(17, jugador.getIniciativa());
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(12, jugador.getIniciativa());
    }

    @Test
    void instintoActivaYRetiraLaFuerzaAlCurarseSinDuplicarElEquipo() {
        Jugador jugador = jugador();
        Jugador enemigo = jugador();
        jugador.setVida(9);
        equipar(jugador, Map.of(Atributo.FUERZA, 5));
        InstintoSupervivencia habilidad = new InstintoSupervivencia();
        EstadoHabilidad estado = estado();
        habilidad.alInicioCombate(jugador, enemigo, estado);
        assertEquals(13, jugador.getAtributoBase(Atributo.FUERZA));
        assertEquals(18, jugador.getFuerza());
        jugador.setVida(20);
        habilidad.alCurarse(jugador, estado);
        assertEquals(10, jugador.getAtributoBase(Atributo.FUERZA));
        assertEquals(15, jugador.getFuerza());
        habilidad.alFinalCombate(jugador, enemigo, estado);
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(10, jugador.getFuerza());
    }

    @Test
    void instintoRetiraSuEfectoAlFinalAunqueLaPenalizacionOculteElAtributo() {
        Jugador jugador = jugador();
        Jugador enemigo = jugador();
        jugador.setVida(9);
        equipar(jugador, Map.of(Atributo.FUERZA, -20));
        InstintoSupervivencia habilidad = new InstintoSupervivencia();
        EstadoHabilidad estado = estado();
        habilidad.alInicioCombate(jugador, enemigo, estado);
        assertEquals(0, jugador.getFuerza());
        assertEquals(13, jugador.getAtributoBase(Atributo.FUERZA));
        habilidad.alFinalCombate(jugador, enemigo, estado);
        assertEquals(10, jugador.getAtributoBase(Atributo.FUERZA));
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(10, jugador.getFuerza());
    }

    @Test
    void furiaContenidaRevierteSuContribucionConEquipoNegativo() {
        Jugador jugador = jugador();
        Jugador enemigo = jugador();
        equipar(jugador, Map.of(Atributo.FUERZA, -20));
        FuriaContenida habilidad = new FuriaContenida();
        EstadoHabilidad estado = estado();
        ContextoAtaque contexto = new ContextoAtaque(enemigo, jugador);
        contexto.setDanioFinal(1);
        habilidad.alRecibirDanio(contexto, estado);
        habilidad.alRecibirDanio(contexto, estado);
        assertEquals(12, jugador.getAtributoBase(Atributo.FUERZA));
        jugador.aumentarAtributo(Atributo.FUERZA, 3);
        habilidad.alFinalCombate(jugador, enemigo, estado);
        assertEquals(13, jugador.getAtributoBase(Atributo.FUERZA));
        assertEquals(0, jugador.getFuerza());
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(13, jugador.getFuerza());
    }

    @Test
    void rabiaSeRestauraSinPerderCambiosPermanentesNiElEquipoNuevo() {
        Jugador jugador = jugador();
        Jugador enemigo = jugador();
        equipar(jugador, Map.of(Atributo.FUERZA, 5, Atributo.AGILIDAD, -20, Atributo.RESISTENCIA, -20));
        Rabia habilidad = new Rabia();
        EstadoHabilidad estado = estado();
        habilidad.alInicioCombate(jugador, enemigo, estado);
        assertEquals(12, jugador.getAtributoBase(Atributo.FUERZA));
        assertEquals(11, jugador.getAtributoBase(Atributo.AGILIDAD));
        assertEquals(9, jugador.getAtributoBase(Atributo.RESISTENCIA));
        assertEquals(17, jugador.getFuerza());
        assertEquals(0, jugador.getAgilidad());
        assertEquals(0, jugador.getResistencia());
        jugador.aumentarAtributo(Atributo.FUERZA, 3);
        jugador.desequipar(TipoEquipo.ARMA);
        equipar(jugador, Map.of(Atributo.FUERZA, -2, Atributo.RESISTENCIA, 4));
        habilidad.alFinalCombate(jugador, enemigo, estado);

        assertEquals(13, jugador.getAtributoBase(Atributo.FUERZA));
        assertEquals(10, jugador.getAtributoBase(Atributo.AGILIDAD));
        assertEquals(10, jugador.getAtributoBase(Atributo.RESISTENCIA));
        assertEquals(11, jugador.getFuerza());
        assertEquals(10, jugador.getAgilidad());
        assertEquals(14, jugador.getResistencia());
        habilidad.alFinalCombate(jugador, enemigo, estado);
        assertEquals(13, jugador.getAtributoBase(Atributo.FUERZA));
        jugador.desequipar(TipoEquipo.ARMA);
        assertEquals(13, jugador.getFuerza());
        assertEquals(10, jugador.getResistencia());
    }

    @Test
    void rabiaRecuperaResistenciaCeroSinCrearUnPuntoExtra() {
        Jugador jugador = jugador();
        jugador.setResistencia(0);
        Rabia habilidad = new Rabia();
        EstadoHabilidad estado = estado();
        habilidad.alInicioCombate(jugador, jugador(), estado);
        assertEquals(0, jugador.getResistencia());
        assertEquals(-1, jugador.getAtributoBase(Atributo.RESISTENCIA));
        habilidad.alFinalCombate(jugador, jugador(), estado);
        assertEquals(0, jugador.getAtributoBase(Atributo.RESISTENCIA));
        assertEquals(0, jugador.getResistencia());
    }

    @Test
    void rabiaSinActivarNoCambiaLosAtributosAlFinal() {
        Jugador jugador = jugador();
        new Rabia().alFinalCombate(jugador, jugador(), estado());
        assertEquals(10, jugador.getFuerza());
        assertEquals(10, jugador.getAgilidad());
        assertEquals(10, jugador.getResistencia());
    }
}
