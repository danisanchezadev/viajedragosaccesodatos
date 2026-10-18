package org.viajedragos.arma;

import org.viajedragos.habilidad.Habilidad;

import java.util.Map;

public class Arma extends Equipo {

    public Arma(String nombre, TipoEquipo tipoEquipo,
                Map<Atributo, Integer> bonificaciones,
                Map<Habilidad, Integer> habilidadesOtorgadas) {

        super(nombre, tipoEquipo, bonificaciones, habilidadesOtorgadas);
    }

    public Arma(String idDefinicion, String nombre, TipoEquipo tipoEquipo,
                Map<Atributo, Integer> bonificaciones, Map<Habilidad, Integer> habilidadesOtorgadas) {
        super(idDefinicion, nombre, tipoEquipo, bonificaciones, habilidadesOtorgadas);
    }
}
