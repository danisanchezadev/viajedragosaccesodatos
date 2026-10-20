package org.viajedragos;

import org.viajedragos.modelo.DefinicionEnemigo;
import java.util.List;

/** Datos de fase 3 congelados para comparar el balance antes y después. Solo pruebas. */
final class CatalogoAntesBalance implements org.viajedragos.catalogo.RepositorioEnemigos {
    private final List<DefinicionEnemigo> definiciones = List.of(
            new DefinicionEnemigo("rata_cavernaria", "Rata cavernaria", "", 2, 0, 2, 1, 2, 10, 5, 1, 1, false),
            new DefinicionEnemigo("murcielago", "Murciélago", "", 2, 0, 3, 2, 3, 10, 5, 1, 1, false),
            new DefinicionEnemigo("esqueleto", "Esqueleto", "", 3, 2, 1, 1, 1, 14, 6, 1, 1, false),
            new DefinicionEnemigo("slime", "Slime", "", 2, 1, 1, 0, 1, 12, 5, 1, 1, false),
            new DefinicionEnemigo("goblin", "Goblin", "", 3, 1, 2, 2, 3, 12, 5, 1, 1, false),
            new DefinicionEnemigo("soldado_corrupto", "Soldado corrupto", "", 6, 3, 3, 2, 3, 24, 15, 3, 2, false),
            new DefinicionEnemigo("arquero_espectral", "Arquero espectral", "", 6, 2, 4, 3, 4, 22, 15, 3, 2, false),
            new DefinicionEnemigo("sabueso", "Sabueso", "", 6, 2, 4, 3, 4, 22, 15, 3, 2, false),
            new DefinicionEnemigo("armadura_animada", "Armadura animada", "", 6, 4, 2, 1, 2, 28, 18, 4, 2, false),
            new DefinicionEnemigo("capitan_barracones", "Capitán de barracones", "", 8, 4, 4, 3, 3, 35, 25, 4, 2, true),
            new DefinicionEnemigo("momia", "Momia", "", 9, 4, 3, 2, 2, 32, 25, 5, 3, false),
            new DefinicionEnemigo("arana_cripta", "Araña de cripta", "", 8, 4, 5, 4, 5, 28, 25, 5, 3, false),
            new DefinicionEnemigo("espectro", "Espectro", "", 9, 4, 5, 4, 4, 30, 28, 6, 3, false),
            new DefinicionEnemigo("necrofago", "Necrófago", "", 10, 5, 4, 3, 3, 34, 30, 6, 3, false),
            new DefinicionEnemigo("sacerdote_oscuro", "Sacerdote oscuro", "", 10, 4, 5, 3, 4, 38, 32, 6, 3, false),
            new DefinicionEnemigo("golem_hierro", "Gólem de hierro", "", 12, 7, 3, 2, 2, 48, 45, 8, 4, false),
            new DefinicionEnemigo("salamandra", "Salamandra", "", 11, 5, 6, 4, 5, 36, 40, 8, 4, false),
            new DefinicionEnemigo("herrero_maldito", "Herrero maldito", "", 12, 6, 4, 3, 3, 42, 45, 8, 4, false),
            new DefinicionEnemigo("centinela_igneo", "Centinela ígneo", "", 12, 6, 5, 4, 4, 44, 48, 9, 4, false),
            new DefinicionEnemigo("demonio_forja", "Demonio de forja", "", 13, 6, 6, 4, 4, 46, 50, 9, 4, false),
            new DefinicionEnemigo("caballero_abisal", "Caballero abisal", "", 15, 8, 5, 4, 4, 54, 60, 11, 5, false),
            new DefinicionEnemigo("sombra_ancestral", "Sombra ancestral", "", 14, 7, 7, 6, 6, 48, 60, 11, 5, false),
            new DefinicionEnemigo("bestia_vacio", "Bestia del vacío", "", 16, 7, 6, 5, 5, 58, 70, 12, 5, false),
            new DefinicionEnemigo("hechicero_abisal", "Hechicero abisal", "", 15, 7, 7, 5, 5, 52, 70, 12, 5, false),
            new DefinicionEnemigo("guardian_abismo", "Guardián del abismo", "", 16, 8, 7, 5, 5, 60, 80, 12, 5, false));

    @Override
    public List<DefinicionEnemigo> obtenerTodos() {
        return definiciones;
    }
}
