package org.viajedragos.habilidad;

public class EstadoHabilidad {

    private boolean aprendida;
    private int vecesEquipada;
    private int nivel;
    private int nivelPermanente;
    // Estos datos pertenecen a la entrada del combatiente, no a la instancia de Habilidad.
    private boolean efectoTemporalActivo;
    private int acumulacionesTemporales;

    public EstadoHabilidad(boolean aprendida, int vecesEquipada, int nivel) {
        this(aprendida, vecesEquipada, nivel, aprendida ? nivel : 0);
    }

    // El constructor anterior no puede deducir un reparto mixto: para ese caso
    // se especifica el nivel permanente o se usan las operaciones de Combatiente.
    public EstadoHabilidad(boolean aprendida, int vecesEquipada, int nivel, int nivelPermanente) {
        if (nivelPermanente < 0 || nivelPermanente > nivel || aprendida != (nivelPermanente > 0)) {
            throw new IllegalArgumentException("Nivel permanente incompatible con el estado");
        }
        this.aprendida = aprendida;
        setVecesEquipada(vecesEquipada);
        setNivel(nivel);
        this.nivelPermanente = nivelPermanente;
    }

    public EstadoHabilidad() {
        this(false, 0, 1);
    }

    public EstadoHabilidad(EstadoHabilidad anterior) {
        this(anterior.aprendida, anterior.vecesEquipada, anterior.nivel, anterior.nivelPermanente);
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

    public int getNivelPermanente() { return nivelPermanente; }

    public int getNivelEquipo() { return isActiva() ? nivel - nivelPermanente : 0; }

    // Actualiza ambas partes juntas, después de comprobar los desbordamientos.
    public void aprender(int niveles) {
        if (niveles <= 0) throw new IllegalArgumentException("Los niveles deben ser positivos");
        int total = Math.addExact(isActiva() ? nivel : 0, niveles);
        int permanente = Math.addExact(nivelPermanente, niveles);
        nivel = total;
        nivelPermanente = permanente;
        aprendida = true;
    }

    public void agregarEquipo(int niveles) {
        if (niveles <= 0) throw new IllegalArgumentException("Los niveles deben ser positivos");
        int total = Math.addExact(isActiva() ? nivel : 0, niveles);
        int equipos = Math.incrementExact(vecesEquipada);
        nivel = total;
        vecesEquipada = equipos;
    }

    public void retirarEquipo(int niveles) {
        if (niveles <= 0 || vecesEquipada <= 0 || niveles > getNivelEquipo()
                || nivel - niveles <= 0) throw new IllegalStateException("Contribución de equipo inválida");
        nivel -= niveles;
        vecesEquipada--;
    }

    public void restaurar(EstadoHabilidad anterior) {
        aprendida = anterior.aprendida;
        vecesEquipada = anterior.vecesEquipada;
        nivel = anterior.nivel;
        nivelPermanente = anterior.nivelPermanente;
        efectoTemporalActivo = anterior.efectoTemporalActivo;
        acumulacionesTemporales = anterior.acumulacionesTemporales;
    }

    public void setAprendida(boolean aprendida) {
        if (aprendida != this.aprendida) nivelPermanente = aprendida ? nivel : 0;
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
        if (aprendida && this.nivel > 0) {
            int permanente = Math.subtractExact(nivel, getNivelEquipo());
            if (permanente <= 0) throw new IllegalArgumentException("El nivel permanente debe ser positivo");
            nivelPermanente = permanente;
        }
        this.nivel = nivel;
    }

    public boolean isActiva() {
        return aprendida || vecesEquipada > 0;
    }
}
