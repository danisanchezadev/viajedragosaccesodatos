package org.viajedragos.habilidad.implementacion;

import org.junit.jupiter.api.Test;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImpetuTest {


    @Test
    void impetuAcumulaHastaTresYSeReinciaAlFinalCombate(){

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 2, 3, 5, 2, 1, 20);

        ContextoAtaque contexto = new ContextoAtaque(jugador, enemigo);

        Impetu impetu = new Impetu();

        //estado inical

        System.out.println("Agilidad al inicio de la prueba: " + jugador.getAgilidad() + ".");
        System.out.println("                       ");
        System.out.println("=======================");
        System.out.println("                       ");
        assertEquals(10, jugador.getAgilidad());

        System.out.println("Revisamos como va creciendo la agilidad al hacer daño" + ".");
        System.out.println("                       ");
        System.out.println("=======================");
        System.out.println("                       ");

        //varios turnos haciendo daño para ver si impetu mejor hasta 3 y luego ya no mejora más

        for(int i = 1; i < 10; i ++){

            contexto.setDanioFinal(1);

            impetu.alHacerDanio(contexto);

            int aumetnoEsperado = Math.min(i,3);

            assertEquals(10 + aumetnoEsperado, jugador.getAgilidad());

        }

        //Agilidad debe volver a su valor de inicio del combate al finalizarlo.

        impetu.alFinalCombate(jugador,enemigo);

        System.out.println("Ahora evalumos si la agilidad vuelve a su estado inicial: " + jugador.getAgilidad() + ".");

        assertEquals(10, jugador.getAgilidad());
    }

    @Test
    void impetuNoSumaSiNoSeHaceDanio(){

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 10, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 2, 3, 5, 2, 1, 20);

        ContextoAtaque contexto = new ContextoAtaque(jugador, enemigo);

        Impetu impetu = new Impetu();

        //Probamos que pasa si el daño es 0 (no puede ser negativo)

        contexto.setDanioFinal(0);

        impetu.alHacerDanio(contexto);

        assertEquals(10, jugador.getAgilidad());

    }
}
