package org.viajedragos.combate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.implementacion.*;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Scanner;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.*;

/** Integración con habilidades de producción, sin modificar la partida de app. */
class HabilidadesRealesIntegracionTest {
    private Jugador jugador(int vida) {
        return new Jugador("Jugador de prueba", "", 10, 0, 3, 0, 0, vida, 30);
    }

    private Enemigo enemigo(int vida) {
        return new Enemigo("Enemigo de prueba", "", 6, 0, 1, 0, 0, vida, 30);
    }

    private ResultadoCombate combatir(Jugador j, Enemigo e, Dados dados) {
        ResultadoCombate resultado = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> new Combate(j, e, dados).combatir());
        dados.comprobarConsumo();
        return resultado;
    }

    @Test
    void rabiaYPrimerImpulsoSeAplicanAntesDeLasTiradasYSeRestauranConEquipo() {
        Jugador j = jugador(30);
        j.setResistencia(4);
        j.setAgilidad(2);
        Rabia rabia = new Rabia();
        PrimerImpulso impulso = new PrimerImpulso(3);
        j.aprenderHabilidad(rabia, 17);
        j.aprenderHabilidad(impulso, 40);
        Arma arma = new Arma("Arma de prueba", TipoEquipo.ARMA,
                Map.of(Atributo.FUERZA, 3, Atributo.RESISTENCIA, -2), Map.of());
        assertTrue(j.equipar(arma));
        Enemigo e = enemigo(20);
        Dados dados = new Dados(1, 1, 1, 1, 4, 2);
        dados.antesDeTirar = indice -> {
            assertEquals(15, j.getFuerza());
            assertEquals(1, j.getResistencia());
            assertEquals(3, j.getAgilidad());
            assertEquals(6, j.getIniciativa());
        };

        // Ataque: 15 + 3 + 4 - 2 = 20. Sin los efectos iniciales no sería letal.
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, combatir(j, e, dados));
        assertEquals(10, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(13, j.getFuerza());
        assertEquals(4, j.getAtributoBase(Atributo.RESISTENCIA));
        assertEquals(2, j.getResistencia());
        assertEquals(2, j.getAgilidad());
        assertEquals(3, j.getIniciativa());
        assertSame(arma, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(17, j.getHabilidades().get(rabia).getNivel());
        assertEquals(40, j.getHabilidades().get(impulso).getNivel());
    }

    @Test
    void impetuTrasDanioMejoraLaDefensaYElSiguienteAtaqueAntesDeRestaurarse() {
        Jugador j = jugador(30);
        Impetu impetu = new Impetu();
        j.aprenderHabilidad(impetu, 40);
        Enemigo e = enemigo(25);
        Dados dados = new Dados(1, 1, 1, 1, 4, 2, 4, 2, 1, 6, 4, 2);
        dados.antesDeTirar = indice -> {
            if (indice == 4) assertEquals(0, j.getAgilidad());
            if (indice == 6 || indice == 10) assertEquals(1, j.getAgilidad());
        };

        // Primer golpe 12; defensa con agilidad 1 recibe 7; segundo golpe 13.
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, combatir(j, e, dados));
        assertEquals(23, j.getVida());
        assertEquals(0, j.getAgilidad());
        assertEquals(40, j.getHabilidades().get(impetu).getNivel());
    }

    @Test
    void furiaContenidaTrasRecibirDanioAumentaElSiguienteAtaqueYSeRestaura() {
        Jugador j = jugador(30);
        FuriaContenida furia = new FuriaContenida();
        j.aprenderHabilidad(furia, 99);
        Enemigo e = enemigo(20);
        Dados dados = new Dados(1, 1, 1, 1, 4, 2, 4, 2, 1, 6, 4, 2);
        dados.antesDeTirar = indice -> {
            if (indice == 6) assertEquals(10, j.getFuerza());
            if (indice == 10) assertEquals(12, j.getFuerza());
        };

        assertEquals(ResultadoCombate.ENEMIGO_MUERE, combatir(j, e, dados));
        assertEquals(22, j.getVida());
        assertEquals(10, j.getFuerza());
        assertEquals(99, j.getHabilidades().get(furia).getNivel());
    }

    @Test
    void instintoSeRetiraPorCuracionAnidadaYSeReactivaTrasOtroGolpe() {
        Jugador j = jugador(8);
        InstintoSupervivencia instinto = new InstintoSupervivencia();
        SedDeBatalla sed = new SedDeBatalla();
        j.aprenderHabilidad(instinto, 1);
        j.aprenderHabilidad(sed, 50);
        Enemigo e = enemigo(20);
        Dados dados = new Dados(1, 1, 1, 1, 4, 2, 4, 2, 1, 6, 4, 2);
        dados.antesDeTirar = indice -> {
            if (indice == 0) assertEquals(13, j.getFuerza());
            if (indice == 6) {
                assertEquals(10, j.getVida());
                assertEquals(10, j.getFuerza(), "alCurarse retira Instinto al superar el 30%");
            }
            if (indice == 10) {
                assertEquals(2, j.getVida());
                assertEquals(13, j.getFuerza(), "alRecibirDanio reactiva Instinto");
            }
        };

        assertEquals(ResultadoCombate.ENEMIGO_MUERE, combatir(j, e, dados));
        assertEquals(4, j.getVida());
        assertEquals(10, j.getFuerza(), "El final retira solo el aumento pendiente");
    }

    @ParameterizedTest
    @CsvSource({"1,40,30", "1,2,3", "15,2,15", "30,2,30"})
    void segundoAlientoUsaNivelDelConstructorSoloEnInicioTurnoYLimitaVida(
            int vidaInicial, int nivelConstructor, int vidaEsperada) {
        Jugador j = jugador(vidaInicial);
        SegundoAliento aliento = new SegundoAliento(nivelConstructor);
        j.aprenderHabilidad(aliento, 11);
        Enemigo e = enemigo(10);
        Dados dados = new Dados(1, 1, 1, 1, 4, 2);
        dados.antesDeTirar = indice -> {
            if (indice == 0) assertEquals(vidaInicial, j.getVida());
            if (indice == 2) assertEquals(vidaEsperada, j.getVida());
        };

        assertEquals(ResultadoCombate.ENEMIGO_MUERE, combatir(j, e, dados));
        assertEquals(vidaEsperada, j.getVida());
        assertTrue(j.getVida() <= j.getVidaMaxima());
        assertEquals(11, j.getHabilidades().get(aliento).getNivel());
    }

    @ParameterizedTest
    @CsvSource({"29,30", "30,30", "25,27"})
    void sedDeBatallaCuraTrasDanioConNivelInternoYRespetaVidaMaxima(int vida, int esperada) {
        Jugador j = jugador(vida);
        SedDeBatalla sed = new SedDeBatalla();
        j.aprenderHabilidad(sed, 50);
        Dados dados = new Dados(1, 1, 1, 1, 4, 2);
        dados.antesDeTirar = indice -> assertEquals(vida, j.getVida());

        assertEquals(ResultadoCombate.ENEMIGO_MUERE, combatir(j, enemigo(10), dados));
        assertEquals(esperada, j.getVida());
        assertEquals(50, j.getHabilidades().get(sed).getNivel());
    }

    @ParameterizedTest
    @CsvSource({"1,2", "4,2"})
    void falloODanioCeroNoActivaSedNiImpetu(int dadoAtaque, int dadoDefensa) {
        Jugador j = jugador(5);
        j.aprenderHabilidad(new SedDeBatalla(), 1);
        j.aprenderHabilidad(new Impetu(), 1);
        Enemigo e = enemigo(30);
        e.setResistencia(100);
        Dados dados = new Dados(1, 1, 1, 1, dadoAtaque, dadoDefensa, 4, 2);
        dados.antesDeTirar = indice -> {
            if (indice == 6) {
                assertEquals(5, j.getVida());
                assertEquals(0, j.getAgilidad());
                assertEquals(30, e.getVida());
            }
        };

        assertEquals(ResultadoCombate.JUGADOR_MUERE, combatir(j, e, dados));
        assertEquals(0, j.getVida());
        assertEquals(0, j.getAgilidad());
    }

    @Test
    void muerteDelJugadorRestauraRabiaSinBorrarEquipoNiEstado() {
        Jugador j = jugador(5);
        Rabia rabia = new Rabia();
        Arma arma = new Arma("Arma de prueba", TipoEquipo.ARMA,
                Map.of(Atributo.FUERZA, 3, Atributo.RESISTENCIA, -2), Map.of(rabia, 4));
        assertTrue(j.equipar(arma));
        EstadoHabilidad estado = j.getHabilidades().get(rabia);
        Enemigo e = enemigo(30);
        e.setIniciativa(10);
        Dados dados = new Dados(1, 1, 1, 1, 4, 2);
        dados.antesDeTirar = indice -> {
            assertEquals(15, j.getFuerza());
            assertEquals(1, j.getAgilidad());
        };

        assertEquals(ResultadoCombate.JUGADOR_MUERE, combatir(j, e, dados));
        assertEquals(0, j.getVida());
        assertEquals(10, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(13, j.getFuerza());
        assertEquals(0, j.getAgilidad());
        assertEquals(0, j.getAtributoBase(Atributo.RESISTENCIA));
        assertEquals(-2, j.getModificadoresEquipo().get(Atributo.RESISTENCIA));
        assertSame(estado, j.getHabilidades().get(rabia));
        assertFalse(estado.isAprendida());
        assertEquals(1, estado.getVecesEquipada());
        assertEquals(4, estado.getNivel());
    }

    @ParameterizedTest
    @CsvSource({"false,0,25,91", "true,0,25,94", "false,1,25,94", "true,1,50,96"})
    void caparazonRecibeEstadoActivoYNivelPropioSinModificarlo(
            boolean aprendida, int equipos, int nivel, int vidaEsperada) {
        Jugador j = new Jugador("J", "", 10, 0, 3, 3, 0, 100, 100);
        Enemigo e = new Enemigo("E", "", 5, 4, 1, 2, 0, 100, 100);
        Caparazon caparazon = new Caparazon();
        EstadoHabilidad estado = new EstadoHabilidad(aprendida, equipos, nivel);
        e.getHabilidades().put(caparazon, estado);
        Dados dados = new Dados(4, 2);
        new Combate(j, e, dados).ataque(j, e);

        dados.comprobarConsumo();
        assertEquals(vidaEsperada, e.getVida());
        assertSame(estado, e.getHabilidades().get(caparazon));
        assertEquals(aprendida, estado.isAprendida());
        assertEquals(equipos, estado.getVecesEquipada());
        assertEquals(nivel, estado.getNivel());
    }

    @Test
    void caparazonCombinaAprendizajeYEquipoYConservaAprendizajeAlDesequipar() {
        Jugador j = new Jugador("J", "", 10, 0, 3, 3, 0, 100, 100);
        Enemigo e = new Enemigo("E", "", 5, 4, 1, 2, 0, 100, 100);
        Caparazon caparazon = new Caparazon();
        e.aprenderHabilidad(caparazon, 20);
        EstadoHabilidad estado = e.getHabilidades().get(caparazon);
        assertTrue(e.equipar(new Arma("Arma de prueba", TipoEquipo.ARMA,
                Map.of(Atributo.FUERZA, 2), Map.of(new Caparazon(), 5))));
        assertEquals(25, estado.getNivel());
        Dados conEquipo = new Dados(4, 2);
        new Combate(j, e, conEquipo).ataque(j, e);
        conEquipo.comprobarConsumo();
        assertEquals(94, e.getVida());

        assertTrue(e.desequipar(TipoEquipo.ARMA));
        assertSame(estado, e.getHabilidades().get(caparazon));
        assertTrue(estado.isAprendida());
        assertEquals(0, estado.getVecesEquipada());
        assertEquals(20, estado.getNivel());
        assertEquals(5, e.getFuerza());
        e.setVida(100);
        Dados sinEquipo = new Dados(4, 2);
        new Combate(j, e, sinEquipo).ataque(j, e);
        sinEquipo.comprobarConsumo();
        assertEquals(93, e.getVida());
    }

    @Test
    void debilidadEnemigaResuelveElCombateEnInicioSinConsumirDados() {
        Jugador j = new Jugador("J", "", 20, 20, 20, 20, 20, 30, 30);
        Enemigo e = new Enemigo("E", "", 5, 5, 5, 5, 5, 30, 30);
        j.aprenderHabilidad(new DebilidadEnemiga(), 1);
        assertEquals(ResultadoCombate.ENEMIGO_MUERE, combatir(j, e, new Dados()));
        assertEquals(30, j.getVida());
        assertEquals(0, e.getVida());
    }

    @Test
    void habilidadesRealesProducenLoMismoEnAutomaticoEInteractivoSinNarracionNueva() {
        Jugador automatico = jugador(29);
        Jugador interactivo = jugador(29);
        automatico.aprenderHabilidad(new Rabia(), 1);
        automatico.aprenderHabilidad(new SedDeBatalla(), 2);
        interactivo.aprenderHabilidad(new Rabia(), 1);
        interactivo.aprenderHabilidad(new SedDeBatalla(), 2);
        Enemigo enemigoAutomatico = enemigo(10);
        Enemigo enemigoInteractivo = enemigo(10);
        Dados dadosAutomaticos = new Dados(1, 1, 1, 1, 4, 2);
        Dados dadosInteractivos = new Dados(1, 1, 1, 1, 4, 2);
        ResultadoCombate esperado = combatir(automatico, enemigoAutomatico, dadosAutomaticos);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ResultadoCombate obtenido = assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                new Combate(interactivo, enemigoInteractivo, dadosInteractivos).combatir(new Scanner("1\n"),
                        new PrintStream(salida, true, StandardCharsets.UTF_8)));

        dadosInteractivos.comprobarConsumo();
        assertEquals(esperado, obtenido);
        assertEquals(automatico.getVida(), interactivo.getVida());
        assertEquals(30, interactivo.getVida());
        assertEquals(automatico.getFuerza(), interactivo.getFuerza());
        assertEquals(automatico.getAgilidad(), interactivo.getAgilidad());
        assertEquals(enemigoAutomatico.getVida(), enemigoInteractivo.getVida());
        String texto = salida.toString(StandardCharsets.UTF_8);
        assertFalse(texto.contains("Rabia"));
        assertFalse(texto.contains("Sed de batalla"));
        assertTrue(texto.contains("1. Atacar"));
    }

    private static class Dados implements TiradorDado {
        private final int[] valores;
        private int usadas;
        private IntConsumer antesDeTirar = indice -> {};

        private Dados(int... valores) {
            this.valores = valores;
        }

        @Override
        public int tirarDado() {
            assertTrue(usadas < valores.length, "Tirada inesperada o combate sin terminar");
            antesDeTirar.accept(usadas);
            return valores[usadas++];
        }

        private void comprobarConsumo() {
            assertEquals(valores.length, usadas, "Consumo exacto de dados");
        }
    }
}
