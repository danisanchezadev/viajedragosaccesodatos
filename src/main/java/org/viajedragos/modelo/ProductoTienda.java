package org.viajedragos.modelo;

// Solo enlaza una definición existente con su precio; no duplica sus atributos.
public record ProductoTienda(DefinicionArma arma, DefinicionPocion pocion, int precio, DefinicionEquipo equipo) {
    public ProductoTienda {
        if (arma != null && !arma.comoEquipo().equals(equipo)) {
            throw new IllegalArgumentException("Definiciones de arma y equipo incompatibles");
        }
        if ((equipo == null) == (pocion == null)) {
            throw new IllegalArgumentException("El producto debe tener exactamente una definición");
        }
        if (precio <= 0) throw new IllegalArgumentException("El precio debe ser positivo");
    }

    public ProductoTienda(DefinicionArma arma, DefinicionPocion pocion, int precio) {
        this(arma, pocion, precio, arma == null ? null : arma.comoEquipo());
    }

    public ProductoTienda(DefinicionArma arma, int precio) {
        this(arma, null, precio);
    }

    public ProductoTienda(DefinicionPocion pocion, int precio) {
        this(null, pocion, precio);
    }

    public ProductoTienda(DefinicionEquipo equipo, int precio) { this(null, null, precio, equipo); }

    public String id() { return equipo != null ? equipo.id() : pocion.id(); }
    public String nombre() { return equipo != null ? equipo.nombre() : pocion.nombre(); }

    public int precioVenta() { return precio / 2; }

    public boolean corresponde(org.viajedragos.arma.Equipo pieza) {
        return equipo != null && pieza != null
                && (pieza.getIdDefinicion() == null || equipo.id().equals(pieza.getIdDefinicion()))
                && equipo.tipo() == pieza.getTipoEquipo()
                && equipo.nombre().equals(pieza.getNombre())
                && equipo.bonificaciones().equals(pieza.getBonificacionesBase())
                && pieza.getHabilidadesOtorgadas().isEmpty();
    }
}
