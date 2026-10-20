package org.viajedragos.catalogo;

import org.viajedragos.arma.Atributo;
import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.modelo.DefinicionEquipo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.modelo.TipoPocion;
import org.viajedragos.modelo.DefinicionBotinEquipo;
import org.viajedragos.modelo.CalidadEquipo;
import java.util.List;
import java.util.Map;

public class RepositorioObjetosMemoria implements RepositorioObjetos {
    private final List<DefinicionArma> armas = List.of(new DefinicionArma(
            DefinicionArma.ID_ESPADA_DESGASTADA, "Espada desgastada", Map.of(Atributo.FUERZA, 1)),
            new DefinicionArma(DefinicionArma.ID_ESPADA_HIERRO, "Espada de hierro", Map.of(Atributo.FUERZA, 2)),
            new DefinicionArma("espada_acero", "Espada de acero", Map.of(Atributo.FUERZA, 3)),
            new DefinicionArma("espada_forja", "Espada de la forja", Map.of(Atributo.FUERZA, 4)));
    private final List<DefinicionPocion> pociones = List.of(
            new DefinicionPocion("pocion_pequena", TipoPocion.PEQUENA,
                    TipoPocion.PEQUENA.getNombre(), TipoPocion.PEQUENA.getRecuperacionPS()),
            new DefinicionPocion("pocion_grande", TipoPocion.GRANDE,
                    TipoPocion.GRANDE.getNombre(), TipoPocion.GRANDE.getRecuperacionPS()));

    @Override
    public List<DefinicionArma> obtenerArmas() { return armas; }

    @Override
    public List<DefinicionPocion> obtenerPociones() { return pociones; }

    private final List<DefinicionEquipo> equipamiento = List.of(
            new DefinicionEquipo("armadura_cuero", "Armadura de cuero", TipoEquipo.ARMADURA, Map.of(Atributo.RESISTENCIA, 1)),
            new DefinicionEquipo("casco_cuero", "Casco de cuero", TipoEquipo.CASCO, Map.of(Atributo.RESISTENCIA, 1)),
            new DefinicionEquipo("guantes_cuero", "Guantes de cuero", TipoEquipo.GUANTES, Map.of(Atributo.FUERZA, 1)),
            new DefinicionEquipo("botas_ligeras", "Botas ligeras", TipoEquipo.BOTAS, Map.of(Atributo.AGILIDAD, 1)),
            new DefinicionEquipo("anillo_sencillo", "Anillo sencillo", TipoEquipo.ANILLO, Map.of(Atributo.INICIATIVA, 1)),
            new DefinicionEquipo("amuleto_sencillo", "Amuleto sencillo", TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 2)),
            new DefinicionEquipo("armadura_reforzada", "Armadura reforzada", TipoEquipo.ARMADURA, Map.of(Atributo.RESISTENCIA, 3)),
            new DefinicionEquipo("casco_hierro", "Casco de hierro", TipoEquipo.CASCO, Map.of(Atributo.RESISTENCIA, 2)),
            new DefinicionEquipo("guantes_reforzados", "Guantes reforzados", TipoEquipo.GUANTES, Map.of(Atributo.FUERZA, 2)),
            new DefinicionEquipo("botas_marcha", "Botas de marcha", TipoEquipo.BOTAS, Map.of(Atributo.AGILIDAD, 1, Atributo.VELOCIDAD, 1)),
            new DefinicionEquipo("anillo_vigilancia", "Anillo de vigilancia", TipoEquipo.ANILLO, Map.of(Atributo.INICIATIVA, 2)),
            new DefinicionEquipo("amuleto_vigor", "Amuleto de vigor", TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 6)),
            new DefinicionEquipo("armadura_acero", "Armadura de acero", TipoEquipo.ARMADURA, Map.of(Atributo.RESISTENCIA, 4)),
            new DefinicionEquipo("casco_acero", "Casco de acero", TipoEquipo.CASCO, Map.of(Atributo.RESISTENCIA, 3)),
            new DefinicionEquipo("guantes_acero", "Guantes de acero", TipoEquipo.GUANTES, Map.of(Atributo.FUERZA, 3)),
            new DefinicionEquipo("botas_veloces", "Botas veloces", TipoEquipo.BOTAS, Map.of(Atributo.AGILIDAD, 2, Atributo.VELOCIDAD, 2)),
            new DefinicionEquipo("anillo_reflejos", "Anillo de reflejos", TipoEquipo.ANILLO, Map.of(Atributo.INICIATIVA, 3)),
            new DefinicionEquipo("amuleto_vital", "Amuleto vital", TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 10)));

    @Override
    public List<DefinicionEquipo> obtenerEquipamiento() { return equipamiento; }

    @Override
    public List<DefinicionBotinEquipo> obtenerBotinEquipamiento() {
        var lista = new java.util.ArrayList<DefinicionBotinEquipo>();
        lista.add(botin("espada_desgastada", CalidadEquipo.BASICO, 1, 2));
        for (String id : List.of("armadura_cuero", "casco_cuero", "guantes_cuero", "botas_ligeras", "anillo_sencillo", "amuleto_sencillo"))
            lista.add(botin(id, CalidadEquipo.BASICO, 1, 2));
        lista.add(botin("espada_hierro", CalidadEquipo.MEJORADO, 2, 4));
        lista.add(botin("espada_acero", CalidadEquipo.MEJORADO, 3, 4));
        lista.add(botin("armadura_reforzada", CalidadEquipo.MEJORADO, 3, 4));
        lista.add(botin("espada_forja", CalidadEquipo.SUPERIOR, 4, 5));
        for (String id : List.of("casco_hierro", "guantes_reforzados", "botas_marcha", "anillo_vigilancia", "amuleto_vigor"))
            lista.add(botin(id, CalidadEquipo.MEJORADO, 2, 4));
        for (String id : List.of("armadura_acero", "casco_acero", "guantes_acero", "botas_veloces", "anillo_reflejos", "amuleto_vital"))
            lista.add(botin(id, CalidadEquipo.SUPERIOR, 4, 5));
        return List.copyOf(lista);
    }

    private DefinicionBotinEquipo botin(String id, CalidadEquipo calidad, int minima, int maxima) {
        var equipo = armas.stream().filter(a -> a.id().equals(id)).map(DefinicionArma::comoEquipo).findFirst()
                .orElseGet(() -> equipamiento.stream().filter(e -> e.id().equals(id)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No existe el equipo " + id)));
        return new DefinicionBotinEquipo(equipo, calidad, minima, maxima);
    }
}
