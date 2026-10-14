package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;

public class DefensaPerfecta implements EventosHabilidad {

    @Override
    public void antesDeAplicarDanio(ContextoAtaque contexto, EstadoHabilidad estado) {
        int resultadoDefensa = contexto.getDadoDefensa();

        if(resultadoDefensa == 6){

            contexto.setDanioFinal(contexto.getDanioFinal() / 2);
        }
    }
}
