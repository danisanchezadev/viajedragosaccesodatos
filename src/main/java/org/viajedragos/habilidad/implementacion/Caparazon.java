package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;

public class Caparazon extends Habilidad {

    public Caparazon() {
        super("Caparazón");
    }


    @Override
    public void antesDeAplicarDanio(ContextoAtaque contexto, EstadoHabilidad estado) {

        // Debe ser el estado asociado a esta habilidad en el defensor.
        if (contexto.getDefensor().getHabilidades().get(this) != estado) return;

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
