package org.viajedragos.habilidad.implementacion;

import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;

public class SegundoAliento extends Habilidad {

    private int nivelHabilidad = 2;

    public SegundoAliento(int nivelHabilidad) {
        super("Segundo aliento");
        this.nivelHabilidad = nivelHabilidad;
    }

    @Override
    public void alInicioTurno(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {

        if (portador.estaVivo() && (portador.getVida() * 2 < portador.getVidaMaxima())) {
            portador.curarse(nivelHabilidad);
        }
    }

}
