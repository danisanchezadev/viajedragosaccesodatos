package org.viajedragos.combate;

import org.junit.jupiter.api.Test;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CombateTest {

    //=====================================================
    //
    //Tests de primerTurno()
    //
    //====================================================



    @Test
    void eligePersonajeCorrectoTurno() {

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);
        TiradorDado tirador = new DadoTrucadoFalso(6, 1);


        Combate combate = new Combate(jugador, enemigo, tirador);

        Combatiente c = combate.primerTurno();

        assertEquals(jugador, c);
    }

    @Test
    void eligePersonajeCorrectoEnemigoTurno() {

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);
        TiradorDado tirador = new DadoTrucadoFalso(1, 6);


        Combate combate = new Combate(jugador, enemigo, tirador);

        Combatiente c = combate.primerTurno();

        assertEquals(enemigo, c);

    }

    //El tirador deberá obtener 4 números por constructor que deben dar ganador al jugador tras un primer empate (2, 3, ...) y por ejemeplo (..., 4, 1)
    @Test
    void eligePersonajeCorrectoJugadorTrasEmpate() {

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);
        TiradorDado tirador = new DadoTrucadoFalso(2, 3, 4, 1);

        Combate combate = new Combate(jugador, enemigo, tirador);

        Combatiente c = combate.primerTurno();

        assertEquals(jugador, c);
    }

    @Test
    void eligePersonajeCorrectoEnemigoTrasEmpate() {

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);

        TiradorDado tirador = new DadoTrucadoFalso(1, 2, 1, 6);

        Combate combate = new Combate(jugador, enemigo, tirador);

        Combatiente c = combate.primerTurno();

        assertEquals(enemigo, c);
    }

    //=====================================================
    //
     //Tests de atacante()
    //
     //====================================================


    @Test
    void determinaAtacanteCorrectamenteJugador(){

        //Vamos a supoenr que este es el que sacó el primer turno.
        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        //El enemigo perdió en la tirada de turno y no tiene iniciativa bonificada. Subo la iniciativa a 4 para el test.
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 4, 5, 2, 1, 20);

        TiradorDado tirador = new DadoTrucadoFalso(6, 1);

        Combate combate = new Combate(jugador, enemigo, tirador);

        Combatiente c = combate.atacante(jugador, enemigo);

        assertEquals(jugador, c);
    }

    @Test
    void determinaAtacanteCorrectamenteEnemigo(){

        //Vamos a supoenr que este es el que sacó el primer turno.
        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        //El enemigo perdió en la tirada de turno y no tiene iniciativa bonificada. Subo la iniciativa a 4 para el test.
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 4, 5, 2, 1, 20);

        TiradorDado tirador = new DadoTrucadoFalso(1, 6);

        Combate combate = new Combate(jugador, enemigo, tirador);

        Combatiente c = combate.atacante(jugador, enemigo);

        assertEquals(enemigo, c);
    }

    @Test
    void determinaAtacanteCorrectamentePoseedorDelTurnoConEmpate(){

        //Vamos a supoenr que este es el que sacó el primer turno.
        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);
        //El enemigo perdió en la tirada de turno y no tiene iniciativa bonificada. Dejo la iniciativa en 3 para este test.
        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);

        TiradorDado tirador = new DadoTrucadoFalso(1, 6);

        Combate combate = new Combate(jugador, enemigo, tirador);

        Combatiente c = combate.atacante(jugador, enemigo);

        assertEquals(jugador, c);
    }

    //=====================================================
    //
    //Tests de defensor()
    //
    //====================================================

    @Test
    void determinaDefensorCorrectamenteEnemigo(){

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);

        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);

        TiradorDado tirador = new DadoTrucadoFalso();

        Combate combate = new Combate(jugador, enemigo, tirador);

        Combatiente c = combate.determinarDefensor(jugador);

        assertEquals(enemigo, c);

    }

    @Test
    void determinaDefensorCorrectamenteJugador(){

        Combatiente jugador = new Jugador("Dani", "Guerrero", 10, 0, 6, 10, 4, 15, 31);

        Combatiente enemigo = new Enemigo("Eloy", "Mago", 8, 0, 3, 5, 2, 1, 20);

        TiradorDado tirador = new DadoTrucadoFalso();

        Combate combate = new Combate(jugador, enemigo, tirador);

        Combatiente c = combate.determinarDefensor(enemigo);

        assertEquals(jugador, c);

    }




    public static class DadoTrucadoFalso implements TiradorDado {

        private int contador;
        private final int[] resultadoPactados;


        public DadoTrucadoFalso(int... resultados) {

            this.resultadoPactados = resultados;
        }

        public int tirarDado() {

            int resultado = resultadoPactados[contador];

            contador++;

            return resultado;

        }
    }



















}