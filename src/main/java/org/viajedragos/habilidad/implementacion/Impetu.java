package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.arma.Atributo;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;

public class Impetu extends Habilidad {

    private static final int MAXIMO_CONTADOR = 3;

    public Impetu(){
        super("Ímpetu");
    }

    @Override
    public void alHacerDanio(ContextoAtaque contexto, EstadoHabilidad estado) {

        int agilidad = contexto.getAtacante().getAtributoBase(Atributo.AGILIDAD);

        if(contexto.getDanioFinal() > 0 && estado.getAcumulacionesTemporales() < MAXIMO_CONTADOR){

            estado.setAcumulacionesTemporales(estado.getAcumulacionesTemporales() + 1);

            contexto.getAtacante().setAgilidad(agilidad + 1);

        }
    }

    @Override
    public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
        retirarEfectosTemporales(portador, estado);
    }

    @Override
    public void retirarEfectosTemporales(Combatiente portador, EstadoHabilidad estado) {

        //Como la agilidad habrá subido tantos puntos como el contador,
        //resto el contador y se quedará en la agilidad que tiene como atributo

        portador.setAgilidad(portador.getAtributoBase(Atributo.AGILIDAD) - estado.getAcumulacionesTemporales());

        estado.setAcumulacionesTemporales(0);

    }
}
