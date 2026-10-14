package org.viajedragos.combate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.excepciones.DadoInvalidoException;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.*;

class AtaqueTest {

    private Jugador jugador() {
        return new Jugador("Jugador", "", 10, 0, 1, 3, 1, 100, 100);
    }

    private Enemigo enemigo() {
        return new Enemigo("Enemigo", "", 5, 4, 1, 2, 1, 100, 100);
    }

    @ParameterizedTest
    @CsvSource({
            "1,1,0,0", "1,2,0,0", "1,3,0,0", "1,4,0,0", "1,5,0,0", "1,6,0,5",
            "2,1,15,0", "2,2,7,0", "2,3,6,0", "2,4,5,0", "2,5,4,0", "2,6,3,0",
            "3,1,16,0", "3,2,8,0", "3,3,7,0", "3,4,6,0", "3,5,5,0", "3,6,4,0",
            "4,1,17,0", "4,2,9,0", "4,3,8,0", "4,4,7,0", "4,5,6,0", "4,6,5,0",
            "5,1,18,0", "5,2,10,0", "5,3,9,0", "5,4,8,0", "5,5,7,0", "5,6,6,0",
            "6,1,23,0", "6,2,11,0", "6,3,10,0", "6,4,9,0", "6,5,8,0", "6,6,7,0"
    })
    void resuelveTodasLasCombinacionesDeDados(int ataque, int defensa, int danioDefensor, int danioAtacante) {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        DadosControlados dados = new DadosControlados(ataque, defensa, 4);

        new Combate(jugador, enemigo, dados).ataque(jugador, enemigo);

        assertEquals(100 - danioDefensor, enemigo.getVida());
        assertEquals(100 - danioAtacante, jugador.getVida());
        assertEquals(ataque == 6 && defensa == 1 ? 3 : 2, dados.tiradas);
    }

    @Test
    void defensaMayorProduceCeroSinCurarAlDefensor() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        enemigo.setResistencia(100);
        enemigo.setVida(50);
        new Combate(jugador, enemigo, new DadosControlados(3, 4)).ataque(jugador, enemigo);
        assertEquals(50, enemigo.getVida());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void criticoSumaExactamenteLaTiradaExtraSinDefensa(int extra) {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        DadosControlados dados = new DadosControlados(6, 1, extra);
        new Combate(jugador, enemigo, dados).ataque(jugador, enemigo);
        assertEquals(100 - 19 - extra, enemigo.getVida());
        assertEquals(100, jugador.getVida());
        assertEquals(3, dados.tiradas);
    }

    @Test
    void danioLetalSeAplicaUnaVezYLaVidaNoBajaDeCero() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        enemigo.setVida(2);
        new Combate(jugador, enemigo, new DadosControlados(4, 2)).ataque(jugador, enemigo);
        assertEquals(0, enemigo.getVida());
        assertEquals(100, jugador.getVida());
    }

    @Test
    void admiteAlEnemigoComoAtacante() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        new Combate(jugador, enemigo, new DadosControlados(4, 2)).ataque(enemigo, jugador);
        // Ataque: 5 + 2 + 4 = 11; defensa: 0 + 3 + 2 = 5.
        assertEquals(94, jugador.getVida());
        assertEquals(100, enemigo.getVida());
    }

    @Test
    void dadoInvalidoNoLlegaAAplicarDanio() {
        Jugador jugador = jugador();
        Enemigo enemigo = enemigo();
        assertThrows(DadoInvalidoException.class,
                () -> new Combate(jugador, enemigo, new DadosControlados(3, 7)).ataque(jugador, enemigo));
        assertEquals(100, jugador.getVida());
        assertEquals(100, enemigo.getVida());
    }

    private static class DadosControlados implements TiradorDado {
        private final int[] resultados;
        private int tiradas;

        private DadosControlados(int... resultados) {
            this.resultados = resultados;
        }

        @Override
        public int tirarDado() {
            assertTrue(tiradas < resultados.length, "El ataque solicitó una tirada no prevista");
            return resultados[tiradas++];
        }
    }
}
