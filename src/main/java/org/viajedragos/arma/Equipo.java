
package org.viajedragos.arma;

import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Objeto;

import java.util.*;
import java.io.PrintStream;
import org.viajedragos.consola.PanelConsola;

public class Equipo extends Objeto implements Equipable {

    private static final Random AZAR_MEJORAS = new Random();

    private final String idDefinicion;

    private final TipoEquipo tipoEquipo;
    private final Map<Atributo, Integer> bonificaciones;
    private final Map<Atributo, Integer> bonificacionesBase;
    private final Map<Atributo, Integer> mejoras = new EnumMap<>(Atributo.class);
    private int mejorasDisponibles;
    private final Map<Habilidad, Integer> habilidadesOtorgadas;

    public Equipo(String nombre, TipoEquipo tipoEquipo,
                  Map<Atributo, Integer> bonificaciones,
                  Map<Habilidad, Integer> habilidadesOtorgadas) {
        this(null, nombre, tipoEquipo, bonificaciones, habilidadesOtorgadas);
    }

    public Equipo(String idDefinicion, String nombre, TipoEquipo tipoEquipo,
                  Map<Atributo, Integer> bonificaciones, Map<Habilidad, Integer> habilidadesOtorgadas) {
        this(idDefinicion, nombre, tipoEquipo, bonificaciones, habilidadesOtorgadas, AZAR_MEJORAS.nextInt(4));
    }

    public Equipo(String idDefinicion, String nombre, TipoEquipo tipoEquipo,
                  Map<Atributo, Integer> bonificaciones, Map<Habilidad, Integer> habilidadesOtorgadas,
                  int mejorasDisponibles) {

        super(nombre);
        if (mejorasDisponibles < 0 || mejorasDisponibles > 3) throw new IllegalArgumentException("Mejoras entre 0 y 3");
        this.mejorasDisponibles = mejorasDisponibles;
        if (idDefinicion != null && idDefinicion.isBlank()) throw new IllegalArgumentException("ID vacío");
        this.idDefinicion = idDefinicion;
        this.tipoEquipo = Objects.requireNonNull(tipoEquipo, "El tipo de equipo no puede ser null");
        Map<Atributo, Integer> copiaBonificaciones = new EnumMap<>(Atributo.class);
        bonificaciones.forEach((atributo, cantidad) -> {
            Objects.requireNonNull(atributo, "El atributo no puede ser null");
            Objects.requireNonNull(cantidad, "La bonificación no puede ser null");
            if (cantidad == Integer.MIN_VALUE) throw new IllegalArgumentException("Bonificación no reversible");
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
        this.bonificaciones = copiaBonificaciones;
        this.bonificacionesBase = Collections.unmodifiableMap(new EnumMap<>(copiaBonificaciones));
        this.habilidadesOtorgadas = Collections.unmodifiableMap(copiaHabilidades);
    }

    @Override
    // Operación de efectos: cada aplicación debe emparejarse con una retirada.
    // Para gestionar ranuras se utilizan Combatiente.equipar/desequipar.
    public void aplicarEfectos(Combatiente combatiente) {

        Objects.requireNonNull(combatiente, "El combatiente no puede ser null");
        bonificaciones.forEach((atributo, cantidad) -> aplicarBonificacion(combatiente, atributo, cantidad));
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
        bonificaciones.forEach((atributo, cantidad) -> aplicarBonificacion(combatiente, atributo, -cantidad));
        habilidadesOtorgadas.forEach(combatiente::quitarHabilidadEquipo);
    }

    public TipoEquipo getTipoEquipo() {
        return tipoEquipo;
    }

    public String getIdDefinicion() { return idDefinicion; }

    private void aplicarBonificacion(Combatiente combatiente, Atributo atributo, int cantidad) {
        if (atributo == Atributo.VIDA) combatiente.modificarVidaMaximaEquipo(cantidad);
        else combatiente.modificarAtributo(atributo, cantidad);
    }

    public Map<Atributo, Integer> getBonificaciones() {
        return Collections.unmodifiableMap(bonificaciones);
    }

    public Map<Atributo, Integer> getBonificacionesBase() { return bonificacionesBase; }
    public Map<Atributo, Integer> getBonificacionesAdicionales() { return Collections.unmodifiableMap(mejoras); }
    public int getMejorasDisponibles() { return mejorasDisponibles; }
    public int getMejorasConsumidas() { return mejoras.values().stream().mapToInt(Integer::intValue).sum(); }

    // Primitiva de mejora: Jugador coordina el pergamino y Combatiente protege el estado activo.
    public final boolean aplicarMejora(Atributo atributo, Combatiente portador) {
        if (atributo == null || portador == null || mejorasDisponibles == 0) return false;
        int bono = Math.incrementExact(bonificaciones.getOrDefault(atributo, 0));
        int adicional = Math.incrementExact(mejoras.getOrDefault(atributo, 0));
        boolean equipada = portador.getEquipoEquipado().get(tipoEquipo) == this;
        if (!equipada && (!(portador instanceof org.viajedragos.modelo.Jugador jugador)
                || jugador.getInventarioEquipo().stream().noneMatch(pieza -> pieza == this))) return false;
        if (equipada) {
            int modificador = Math.incrementExact(portador.getModificadoresEquipo().getOrDefault(atributo, 0));
            if (atributo != Atributo.VIDA) {
                Math.addExact(portador.getAtributoBase(atributo), modificador);
            }
            aplicarBonificacion(portador, atributo, 1);
            if (portador.getModificadoresEquipo().getOrDefault(atributo, 0) != modificador) {
                throw new IllegalStateException("No se aplicó el incremento al portador");
            }
        }
        // Tras actualizar al portador no hay llamadas externas ni operaciones que puedan rechazar la mejora.
        bonificaciones.put(atributo, bono);
        mejoras.put(atributo, adicional);
        mejorasDisponibles--;
        return true;
    }

    public Map<Habilidad, Integer> getHabilidadesOtorgadas() {
        return habilidadesOtorgadas;
    }

    public void compararCon(Equipo otroEquipo) {
        compararCon(otroEquipo, System.out, false);
    }

    public void compararCon(Equipo otroEquipo, PrintStream salida, boolean coloresAnsi) {
        Objects.requireNonNull(otroEquipo, "El otro equipo no puede ser null");
        Objects.requireNonNull(salida, "La salida no puede ser null");

        Set<Atributo> atributos = EnumSet.noneOf(Atributo.class);
        atributos.addAll(bonificaciones.keySet());

        atributos.addAll(otroEquipo.bonificaciones.keySet());

        PanelConsola panel = new PanelConsola(salida, "COMPARACIÓN DE EQUIPAMIENTO");
        int[] anchos = {11, 8, 8, 11};
        panel.columnas(anchos, "ATRIBUTO", "ACTUAL", "NUEVO", "DIFERENCIA");
        panel.separador();

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
            String textoDiferencia = signo + diferencia;
            String columnaDiferencia = textoDiferencia;
            if (coloresAnsi && diferencia != 0) {
                columnaDiferencia = (diferencia > 0 ? "\u001B[32m" : "\u001B[31m")
                        + textoDiferencia + "\u001B[0m";
            }

            panel.columnas(anchos, a == Atributo.VIDA ? "VIDA MÁX." : a.name(), textoViejo, textoNuevo, columnaDiferencia);
        }
        panel.cerrar();
    }
}
