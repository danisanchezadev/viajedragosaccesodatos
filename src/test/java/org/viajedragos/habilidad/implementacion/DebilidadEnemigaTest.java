package org.viajedragos.habilidad.implementacion;

import org.viajedragos.habilidad.EstadoHabilidad;

import org.junit.jupiter.api.Test;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.habilidad.implementacion.DebilidadEnemiga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class DebilidadEnemigaTest {

    @Test
    void eliminaOponenteSiMediaEsAlMenosElDoble(){

        Combatiente combatiente = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 1, 30);

        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 2, 3, 5, 2, 1, 20);

        DebilidadEnemiga deb = new DebilidadEnemiga();

        deb.alInicioCombate(combatiente, enemigo, new EstadoHabilidad(true, 0, 1));

        assertEquals(0, enemigo.getVida());

    }

    @Test
    void noEliminaOponenteSiMediaNoEsElDoble() {

        // Arrange
        Combatiente combatiente = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 1, 30);

        Combatiente enemigo = new Enemigo("Eloy", "Mago", 20, 2, 3, 5, 2, 1, 20);

        // Act
        DebilidadEnemiga deb = new DebilidadEnemiga();

        deb.alInicioCombate(combatiente, enemigo, new EstadoHabilidad(true, 0, 1));

        // Assert
        assertNotEquals(20, enemigo.getVida());


    }
}
