package org.viajedragos.habilidad.implementacion;

import org.viajedragos.habilidad.EstadoHabilidad;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CastigoAlTorpeTest {

    @ParameterizedTest
    @CsvSource({"false, 1, 14", "false, 2, 10", "true, 1, 10", "true, 2, 10"})
    void castigaConAciertoEnAtaqueYFalloEnDefensa(boolean fallido, int dadoDef, int danioEsperado){

        //Arrange

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);

        ContextoAtaque contexto = new ContextoAtaque(jugador, enemigo);

        contexto.setAtaqueFallido(fallido);
        contexto.setDanioFinal(10);
        contexto.setDadoDefensa(dadoDef);

        CastigoAlTorpe ct = new CastigoAlTorpe();

        //Act
        ct.antesDeAplicarDanio(contexto, new EstadoHabilidad(true, 0, 4));

        //assert

        assertEquals(danioEsperado, contexto.getDanioFinal());
    }
}
