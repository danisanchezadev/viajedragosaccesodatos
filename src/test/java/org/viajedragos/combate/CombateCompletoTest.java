package org.viajedragos.combate;

import org.junit.jupiter.api.Test;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.implementacion.*;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class CombateCompletoTest {
    private Jugador jugador() {
        return new Jugador("Jugador", "", 10, 0, 3, 0, 0, 30, 30);
    }

    private Enemigo enemigo(int vida) {
        return new Enemigo("Enemigo", "", 6, 0, 1, 0, 0, vida, 30);
    }

    private Registro registrar(Combatiente portador, String nombre, List<String> eventos) {
        Registro registro = new Registro(nombre, eventos);
        portador.getHabilidades().put(registro, registro.estado);
        return registro;
    }

    private ResultadoCombate ejecutar(Jugador jugador, Enemigo enemigo, int... tiradas) {
        Dados dados = new Dados(tiradas);
        ResultadoCombate resultado = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> new Combate(jugador, enemigo, dados).combatir());
        assertEquals(tiradas.length, dados.usadas, "El combate debe consumir solo las tiradas previstas");
        return resultado;
    }

    @Test
    void victoriaDelJugadorCierraSinFinalDeTurnoNiFinalDelEnemigo() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo(10);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        Registro e = registrar(enemigo, "E", eventos);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo, 1, 1, 1, 1, 4, 2));
        assertEquals(30, jugador.getVida());
        assertEquals(0, enemigo.getVida());
        assertEquals(List.of("J:inicio", "E:inicio", "J:turno", "E:turno", "J:ataque", "E:morir", "J:final"), eventos);
        assertEquals(1, j.finales);
        assertEquals(0, e.finales);
        assertEquals(0, j.finalesTurno);
        assertEquals(0, e.finalesTurno);
    }

    @Test
    void muerteDelJugadorEjecutaSoloSuLimpiezaFinal() {
        Jugador jugador = jugador();
        jugador.setVida(5);
        Enemigo enemigo = enemigo(30);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        Registro e = registrar(enemigo, "E", eventos);
        assertEquals(ResultadoCombate.JUGADOR_MUERE, ejecutar(jugador, enemigo, 1, 6, 6, 1, 4, 2));
        assertEquals(0, jugador.getVida());
        assertEquals(30, enemigo.getVida());
        assertEquals(1, j.muertes);
        assertEquals(1, j.finales);
        assertEquals(0, e.finales);
        assertEquals(0, e.finalesTurno);
    }

    @Test
    void alternaPoseedorAunqueElAtacanteSeaElOponenteEnUnTurno() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo(25);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        Registro e = registrar(enemigo, "E", eventos);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo,
                2, 1, 1, 1, 4, 2, 6, 1, 4, 2, 1, 1, 4, 2, 1, 6, 4, 2));
        assertEquals(List.of("J:ataque", "E:ataque", "J:ataque", "J:ataque"),
                eventos.stream().filter(s -> s.endsWith(":ataque")).toList());
        assertEquals(4, j.turnos);
        assertEquals(4, e.turnos);
        assertEquals(3, j.finalesTurno);
        assertEquals(3, e.finalesTurno);
        assertEquals(22, jugador.getVida());
        assertEquals(3, jugador.getIniciativa());
        assertEquals(1, enemigo.getIniciativa());
    }

    @Test
    void atacantePuedeSerDistintoDelPrimerPoseedorSinModificarIniciativa() {
        Jugador jugador = jugador();
        jugador.setVida(5);
        Enemigo enemigo = enemigo(30);
        List<String> eventos = new ArrayList<>();
        registrar(jugador, "J", eventos);
        registrar(enemigo, "E", eventos);
        assertEquals(ResultadoCombate.JUGADOR_MUERE, ejecutar(jugador, enemigo, 2, 1, 1, 6, 4, 2));
        assertFalse(eventos.contains("J:ataque"));
        assertTrue(eventos.contains("E:ataque"));
        assertEquals(3, jugador.getAtributoBase(Atributo.INICIATIVA));
        assertEquals(1, enemigo.getAtributoBase(Atributo.INICIATIVA));
    }

    @Test
    void ataqueConEfectoDeMuerteSimultaneaResuelveAmbosUnaVez() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo(10);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        Registro e = registrar(enemigo, "E", eventos);
        e.recibir = ca -> ca.getAtacante().recibirDanio(ca.getAtacante().getVida());
        assertEquals(ResultadoCombate.AMBOS_MUEREN, ejecutar(jugador, enemigo, 1, 1, 1, 1, 4, 2));
        assertEquals(1, j.muertes);
        assertEquals(1, e.muertes);
        assertTrue(eventos.indexOf("J:morir") < eventos.indexOf("E:morir"));
        assertEquals(1, j.finales);
        assertEquals(0, e.finales);
    }

    @Test
    void resurreccionPermiteContinuarYUnaMuertePosteriorTieneOtraResolucion() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo(10);
        List<String> eventos = new ArrayList<>();
        Registro e = registrar(enemigo, "E", eventos);
        Registro j = registrar(jugador, "J", eventos);
        e.muerte = (p, o) -> { if (e.muertes == 1) p.curarse(20); };
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo,
                1, 1, 1, 1, 4, 2, 1, 6, 4, 2, 1, 1, 4, 2));
        assertEquals(2, e.muertes);
        assertEquals(3, j.turnos);
        assertEquals(2, j.finalesTurno);
        assertEquals(1, j.finales);
        assertEquals(0, enemigo.getVida());
    }

    @Test
    void jugadorResucitaTrasUnAtaqueLetalYDespuesGanaElCombate() {
        Jugador jugador = jugador();
        jugador.setVida(5);
        Enemigo enemigo = enemigo(30);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        j.muerte = (p, o) -> { if (j.muertes == 1) p.curarse(20); };
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo,
                1, 6, 6, 1, 4, 2, 1, 1, 4, 2, 1, 6, 4, 2, 1, 1, 4, 2));
        assertEquals(1, j.muertes);
        assertEquals(20, jugador.getVida());
        assertEquals(4, j.turnos);
        assertEquals(3, j.finalesTurno);
        assertEquals(1, j.finales);
    }

    @Test
    void contraataqueLetalCierraElCombateSinContinuarElTurno() {
        Jugador jugador = jugador();
        jugador.setVida(5);
        Enemigo enemigo = enemigo(30);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        Registro e = registrar(enemigo, "E", eventos);
        assertEquals(ResultadoCombate.JUGADOR_MUERE, ejecutar(jugador, enemigo, 1, 1, 1, 1, 1, 6));
        assertEquals(0, jugador.getVida());
        assertEquals(30, enemigo.getVida());
        assertEquals(1, j.muertes);
        assertEquals(1, j.finales);
        assertEquals(0, j.finalesTurno);
        assertEquals(0, e.finalesTurno);
        assertEquals(0, e.finales);
    }

    @Test
    void cadenaDeMuertesEnUnAtaqueNoVuelveAEjecutarAlMorirDelPrimero() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo(10);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        Registro e = registrar(enemigo, "E", eventos);
        e.muerte = (p, o) -> {
            p.curarse(10);
            o.recibirDanio(o.getVida());
        };
        j.muerte = (p, o) -> {
            p.curarse(10);
            o.recibirDanio(o.getVida());
        };
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo, 1, 1, 1, 1, 4, 2));
        assertEquals(1, j.muertes);
        assertEquals(1, e.muertes);
        assertEquals(10, jugador.getVida());
        assertEquals(0, enemigo.getVida());
    }

    @Test
    void empateDePrioridadDuranteElTurnoFavoreceAlPoseedorSinTiradasExtra() {
        Jugador jugador = jugador();
        jugador.setIniciativa(1);
        jugador.setVida(5);
        Enemigo enemigo = enemigo(10);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        registrar(enemigo, "E", eventos);
        j.recibir = ca -> ca.getAtacante().recibirDanio(ca.getAtacante().getVida());
        assertEquals(ResultadoCombate.AMBOS_MUEREN, ejecutar(jugador, enemigo,
                6, 1, 1, 6, 1, 1, 4, 2));
        assertTrue(eventos.indexOf("E:turno") < eventos.indexOf("J:turno"));
        assertTrue(eventos.indexOf("E:morir") < eventos.indexOf("J:morir"));
    }

    @Test
    void finalDeTurnoUsaLaIniciativaActualModificadaEnElInicio() {
        Jugador jugador = jugador();
        jugador.setVida(10);
        Enemigo enemigo = enemigo(30);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        registrar(enemigo, "E", eventos);
        j.turno = (p, o) -> {
            p.setIniciativa(0);
            o.setIniciativa(5);
        };
        assertEquals(ResultadoCombate.JUGADOR_MUERE, ejecutar(jugador, enemigo,
                1, 1, 1, 1, 4, 2, 1, 1, 4, 2));
        assertTrue(eventos.indexOf("J:turno") < eventos.indexOf("E:turno"));
        assertTrue(eventos.indexOf("E:finTurno") < eventos.indexOf("J:finTurno"));
        assertEquals(2, eventos.stream().filter("E:ataque"::equals).count());
    }

    @Test
    void muerteEnInicioDeTurnoImpideElInicioDelMuertoYElAtaque() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo(10);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        Registro e = registrar(enemigo, "E", eventos);
        j.turno = (p, o) -> o.recibirDanio(o.getVida());
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo, 1, 1));
        assertEquals(0, e.turnos);
        assertFalse(eventos.contains("J:ataque"));
        assertEquals(1, e.muertes);
        assertEquals(0, j.finalesTurno);
    }

    @Test
    void muerteEnFinalDeTurnoImpideEventosPosterioresDelMuerto() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo(30);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        Registro e = registrar(enemigo, "E", eventos);
        j.finTurno = (p, o) -> o.recibirDanio(o.getVida());
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo, 1, 1, 1, 1, 4, 2));
        assertEquals(1, j.finalesTurno);
        assertEquals(0, e.finalesTurno);
        assertEquals(0, e.finales);
    }

    @Test
    void resucitadoEnInicioDeTurnoPuedeActuarAlEstarVivoOtraVez() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo(10);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        Registro e = registrar(enemigo, "E", eventos);
        j.turno = (p, o) -> { if (j.turnos == 1) o.recibirDanio(o.getVida()); };
        e.muerte = (p, o) -> { if (e.muertes == 1) p.curarse(10); };
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo, 1, 1, 1, 1, 4, 2));
        assertEquals(1, e.turnos);
        assertEquals(2, e.muertes);
    }

    @Test
    void segundoAlientoSeEjecutaParaAmbosEnElInicioDeTurno() {
        Jugador jugador = jugador();
        jugador.setVida(10);
        Enemigo enemigo = enemigo(10);
        jugador.aprenderHabilidad(new SegundoAliento(2), 2);
        enemigo.aprenderHabilidad(new SegundoAliento(2), 2);
        Registro j = registrar(jugador, "J", new ArrayList<>());
        int[] vidaAntesDeAtacar = {0};
        j.antes = ca -> vidaAntesDeAtacar[0] = enemigo.getVida();
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo, 1, 1, 1, 1, 4, 2));
        assertEquals(12, vidaAntesDeAtacar[0]);
        assertEquals(12, jugador.getVida());
    }

    @Test
    void victoriaRestauraTodosLosEfectosTemporalesDelJugadorConservandoEquipo() {
        Jugador jugador = jugador();
        jugador.setVida(8);
        Enemigo enemigo = enemigo(30);
        jugador.equipar(new Arma("Arma", TipoEquipo.ARMA,
                Map.of(Atributo.FUERZA, 2, Atributo.RESISTENCIA, -4), Map.of()));
        jugador.aprenderHabilidad(new Rabia(), 1);
        jugador.aprenderHabilidad(new PrimerImpulso(3), 3);
        jugador.aprenderHabilidad(new Impetu(), 1);
        jugador.aprenderHabilidad(new InstintoSupervivencia(), 1);
        jugador.aprenderHabilidad(new FuriaContenida(), 1);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo,
                1, 1, 1, 1, 4, 2, 6, 1, 4, 2, 1, 1, 4, 2));
        assertEquals(10, jugador.getAtributoBase(Atributo.FUERZA));
        assertEquals(12, jugador.getFuerza());
        assertEquals(0, jugador.getAtributoBase(Atributo.AGILIDAD));
        assertEquals(0, jugador.getAtributoBase(Atributo.RESISTENCIA));
        assertEquals(3, jugador.getIniciativa());
        assertEquals(2, jugador.getVida());
        assertEquals(-4, jugador.getModificadoresEquipo().get(Atributo.RESISTENCIA));
    }

    @Test
    void jugadorMuertoTambienRestauraSusEfectosTemporales() {
        Jugador jugador = jugador();
        jugador.setVida(5);
        Enemigo enemigo = enemigo(30);
        enemigo.setIniciativa(10);
        jugador.aprenderHabilidad(new Rabia(), 1);
        jugador.aprenderHabilidad(new PrimerImpulso(3), 3);
        jugador.aprenderHabilidad(new FuriaContenida(), 1);
        Registro e = registrar(enemigo, "E", new ArrayList<>());
        assertEquals(ResultadoCombate.JUGADOR_MUERE, ejecutar(jugador, enemigo, 1, 1, 1, 1, 4, 2));
        assertEquals(0, jugador.getVida());
        assertEquals(10, jugador.getFuerza());
        assertEquals(0, jugador.getAgilidad());
        assertEquals(0, jugador.getAtributoBase(Atributo.RESISTENCIA));
        assertEquals(3, jugador.getIniciativa());
        assertEquals(0, e.finales);
    }

    @Test
    void jugadorMuertoAntesDelInicioResucitaYContinuaSinEjecutarSuInicio() {
        Jugador jugador = jugador();
        jugador.setVida(0);
        Enemigo enemigo = enemigo(10);
        Registro j = registrar(jugador, "J", new ArrayList<>());
        j.muerte = (p, o) -> p.curarse(8);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo, 1, 1, 1, 1, 4, 2));
        assertEquals(0, j.inicios);
        assertEquals(1, j.muertes);
        assertEquals(1, j.finales);
        assertEquals(8, jugador.getVida());
    }

    @Test
    void habilidadInactivaNoParticipaEnNingunaFaseDelCombate() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo(10);
        List<String> eventos = new ArrayList<>();
        Registro j = registrar(jugador, "J", eventos);
        j.estado.setAprendida(false);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo, 1, 1, 1, 1, 4, 2));
        assertTrue(eventos.isEmpty());
        assertEquals(0, j.inicios);
        assertEquals(0, j.turnos);
        assertEquals(0, j.finales);
    }

    @Test
    void combateConAmbasIniciativasCeroTerminaUsandoLaExcepcionConfirmada() {
        Jugador jugador = jugador();
        jugador.setIniciativa(0);
        Enemigo enemigo = enemigo(10);
        enemigo.setIniciativa(0);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo, 6, 1, 6, 1, 1, 1, 4, 2));
    }

    @Test
    void efectosRestauradosPuedenAplicarseOtraVezEnUnNuevoCombate() {
        Jugador jugador = jugador();
        jugador.aprenderHabilidad(new Rabia(), 1);
        jugador.aprenderHabilidad(new PrimerImpulso(3), 3);
        for (int i = 0; i < 2; i++) {
            assertEquals(ResultadoCombate.ENEMIGO_MUERE, ejecutar(jugador, enemigo(10), 1, 1, 1, 1, 4, 2));
            assertEquals(10, jugador.getFuerza());
            assertEquals(0, jugador.getAgilidad());
            assertEquals(0, jugador.getAtributoBase(Atributo.RESISTENCIA));
            assertEquals(3, jugador.getIniciativa());
        }
    }

    private static class Dados implements TiradorDado {
        private final int[] valores;
        private int usadas;

        private Dados(int... valores) {
            this.valores = valores;
        }

        @Override
        public int tirarDado() {
            assertTrue(usadas < valores.length, "Se pidió una tirada inesperada o el combate no terminó");
            return valores[usadas++];
        }
    }

    private static class Registro extends Habilidad {
        private final EstadoHabilidad estado = new EstadoHabilidad(true, 0, 1);
        private final List<String> eventos;
        private BiConsumer<Combatiente, Combatiente> turno = (p, o) -> {};
        private BiConsumer<Combatiente, Combatiente> finTurno = (p, o) -> {};
        private BiConsumer<Combatiente, Combatiente> muerte = (p, o) -> {};
        private Consumer<ContextoAtaque> recibir = ca -> {};
        private Consumer<ContextoAtaque> antes = ca -> {};
        private int inicios;
        private int turnos;
        private int finalesTurno;
        private int muertes;
        private int finales;

        private Registro(String nombre, List<String> eventos) {
            super(nombre);
            this.eventos = eventos;
        }

        private void registrar(String evento, EstadoHabilidad recibido) {
            assertSame(estado, recibido);
            eventos.add(getNombre() + ":" + evento);
        }

        @Override
        public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
            registrar("inicio", estado);
            inicios++;
        }

        @Override
        public void alInicioTurno(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
            registrar("turno", estado);
            turnos++;
            turno.accept(portador, oponente);
        }

        @Override
        public void antesDeAtacar(ContextoAtaque contexto, EstadoHabilidad estado) {
            registrar("ataque", estado);
            antes.accept(contexto);
        }

        @Override
        public void alRecibirDanio(ContextoAtaque contexto, EstadoHabilidad estado) {
            assertSame(this.estado, estado);
            recibir.accept(contexto);
        }

        @Override
        public void alFinalTurno(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
            registrar("finTurno", estado);
            finalesTurno++;
            finTurno.accept(portador, oponente);
        }

        @Override
        public void alMorir(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
            registrar("morir", estado);
            muertes++;
            muerte.accept(portador, oponente);
        }

        @Override
        public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
            registrar("final", estado);
            finales++;
        }
    }
}
