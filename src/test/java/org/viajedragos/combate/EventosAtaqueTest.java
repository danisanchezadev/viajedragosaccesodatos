package org.viajedragos.combate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.implementacion.*;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class EventosAtaqueTest {

    private Jugador jugador() {
        return new Jugador("Jugador", "", 10, 0, 1, 3, 1, 100, 100);
    }

    private Enemigo enemigo() {
        return new Enemigo("Enemigo", "", 5, 4, 1, 2, 1, 100, 100);
    }

    private Registro registrar(Combatiente portador, String nombre, int nivel, List<String> eventos) {
        EstadoHabilidad estado = new EstadoHabilidad(true, 0, nivel);
        Registro registro = new Registro(nombre, estado, eventos);
        portador.getHabilidades().put(registro, estado);
        return registro;
    }

    @Test
    void respetaTodasLasFasesDestinatariosContextoYEstadoAsociado() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        List<String> eventos = new ArrayList<>();
        Registro atacante = registrar(jugador, "A", 7, eventos);
        Registro defensor = registrar(enemigo, "D", 23, eventos);
        atacante.antes = ca -> {
            assertEquals(0, ca.getDadoAtaque());
            assertEquals(0, ca.getDadoDefensa());
        };
        atacante.calcular = ca -> {
            assertEquals(4, ca.getDadoAtaque());
            assertEquals(2, ca.getDadoDefensa());
            assertEquals(0, ca.getDanioBruto());
        };
        atacante.aplicar = ca -> {
            assertEquals(9, ca.getDanioBruto());
            assertEquals(9, ca.getDanioFinal());
            assertEquals(100, enemigo.getVida());
        };
        atacante.hacer = ca -> assertEquals(91, enemigo.getVida());
        defensor.recibir = ca -> assertEquals(91, enemigo.getVida());
        int[] resultados = {4, 2};
        int[] contador = {0};
        TiradorDado dados = () -> {
            eventos.add("dado" + ++contador[0]);
            return resultados[contador[0] - 1];
        };

        new Combate(jugador, enemigo, dados).ataque(jugador, enemigo);

        assertEquals(List.of("A:antes", "dado1", "dado2", "A:calcular", "D:calcular",
                "A:aplicar", "D:aplicar", "A:hacer", "D:recibir"), eventos);
        assertSame(atacante.contexto, defensor.contexto);
        assertSame(jugador, atacante.contexto.getAtacante());
        assertSame(enemigo, atacante.contexto.getDefensor());
        assertSame(atacante.estado, jugador.getHabilidades().get(atacante));
        assertSame(defensor.estado, enemigo.getHabilidades().get(defensor));
    }

    @Test
    void cambiosDeAtributosPreviosSeUsanAlCalcularElBruto() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        List<String> eventos = new ArrayList<>();
        Registro atacante = registrar(jugador, "A", 1, eventos);
        Registro defensor = registrar(enemigo, "D", 2, eventos);
        atacante.antes = ca -> jugador.setFuerza(12);
        atacante.calcular = ca -> jugador.setAgilidad(4);
        defensor.calcular = ca -> enemigo.setResistencia(6);

        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);

        assertEquals(10, atacante.contexto.getDanioBruto());
        assertEquals(10, atacante.contexto.getDanioFinal());
        assertEquals(90, enemigo.getVida());
    }

    @Test
    void variasHabilidadesRecibenCadaUnaSuEstadoYTerminanSuFaseAntesDelDefensor() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        List<String> eventos = new ArrayList<>();
        Registro primero = registrar(jugador, "A1", 3, eventos);
        EstadoHabilidad estadoSegundo = new EstadoHabilidad(false, 2, 17);
        Registro segundo = new OtroRegistro("A2", estadoSegundo, eventos);
        jugador.getHabilidades().put(segundo, estadoSegundo);
        Registro defensor = registrar(enemigo, "D", 29, eventos);

        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);

        assertSame(primero.contexto, segundo.contexto);
        assertSame(segundo.contexto, defensor.contexto);
        assertTrue(eventos.indexOf("A1:calcular") < eventos.indexOf("D:calcular"));
        assertTrue(eventos.indexOf("A2:calcular") < eventos.indexOf("D:calcular"));
        assertTrue(eventos.indexOf("A1:aplicar") < eventos.indexOf("D:aplicar"));
        assertTrue(eventos.indexOf("A2:aplicar") < eventos.indexOf("D:aplicar"));
        assertTrue(eventos.indexOf("A1:calcular") < eventos.indexOf("A2:aplicar"));
        assertTrue(eventos.indexOf("A2:calcular") < eventos.indexOf("A1:aplicar"));
        assertEquals(17, estadoSegundo.getNivel());
        assertEquals(2, estadoSegundo.getVecesEquipada());
        assertFalse(estadoSegundo.isAprendida());
    }

    @Test
    void modificadoresDeBrutoYFinalSeResuelvenAtacanteAntesQueDefensor() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        List<String> eventos = new ArrayList<>();
        Registro atacante = registrar(jugador, "A", 4, eventos);
        Registro defensor = registrar(enemigo, "D", 2, eventos);
        atacante.aplicar = ca -> {
            ca.setDanioBruto(ca.getDanioBruto() + atacante.estado.getNivel());
            ca.setDanioFinal(ca.getDanioBruto());
        };
        defensor.aplicar = ca -> {
            assertEquals(13, ca.getDanioFinal());
            ca.setDanioFinal(ca.getDanioFinal() / defensor.estado.getNivel());
        };

        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);

        assertEquals(13, atacante.contexto.getDanioBruto());
        assertEquals(6, atacante.contexto.getDanioFinal());
        assertEquals(94, enemigo.getVida());
    }

    @Test
    void golpeCerteroSeSumaAntesDeLaDefensaPerfecta() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        jugador.aprenderHabilidad(new GolpeCertero(), 50);
        enemigo.aprenderHabilidad(new DefensaPerfecta(), 1);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 6)).ataque(jugador, enemigo);
        // Bruto 5, GolpeCertero sigue usando su nivel interno 2; (5 + 2) / 2 = 3.
        assertEquals(97, enemigo.getVida());
    }

    @Test
    void guardiaFerreaReduceElDanioFinalDelAtaque() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        enemigo.aprenderHabilidad(new GuardiaFerrea(), 40);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);
        assertEquals(93, enemigo.getVida());
    }

    @Test
    void caparazonRecibeElNivelDeSuEstadoYConservaSuFormula() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        enemigo.aprenderHabilidad(new Caparazon(), 25);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);
        // 9 menos 25%, truncado a 6.
        assertEquals(94, enemigo.getVida());
    }

    @Test
    void castigoAlTorpeSeIntegraEnLaDefensaFallida() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        jugador.aprenderHabilidad(new CastigoAlTorpe(), 20);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 1)).ataque(jugador, enemigo);
        // Daño bruto sin defensa 17, más el nivel interno 4 de CastigoAlTorpe.
        assertEquals(79, enemigo.getVida());
    }

    @Test
    void criticoRecibeLosModificadoresSinOtraSecuenciaDeAtaque() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        jugador.aprenderHabilidad(new GolpeCertero(), 1);
        enemigo.aprenderHabilidad(new GuardiaFerrea(), 1);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(6, 1, 4)).ataque(jugador, enemigo);
        assertEquals(77, enemigo.getVida());
    }

    @ParameterizedTest
    @CsvSource({"1,1,100", "1,6,95"})
    void falloNoAplicaDanioNiEventosPosterioresAunqueUnEventoEleveElFinal(int ataque, int defensa, int vidaAtacante) {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        List<String> eventos = new ArrayList<>();
        Registro atacante = registrar(jugador, "A", 1, eventos);
        Registro defensor = registrar(enemigo, "D", 1, eventos);
        atacante.aplicar = ca -> ca.setDanioFinal(99);

        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(ataque, defensa)).ataque(jugador, enemigo);

        assertEquals(List.of("A:antes", "A:calcular", "D:calcular", "A:aplicar", "D:aplicar"), eventos);
        assertEquals(vidaAtacante, jugador.getVida());
        assertEquals(100, enemigo.getVida());
        assertTrue(atacante.contexto.isAtaqueFallido());
        assertEquals(defensa == 6, atacante.contexto.isContraataque());
        assertEquals(0, atacante.contexto.getDanioFinal());
        assertSame(atacante.contexto, defensor.contexto);
    }

    @Test
    void contraataqueDirectoNoActivaCuracionBonificacionesNiHabilidadesDelQueRecibeEseDanio() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        jugador.setVida(50);
        jugador.aprenderHabilidad(new SedDeBatalla(), 1);
        jugador.aprenderHabilidad(new Impetu(), 1);
        jugador.aprenderHabilidad(new FuriaContenida(), 1);
        enemigo.aprenderHabilidad(new FuriaContenida(), 1);
        enemigo.aprenderHabilidad(new GolpeCertero(), 1);
        List<String> eventos = new ArrayList<>();
        Registro atacante = registrar(jugador, "A", 1, eventos);
        registrar(enemigo, "D", 1, eventos);

        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(1, 6)).ataque(jugador, enemigo);

        assertEquals(45, jugador.getVida());
        assertEquals(100, enemigo.getVida());
        assertEquals(10, jugador.getFuerza());
        assertEquals(3, jugador.getAgilidad());
        assertEquals(5, enemigo.getFuerza());
        assertEquals(List.of("A:antes", "A:calcular", "D:calcular", "A:aplicar", "D:aplicar"), eventos);
        assertSame(jugador, atacante.contexto.getAtacante());
        assertSame(enemigo, atacante.contexto.getDefensor());
    }

    @Test
    void finalReducidoACeroNoActivaLosEventosPosteriores() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        List<String> eventos = new ArrayList<>();
        registrar(jugador, "A", 1, eventos);
        Registro defensor = registrar(enemigo, "D", 1, eventos);
        defensor.aplicar = ca -> ca.setDanioFinal(-5);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);
        assertEquals(100, enemigo.getVida());
        assertEquals(0, defensor.contexto.getDanioFinal());
        assertFalse(eventos.contains("A:hacer"));
        assertFalse(eventos.contains("D:recibir"));
    }

    @Test
    void brutoCeroNoActivaHabilidadesPosterioresSiElFinalSigueEnCero() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        jugador.setVida(50);
        enemigo.setResistencia(100);
        enemigo.setVida(20);
        jugador.aprenderHabilidad(new SedDeBatalla(), 1);
        jugador.aprenderHabilidad(new Impetu(), 1);
        enemigo.aprenderHabilidad(new FuriaContenida(), 1);
        enemigo.aprenderHabilidad(new InstintoSupervivencia(), 1);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);
        assertEquals(50, jugador.getVida());
        assertEquals(20, enemigo.getVida());
        assertEquals(3, jugador.getAgilidad());
        assertEquals(5, enemigo.getFuerza());
    }

    @Test
    void golpeCerteroPuedeCrearDanioDesdeBrutoCeroSiElAtaqueNoFalla() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        enemigo.setResistencia(100);
        jugador.setVida(50);
        jugador.aprenderHabilidad(new GolpeCertero(), 1);
        jugador.aprenderHabilidad(new SedDeBatalla(), 1);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);
        assertEquals(98, enemigo.getVida());
        assertEquals(52, jugador.getVida());
    }

    @Test
    void eventosPosterioresAplicanCuracionYBonificacionesDespuesDelDanio() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        jugador.setVida(50);
        enemigo.setVida(12);
        jugador.aprenderHabilidad(new SedDeBatalla(), 1);
        jugador.aprenderHabilidad(new Impetu(), 1);
        enemigo.aprenderHabilidad(new FuriaContenida(), 1);
        enemigo.aprenderHabilidad(new InstintoSupervivencia(), 1);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);
        assertEquals(3, enemigo.getVida());
        assertEquals(52, jugador.getVida());
        assertEquals(4, jugador.getAgilidad());
        assertEquals(10, enemigo.getFuerza());
    }

    @Test
    void danioLetalActivaLosEventosUnaVezSinResolverMuertes() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        enemigo.setVida(2);
        List<String> eventos = new ArrayList<>();
        Registro atacante = registrar(jugador, "A", 1, eventos);
        registrar(enemigo, "D", 1, eventos);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);
        assertEquals(0, enemigo.getVida());
        assertEquals(9, atacante.contexto.getDanioFinal());
        assertEquals(1, eventos.stream().filter("A:hacer"::equals).count());
        assertEquals(1, eventos.stream().filter("D:recibir"::equals).count());
        assertFalse(eventos.contains("D:morir"));
    }

    @Test
    void defensorSinVidaNoGeneraDanioEfectivoNiEventosPosteriores() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        enemigo.setVida(0);
        List<String> eventos = new ArrayList<>();
        registrar(jugador, "A", 1, eventos);
        registrar(enemigo, "D", 1, eventos);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);
        assertEquals(0, enemigo.getVida());
        assertFalse(eventos.contains("A:hacer"));
        assertFalse(eventos.contains("D:recibir"));
    }

    @Test
    void recibirDanioSeDecidePorLaPerdidaRealAunqueAlHacerDanioModifiqueElContexto() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        List<String> eventos = new ArrayList<>();
        Registro atacante = registrar(jugador, "A", 1, eventos);
        registrar(enemigo, "D", 1, eventos);
        atacante.hacer = ca -> ca.setDanioFinal(0);
        new Combate(jugador, enemigo, new CombateTest.DadoTrucadoFalso(4, 2)).ataque(jugador, enemigo);
        assertEquals(91, enemigo.getVida());
        assertTrue(eventos.contains("D:recibir"));
    }

    private static class Registro extends Habilidad {
        private final EstadoHabilidad estado;
        private final List<String> eventos;
        private ContextoAtaque contexto;
        private Consumer<ContextoAtaque> antes = ca -> {};
        private Consumer<ContextoAtaque> calcular = ca -> {};
        private Consumer<ContextoAtaque> aplicar = ca -> {};
        private Consumer<ContextoAtaque> hacer = ca -> {};
        private Consumer<ContextoAtaque> recibir = ca -> {};

        private Registro(String nombre, EstadoHabilidad estado, List<String> eventos) {
            super(nombre);
            this.estado = estado;
            this.eventos = eventos;
        }

        private void registrar(String evento, ContextoAtaque ca, EstadoHabilidad recibido) {
            assertSame(estado, recibido, "Cada evento debe recibir el estado de su propia entrada");
            if (contexto == null) {
                contexto = ca;
            }
            assertSame(contexto, ca, "Toda la secuencia usa el mismo contexto");
            eventos.add(getNombre() + ":" + evento);
        }

        @Override
        public void antesDeAtacar(ContextoAtaque ca, EstadoHabilidad estado) {
            registrar("antes", ca, estado);
            antes.accept(ca);
        }

        @Override
        public void antesDeCalcularDanio(ContextoAtaque ca, EstadoHabilidad estado) {
            registrar("calcular", ca, estado);
            calcular.accept(ca);
        }

        @Override
        public void antesDeAplicarDanio(ContextoAtaque ca, EstadoHabilidad estado) {
            registrar("aplicar", ca, estado);
            aplicar.accept(ca);
        }

        @Override
        public void alHacerDanio(ContextoAtaque ca, EstadoHabilidad estado) {
            registrar("hacer", ca, estado);
            hacer.accept(ca);
        }

        @Override
        public void alRecibirDanio(ContextoAtaque ca, EstadoHabilidad estado) {
            registrar("recibir", ca, estado);
            recibir.accept(ca);
        }

        @Override
        public void alMorir(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
            eventos.add(getNombre() + ":morir");
        }
    }

    private static class OtroRegistro extends Registro {
        private OtroRegistro(String nombre, EstadoHabilidad estado, List<String> eventos) {
            super(nombre, estado, eventos);
        }
    }
}
