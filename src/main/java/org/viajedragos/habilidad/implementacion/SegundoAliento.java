package org.viajedragos.habilidad.implementacion;

import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;
import org.viajedragos.modelo.Combatiente;

public class SegundoAliento implements EventosHabilidad {

    private int nivelHabilidad = 2;

    public SegundoAliento(int nivelHabilidad) {
        this.nivelHabilidad = nivelHabilidad;
    }

    @Override
    public void alInicioTurno(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {

        if (portador.estaVivo() && (portador.getVida() * 2 < portador.getVidaMaxima())) {
            portador.curarse(nivelHabilidad);
        }
    }

}
