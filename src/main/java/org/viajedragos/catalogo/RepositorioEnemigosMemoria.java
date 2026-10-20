package org.viajedragos.catalogo;

import org.viajedragos.modelo.DefinicionEnemigo;
import java.util.List;

public class RepositorioEnemigosMemoria implements RepositorioEnemigos {
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
            new DefinicionEnemigo("capitan_barracones", "Capitán de barracones", "", 11, 7, 5, 5, 3, 35, 25, 4, 2, true),
            new DefinicionEnemigo("momia", "Momia", "", 18, 9, 4, 6, 2, 32, 18, 5, 3, false),
            new DefinicionEnemigo("arana_cripta", "Araña de cripta", "", 18, 8, 6, 7, 5, 28, 18, 5, 3, false),
            new DefinicionEnemigo("espectro", "Espectro", "", 19, 9, 6, 7, 4, 30, 20, 6, 3, false),
            new DefinicionEnemigo("necrofago", "Necrófago", "", 20, 10, 5, 6, 3, 34, 22, 6, 3, false),
            new DefinicionEnemigo("sacerdote_oscuro", "Sacerdote oscuro", "", 20, 9, 6, 7, 4, 38, 24, 6, 3, false),
            new DefinicionEnemigo("golem_hierro", "Gólem de hierro", "", 26, 14, 5, 7, 2, 48, 26, 8, 4, false),
            new DefinicionEnemigo("salamandra", "Salamandra", "", 25, 12, 8, 9, 5, 36, 24, 8, 4, false),
            new DefinicionEnemigo("herrero_maldito", "Herrero maldito", "", 26, 13, 6, 8, 3, 42, 26, 8, 4, false),
            new DefinicionEnemigo("centinela_igneo", "Centinela ígneo", "", 27, 13, 7, 9, 4, 44, 28, 9, 4, false),
            new DefinicionEnemigo("demonio_forja", "Demonio de forja", "", 28, 14, 8, 8, 4, 46, 30, 9, 4, false),
            new DefinicionEnemigo("caballero_abisal", "Caballero abisal", "", 32, 17, 8, 11, 4, 54, 36, 11, 5, false),
            new DefinicionEnemigo("sombra_ancestral", "Sombra ancestral", "", 31, 16, 10, 13, 6, 48, 36, 11, 5, false),
            new DefinicionEnemigo("bestia_vacio", "Bestia del vacío", "", 33, 17, 9, 12, 5, 58, 40, 12, 5, false),
            new DefinicionEnemigo("hechicero_abisal", "Hechicero abisal", "", 32, 16, 10, 13, 5, 52, 40, 12, 5, false),
            new DefinicionEnemigo("guardian_abismo", "Guardián del abismo", "", 34, 18, 10, 12, 5, 60, 45, 12, 5, false));

    @Override
    public List<DefinicionEnemigo> obtenerTodos() {
        return definiciones;
    }
}
