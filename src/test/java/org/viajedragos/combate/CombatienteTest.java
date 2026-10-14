package org.viajedragos.combate;

import org.junit.jupiter.api.Test;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CombatienteTest {

    @Test
    void pruebaConVidaEstaVivo(){

        Combatiente combatiente = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 1, 30);

        boolean vivo = combatiente.estaVivo();

        assertTrue(vivo);
    }

    @Test
    void pruebaCombatienteMuerto(){

        Combatiente combatiente = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 0, 20);

        boolean vivo = combatiente.estaVivo();

        assertFalse(vivo);
    }
}
