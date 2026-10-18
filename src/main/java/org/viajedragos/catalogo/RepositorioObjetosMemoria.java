package org.viajedragos.catalogo;

import org.viajedragos.arma.Atributo;
import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.modelo.DefinicionEquipo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.modelo.TipoPocion;
import java.util.List;
import java.util.Map;

public class RepositorioObjetosMemoria implements RepositorioObjetos {
    private final List<DefinicionArma> armas = List.of(new DefinicionArma(
            DefinicionArma.ID_ESPADA_DESGASTADA, "Espada desgastada", Map.of(Atributo.FUERZA, 1)),
            new DefinicionArma(DefinicionArma.ID_ESPADA_HIERRO, "Espada de hierro", Map.of(Atributo.FUERZA, 2)));
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
            new DefinicionEquipo("amuleto_sencillo", "Amuleto sencillo", TipoEquipo.AMULETO, Map.of(Atributo.VIDA, 2)));

    @Override
    public List<DefinicionEquipo> obtenerEquipamiento() { return equipamiento; }
}
