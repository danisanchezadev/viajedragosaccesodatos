package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;

public class CastigoAlTorpe extends Habilidad {

    private int nivel;

    public CastigoAlTorpe(){
        super("Castigo al torpe");
        this.nivel = 4;
    }

    @Override
    public void antesDeAplicarDanio(ContextoAtaque contexto, EstadoHabilidad estado) {

        // Debe ser el estado asociado a esta habilidad en el atacante.
        if (contexto.getAtacante().getHabilidades().get(this) != estado) return;

        if(contexto.getDadoDefensa() == 1 && !contexto.isAtaqueFallido())
            contexto.setDanioFinal(contexto.getDanioFinal() + nivel);
    }
}
