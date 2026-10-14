package org.viajedragos.habilidad.implementacion;

import org.viajedragos.arma.Atributo;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;
import org.viajedragos.modelo.Combatiente;

public class Rabia implements EventosHabilidad {

    @Override
    public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
        portador.aumentarAtributo(Atributo.FUERZA, 2);
        portador.setAgilidad(portador.getAgilidad() + 1);
        portador.setResistencia(portador.getResistencia() - 1);
    }
}