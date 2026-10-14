package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;

public class GuardiaFerrea implements EventosHabilidad {

    private int nivelGuardia;

    public GuardiaFerrea(){
        this.nivelGuardia = 2;
    }

    @Override
    public void antesDeAplicarDanio(ContextoAtaque contexto, EstadoHabilidad estado) {

        //Primero veo si el ataque haría daño. Si el daño es 0 o menor no me interesa siquiera usar la guardia
        if (contexto.getDanioFinal() > 0) {

            //Cambio el daño final reduciendolo en el nivel de la habilidad.
            contexto.setDanioFinal(contexto.getDanioFinal() - nivelGuardia);

            //Si el daño termina siendo negativo, se cambia a 0.
            if (contexto.getDanioFinal() < 0) {
                contexto.setDanioFinal(0);
            }
        }
    }
}

