package org.viajedragos.modelo;

/** Ubicaciones de esta fase; el catálogo de enemigos se asocia por número de planta. */
public enum Planta {
    VESTIBULO("vestibulo", 1, "El vestíbulo", "La entrada de la mazmorra.", 5),
    BARRACONES("barracones", 2, "Los barracones", "Antiguos soldados custodian el descenso.", 8),
    CRIPTAS("criptas", 3, "Las criptas", "Cinco especies acechan entre las tumbas.", 15),
    FORJA("forja_olvidada", 4, "La forja olvidada", "El sello permanece entre las brasas.", 24),
    ABISMO("abismo", 5, "El abismo", "La oscuridad ocupa la última planta conocida.", 35);

    private final String id;
    private final int numero;
    private final String nombre;
    private final String descripcion;
    private final int oroVictoria;

    Planta(String id, int numero, String nombre, String descripcion, int oroVictoria) {
        this.id = id; this.numero = numero; this.nombre = nombre; this.descripcion = descripcion;
        this.oroVictoria = oroVictoria;
    }

    public String id() { return id; }
    public int numero() { return numero; }
    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public int oroVictoria() { return oroVictoria; }

    public static Planta porNumero(int numero) {
        for (Planta planta : values()) if (planta.numero == numero) return planta;
        throw new IllegalArgumentException("Planta fuera de rango: " + numero);
    }
}
