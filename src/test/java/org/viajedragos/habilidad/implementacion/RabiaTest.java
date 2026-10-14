package org.viajedragos.habilidad.implementacion;

import org.junit.jupiter.api.Test;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RabiaTest {

    @Test
    void RabiaModificaFuerzaAgilidadYResistencia(){

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo();
        Rabia rabia = new Rabia();

        rabia.alInicioCombate(jugador, enemigo);

        assertAll(()-> assertEquals(12, jugador.getFuerza()),
                ()-> assertEquals(11, jugador.getAgilidad()),
                () -> assertEquals(9, jugador.getResistencia()));

    }
}
