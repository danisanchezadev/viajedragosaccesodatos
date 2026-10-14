package org.viajedragos.modelo;

import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.Equipo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;
import org.viajedragos.habilidad.Habilidad;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

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
    private Map<TipoEquipo, Equipo> equipoEquipado = new EnumMap<>(TipoEquipo.class);

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

        int base = atributosBase.get(atributo);
        int modificador = modificadoresEquipo.get(atributo);

        return Math.max(0, base + modificador);
    }

    public int getAtributoBase(Atributo atributo) {
        return atributosBase.get(atributo);
    }

    public Map<Atributo, Integer> getModificadoresEquipo() {
        return modificadoresEquipo;
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
        return vidaMaxima;
    }

    public void setVidaMaxima(int vidaMaxima) {
        this.vidaMaxima = vidaMaxima;
    }

    public Map<TipoEquipo, Equipo> getEquipoEquipado() {
        return equipoEquipado;
    }

    public void setEquipo(Equipo equipo) {

        TipoEquipo tipo = equipo.getTipoEquipo();

        if (equipoEquipado.containsKey(tipo)) {
            System.out.println("No se puede equipar");
        } else {
            equipoEquipado.put(tipo, equipo);
        }
    }

    public Map<Habilidad, EstadoHabilidad> getHabilidades() {
        return habilidades;
    }

    public void setHabilidades(Map<Habilidad, EstadoHabilidad> setHabilidades) {
        this.habilidades = habilidades;
    }

    public void recibirDanio(int danio) {

        vida -= danio;

        if (vida <= 0) {
            vida = 0;
        }
    }

    public void curarse(int curacion) {

        vida += curacion;

        if (vida > vidaMaxima) {
            vida = vidaMaxima;
        }

        for (Map.Entry<Habilidad, EstadoHabilidad> hab : habilidades.entrySet()) {
            hab.getKey().alCurarse(this, hab.getValue());
        }
    }

    public boolean estaVivo() {
        return vida > 0;
    }

    public void aumentarAtributo(Atributo atributo, int aumento){
        atributosBase.merge(atributo, aumento, Integer::sum);
    }

    public void disminuirAtributo(Atributo atributo, int decremento){

        atributosBase.merge(atributo, decremento,(valorAntes, dism) -> Math.max(0, valorAntes - dism));
    }

    public void aprenderHabilidad(Habilidad habilidad, int niveles) {

        EstadoHabilidad estado = habilidades.get(habilidad);

        if (habilidades.containsKey(habilidad) && estado.isAprendida()) {
            estado.setNivel(estado.getNivel() + niveles);

        } else if (habilidades.containsKey(habilidad) && !estado.isAprendida()) {

            estado.setAprendida(true);
            estado.setNivel(estado.getNivel() + niveles);

        } else {

            habilidades.put(habilidad, new EstadoHabilidad(true, 0, niveles));
        }
    }

    // Equipar arma
    public void agregarHabilidadEquipo(Habilidad habilidad, int niveles) {

        EstadoHabilidad estado = habilidades.get(habilidad);

        if (estado == null) {

            habilidades.put(habilidad, new EstadoHabilidad(false, 1, niveles));

            // Si ya está en habilidades, a equipar le da igual
            // si era por estar aprendida o no
        } else {

            // Le aumentamos el nivel de equipada en 1
            estado.setVecesEquipada(estado.getVecesEquipada() + 1);

            // Aumentamos los niveles de la habilidad correspondientes
            estado.setNivel(estado.getNivel() + niveles);
        }
    }

    public void quitarHabilidadEquipo(Habilidad habilidad, int niveles) {

        EstadoHabilidad estado = habilidades.get(habilidad);

        estado.setNivel(estado.getNivel() - niveles);
        estado.setVecesEquipada(estado.getVecesEquipada() - 1);

        if (!estado.isAprendida() && estado.getVecesEquipada() == 0) {
            eliminarHabilidad(habilidad);
        }
    }

    public void agregarHabilidad(Habilidad habilidad) {
        habilidades.compute(habilidad, (k, v) -> v);
    }

    public void eliminarHabilidad(Habilidad habilidad) {
        habilidades.remove(habilidad);
    }

    public void modificarAtributo(Atributo atributo, int cantidad) {
        modificadoresEquipo.merge(atributo, cantidad, Integer::sum);
    }
}