package org.viajedragos.combate;

import org.junit.jupiter.api.Test;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.implementacion.PrimerImpulso;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

class InicioCombateTest {
    private Jugador jugador() {
        return new Jugador("Jugador", "", 10, 0, 3, 3, 1, 30, 30);
    }

    private Enemigo enemigo() {
        return new Enemigo("Enemigo", "", 5, 4, 1, 2, 1, 30, 30);
    }

    private Registro registrar(Combatiente combatiente, String nombre, List<String> eventos) {
        Registro registro = new Registro(nombre, eventos);
        combatiente.getHabilidades().put(registro, registro.estado);
        return registro;
    }

    private TiradorDado sinDados() {
        return () -> { throw new AssertionError("Este final de inicio no necesita tiradas"); };
    }

    @Test
    void ambosEjecutanInicioPorIniciativaYFinalTrasUnaMuerte() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        List<String> eventos = new ArrayList<>();
        registrar(jugador, "J", eventos);
        Registro criatura = registrar(enemigo, "E", eventos);
        criatura.inicio = (p, o) -> o.recibirDanio(o.getVida());
        assertEquals(ResultadoCombate.JUGADOR_MUERE, new Combate(jugador, enemigo, sinDados()).combatir());
        assertEquals(List.of("J:inicio", "E:inicio", "J:morir", "J:final"), eventos);
    }

    @Test
    void muertoAntesDeSuInicioEjecutaMorirYSuPrimerImpulsoNoSeResta() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        enemigo.aprenderHabilidad(new PrimerImpulso(4), 4);
        List<String> eventos = new ArrayList<>();
        Registro personaje = registrar(jugador, "J", eventos);
        registrar(enemigo, "E", eventos);
        personaje.inicio = (p, o) -> o.recibirDanio(o.getVida());
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, new Combate(jugador, enemigo, sinDados()).combatir());
        assertEquals(List.of("J:inicio", "E:morir", "J:final"), eventos);
        assertEquals(1, enemigo.getIniciativa());
    }

    @Test
    void limpiezaDelJugadorMuertoNoRestaUnPrimerImpulsoQueNoSeAplico() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        enemigo.setIniciativa(10);
        jugador.aprenderHabilidad(new PrimerImpulso(4), 4);
        List<String> eventos = new ArrayList<>();
        registrar(jugador, "J", eventos);
        Registro criatura = registrar(enemigo, "E", eventos);
        criatura.inicio = (p, o) -> o.recibirDanio(o.getVida());
        assertEquals(ResultadoCombate.JUGADOR_MUERE, new Combate(jugador, enemigo, sinDados()).combatir());
        assertEquals(List.of("E:inicio", "J:morir", "J:final"), eventos);
        assertEquals(3, jugador.getIniciativa());
    }

    @Test
    void muerteSimultaneaDisparaAmbosEfectosUnaVez() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        List<String> eventos = new ArrayList<>();
        Registro personaje = registrar(jugador, "J", eventos);
        Registro criatura = registrar(enemigo, "E", eventos);
        personaje.inicio = (p, o) -> {
            p.recibirDanio(p.getVida());
            o.recibirDanio(o.getVida());
        };
        assertEquals(ResultadoCombate.AMBOS_MUEREN, new Combate(jugador, enemigo, sinDados()).combatir());
        assertEquals(List.of("J:inicio", "J:morir", "E:morir", "J:final"), eventos);
        assertEquals(1, personaje.muertes);
        assertEquals(1, criatura.muertes);
    }

    @Test
    void prioridadDeMuertesSeEvaluaConLaIniciativaActual() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        List<String> eventos = new ArrayList<>();
        Registro personaje = registrar(jugador, "J", eventos);
        registrar(enemigo, "E", eventos);
        personaje.inicio = (p, o) -> {
            p.setIniciativa(0);
            o.setIniciativa(5);
            p.recibirDanio(p.getVida());
            o.recibirDanio(o.getVida());
        };
        assertEquals(ResultadoCombate.AMBOS_MUEREN, new Combate(jugador, enemigo, sinDados()).combatir());
        assertEquals(List.of("J:inicio", "E:morir", "J:morir", "J:final"), eventos);
    }

    @Test
    void cadenaDeResurreccionYMuerteNoRepiteAlMorirEnLaMismaResolucion() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        List<String> eventos = new ArrayList<>();
        Registro personaje = registrar(jugador, "J", eventos);
        Registro criatura = registrar(enemigo, "E", eventos);
        personaje.inicio = (p, o) -> o.recibirDanio(o.getVida());
        criatura.muerte = (p, o) -> {
            p.curarse(10);
            o.recibirDanio(o.getVida());
        };
        personaje.muerte = (p, o) -> o.recibirDanio(o.getVida());
        assertEquals(ResultadoCombate.AMBOS_MUEREN, new Combate(jugador, enemigo, sinDados()).combatir());
        assertEquals(1, personaje.muertes);
        assertEquals(1, criatura.muertes);
        assertFalse(eventos.contains("E:inicio"));
    }

    @Test
    void ordenInicialEmpatadoSeDesempataConDadosAntesDeEventos() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        enemigo.setIniciativa(3);
        List<String> eventos = new ArrayList<>();
        registrar(jugador, "J", eventos);
        Registro criatura = registrar(enemigo, "E", eventos);
        criatura.inicio = (p, o) -> {
            p.setIniciativa(4);
            o.recibirDanio(o.getVida());
        };
        assertEquals(ResultadoCombate.JUGADOR_MUERE,
                new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(2, 5)).combatir());
        assertEquals("E:inicio", eventos.getFirst());
        assertFalse(eventos.contains("J:inicio"));
    }

    @Test
    void ambasIniciativasCeroComparanDadosYRepitenSoloEmpates() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        jugador.setIniciativa(0);
        enemigo.setIniciativa(0);
        Combate combate = new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(3, 3, 2, 5));
        assertSame(enemigo, assertTimeoutPreemptively(Duration.ofSeconds(2), combate::primerTurno));
    }

    @Test
    void muchosEmpatesNoAcumulanLlamadasRecursivas() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        jugador.setIniciativa(1);
        enemigo.setIniciativa(1);
        int[] tiradas = {0};
        TiradorDado dados = () -> ++tiradas[0] <= 20000 ? 1 : (tiradas[0] == 20001 ? 6 : 1);
        assertSame(jugador, assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> new Combate(jugador, enemigo, dados).primerTurno()));
        assertEquals(20002, tiradas[0]);
    }

    private static class Registro extends Habilidad {
        private final EstadoHabilidad estado = new EstadoHabilidad(true, 0, 1);
        private final List<String> eventos;
        private BiConsumer<Combatiente, Combatiente> inicio = (p, o) -> {};
        private BiConsumer<Combatiente, Combatiente> muerte = (p, o) -> {};
        private int muertes;

        private Registro(String nombre, List<String> eventos) {
            super(nombre);
            this.eventos = eventos;
        }

        @Override
        public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad recibido) {
            assertSame(estado, recibido);
            eventos.add(getNombre() + ":inicio");
            inicio.accept(portador, oponente);
        }

        @Override
        public void alMorir(Combatiente portador, Combatiente oponente, EstadoHabilidad recibido) {
            assertSame(estado, recibido);
            eventos.add(getNombre() + ":morir");
            muertes++;
            muerte.accept(portador, oponente);
        }

        @Override
        public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad recibido) {
            assertSame(estado, recibido);
            eventos.add(getNombre() + ":final");
        }
    }
}
