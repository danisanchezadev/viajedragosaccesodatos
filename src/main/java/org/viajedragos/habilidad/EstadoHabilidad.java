package org.viajedragos.habilidad;

public class EstadoHabilidad {

    private boolean aprendida;
    private int vecesEquipada;
    private int nivel;
    // Estos datos pertenecen a la entrada del combatiente, no a la instancia de Habilidad.
    private boolean efectoTemporalActivo;
    private int acumulacionesTemporales;

    public EstadoHabilidad(boolean aprendida, int vecesEquipada, int nivel) {
        this.aprendida = aprendida;
        setVecesEquipada(vecesEquipada);
        setNivel(nivel);
    }

    public EstadoHabilidad() {
        this(false, 0, 1);
    }

    public EstadoHabilidad(EstadoHabilidad anterior) {
        this(anterior.aprendida, anterior.vecesEquipada, anterior.nivel);
        efectoTemporalActivo = anterior.efectoTemporalActivo;
        acumulacionesTemporales = anterior.acumulacionesTemporales;
    }

    public boolean isEfectoTemporalActivo() {
        return efectoTemporalActivo;
    }

    public void setEfectoTemporalActivo(boolean efectoTemporalActivo) {
        this.efectoTemporalActivo = efectoTemporalActivo;
    }

    public int getAcumulacionesTemporales() {
        return acumulacionesTemporales;
    }

    public void setAcumulacionesTemporales(int acumulacionesTemporales) {
        if (acumulacionesTemporales < 0) {
            throw new IllegalArgumentException("Las acumulaciones temporales no pueden ser negativas");
        }
        this.acumulacionesTemporales = acumulacionesTemporales;
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
