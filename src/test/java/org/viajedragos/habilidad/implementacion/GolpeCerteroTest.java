package org.viajedragos.habilidad.implementacion;

import org.viajedragos.habilidad.EstadoHabilidad;

import org.junit.jupiter.api.Test;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GolpeCerteroTest {

    //Si el ataque no es un ataque fallido, siempre aumenta en dos,
    // por lo que un ataque que por resistencias terminara siendo 0, hará 2 de daño.


    @Test
    void golpeCerteroAumentaElDanioFinalEnDos(){

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);

        ContextoAtaque contexto = new ContextoAtaque(jugador, enemigo);
        contexto.setDanioFinal(10);

        GolpeCertero gp = new GolpeCertero();
        gp.antesDeAplicarDanio(contexto, new EstadoHabilidad(true, 0, 2));

        assertEquals(12, contexto.getDanioFinal());
    }

    @Test
    void golpeCerteroAumentaElDanioFinalEnDosConCero(){

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);

        ContextoAtaque contexto = new ContextoAtaque(jugador, enemigo);
        contexto.setDanioFinal(0);

        GolpeCertero gp = new GolpeCertero();
        gp.antesDeAplicarDanio(contexto, new EstadoHabilidad(true, 0, 2));

        assertEquals(2, contexto.getDanioFinal());
    }

    @Test
    void golpeCerteroRecibeDanioNegativo(){

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);

        ContextoAtaque contexto = new ContextoAtaque(jugador, enemigo);
        contexto.setDanioFinal(-1);

        GolpeCertero gp = new GolpeCertero();
        gp.antesDeAplicarDanio(contexto, new EstadoHabilidad(true, 0, 2));

        assertEquals(2, contexto.getDanioFinal());
    }



}
