package org.viajedragos;

/** Extrae el contenido para que las regresiones de negocio no dependan de bordes o saltos de ajuste.
 * Las pruebas visuales comprueban por separado la salida original y las opciones [n]. */
public final class TextoConsola {
    private TextoConsola() { }

    static java.util.Random sinBotinEquipamiento() {
        return new java.util.Random(0) {
            @Override public int nextInt(int limite) {
                org.junit.jupiter.api.Assertions.assertEquals(100, limite, "Solo la comprobación de aparición");
                return 99;
            }
        };
    }

    /** Ejecuta sesiones consecutivas con los mismos generadores de prueba; 0 cierra cada sesión. */
    static void ejecutarSesiones(java.util.Scanner entrada, java.util.function.Consumer<java.util.Scanner> sesion) {
        do { sesion.accept(entrada); } while (entrada.hasNextLine());
    }

    public static void comprobarPaneles(String salida) {
        int ancho = 0;
        for (String original : salida.split("\\R")) {
            String linea = original.replaceAll("\u001b\\[[0-9;]*m", "");
            if (linea.startsWith("┌")) {
                ancho = org.viajedragos.consola.PanelConsola.anchoVisible(linea);
                org.junit.jupiter.api.Assertions.assertTrue(ancho == 50 || ancho == 102, linea);
            }
            if (linea.matches("^[│┌├└].*")) {
                org.junit.jupiter.api.Assertions.assertEquals(ancho,
                        org.viajedragos.consola.PanelConsola.anchoVisible(linea), linea);
            }
        }
    }

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
