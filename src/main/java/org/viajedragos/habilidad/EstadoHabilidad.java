package org.viajedragos.habilidad;

public class EstadoHabilidad {

    private boolean aprendida;
    private int vecesEquipada;
    private int nivel;

    public EstadoHabilidad(boolean aprendida, int vecesEquipada, int nivel) {
        this.aprendida = aprendida;
        this.vecesEquipada = vecesEquipada;
        this.nivel = nivel;
    }

    public EstadoHabilidad() {
    }

    public boolean isAprendida() {
        return aprendida;
    }

    public void setAprendida(boolean aprendida) {
        this.aprendida = aprendida;
    }

    public int getVecesEquipada() {
        return vecesEquipada;
    }

    public void setVecesEquipada(int vecesEquipada) {
        this.vecesEquipada = vecesEquipada;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        this.nivel = nivel;
    }
}
