package org.viajedragos.modelo;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class Jugador extends Combatiente{

    private final Map<TipoPocion, Integer> inventarioPociones = new EnumMap<>(TipoPocion.class);

    public Jugador(String nombre, String descripcion, int fuerza, int resistencia, int iniciativa, int agilidad, int velocidad, int vida, int vidaMaxima) {
        super(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad, velocidad, vida, vidaMaxima);

    }

    public Jugador(){
    }

    public void agregarPocion(TipoPocion tipo) {
        inventarioPociones.merge(Objects.requireNonNull(tipo), 1, Integer::sum);
    }

    public int getCantidadPociones(TipoPocion tipo) {
        return inventarioPociones.getOrDefault(Objects.requireNonNull(tipo), 0);
    }

    public Map<TipoPocion, Integer> getInventarioPociones() {
        return Collections.unmodifiableMap(inventarioPociones);
    }

    // Devuelve los PS recuperados; cero indica que no se ha consumido la poción.
    public int usarPocion(TipoPocion tipo) {
        int cantidad = getCantidadPociones(tipo);
        if (cantidad == 0 || getVida() >= getVidaMaxima()) return 0;

        int vidaAnterior = getVida();
        curarse(tipo.getRecuperacionPS());
        int recuperacion = getVida() - vidaAnterior;
        if (recuperacion > 0) {
            if (cantidad == 1) inventarioPociones.remove(tipo);
            else inventarioPociones.put(tipo, cantidad - 1);
        }
        return recuperacion;
    }

}
