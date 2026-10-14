package org.viajedragos.habilidad.implementacion;

import org.viajedragos.arma.Atributo;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;

public class Rabia extends Habilidad {

    private boolean activa;

    public Rabia() {
        super("Rabia");
    }

    @Override
    public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
        if (activa) {
            return;
        }
        portador.aumentarAtributo(Atributo.FUERZA, 2);
        portador.setAgilidad(portador.getAtributoBase(Atributo.AGILIDAD) + 1);
        portador.setResistencia(portador.getAtributoBase(Atributo.RESISTENCIA) - 1);
        activa = true;
    }

    @Override
    public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
        if (!activa) {
            return;
        }
        portador.setFuerza(portador.getAtributoBase(Atributo.FUERZA) - 2);
        portador.setAgilidad(portador.getAtributoBase(Atributo.AGILIDAD) - 1);
        portador.setResistencia(portador.getAtributoBase(Atributo.RESISTENCIA) + 1);
        activa = false;
    }
}
