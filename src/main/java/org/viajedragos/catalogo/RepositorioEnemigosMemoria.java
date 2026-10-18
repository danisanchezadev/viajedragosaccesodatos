package org.viajedragos.catalogo;

import org.viajedragos.modelo.DefinicionEnemigo;
import java.util.List;

public class RepositorioEnemigosMemoria implements RepositorioEnemigos {
    private final List<DefinicionEnemigo> definiciones = List.of(
            new DefinicionEnemigo("lobo", "Lobo", "", 5, 2, 2, 1, 2, 18),
            new DefinicionEnemigo("goblin", "Goblin", "", 3, 1, 2, 2, 3, 12),
            new DefinicionEnemigo("bandido", "Bandido", "", 4, 1, 2, 1, 2, 14),
            new DefinicionEnemigo("esqueleto", "Esqueleto", "", 3, 2, 1, 1, 1, 14));

    @Override
    public List<DefinicionEnemigo> obtenerTodos() {
        return definiciones;
    }
}
