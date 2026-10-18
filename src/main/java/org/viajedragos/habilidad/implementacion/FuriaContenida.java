package org.viajedragos.habilidad.implementacion;

import org.viajedragos.arma.Atributo;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;

public class FuriaContenida extends Habilidad {

    public FuriaContenida() {
        super("Furia contenida");
    }

    private int nivel = 2;


    @Override
    public void alRecibirDanio(ContextoAtaque contexto, EstadoHabilidad estado) {



        if(!estado.isEfectoTemporalActivo() && contexto.getDanioFinal() > 0){

            estado.setEfectoTemporalActivo(true);
            contexto.getDefensor().aumentarAtributo(Atributo.FUERZA, nivel);
        }
    }

    @Override
    public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
        retirarEfectosTemporales(portador, estado);
    }

    @Override
    public void retirarEfectosTemporales(Combatiente portador, EstadoHabilidad estado) {

        if (estado.isEfectoTemporalActivo()) {

            portador.setFuerza(portador.getAtributoBase(Atributo.FUERZA) - nivel);
            estado.setEfectoTemporalActivo(false);
        }
    }
}
