package org.viajedragos.catalogo;

import org.viajedragos.modelo.DefinicionArma;
import org.viajedragos.modelo.DefinicionPocion;
import org.viajedragos.modelo.DefinicionEquipo;
import java.util.List;

/** Punto de selección de los orígenes de datos. No contiene reglas del juego. */
public final class ConfiguracionRepositorios {
    private ConfiguracionRepositorios() { }

    public static RepositorioEnemigos enemigos() {
        return new RepositorioEnemigosMemoria();
    }

    public static RepositorioObjetos objetos() {
        return new RepositorioObjetosMemoria();
    }

    // Recibe las definiciones ya cargadas para no releer ni duplicar objetos.
    public static RepositorioTienda tienda(List<DefinicionArma> armas, List<DefinicionPocion> pociones) {
        return new RepositorioTiendaMemoria(armas, pociones);
    }

    public static RepositorioTienda tienda(List<DefinicionArma> armas, List<DefinicionPocion> pociones,
                                           List<DefinicionEquipo> equipos) {
        return new RepositorioTiendaMemoria(armas, pociones, equipos.toArray(DefinicionEquipo[]::new));
    }
}
