package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;

public class CastigoAlTorpe implements EventosHabilidad {

    private int nivel;

    public CastigoAlTorpe(){
        this.nivel = 4;
    }

    @Override
    public void antesDeAplicarDanio(ContextoAtaque contexto, EstadoHabilidad estado) {

        if(contexto.getDadoDefensa() == 1 && !contexto.isAtaqueFallido())
            contexto.setDanioFinal(contexto.getDanioFinal() + nivel);
    }
}
