package org.viajedragos.habilidad;

public class EstadoHabilidad {

    private boolean aprendida;
    private int vecesEquipada;
    private int nivel;

    public EstadoHabilidad(boolean aprendida, int vecesEquipada, int nivel) {
        this.aprendida = aprendida;
        setVecesEquipada(vecesEquipada);
        setNivel(nivel);
    }

    public EstadoHabilidad() {
        this(false, 0, 1);
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
        if (vecesEquipada < 0) {
            throw new IllegalArgumentException("El número de equipos no puede ser negativo");
        }
        this.vecesEquipada = vecesEquipada;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        if (nivel <= 0) {
            throw new IllegalArgumentException("El nivel de una habilidad debe ser positivo");
        }
        this.nivel = nivel;
    }

    public boolean isActiva() {
        return aprendida || vecesEquipada > 0;
    }
}
