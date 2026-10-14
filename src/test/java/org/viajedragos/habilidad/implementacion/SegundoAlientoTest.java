package org.viajedragos.habilidad.implementacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SegundoAlientoTest {

    private Combatiente personaje;

    @BeforeEach
    void prepararPersonaje(){

        personaje = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 1, 30);
    }

    @Test
    void seCuraSiVidaEsMenosDeLaMitad(){

        Combatiente personaje = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 1, 30);

        SegundoAliento sa = new SegundoAliento(2);

        sa.alInicioTurno(personaje, new Enemigo());

        assertEquals(3, personaje.getVida());

    }

    @Test
    void seCuraSiVidaEsLaMitad(){

        Combatiente personaje = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 15, 30);

        SegundoAliento sa = new SegundoAliento(2);

        sa.alInicioTurno(personaje, new Enemigo());

        assertEquals(15, personaje.getVida());

    }

    @Test
    void seCuraSiVidaEsLaMitadImpar(){

        Combatiente personaje = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 15, 31);

        SegundoAliento sa = new SegundoAliento(2);

        sa.alInicioTurno(personaje, new Enemigo());

        assertEquals(17, personaje.getVida());

    }

    @ParameterizedTest
    @CsvSource({
            "14, 30, 1, 15",
            "14, 30, 2, 16",
            "14, 30, 3, 17",
            "14, 30, 5, 19",
            "15, 31, 6, 21",
            "0, 40, 7, 0"
    })
    void curaSegunNivel(
            int vida,
            int vidaMaxima,
            int nivel,
            int vidaEsperada) {

        Combatiente personaje =
                new Jugador("Dani", "Guerrero",
                        10, 10, 6, 10, 4,
                        vida, vidaMaxima);

        SegundoAliento sa = new SegundoAliento(nivel);

        sa.alInicioTurno(personaje, new Enemigo());

        assertEquals(vidaEsperada, personaje.getVida());
    }
}
