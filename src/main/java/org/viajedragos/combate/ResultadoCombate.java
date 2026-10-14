package org.viajedragos.combate;

public enum ResultadoCombate {

    CONTINUA("El combate continúa."),
    JUGADOR_HUYE("EL jugador ha conseguido huir del combate."),
    ENEMIGO_HUYE("El enemigo huye del combate."),
    JUGADOR_MUERE("El jugador ha muerto en combate. ¡Qué pena! Con lo bien que ibas..."),
    ENEMIGO_MUERE("Has derrotado al enemigo."),
    AMBOS_MUEREN("Conseguiste acabar con tu enemigo... a costa de tu propia vida...");

    private final String descripcion;

    ResultadoCombate(String descripcion){
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
