package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;

public class GuardiaFerrea extends Habilidad {

    private int nivelGuardia;

    public GuardiaFerrea(){
        super("Guardia férrea");
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

