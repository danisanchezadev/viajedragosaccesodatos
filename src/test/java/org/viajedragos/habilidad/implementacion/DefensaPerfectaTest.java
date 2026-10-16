package org.viajedragos.habilidad.implementacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefensaPerfectaTest {

    private Combatiente jugador;
    private Combatiente enemigo;

    @BeforeEach
    void preparar(){
        jugador = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 1, 30);
        enemigo = new Enemigo("Eloy", "Mago", 8, 2, 3, 5, 2, 1, 20);
    }


    @ParameterizedTest
    @CsvSource({"6, 10, 5",
            "6, 9, 4",
            "5, 9, 9",
            "1, 9, 9"})
    void defensaPerfectaReduceDanioALaMitadSiSeis(int tiradaDadoDefensa, int danio, int danioFinalEsperado){

        DefensaPerfecta df = new DefensaPerfecta();
        enemigo.aprenderHabilidad(df, 1);

        ContextoAtaque contexto = new ContextoAtaque(jugador, enemigo);

        contexto.setDadoDefensa(tiradaDadoDefensa);
        contexto.setDanioFinal(danio);

        df.antesDeAplicarDanio(contexto, enemigo.getHabilidades().get(df));

        assertEquals(danioFinalEsperado, contexto.getDanioFinal());

    }
}

