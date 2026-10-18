package org.viajedragos.consola;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Presentación de texto, sin entrada de usuario ni reglas del juego. */
public final class PanelConsola {
    public static final int ANCHO_INTERIOR = 48;
    private static final int ANCHO_TEXTO = ANCHO_INTERIOR - 4;
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;]*m");
    private final PrintStream salida;

    public PanelConsola(PrintStream salida, String titulo) {
        this.salida = Objects.requireNonNull(salida);
        salida.println();
        borde("┌", "┐");
        titulo(titulo);
        separador();
    }

    public void titulo(String texto) {
        for (String parte : dividir(texto, ANCHO_TEXTO)) {
            int libres = ANCHO_INTERIOR - anchoVisible(parte);
            salida.println("│" + " ".repeat(libres / 2) + parte + " ".repeat(libres - libres / 2) + "│");
        }
    }

    public void linea(String texto) {
        for (String parte : dividir(texto, ANCHO_TEXTO)) fila(parte);
    }

    public void espacio() { fila(""); }
    public void separador() { borde("├", "┤"); }
    public void cerrar() { borde("└", "┘"); }
    public void opcion(int numero, String texto) { linea("[" + numero + "] " + texto); }

    public void columnas(String izquierda, String derecha) {
        columnas(new int[]{21, 21}, izquierda, derecha);
    }

    public void columnas(int[] anchos, String... valores) {
        if (anchos.length != valores.length || anchos.length == 0) throw new IllegalArgumentException("Columnas incompatibles");
        int total = 2 * (anchos.length - 1);
        List<List<String>> partes = new ArrayList<>();
        int filas = 0;
        for (int i = 0; i < anchos.length; i++) {
            if (anchos[i] <= 0) throw new IllegalArgumentException("Anchura no positiva");
            total += anchos[i];
            partes.add(dividir(valores[i], anchos[i]));
            filas = Math.max(filas, partes.get(i).size());
        }
        if (total > ANCHO_TEXTO) throw new IllegalArgumentException("Las columnas no caben");
        for (int fila = 0; fila < filas; fila++) {
            StringBuilder texto = new StringBuilder();
            for (int i = 0; i < anchos.length; i++) {
                if (i > 0) texto.append("  ");
                String parte = fila < partes.get(i).size() ? partes.get(i).get(fila) : "";
                texto.append(parte).append(" ".repeat(anchos[i] - anchoVisible(parte)));
            }
            fila(texto.toString());
        }
    }

    public static String barraVida(int vida, int maxima) {
        int llenas = maxima <= 0 ? 0 : (int) Math.round(10.0 * Math.max(0, Math.min(vida, maxima)) / maxima);
        return "[" + "█".repeat(llenas) + "░".repeat(10 - llenas) + "]";
    }

    public static int anchoVisible(String texto) {
        String limpio = ANSI.matcher(texto).replaceAll("");
        return (int) limpio.codePoints().filter(cp -> !esMarca(cp)).count();
    }

    private static boolean esMarca(int cp) {
        int tipo = Character.getType(cp);
        return tipo == Character.NON_SPACING_MARK || tipo == Character.COMBINING_SPACING_MARK
                || tipo == Character.ENCLOSING_MARK;
    }

    private void borde(String izquierda, String derecha) {
        salida.println(izquierda + "─".repeat(ANCHO_INTERIOR) + derecha);
    }

    private void fila(String texto) {
        salida.println("│  " + texto + " ".repeat(ANCHO_TEXTO - anchoVisible(texto)) + "  │");
    }

    private static List<String> dividir(String texto, int ancho) {
        List<String> partes = new ArrayList<>();
        for (String original : Objects.requireNonNull(texto).replace('\t', ' ').split("\\R", -1)) {
            String resto = original;
            while (anchoVisible(resto) > ancho) {
                int fin = 0;
                int visibles = 0;
                int espacio = -1;
                while (fin < resto.length()) {
                    var escape = ANSI.matcher(resto).region(fin, resto.length());
                    if (escape.lookingAt()) { fin = escape.end(); continue; }
                    int cp = resto.codePointAt(fin);
                    if (!esMarca(cp) && visibles == ancho) break;
                    if (cp == ' ') espacio = fin;
                    if (!esMarca(cp)) visibles++;
                    fin += Character.charCount(cp);
                }
                int corte = espacio > 0 ? espacio : fin;
                String parte = resto.substring(0, corte).stripTrailing();
                String color = colorActivo(parte);
                partes.add(parte + (color.isEmpty() ? "" : "\u001B[0m"));
                resto = color + resto.substring(corte).stripLeading();
            }
            partes.add(resto + (colorActivo(resto).isEmpty() ? "" : "\u001B[0m"));
        }
        return partes;
    }

    private static String colorActivo(String texto) {
        String color = "";
        var escapes = ANSI.matcher(texto);
        while (escapes.find()) {
            String codigo = escapes.group();
            color = codigo.equals("\u001B[0m") || codigo.equals("\u001B[m") ? "" : color + codigo;
        }
        return color;
    }
}
