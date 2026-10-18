package org.viajedragos;

/** Extrae el contenido para que las regresiones de negocio no dependan de bordes o saltos de ajuste.
 * Las pruebas visuales comprueban por separado la salida original y las opciones [n]. */
public final class TextoConsola {
    private TextoConsola() { }

    public static String contenido(String salida) {
        StringBuilder texto = new StringBuilder();
        for (String linea : salida.split("\\R")) {
            if (linea.matches("[┌├└]─*[┐┤┘]")) {
                texto.append(System.lineSeparator());
            } else if (linea.startsWith("│") && linea.endsWith("│")) {
                texto.append(linea.substring(1, linea.length() - 1).strip()
                        .replaceAll("\\[(\\d+)] ", "$1. ").replaceAll(" {2,}", " ")).append(' ');
            } else texto.append(linea).append(System.lineSeparator());
        }
        return texto.toString();
    }
}
