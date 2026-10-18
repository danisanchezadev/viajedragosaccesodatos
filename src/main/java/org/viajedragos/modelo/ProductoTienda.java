package org.viajedragos.modelo;

// Solo enlaza una definición existente con su precio; no duplica sus atributos.
public record ProductoTienda(DefinicionArma arma, DefinicionPocion pocion, int precio) {
    public ProductoTienda {
        if ((arma == null) == (pocion == null)) {
            throw new IllegalArgumentException("El producto debe tener exactamente una definición");
        }
        if (precio <= 0) throw new IllegalArgumentException("El precio debe ser positivo");
    }

    public ProductoTienda(DefinicionArma arma, int precio) {
        this(arma, null, precio);
    }

    public ProductoTienda(DefinicionPocion pocion, int precio) {
        this(null, pocion, precio);
    }

    public String id() { return arma != null ? arma.id() : pocion.id(); }
    public String nombre() { return arma != null ? arma.nombre() : pocion.nombre(); }

    public int precioVenta() { return precio / 2; }

    public boolean corresponde(org.viajedragos.arma.Equipo equipo) {
        return arma != null && equipo instanceof org.viajedragos.arma.Arma
                && equipo.getTipoEquipo() == org.viajedragos.arma.TipoEquipo.ARMA
                && arma.nombre().equals(equipo.getNombre())
                && arma.bonificaciones().equals(equipo.getBonificaciones())
                && equipo.getHabilidadesOtorgadas().isEmpty();
    }
}
