package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;

public class SedDeBatalla implements EventosHabilidad {

    private int nivel;

    public SedDeBatalla(){
        this.nivel = 2;
    }

    @Override
    public void alHacerDanio(ContextoAtaque contexto, EstadoHabilidad estado) {

        if(!contexto.isAtaqueFallido() && contexto.getDanioFinal() > 0){

            contexto.getAtacante().curarse(nivel);
        }
    }
}
