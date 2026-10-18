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
        if (estado.isEfectoTemporalActivo()) {
            return;
        }
        portador.setIniciativa(portador.getAtributoBase(Atributo.INICIATIVA) + nivel);
        estado.setEfectoTemporalActivo(true);
    }

    @Override
    public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
        retirarEfectosTemporales(portador, estado);
    }

    @Override
    public void retirarEfectosTemporales(Combatiente portador, EstadoHabilidad estado) {
        if (estado.isEfectoTemporalActivo()) {
            portador.setIniciativa(portador.getAtributoBase(Atributo.INICIATIVA) - nivel);
            estado.setEfectoTemporalActivo(false);
        }
    }
}
