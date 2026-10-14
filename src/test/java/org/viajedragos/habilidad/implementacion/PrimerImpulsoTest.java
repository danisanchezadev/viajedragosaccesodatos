package org.viajedragos.habilidad.implementacion;

import org.viajedragos.habilidad.EstadoHabilidad;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrimerImpulsoTest {

    @ParameterizedTest
    @CsvSource({"9, 15", "10, 16", "11, 17"})
    void iniciativaAumentadaPorImpulso(int nivel, int iniciativaEsperada){

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 1, 30);

        PrimerImpulso pi = new PrimerImpulso(nivel);

        pi.alInicioCombate(jugador, new Enemigo(), new EstadoHabilidad(true, 0, nivel));

        assertEquals(iniciativaEsperada, jugador.getIniciativa());
    }

    @ParameterizedTest
    @CsvSource({"9, 15", "10, 16", "11, 17"})
    void iniciativaAumentadaPorImpulsoFinCombate(int nivel, int iniciativaAumentada){

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 1, 30);

        //jugador.setIniciativa(jugador.getIniciativa() + nivel);

        PrimerImpulso pi = new PrimerImpulso(nivel);

        pi.alInicioCombate(jugador, new Enemigo(), new EstadoHabilidad(true, 0, nivel));

        assertEquals(iniciativaAumentada, jugador.getIniciativa());

        pi.alFinalCombate(jugador, new Enemigo(), new EstadoHabilidad(true, 0, nivel));

        assertEquals(6, jugador.getIniciativa());
    }
}
