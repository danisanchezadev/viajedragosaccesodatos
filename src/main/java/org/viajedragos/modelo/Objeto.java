package org.viajedragos.modelo;

public abstract class Objeto {

    private String nombre;

    public Objeto(String nombre){
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
