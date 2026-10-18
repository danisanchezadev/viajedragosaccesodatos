package org.viajedragos.modelo;

public class Enemigo extends Combatiente{

    private final int experiencia;
    private final int nivel;

    public int getExperiencia() { return experiencia; }
    public int getNivel() { return nivel; }

    public Enemigo(String nombre, String descripcion, int fuerza, int resistencia, int iniciativa, int agilidad, int velocidad, int vida, int vidaMaxima) {
        this(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad, velocidad, vida, vidaMaxima, 5);
    }

    public Enemigo(String nombre, String descripcion, int fuerza, int resistencia, int iniciativa,
                   int agilidad, int velocidad, int vida, int vidaMaxima, int experiencia) {
        this(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad, velocidad, vida, vidaMaxima, experiencia, 1);
    }

    public Enemigo(String nombre, String descripcion, int fuerza, int resistencia, int iniciativa,
                   int agilidad, int velocidad, int vida, int vidaMaxima, int experiencia, int nivel) {
        super(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad, velocidad, vida, vidaMaxima);
        if (experiencia <= 0) throw new IllegalArgumentException("La experiencia debe ser positiva");
        if (nivel < 1) throw new IllegalArgumentException("El nivel debe ser al menos 1");
        this.experiencia = experiencia;
        this.nivel = nivel;
    }

    public Enemigo() {
        experiencia = 5;
        nivel = 1;
    }
}
