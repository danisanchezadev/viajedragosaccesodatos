package org.viajedragos.catalogo;

import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.modelo.DefinicionEquipo;
import org.viajedragos.modelo.DefinicionBotinEquipo;
import org.viajedragos.modelo.CalidadEquipo;
import java.util.List;

public interface RepositorioObjetos {
    List<DefinicionArma> obtenerArmas();
    List<DefinicionPocion> obtenerPociones();
    // Definiciones adicionales a obtenerArmas(), sin repetir sus identificadores.
    default List<DefinicionEquipo> obtenerEquipamiento() { return List.of(); }

    /** Los orígenes anteriores no ofrecen botín hasta definir sus asociaciones. */
    default List<DefinicionBotinEquipo> obtenerBotinEquipamiento() { return List.of(); }

    default List<DefinicionBotinEquipo> obtenerBotinPorPlanta(int planta, CalidadEquipo calidad) {
        org.viajedragos.modelo.Planta.porNumero(planta);
        java.util.Objects.requireNonNull(calidad);
        return obtenerBotinEquipamiento().stream().filter(d -> d.disponibleEn(planta) && d.calidad() == calidad).toList();
    }

    static List<DefinicionBotinEquipo> validarBotin(List<DefinicionBotinEquipo> botin,
            List<DefinicionArma> armas, List<DefinicionEquipo> equipos) {
        var definiciones = new java.util.HashMap<String, DefinicionEquipo>();
        for (var arma : List.copyOf(armas)) {
            if (definiciones.putIfAbsent(arma.id(), arma.comoEquipo()) != null)
                throw new IllegalArgumentException("Definición de objeto repetida");
        }
        for (var equipo : List.copyOf(equipos)) {
            if (definiciones.putIfAbsent(equipo.id(), equipo) != null)
                throw new IllegalArgumentException("Definición de objeto repetida");
        }
        var copia = List.copyOf(botin);
        var ids = new java.util.HashSet<String>();
        for (var entrada : copia) {
            if (!ids.add(entrada.equipo().id()) || !entrada.equipo().equals(definiciones.get(entrada.equipo().id())))
                throw new IllegalArgumentException("Botín duplicado o sin definición coincidente");
        }
        return copia;
    }
}
