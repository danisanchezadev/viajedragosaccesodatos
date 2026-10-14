package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;

public class Caparazon implements EventosHabilidad {


    @Override
    public void antesDeAplicarDanio(ContextoAtaque contexto, EstadoHabilidad estado) {

        int nivel = estado.getNivel();

        if(contexto.getDanioFinal() > 0){

            double danioFinal = contexto.getDanioFinal() - contexto.getDanioFinal() * ((double) nivel /100);

            contexto.setDanioFinal((int) danioFinal);
        }

        if(contexto.getDanioFinal() < 0){
            contexto.setDanioFinal(0);
        }
    }
}
