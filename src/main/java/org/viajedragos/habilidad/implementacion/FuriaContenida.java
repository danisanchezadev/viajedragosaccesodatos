package org.viajedragos.habilidad.implementacion;

import org.viajedragos.arma.Atributo;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;
import org.viajedragos.modelo.Combatiente;

public class FuriaContenida implements EventosHabilidad {

    private boolean activo = false;
    private int nivel = 2;


    @Override
    public void alRecibirDanio(ContextoAtaque contexto, EstadoHabilidad estado) {



        if(!activo && contexto.getDanioFinal() > 0){

            activo = true;
            contexto.getDefensor().aumentarAtributo(Atributo.FUERZA, nivel);
        }
    }

    @Override
    public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {

        if (activo) {

            portador.disminuirAtributo(Atributo.FUERZA, nivel);
            activo = false;
        }
    }
}
