package org.viajedragos.habilidad.implementacion;

import org.viajedragos.habilidad.EstadoHabilidad;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SedBatallaTest {

    @ParameterizedTest
    @CsvSource({"true, 1, 15", "true, 0, 15", "false, 1, 17", "false, 0, 15"})
    void soloCuraSiAtaqueEsExitosoYHaceDanio(boolean fallido, int danio, int esperada){

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);

        ContextoAtaque contexto = new ContextoAtaque(jugador, enemigo);

        contexto.setAtaqueFallido(fallido);
        contexto.setDanioFinal(danio);

        SedDeBatalla sdb = new SedDeBatalla();

        sdb.alHacerDanio(contexto, new EstadoHabilidad(true, 0, 2));

        assertEquals(esperada, jugador.getVida());
    }
}
