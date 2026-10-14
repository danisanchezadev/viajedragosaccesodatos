package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;

public class GolpeCertero extends Habilidad {


    //Si el ataque no es un ataque fallido, siempre aumenta en dos,
    // por lo que un ataque que por resistencias terminara siendo 0, hará (2) de daño.


    private int nivel;

    public GolpeCertero(){
        super("Golpe certero");
        this.nivel = 2;
    }

    @Override
    public void antesDeAplicarDanio(ContextoAtaque contexto, EstadoHabilidad estado) {

        if(!contexto.isAtaqueFallido()){

            contexto.setDanioFinal(contexto.getDanioFinal() + nivel);
        }
    }
}
