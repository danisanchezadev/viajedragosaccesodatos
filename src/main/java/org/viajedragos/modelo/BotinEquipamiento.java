package org.viajedragos.modelo;

import org.viajedragos.arma.Equipo;
import java.util.*;

/** Selección de una pieza, sin conceder recompensas ni modificar el inventario. */
public final class BotinEquipamiento {
    // Filas: nivel 1 a 12 (12 también para superiores). Columnas: básico, mejorado, superior.
    private static final int[][] PESOS = {
            {10, 2, 1}, {10, 3, 2}, {10, 4, 3}, {10, 5, 4},
            {10, 6, 5}, {10, 7, 6}, {10, 8, 7}, {10, 9, 8},
            {10, 10, 9}, {10, 11, 10}, {10, 12, 11}, {10, 13, 12}};
    private static final int[][] MEJORAS = {{0, 1}, {0, 2}, {1, 2}, {1, 3}, {2, 3}};
    private final List<DefinicionBotinEquipo> catalogo;

    public BotinEquipamiento(List<DefinicionBotinEquipo> catalogo) { this.catalogo = List.copyOf(catalogo); }

    public boolean tieneDefiniciones(int planta) {
        Planta.porNumero(planta);
        return catalogo.stream().anyMatch(d -> d.disponibleEn(planta));
    }

    public Optional<Equipo> sortear(int planta, int nivelEnemigo, Random azar) {
        Planta.porNumero(planta);
        if (nivelEnemigo < 1) throw new IllegalArgumentException("Nivel de enemigo inválido");
        Objects.requireNonNull(azar);
        if (azar.nextInt(100) >= 20) return Optional.empty();
        var elegibles = catalogo.stream().filter(d -> d.disponibleEn(planta)).toList();
        if (elegibles.isEmpty()) return Optional.empty();
        var calidades = Arrays.stream(CalidadEquipo.values())
                .filter(c -> elegibles.stream().anyMatch(d -> d.calidad() == c)).toList();
        int[] pesos = PESOS[Math.min(nivelEnemigo, PESOS.length) - 1];
        int total = calidades.stream().mapToInt(c -> pesos[c.ordinal()]).sum();
        int tirada = azar.nextInt(total);
        CalidadEquipo calidad = calidades.getLast();
        for (var candidata : calidades) {
            if (tirada < pesos[candidata.ordinal()]) { calidad = candidata; break; }
            tirada -= pesos[candidata.ordinal()];
        }
        var seleccionada = calidad;
        var definiciones = elegibles.stream().filter(d -> d.calidad() == seleccionada).toList();
        var definicion = definiciones.get(azar.nextInt(definiciones.size())).equipo();
        int[] rango = MEJORAS[planta - 1];
        int mejoras = rango[0] + azar.nextInt(rango[1] - rango[0] + 1);
        return Optional.of(definicion.crearEquipo(mejoras));
    }
}
