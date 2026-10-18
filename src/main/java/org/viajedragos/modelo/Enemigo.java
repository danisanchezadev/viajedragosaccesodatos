package org.viajedragos.modelo;

public class Enemigo extends Combatiente{

    private final int experiencia;

    public int getExperiencia() { return experiencia; }

    public Enemigo(String nombre, String descripcion, int fuerza, int resistencia, int iniciativa, int agilidad, int velocidad, int vida, int vidaMaxima) {
        this(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad, velocidad, vida, vidaMaxima, 5);
    }

    public Enemigo(String nombre, String descripcion, int fuerza, int resistencia, int iniciativa,
                   int agilidad, int velocidad, int vida, int vidaMaxima, int experiencia) {
        super(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad, velocidad, vida, vidaMaxima);
        if (experiencia <= 0) throw new IllegalArgumentException("La experiencia debe ser positiva");
        this.experiencia = experiencia;
    }

    public Enemigo() {
        experiencia = 5;
    }
}
