package org.viajedragos.modelo;

import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.Equipo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;

import java.util.EnumMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class Combatiente {

    private String nombre;
    private String descripcion;

    private final Map<Atributo, Integer> atributosBase =
            new EnumMap<>(Atributo.class);

    private final Map<Atributo, Integer> modificadoresEquipo =
            new EnumMap<>(Atributo.class);

    private int vida;
    public int vidaMaxima;

    private Map<Habilidad, EstadoHabilidad> habilidades = new HashMap<>();
    private final Map<TipoEquipo, Equipo> equipoEquipado = new EnumMap<>(TipoEquipo.class);

    public Combatiente() {
        inicializarAtributos();
    }

    public Combatiente(String nombre, String descripcion,
                       int fuerza, int resistencia, int iniciativa,
                       int agilidad, int velocidad, int vida, int vidaMaxima) {

        this.nombre = nombre;
        this.descripcion = descripcion;

        inicializarAtributos();

        setFuerza(fuerza);
        setResistencia(resistencia);
        setIniciativa(iniciativa);
        setAgilidad(agilidad);
        setVelocidad(velocidad);

        this.vida = vida;
        this.vidaMaxima = vidaMaxima;
    }

    private void inicializarAtributos() {

        for (Atributo atributo : Atributo.values()) {
            if (atributo != Atributo.VIDA) {
                atributosBase.put(atributo, 0);
                modificadoresEquipo.put(atributo, 0);
            }
        }
    }

    public int getAtributo(Atributo atributo) {
        validarAtributo(atributo);
        int base = atributosBase.get(atributo);
        int modificador = modificadoresEquipo.get(atributo);

        return Math.max(0, base + modificador);
    }

    public int getAtributoBase(Atributo atributo) {
        validarAtributo(atributo);
        return atributosBase.get(atributo);
    }

    private void validarAtributo(Atributo atributo) {
        Objects.requireNonNull(atributo, "El atributo no puede ser null");
        if (atributo == Atributo.VIDA) {
            throw new IllegalArgumentException("La vida se gestiona con vida y vidaMaxima, no con modificadores de atributos");
        }
    }

    public Map<Atributo, Integer> getModificadoresEquipo() {
        return Collections.unmodifiableMap(modificadoresEquipo);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }


    public int getFuerza() {
        return getAtributo(Atributo.FUERZA);
    }

    public void setFuerza(int fuerza) {
        atributosBase.put(Atributo.FUERZA, fuerza);
    }

    public int getResistencia() {
        return getAtributo(Atributo.RESISTENCIA);
    }

    public void setResistencia(int resistencia) {
        atributosBase.put(Atributo.RESISTENCIA, resistencia);
    }

    public int getIniciativa() {
        return getAtributo(Atributo.INICIATIVA);
    }

    public void setIniciativa(int iniciativa) {
        atributosBase.put(Atributo.INICIATIVA, iniciativa);
    }

    public int getAgilidad() {
        return getAtributo(Atributo.AGILIDAD);
    }

    public void setAgilidad(int agilidad) {
        atributosBase.put(Atributo.AGILIDAD, agilidad);
    }

    public int getVelocidad() {
        return getAtributo(Atributo.VELOCIDAD);
    }

    public void setVelocidad(int velocidad) {
        atributosBase.put(Atributo.VELOCIDAD, velocidad);
    }

    public int getVida() {
        return vida;
    }

    public void setVida(int vida) {
        this.vida = vida;
    }

    public int getVidaMaxima() {
        return Math.max(0, Math.addExact(vidaMaxima, modificadoresEquipo.getOrDefault(Atributo.VIDA, 0)));
    }

    public int getVidaMaximaBase() { return vidaMaxima; }

    public void modificarVidaMaximaEquipo(int cantidad) {
        int modificador = Math.addExact(modificadoresEquipo.getOrDefault(Atributo.VIDA, 0), cantidad);
        int maxima = Math.max(0, Math.addExact(vidaMaxima, modificador));
        modificadoresEquipo.put(Atributo.VIDA, modificador);
        if (vida > maxima) vida = maxima;
    }

    public void setVidaMaxima(int vidaMaxima) {
        this.vidaMaxima = vidaMaxima;
    }

    public Map<TipoEquipo, Equipo> getEquipoEquipado() {
        return Collections.unmodifiableMap(equipoEquipado);
    }

    public void setEquipo(Equipo equipo) {
        if (!equipar(equipo)) {
            System.out.println("No se puede equipar");
        }
    }

    public boolean equipar(Equipo equipo) {
        Objects.requireNonNull(equipo, "El equipo no puede ser null");
        TipoEquipo tipo = equipo.getTipoEquipo();
        if (equipoEquipado.containsKey(tipo)) {
            return false;
        }
        equipo.aplicarEfectos(this);
        equipoEquipado.put(tipo, equipo);
        return true;
    }

    public boolean desequipar(TipoEquipo tipo) {
        Objects.requireNonNull(tipo, "El tipo de equipo no puede ser null");
        Equipo equipo = equipoEquipado.get(tipo);
        if (equipo == null) {
            return false;
        }
        equipo.retirarEfectos(this);
        equipoEquipado.remove(tipo);
        return true;
    }

    public Map<Habilidad, EstadoHabilidad> getHabilidades() {
        return habilidades;
    }

    // El intercambio reutiliza las operaciones normales, restaurando su estado si fallan.
    protected boolean cambiarEquipoSeguro(Equipo nuevo) {
        return modificarEquipoSeguro(nuevo.getTipoEquipo(), nuevo);
    }

    protected boolean desequiparSeguro(TipoEquipo tipo) {
        return modificarEquipoSeguro(tipo, null);
    }

    private boolean modificarEquipoSeguro(TipoEquipo tipo, Equipo nuevo) {
        int vidaAntes = vida;
        return ejecutarCambioSeguro(nuevo, () -> {
            if (nuevo == null) return desequipar(tipo) && !equipoEquipado.containsKey(tipo);
            if ((!equipoEquipado.containsKey(tipo) || desequipar(tipo)) && equipar(nuevo)) {
                vida = Math.min(vidaAntes, getVidaMaxima());
                return true;
            }
            return false;
        });
    }

    protected boolean mejorarEquipoSeguro(Equipo equipo, Atributo atributo) {
        return ejecutarCambioSeguro(equipo, () -> equipo.aplicarMejora(atributo, this));
    }

    private boolean ejecutarCambioSeguro(Equipo nuevo, java.util.function.BooleanSupplier cambio) {
        var ranurasAntes = new EnumMap<>(equipoEquipado);
        var modificadoresAntes = new EnumMap<>(modificadoresEquipo);
        var baseAntes = new EnumMap<>(atributosBase);
        int vidaAntes = vida;
        int maximaAntes = vidaMaxima;
        var mapaAntes = habilidades;
        var habilidadesAntes = new HashMap<>(habilidades);
        var estadosAntes = new java.util.IdentityHashMap<EstadoHabilidad, EstadoHabilidad>();
        habilidades.values().forEach(estado -> estadosAntes.put(estado,
                new EstadoHabilidad(estado)));
        var instancias = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Habilidad, Boolean>());
        instancias.addAll(habilidades.keySet());
        if (nuevo != null) instancias.addAll(nuevo.getHabilidadesOtorgadas().keySet());
        var restauracionesTemporales = instancias.stream().map(Habilidad::guardarEstadoTemporal).toList();
        try {
            if (cambio.getAsBoolean()) return true;
        } catch (RuntimeException falloEfectos) {
            // No se intentan efectos inversos: podrían volver a fallar o retirar otras contribuciones.
        }
        equipoEquipado.clear();
        equipoEquipado.putAll(ranurasAntes);
        modificadoresEquipo.clear();
        modificadoresEquipo.putAll(modificadoresAntes);
        atributosBase.clear();
        atributosBase.putAll(baseAntes);
        vida = vidaAntes;
        vidaMaxima = maximaAntes;
        habilidades = mapaAntes;
        if (!habilidades.equals(habilidadesAntes)) {
            habilidades.clear();
            habilidades.putAll(habilidadesAntes);
        }
        estadosAntes.forEach(EstadoHabilidad::restaurar);
        restauracionesTemporales.forEach(Runnable::run);
        return false;
    }

    public void setHabilidades(Map<Habilidad, EstadoHabilidad> setHabilidades) {
        this.habilidades = setHabilidades;
    }

    public void recibirDanio(int danio) {

        vida -= danio;

        if (vida <= 0) {
            vida = 0;
        }
    }

    public void curarse(int curacion) {

        vida += curacion;

        if (vida > getVidaMaxima()) {
            vida = getVidaMaxima();
        }

        for (Map.Entry<Habilidad, EstadoHabilidad> hab : habilidades.entrySet()) {
            if (hab.getValue().isActiva()) {
                hab.getKey().alCurarse(this, hab.getValue());
            }
        }
    }

    public boolean estaVivo() {
        return vida > 0;
    }

    public void aumentarAtributo(Atributo atributo, int aumento){
        validarAtributo(atributo);
        atributosBase.merge(atributo, aumento, Integer::sum);
    }

    public void disminuirAtributo(Atributo atributo, int decremento){
        validarAtributo(atributo);
        atributosBase.merge(atributo, decremento,(valorAntes, dism) -> Math.max(0, valorAntes - dism));
    }

    public void aprenderHabilidad(Habilidad habilidad, int niveles) {

        Objects.requireNonNull(habilidad, "La habilidad no puede ser null");
        if (niveles <= 0) {
            throw new IllegalArgumentException("Los niveles aprendidos deben ser positivos");
        }

        EstadoHabilidad estado = habilidades.get(habilidad);

        if (estado != null) {
            estado.aprender(niveles);
        } else {
            habilidades.put(habilidad, new EstadoHabilidad(true, 0, niveles));
        }
    }

    // Reutiliza la misma recuperación de atributos, mapas e instancias que el intercambio.
    protected final boolean aprenderHabilidadSeguro(Habilidad habilidad) {
        return ejecutarCambioSeguro(null, () -> {
            EstadoHabilidad anterior = habilidades.get(habilidad);
            int total = anterior == null || !anterior.isActiva() ? 1 : Math.incrementExact(anterior.getNivel());
            int permanente = anterior == null ? 1 : Math.incrementExact(anterior.getNivelPermanente());
            aprenderHabilidad(habilidad, 1);
            EstadoHabilidad actual = habilidades.get(habilidad);
            return actual != null && actual.isAprendida() && actual.getNivel() == total
                    && actual.getNivelPermanente() == permanente;
        });
    }

    // Equipar arma
    public void agregarHabilidadEquipo(Habilidad habilidad, int niveles) {

        Objects.requireNonNull(habilidad, "La habilidad no puede ser null");
        if (niveles <= 0) {
            throw new IllegalArgumentException("Los niveles otorgados deben ser positivos");
        }

        EstadoHabilidad estado = habilidades.get(habilidad);

        if (estado == null) {

            habilidades.put(habilidad, new EstadoHabilidad(false, 1, niveles));

            // Si ya está en habilidades, a equipar le da igual
            // si era por estar aprendida o no
        } else {

            // Aumentamos los niveles de la habilidad correspondientes
            estado.agregarEquipo(niveles);
        }
    }

    public void quitarHabilidadEquipo(Habilidad habilidad, int niveles) {

        EstadoHabilidad estado = habilidades.get(habilidad);

        if (niveles <= 0) {
            throw new IllegalArgumentException("Los niveles a retirar deben ser positivos");
        }
        if (estado == null || estado.getVecesEquipada() <= 0 || estado.getNivel() < niveles) {
            throw new IllegalStateException("La habilidad no tiene esa contribución de equipo para retirar");
        }

        int equiposRestantes = estado.getVecesEquipada() - 1;
        if (!estado.isAprendida() && equiposRestantes == 0) {
            // La igualdad es por clase: el argumento puede ser otra instancia sin el efecto activo.
            for (Habilidad propia : habilidades.keySet()) {
                if (propia.equals(habilidad)) {
                    propia.retirarEfectosTemporales(this, estado);
                    break;
                }
            }
            estado.setVecesEquipada(0);
            eliminarHabilidad(habilidad);
            return;
        }
        if (estado.getNivel() - niveles <= 0) {
            throw new IllegalStateException("Una habilidad que permanece activa debe conservar nivel positivo");
        }
        estado.retirarEquipo(niveles);
    }

    public void agregarHabilidad(Habilidad habilidad) {
        habilidades.compute(habilidad, (k, v) -> v);
    }

    public void eliminarHabilidad(Habilidad habilidad) {
        habilidades.remove(habilidad);
    }

    public void modificarAtributo(Atributo atributo, int cantidad) {
        validarAtributo(atributo);
        modificadoresEquipo.merge(atributo, cantidad, Integer::sum);
    }
}
