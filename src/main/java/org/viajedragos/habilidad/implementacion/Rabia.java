package org.viajedragos.habilidad.implementacion;

import org.viajedragos.arma.Atributo;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;

public class Rabia extends Habilidad {

    public Rabia() {
        super("Rabia");
    }

    @Override
    public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
        if (estado.isEfectoTemporalActivo()) {
            return;
        }
        portador.aumentarAtributo(Atributo.FUERZA, 2);
        portador.setAgilidad(portador.getAtributoBase(Atributo.AGILIDAD) + 1);
        portador.setResistencia(portador.getAtributoBase(Atributo.RESISTENCIA) - 1);
        estado.setEfectoTemporalActivo(true);
    }

    @Override
    public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
        retirarEfectosTemporales(portador, estado);
    }

    @Override
    public void retirarEfectosTemporales(Combatiente portador, EstadoHabilidad estado) {
        if (!estado.isEfectoTemporalActivo()) {
            return;
        }
        portador.setFuerza(portador.getAtributoBase(Atributo.FUERZA) - 2);
        portador.setAgilidad(portador.getAtributoBase(Atributo.AGILIDAD) - 1);
        portador.setResistencia(portador.getAtributoBase(Atributo.RESISTENCIA) + 1);
        estado.setEfectoTemporalActivo(false);
    }
}
