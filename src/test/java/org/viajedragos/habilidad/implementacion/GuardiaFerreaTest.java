package org.viajedragos.habilidad.implementacion;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GuardiaFerreaTest {

    @ParameterizedTest
    @CsvSource({"10, 8",
            "8, 6",
            "2, 0",
            "1, 0",
            "0, 0"})
    void comprobarDiferentesDaniosEnValoresLimite(int danioFinal, int danioReducido) {

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);

        ContextoAtaque contexto = new ContextoAtaque(jugador, enemigo);

        contexto.setDanioFinal(danioFinal);

        GuardiaFerrea gf = new GuardiaFerrea();

        gf.antesDeAplicarDanio(contexto);

        assertEquals(danioReducido, contexto.getDanioFinal());

    }
}
