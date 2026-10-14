package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;

public class SedDeBatalla extends Habilidad {

    private int nivel;

    public SedDeBatalla(){
        super("Sed de batalla");
        this.nivel = 2;
    }

    @Override
    public void alHacerDanio(ContextoAtaque contexto, EstadoHabilidad estado) {

        if(!contexto.isAtaqueFallido() && contexto.getDanioFinal() > 0){

            contexto.getAtacante().curarse(nivel);
        }
    }
}
