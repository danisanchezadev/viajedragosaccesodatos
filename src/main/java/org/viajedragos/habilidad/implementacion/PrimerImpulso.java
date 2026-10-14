package org.viajedragos.habilidad.implementacion;

import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.arma.Atributo;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;

public class PrimerImpulso extends Habilidad {

    private int nivel = 3;

    public PrimerImpulso(int nivel){
        super("Primer impulso");
        this.nivel = nivel;
    }

    @Override
    public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {

        portador.setIniciativa(portador.getAtributoBase(Atributo.INICIATIVA) + nivel);
    }

    @Override
    public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {

        portador.setIniciativa(portador.getAtributoBase(Atributo.INICIATIVA) - nivel);
    }
}
