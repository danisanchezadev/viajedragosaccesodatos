package org.viajedragos.modelo;

public class Jugador extends Combatiente{

    public Jugador(String nombre, String descripcion, int fuerza, int resistencia, int iniciativa, int agilidad, int velocidad, int vida, int vidaMaxima) {
        super(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad, velocidad, vida, vidaMaxima);

    }

    public Jugador(){
    }

}
