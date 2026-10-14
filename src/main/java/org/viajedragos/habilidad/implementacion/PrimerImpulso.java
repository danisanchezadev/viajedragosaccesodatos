package org.viajedragos.habilidad.implementacion;

import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;
import org.viajedragos.modelo.Combatiente;

public class PrimerImpulso implements EventosHabilidad {

    private int nivel = 3;

    public PrimerImpulso(int nivel){
        this.nivel = nivel;
    }

    @Override
    public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {

        portador.setIniciativa(portador.getIniciativa() + nivel);
    }

    @Override
    public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {

        portador.setIniciativa(portador.getIniciativa() - nivel);
    }
}
