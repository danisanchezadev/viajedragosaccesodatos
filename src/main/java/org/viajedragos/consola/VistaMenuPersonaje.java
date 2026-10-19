package org.viajedragos.consola;

import org.viajedragos.arma.*;
import org.viajedragos.modelo.*;
import java.io.PrintStream;
import java.util.*;

/** Dibuja datos actuales; no lee entradas ni modifica personajes u objetos. */
public final class VistaMenuPersonaje {
    public static final int TAMANO_PAGINA = 8;
    private VistaMenuPersonaje() { }

    public record Elemento(String nombre, int cantidad) {
        public String texto() { return nombre + " x" + cantidad; }
    }

    public static List<Elemento> consumibles(Jugador j) {
        var lista = new ArrayList<Elemento>();
        for (TipoPocion tipo : TipoPocion.values()) {
            int cantidad = j.getCantidadPociones(tipo);
            if (cantidad > 0) lista.add(new Elemento(j.getDefinicionPocion(tipo).nombre(), cantidad));
        }
        return List.copyOf(lista);
    }

    public static List<Elemento> pergaminos(Jugador j) {
        var lista = new ArrayList<Elemento>();
        j.getPergaminos().forEach((a, cantidad) -> {
            if (cantidad <= 0) return;
            String nombre = new Pergamino(a).nombre().substring("Pergamino de ".length());
            lista.add(new Elemento(nombre.substring(0, 1).toUpperCase(Locale.ROOT) + nombre.substring(1), cantidad));
        });
        j.getPergaminosHabilidad().forEach((h, cantidad) -> {
            if (cantidad > 0) lista.add(new Elemento("Habilidad: " + h.getNombre(), cantidad));
        });
        if (j.getCantidadPergaminosMaestria() > 0) lista.add(new Elemento("Maestría", j.getCantidadPergaminosMaestria()));
        return List.copyOf(lista);
    }

    public static List<Elemento> equipamiento(Jugador j) {
        var lista = new ArrayList<Elemento>();
        j.getEquipoEquipado().values().forEach(p -> lista.add(new Elemento("[E] " + p.getNombre(), 1)));
        j.getInventarioEquipo().forEach(p -> lista.add(new Elemento(p.getNombre(), 1)));
        return List.copyOf(lista);
    }

    public static int anchoMenu() {
        return Integer.getInteger("viajedragos.columnas", 102) < 102 ? 48 : 100;
    }

    public static void general(Jugador j, PrintStream salida, TiempoAventura tiempo, boolean ansi) {
        int ancho = anchoMenu();
        var panel = new PanelConsola(salida, "VIAJE A DRAGOS — MENÚ DE PERSONAJE", ancho, ansi);
        if (ancho == 100) panel.columnas(new int[]{30, 16, 22, 22}, j.getNombre(), "NIVEL " + j.getNivel(),
                "ORO " + j.getOro(), "TIEMPO " + tiempo.transcurrido());
        else {
            panel.linea(j.getNombre());
            panel.columnas("NIVEL " + j.getNivel(), "ORO " + j.getOro());
            panel.linea("TIEMPO " + tiempo.transcurrido());
        }
        panel.columnas("VIDA " + j.getVida() + "/" + j.getVidaMaxima() + " "
                        + colorEstado(barraProgresoEstado(j.getVida(), j.getVidaMaxima()), "32", ansi),
                "EXP " + j.getExperiencia() + "/" + j.getExperienciaNecesaria() + " "
                        + colorEstado(barraProgresoEstado(j.getExperiencia(), j.getExperienciaNecesaria()), "36", ansi));
        if (j.getPuntosPendientes() > 0) panel.linea("Puntos pendientes: " + j.getPuntosPendientes());
        panel.separador(); panel.titulo("ATRIBUTOS");
        String[] atributos = {"FUERZA " + j.getFuerza(), "RESISTENCIA " + j.getResistencia(), "INICIATIVA " + j.getIniciativa(),
                "AGILIDAD " + j.getAgilidad(), "VELOCIDAD " + j.getVelocidad(), "VIDA MÁXIMA " + j.getVidaMaxima()};
        if (ancho == 100) {
            panel.columnas(new int[]{22, 22, 22, 24}, atributos[0], atributos[1], atributos[2], atributos[3]);
            panel.columnas(atributos[4], atributos[5]);
        } else for (int i = 0; i < atributos.length; i += 2) panel.columnas(atributos[i], atributos[i + 1]);
        resumenHabilidades(panel, j, ancho);
        panel.separador();
        var listas = List.of(consumibles(j), pergaminos(j), equipamiento(j));
        if (ancho == 100) resumenHorizontal(panel, listas);
        else {
            String[] titulos = {"CONSUMIBLES", "PERGAMINOS", "EQUIPAMIENTO"};
            for (int col = 0; col < 3; col++) {
                if (col > 0) panel.separador();
                panel.titulo(titulos[col]);
                var lista = listas.get(col);
                if (lista.isEmpty()) panel.linea("Sin objetos");
                lista.stream().limit(5).forEach(e -> panel.linea(abreviar(e, 44)));
                if (lista.size() > 5) panel.linea(restantes(lista));
            }
        }
        panel.separador(); panel.titulo("ACCIONES");
        panel.opciones(new int[]{1, 2, 3, 4}, "Estadísticas", "Inventario", "Usar objeto", "Equipar");
        panel.opciones(new int[]{5, 6, 0}, "Desequipar", "Habilidades", "Volver a exploración"); panel.cerrar();
    }

    private static void resumenHabilidades(PanelConsola panel, Jugador jugador, int ancho) {
        panel.separador(); panel.titulo("HABILIDADES");
        var habilidades = jugador.getHabilidades().entrySet().stream()
                .filter(e -> e.getValue().isActiva())
                .sorted(Comparator.comparing(e -> e.getKey().getNombre()))
                .map(e -> {
                    var estado = e.getValue();
                    String origen = estado.isAprendida() ? estado.getNivelEquipo() > 0 ? "A+E" : "A" : "E";
                    return "[" + origen + "] " + e.getKey().getNombre() + " Nv." + estado.getNivel();
                }).toList();
        if (habilidades.isEmpty()) { panel.linea("Sin habilidades"); return; }
        int limite = Math.min(6, habilidades.size());
        for (int inicio = 0; inicio < limite;) {
            int columnas = ancho == 100 ? 3 : 2;
            // Reducir columnas antes de ajustar líneas, sin abreviar nombres ni niveles.
            while (columnas > 1) {
                int celda = (ancho - 4 - 2 * (columnas - 1)) / columnas;
                boolean caben = true;
                for (int i = inicio; i < Math.min(limite, inicio + columnas); i++) {
                    if (PanelConsola.anchoVisible(habilidades.get(i)) > celda) caben = false;
                }
                if (caben) break;
                columnas--;
            }
            int disponible = ancho - 4 - 2 * (columnas - 1);
            int[] anchos = new int[columnas];
            String[] textos = new String[columnas];
            for (int col = 0; col < columnas; col++) {
                anchos[col] = disponible / columnas + (col == columnas - 1 ? disponible % columnas : 0);
                textos[col] = inicio + col < limite ? habilidades.get(inicio + col) : "";
            }
            panel.columnas(anchos, textos);
            inicio += columnas;
        }
        if (habilidades.size() > limite) panel.linea("+" + (habilidades.size() - limite) + " habilidades más");
    }

    private static void resumenHorizontal(PanelConsola panel, List<List<Elemento>> listas) {
        int[] columnas = {30, 30, 32};
        panel.columnas(columnas, "CONSUMIBLES", "PERGAMINOS", "EQUIPAMIENTO");
        int filas = listas.stream().mapToInt(l -> Math.max(1, Math.min(5, l.size()))).max().orElse(1);
        for (int fila = 0; fila < filas; fila++) {
            String[] celdas = new String[3];
            for (int col = 0; col < 3; col++) {
                var elementos = listas.get(col);
                celdas[col] = elementos.isEmpty() && fila == 0 ? "Sin objetos"
                        : fila < elementos.size() ? abreviar(elementos.get(fila), columnas[col]) : "";
            }
            panel.columnas(columnas, celdas);
        }
        if (listas.stream().anyMatch(l -> l.size() > 5)) panel.columnas(columnas,
                restantes(listas.get(0)), restantes(listas.get(1)), restantes(listas.get(2)));
    }

    private static String restantes(List<Elemento> lista) {
        return lista.size() > 5 ? "+" + (lista.size() - 5) + " objetos más" : "";
    }

    // La abreviación es solo visual. Las acciones trabajan con las referencias del modelo.
    private static String abreviar(Elemento elemento, int ancho) {
        String cantidad = "x" + elemento.cantidad();
        String nombre = elemento.nombre().replaceAll("\u001b\\[[0-9;]*m", "");
        int espacio = ancho - cantidad.length() - 1;
        if (PanelConsola.anchoVisible(nombre) > espacio) {
            StringBuilder corto = new StringBuilder();
            for (int cp : nombre.codePoints().toArray()) {
                String siguiente = corto.toString() + new String(Character.toChars(cp));
                if (PanelConsola.anchoVisible(siguiente) > espacio - 1) break;
                corto.appendCodePoint(cp);
            }
            nombre = corto + "…";
        }
        return PanelConsola.alinearExtremos(nombre, cantidad, ancho);
    }

    public static void estadisticas(Jugador j, PrintStream salida, boolean ansi) {
        var panel = new PanelConsola(salida, "ESTADÍSTICAS", anchoMenu(), ansi);
        mostrarIdentidadEstado(panel, j, ansi);
        panel.separador(); panel.titulo("ATRIBUTOS: BASE + EQUIPO = EFECTIVO");
        if (j.getHabilidades().values().stream().anyMatch(e -> e.isEfectoTemporalActivo() || e.getAcumulacionesTemporales() > 0)) {
            panel.linea("Efectos temporales activos: la base actual incluye sus modificaciones.");
        }
        Atributo[][] parejas = {{Atributo.FUERZA, Atributo.AGILIDAD}, {Atributo.RESISTENCIA, Atributo.VELOCIDAD}, {Atributo.INICIATIVA, Atributo.VIDA}};
        for (Atributo[] pareja : parejas) {
            String[] titulos = new String[2], detalles = new String[2];
            for (int col = 0; col < 2; col++) {
                Atributo a = pareja[col];
                int total = a == Atributo.VIDA ? j.getVidaMaxima() : j.getAtributo(a);
                int base = a == Atributo.VIDA ? j.getVidaMaximaBase() : j.getAtributoBase(a);
                titulos[col] = (a == Atributo.VIDA ? "VIDA MÁXIMA" : a.name()) + " | Efectivo: " + total;
                detalles[col] = "Base: " + base + " | Equipo: " + formatearBonoEstado(j.getModificadoresEquipo().getOrDefault(a, 0), ansi);
            }
            panel.columnas(titulos[0], titulos[1]); panel.columnas(detalles[0], detalles[1]);
        }
        panel.opcion(0, "Volver al menú de personaje"); panel.cerrar();
    }

    public static void habilidades(Jugador j, PrintStream salida, boolean ansi) {
        var panel = new PanelConsola(salida, "HABILIDADES", anchoMenu(), ansi);
        mostrarHabilidadesEstado(panel, j, ansi);
        panel.opcion(0, "Volver al menú de personaje"); panel.cerrar();
    }

    public static int paginasInventario(Jugador j) {
        int cantidad = Math.max(consumibles(j).size(), Math.max(pergaminos(j).size(), equipamiento(j).size()));
        return Math.max(1, (cantidad - 1) / TAMANO_PAGINA + 1);
    }

    public static void inventario(Jugador j, PrintStream salida, int pagina) {
        var panel = new PanelConsola(salida, "INVENTARIO");
        panel.titulo("CONSUMIBLES"); categoriaCompleta(panel, consumibles(j), pagina, "Sin consumibles", false);
        panel.separador(); panel.titulo("PERGAMINOS"); categoriaCompleta(panel, pergaminos(j), pagina, "Sin pergaminos", false);
        panel.separador(); panel.titulo("EQUIPAMIENTO"); categoriaCompleta(panel, equipamiento(j), pagina, "Sin equipamiento", true);
        panel.separador(); panel.titulo("ACCIONES");
        panel.opcion(1, "Comparar equipo", 2, "Equipar objeto");
        panel.opcion(3, "Desequipar objeto", 4, "Perg. atributos");
        panel.opcion(5, "Perg. habilidad", 6, "Perg. maestría");
        panel.opcion(7, "Usar consumible", 8, "Inspeccionar");
        int paginas = paginasInventario(j);
        if (paginas > 1) {
            panel.linea("Página " + (pagina + 1) + "/" + paginas);
            if (pagina + 1 < paginas) panel.opcion(9, "Página siguiente");
            if (pagina > 0) panel.opcion(10, "Página anterior");
        }
        panel.opcion(0, "Volver"); panel.cerrar();
    }

    private static void categoriaCompleta(PanelConsola panel, List<Elemento> lista, int pagina, String vacio, boolean equipo) {
        if (lista.isEmpty()) { panel.linea(vacio); return; }
        int inicio = pagina * TAMANO_PAGINA, fin = Math.min(lista.size(), inicio + TAMANO_PAGINA);
        if (inicio >= lista.size()) { panel.linea("Sin más objetos en esta página"); return; }
        for (int i = inicio; i < fin; i++) {
            String texto = equipo ? lista.get(i).nombre() : lista.get(i).texto();
            if (i + 1 < fin) {
                String siguiente = equipo ? lista.get(i + 1).nombre() : lista.get(i + 1).texto();
                if (PanelConsola.anchoVisible(texto) <= 21 && PanelConsola.anchoVisible(siguiente) <= 21) {
                    panel.columnas(texto, siguiente); i++; continue;
                }
            }
            panel.linea(texto);
        }
    }

    public static void estadoCompacto(Jugador jugador, PrintStream salida, boolean ansi) {
        PanelConsola panel = new PanelConsola(salida, "ESTADO DEL PERSONAJE");
        mostrarIdentidadEstado(panel, jugador, ansi);
        mostrarAtributosEstado(panel, jugador);
        mostrarHabilidadesEstado(panel, jugador, ansi);
        panel.separador(); panel.opcion(0, "Volver a exploración"); panel.cerrar();
    }

    private static void mostrarIdentidadEstado(PanelConsola panel, Jugador jugador, boolean coloresAnsi) {
        panel.linea("Nombre: " + jugador.getNombre());
        if (jugador.getDescripcion() != null && !jugador.getDescripcion().isBlank()) {
            panel.linea("Descripción: " + jugador.getDescripcion());
        }
        panel.columnas("Nivel: " + jugador.getNivel(), "Oro: " + jugador.getOro() + " monedas");
        panel.espacio();
        panel.columnas("VIDA " + jugador.getVida() + "/" + jugador.getVidaMaxima() + " PS",
                "EXPERIENCIA " + jugador.getExperiencia() + "/" + jugador.getExperienciaNecesaria());
        panel.columnas(colorEstado(barraProgresoEstado(jugador.getVida(), jugador.getVidaMaxima()), "32", coloresAnsi),
                colorEstado(barraProgresoEstado(jugador.getExperiencia(), jugador.getExperienciaNecesaria()), "36", coloresAnsi));
        if (jugador.getPuntosPendientes() > 0) panel.linea("Puntos pendientes: " + jugador.getPuntosPendientes());
    }

    // La división se hace antes del escalado en double: nunca multiplica enteros grandes.
    public static String barraProgresoEstado(long actual, long maximo) {
        int llenos = maximo <= 0 ? 0 : (int) Math.round((double) Math.max(0, Math.min(actual, maximo)) / maximo * 10);
        return "[" + "█".repeat(llenos) + "░".repeat(10 - llenos) + "]";
    }

    public static String barraAtributoEstado(int valor) {
        int llenos = Math.max(0, Math.min(valor, 16));
        return "[" + "█".repeat(llenos) + "░".repeat(16 - llenos) + "]";
    }

    private static void mostrarAtributosEstado(PanelConsola panel, Jugador jugador) {
        panel.separador();
        panel.titulo("ATRIBUTOS");
        Atributo[][] parejas = {{Atributo.FUERZA, Atributo.AGILIDAD},
                {Atributo.RESISTENCIA, Atributo.VELOCIDAD}, {Atributo.INICIATIVA, Atributo.VIDA}};
        for (Atributo[] pareja : parejas) {
            int izquierda = jugador.getAtributo(pareja[0]);
            int derecha = pareja[1] == Atributo.VIDA ? jugador.getVidaMaxima() : jugador.getAtributo(pareja[1]);
            // 22 + 2 de separación + 20 = las 44 columnas de texto del panel.
            // Con cifras muy grandes se abrevia solo la etiqueta de vida, nunca el valor.
            String nombreDerecha = pareja[1] == Atributo.VIDA
                    ? ("VIDA MÁXIMA " + derecha).length() <= 20 ? "VIDA MÁXIMA" : "VIDA MÁX."
                    : pareja[1].name();
            panel.columnas(new int[]{22, 20}, pareja[0].name() + " " + izquierda, nombreDerecha + " " + derecha);
            panel.columnas(new int[]{22, 20}, barraAtributoEstado(izquierda), barraAtributoEstado(derecha));
        }
    }

    private static String formatearBonoEstado(int bono, boolean coloresAnsi) {
        String numero = String.format(java.util.Locale.ROOT, "%+d", bono);
        return bono == 0 ? numero : colorEstado(numero, bono > 0 ? "32" : "31", coloresAnsi);
    }

    private static String colorEstado(String texto, String codigo, boolean coloresAnsi) {
        return coloresAnsi ? "\u001b[" + codigo + "m" + texto + "\u001b[0m" : texto;
    }

    private static void mostrarHabilidadesEstado(PanelConsola panel, Jugador jugador, boolean coloresAnsi) {
        var habilidades = jugador.getHabilidades().entrySet().stream()
                .filter(e -> e.getValue().isActiva())
                .sorted(java.util.Comparator.comparing(e -> e.getKey().getNombre())).toList();
        panel.separador();
        panel.titulo("HABILIDADES APRENDIDAS");
        boolean aprendidas = false;
        for (var entradaHabilidad : habilidades) {
            var estado = entradaHabilidad.getValue();
            if (!estado.isAprendida()) continue;
            aprendidas = true;
            panel.linea(entradaHabilidad.getKey().getNombre());
            panel.linea("Permanente: " + estado.getNivelPermanente() + " | Equipo: "
                    + formatearBonoEstado(estado.getNivelEquipo(), coloresAnsi) + " | Total: " + estado.getNivel());
        }
        if (!aprendidas) panel.linea("No tienes habilidades aprendidas.");
        panel.separador();
        panel.titulo("HABILIDADES OTORGADAS POR EQUIPO");
        boolean otorgadas = false;
        for (var entradaHabilidad : habilidades) {
            var estado = entradaHabilidad.getValue();
            if (estado.isAprendida() || estado.getVecesEquipada() == 0) continue;
            otorgadas = true;
            var habilidad = entradaHabilidad.getKey();
            panel.linea(habilidad.getNombre() + " | Nivel de equipo: " + estado.getNivelEquipo());
            jugador.getEquipoEquipado().values().forEach(pieza -> {
                Integer niveles = pieza.getHabilidadesOtorgadas().get(habilidad);
                if (niveles != null) panel.linea("Fuente: " + pieza.getNombre() + " ("
                        + formatearBonoEstado(niveles, coloresAnsi) + ")");
            });
        }
        if (!otorgadas) panel.linea("No tienes habilidades exclusivas de equipo.");
    }

}
