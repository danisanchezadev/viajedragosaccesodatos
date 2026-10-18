package org.viajedragos.catalogo;

import org.viajedragos.arma.Atributo;
import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.modelo.TipoPocion;
import java.util.List;
import java.util.Map;

public class RepositorioObjetosMemoria implements RepositorioObjetos {
    private final List<DefinicionArma> armas = List.of(new DefinicionArma(
            DefinicionArma.ID_ESPADA_DESGASTADA, "Espada desgastada", Map.of(Atributo.FUERZA, 1)));
    private final List<DefinicionPocion> pociones = List.of(
            new DefinicionPocion("pocion_pequena", TipoPocion.PEQUENA,
                    TipoPocion.PEQUENA.getNombre(), TipoPocion.PEQUENA.getRecuperacionPS()),
            new DefinicionPocion("pocion_grande", TipoPocion.GRANDE,
                    TipoPocion.GRANDE.getNombre(), TipoPocion.GRANDE.getRecuperacionPS()));

    @Override
    public List<DefinicionArma> obtenerArmas() { return armas; }

    @Override
    public List<DefinicionPocion> obtenerPociones() { return pociones; }
}
