package org.viajedragos.catalogo;

import org.viajedragos.modelo.DefinicionEnemigo;
import java.util.List;

public class RepositorioEnemigosMemoria implements RepositorioEnemigos {
    private final List<DefinicionEnemigo> definiciones = List.of(
            new DefinicionEnemigo("lobo", "Lobo", "", 5, 2, 2, 1, 2, 18, 10, 1),
            new DefinicionEnemigo("goblin", "Goblin", "", 3, 1, 2, 2, 3, 12, 5, 1),
            new DefinicionEnemigo("bandido", "Bandido", "", 4, 1, 2, 1, 2, 14, 7, 1),
            new DefinicionEnemigo("esqueleto", "Esqueleto", "", 3, 2, 1, 1, 1, 14, 6, 1),
            new DefinicionEnemigo("goblin_guerrero", "Goblin guerrero", "", 5, 2, 3, 2, 3, 20, 15, 3),
            new DefinicionEnemigo("esqueleto_veterano", "Esqueleto veterano", "", 6, 3, 2, 2, 2, 24, 20, 4),
            new DefinicionEnemigo("bandido_elite", "Bandido de élite", "", 7, 3, 3, 2, 3, 28, 25, 5));

    @Override
    public List<DefinicionEnemigo> obtenerTodos() {
        return definiciones;
    }
}
