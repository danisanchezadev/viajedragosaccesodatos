package org.viajedragos.modelo;

import java.util.*;

/** Progreso de una sola aventura, independiente del personaje y de los repositorios. */
public final class ProgresoAventura {
    private int plantaActual = 1;
    private int ultimaDesbloqueada = 1;
    private final Map<Integer, Integer> victorias = new HashMap<>();
    private final Map<Integer, Set<String>> especies = new HashMap<>();
    private final Set<Integer> encuentrosPremiados = new HashSet<>();
    private boolean guardianDerrotado;
    private int fragmentos;

    public Planta plantaActual() { return Planta.porNumero(plantaActual); }
    public boolean desbloqueada(int planta) { Planta.porNumero(planta); return planta <= ultimaDesbloqueada; }
    public int victorias(int planta) { Planta.porNumero(planta); return victorias.getOrDefault(planta, 0); }
    public Set<String> especiesDerrotadas(int planta) {
        Planta.porNumero(planta);
        return Set.copyOf(especies.getOrDefault(planta, Set.of()));
    }
    public boolean guardianDerrotado() { return guardianDerrotado; }
    public int fragmentos() { return fragmentos; }

    /** Solo se invoca tras la victoria definitiva; el número de encuentro evita duplicarla. */
    public boolean registrarVictoria(int encuentro, DefinicionEnemigo enemigo) {
        Objects.requireNonNull(enemigo);
        if (encuentro < 1 || enemigo.planta() != plantaActual) throw new IllegalArgumentException("Victoria fuera de su encuentro/planta");
        if (!encuentrosPremiados.add(encuentro)) return false;
        victorias.merge(plantaActual, 1, Integer::sum);
        especies.computeIfAbsent(plantaActual, n -> new HashSet<>()).add(enemigo.id());
        if (plantaActual == 2 && enemigo.guardian()) guardianDerrotado = true;
        if (plantaActual == 4) fragmentos = Math.min(3, fragmentos + 1);
        boolean cumplido = switch (plantaActual) {
            case 1 -> victorias(1) >= 3;
            case 2 -> guardianDerrotado;
            case 3 -> especiesDerrotadas(3).size() >= 3;
            case 4 -> fragmentos >= 3;
            default -> false;
        };
        if (cumplido) ultimaDesbloqueada = Math.max(ultimaDesbloqueada, plantaActual + 1);
        return true;
    }

    public boolean subir() {
        if (plantaActual == 1) return false;
        plantaActual--; return true;
    }

    public boolean bajar() {
        if (plantaActual == 5 || !desbloqueada(plantaActual + 1)) return false;
        plantaActual++; return true;
    }

    public String resumenProgreso() {
        return switch (plantaActual) {
            case 1 -> "Victorias necesarias: " + Math.min(3, victorias(1)) + "/3";
            case 2 -> "Guardián: " + (guardianDerrotado ? "derrotado" : "pendiente");
            case 3 -> "Especies diferentes derrotadas: " + Math.min(3, especiesDerrotadas(3).size()) + "/3";
            case 4 -> "Fragmentos del sello: " + fragmentos + "/3";
            default -> "Última planta disponible";
        };
    }

    public String requisitoDescenso() {
        return switch (plantaActual) {
            case 1 -> "Derrota tres enemigos: " + victorias(1) + "/3 victorias.";
            case 2 -> "Derrota al Capitán de barracones.";
            case 3 -> "Derrota tres especies diferentes: " + especiesDerrotadas(3).size() + "/3.";
            case 4 -> "Obtén tres fragmentos de sello: " + fragmentos + "/3.";
            default -> "Todavía no se han desarrollado más plantas";
        };
    }
}
