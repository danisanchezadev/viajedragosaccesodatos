package org.viajedragos.habilidad;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.modelo.Combatiente;

public interface EventosHabilidad {

    default void alInicioCombate(Combatiente portador,
                                 Combatiente oponente, EstadoHabilidad estado) {
    }

    default void alInicioTurno(Combatiente portador,
                               Combatiente oponente, EstadoHabilidad estado) {
    }

    default void alCurarse(Combatiente combatiente, EstadoHabilidad estado){
    }

    default void antesDeAtacar(ContextoAtaque contexto, EstadoHabilidad estado) {
    }

    default void antesDeCalcularDanio(ContextoAtaque contexto, EstadoHabilidad estado) {
    }

    default void antesDeAplicarDanio(ContextoAtaque contexto, EstadoHabilidad estado) {
    }

    default void alHacerDanio(ContextoAtaque contexto, EstadoHabilidad estado) {
    }

    default void alRecibirDanio(ContextoAtaque contexto, EstadoHabilidad estado) {
    }

    default void alFinalTurno(Combatiente portador,
                              Combatiente oponente, EstadoHabilidad estado) {
    }

    default void alFinalCombate(Combatiente portador,
                                Combatiente oponente, EstadoHabilidad estado) {
    }

    default void alMorir(Combatiente portador, Combatiente oponente, EstadoHabilidad estado){}

    default void alHuir(Combatiente portado, Combatiente oponente, EstadoHabilidad estado){}

    default void alPerseguir(Combatiente portado, Combatiente oponente, EstadoHabilidad estado){}


}