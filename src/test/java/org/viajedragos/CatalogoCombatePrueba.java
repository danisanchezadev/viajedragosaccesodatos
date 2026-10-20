package org.viajedragos;

import org.viajedragos.modelo.DefinicionEnemigo;
import java.util.List;

/** Datos fijos del equilibrio anterior para conservar las regresiones de fórmulas y dados. */
final class CatalogoCombatePrueba implements org.viajedragos.catalogo.RepositorioEnemigos {
    public List<DefinicionEnemigo> obtenerTodos() {
        return List.of(
                new DefinicionEnemigo("lobo", "Lobo", "", 5, 2, 2, 1, 2, 18, 10),
                new DefinicionEnemigo("goblin", "Goblin", "", 3, 1, 2, 2, 3, 12, 5),
                new DefinicionEnemigo("bandido", "Bandido", "", 4, 1, 2, 1, 2, 14, 7),
                new DefinicionEnemigo("esqueleto", "Esqueleto", "", 3, 2, 1, 1, 1, 14, 6));
    }
}
