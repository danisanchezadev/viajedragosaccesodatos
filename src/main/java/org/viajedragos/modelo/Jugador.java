package org.viajedragos.modelo;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import java.util.HashSet;

public class Jugador extends Combatiente{

    private final Map<TipoPocion, Integer> inventarioPociones = new EnumMap<>(TipoPocion.class);
    private Map<TipoPocion, DefinicionPocion> definicionesPociones;

    public Jugador(String nombre, String descripcion, int fuerza, int resistencia, int iniciativa, int agilidad, int velocidad, int vida, int vidaMaxima) {
        this(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad, velocidad, vida, vidaMaxima,
                pocionesPredeterminadas());
    }

    public Jugador(String nombre, String descripcion, int fuerza, int resistencia, int iniciativa,
                   int agilidad, int velocidad, int vida, int vidaMaxima, List<DefinicionPocion> pociones) {
        super(nombre, descripcion, fuerza, resistencia, iniciativa, agilidad, velocidad, vida, vidaMaxima);
        configurarPociones(pociones);
    }

    public Jugador(){
        configurarPociones(pocionesPredeterminadas());
    }

    private static List<DefinicionPocion> pocionesPredeterminadas() {
        return Arrays.stream(TipoPocion.values()).map(tipo -> new DefinicionPocion(
                "pocion_" + tipo.name().toLowerCase(Locale.ROOT), tipo,
                tipo.getNombre(), tipo.getRecuperacionPS())).toList();
    }

    // Se valida la copia completa antes de sustituir la configuración; el inventario no cambia.
    public void configurarPociones(List<DefinicionPocion> pociones) {
        var copia = new EnumMap<TipoPocion, DefinicionPocion>(TipoPocion.class);
        var ids = new HashSet<String>();
        for (DefinicionPocion definicion : List.copyOf(pociones)) {
            if (!ids.add(definicion.id()) || copia.putIfAbsent(definicion.tipo(), definicion) != null) {
                throw new IllegalArgumentException("Hay pociones con identificador o tipo repetido");
            }
        }
        if (copia.size() != TipoPocion.values().length) {
            throw new IllegalArgumentException("Falta la definición de alguna poción");
        }
        definicionesPociones = Collections.unmodifiableMap(copia);
    }

    public DefinicionPocion getDefinicionPocion(TipoPocion tipo) {
        return definicionesPociones.get(Objects.requireNonNull(tipo));
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
        curarse(getDefinicionPocion(tipo).recuperacionPS());
        int recuperacion = getVida() - vidaAnterior;
        if (recuperacion > 0) {
            if (cantidad == 1) inventarioPociones.remove(tipo);
            else inventarioPociones.put(tipo, cantidad - 1);
        }
        return recuperacion;
    }

}
