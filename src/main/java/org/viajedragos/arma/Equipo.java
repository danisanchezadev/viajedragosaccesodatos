
package org.viajedragos.arma;

import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Objeto;

import java.util.*;

public abstract class Equipo extends Objeto implements Equipable {

    private final TipoEquipo tipoEquipo;
    private Map<Atributo, Integer> bonificaciones = new EnumMap<>(Atributo.class);
    private Map<Habilidad, Integer> habilidadesOtorgadas = new LinkedHashMap<>();

    public Equipo(String nombre, TipoEquipo tipoEquipo,
                  Map<Atributo, Integer> bonificaciones,
                  Map<Habilidad, Integer> habilidadesOtorgadas) {

        super(nombre);
        this.tipoEquipo = tipoEquipo;
        this.bonificaciones = bonificaciones;
        this.habilidadesOtorgadas = habilidadesOtorgadas;
    }

    @Override
    public void aplicarEfectos(Combatiente combatiente) {

        //bonificaciones.forEach(combatiente::modificarAtributoEquipo);

        habilidadesOtorgadas.forEach(combatiente::agregarHabilidadEquipo);
    }

    @Override
    public void retirarEfectos(Combatiente combatiente) {

    }

    public TipoEquipo getTipoEquipo() {
        return tipoEquipo;
    }

    public Map<Atributo, Integer> getBonificaciones() {
        return bonificaciones;
    }

    public Map<Habilidad, Integer> getHabilidadesOtorgadas() {
        return habilidadesOtorgadas;
    }

    public void compararCon(Equipo otroEquipo) {

        Set<Atributo> atributos = new HashSet<>(bonificaciones.keySet());

        atributos.addAll(otroEquipo.bonificaciones.keySet());

        System.out.printf("%-15s %-12s %-12s %-12s%n",
                "ATRIBUTO", "ACTUAL", "NUEVO", "DIFERENCIA");

        System.out.println("----------------------------------------------------");

        for (Atributo a : atributos) {

            int atributoViejo = bonificaciones.getOrDefault(a, 0);
            int atributoNuevo = otroEquipo.bonificaciones.getOrDefault(a, 0);

            int diferencia = atributoNuevo - atributoViejo;

            String textoViejo = atributoViejo == 0
                    ? "--"
                    : String.valueOf(atributoViejo);

            String textoNuevo = atributoNuevo == 0
                    ? "--"
                    : String.valueOf(atributoNuevo);

            String signo = diferencia > 0 ? "+" : "";

            System.out.printf("%-15s %-12s %-12s %-12s%n",
                    a,
                    textoViejo,
                    textoNuevo,
                    signo + diferencia);
        }
    }
}
