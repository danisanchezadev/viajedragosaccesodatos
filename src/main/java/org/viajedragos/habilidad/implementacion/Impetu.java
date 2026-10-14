package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;
import org.viajedragos.modelo.Combatiente;

public class Impetu implements EventosHabilidad {

    private int contadorAgilidad;
    private static final int MAXIMO_CONTADOR = 3;

    public Impetu(){
        this.contadorAgilidad = 0;
    }

    @Override
    public void alHacerDanio(ContextoAtaque contexto, EstadoHabilidad estado) {

        int agilidad = contexto.getAtacante().getAgilidad();

        if(contexto.getDanioFinal() > 0 && contadorAgilidad < MAXIMO_CONTADOR){

            contadorAgilidad += 1;

            contexto.getAtacante().setAgilidad(agilidad + 1);

        }
    }

    @Override
    public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {

        //Como la agilidad habrá subido tantos puntos como el contador,
        //resto el contador y se quedará en la agilidad que tiene como atributo

        portador.setAgilidad(portador.getAgilidad() - contadorAgilidad);

        contadorAgilidad = 0;

    }
}
