
package org.viajedragos.arma;

import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Objeto;

import java.util.*;

public abstract class Equipo extends Objeto implements Equipable {

    private final TipoEquipo tipoEquipo;
    private final Map<Atributo, Integer> bonificaciones;
    private final Map<Habilidad, Integer> habilidadesOtorgadas;

    public Equipo(String nombre, TipoEquipo tipoEquipo,
                  Map<Atributo, Integer> bonificaciones,
                  Map<Habilidad, Integer> habilidadesOtorgadas) {

        super(nombre);
        this.tipoEquipo = Objects.requireNonNull(tipoEquipo, "El tipo de equipo no puede ser null");
        Map<Atributo, Integer> copiaBonificaciones = new EnumMap<>(Atributo.class);
        bonificaciones.forEach((atributo, cantidad) -> {
            Objects.requireNonNull(atributo, "El atributo no puede ser null");
            Objects.requireNonNull(cantidad, "La bonificación no puede ser null");
            if (atributo == Atributo.VIDA) {
                throw new IllegalArgumentException("Las bonificaciones de vida necesitan una regla específica");
            }
            copiaBonificaciones.put(atributo, cantidad);
        });
        Map<Habilidad, Integer> copiaHabilidades = new LinkedHashMap<>();
        habilidadesOtorgadas.forEach((habilidad, niveles) -> {
            Objects.requireNonNull(habilidad, "La habilidad no puede ser null");
            Objects.requireNonNull(niveles, "Los niveles no pueden ser null");
            if (niveles <= 0) {
                throw new IllegalArgumentException("Los niveles otorgados deben ser positivos");
            }
            copiaHabilidades.put(habilidad, niveles);
        });
        this.bonificaciones = Collections.unmodifiableMap(copiaBonificaciones);
        this.habilidadesOtorgadas = Collections.unmodifiableMap(copiaHabilidades);
    }

    @Override
    // Operación de efectos: cada aplicación debe emparejarse con una retirada.
    // Para gestionar ranuras se utilizan Combatiente.equipar/desequipar.
    public void aplicarEfectos(Combatiente combatiente) {

        Objects.requireNonNull(combatiente, "El combatiente no puede ser null");
        bonificaciones.forEach(combatiente::modificarAtributo);
        habilidadesOtorgadas.forEach(combatiente::agregarHabilidadEquipo);
    }

    @Override
    public void retirarEfectos(Combatiente combatiente) {
        Objects.requireNonNull(combatiente, "El combatiente no puede ser null");
        // Validar todas las habilidades antes de retirar ningún efecto.
        habilidadesOtorgadas.forEach((habilidad, niveles) -> {
            var estado = combatiente.getHabilidades().get(habilidad);
            if (estado == null || estado.getVecesEquipada() <= 0 || estado.getNivel() < niveles) {
                throw new IllegalStateException("No se pueden retirar las habilidades de este equipo");
            }
            if ((estado.isAprendida() || estado.getVecesEquipada() > 1) && estado.getNivel() == niveles) {
                throw new IllegalStateException("Una habilidad que permanece activa debe conservar nivel positivo");
            }
        });
        bonificaciones.forEach((atributo, cantidad) -> combatiente.modificarAtributo(atributo, -cantidad));
        habilidadesOtorgadas.forEach(combatiente::quitarHabilidadEquipo);
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
