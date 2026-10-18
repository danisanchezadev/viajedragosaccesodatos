package org.viajedragos.habilidad.implementacion;

import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.arma.Atributo;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;

public class InstintoSupervivencia extends Habilidad {

    public InstintoSupervivencia() {
        super("Instinto de supervivencia");
    }

    private double obtenerPorcentajeVida(Combatiente portador) {

        int vidaActual = portador.getVida();
        int vidaMaxima = portador.getVidaMaxima();

        return (double) (vidaActual * 100) / vidaMaxima;
    }

    private void modificarFuerza(Combatiente combatiente, EstadoHabilidad estado) {

        double porcentaje = obtenerPorcentajeVida(combatiente);

        if (porcentaje <= 30 && !estado.isEfectoTemporalActivo()) {
            combatiente.setFuerza(combatiente.getAtributoBase(Atributo.FUERZA) + 3);
            estado.setEfectoTemporalActivo(true);
        } else if (porcentaje > 30 && estado.isEfectoTemporalActivo()) {
            combatiente.setFuerza(combatiente.getAtributoBase(Atributo.FUERZA) - 3);
            estado.setEfectoTemporalActivo(false);
        }
    }

    @Override
    public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {

        modificarFuerza(portador, estado);
    }

    @Override
    public void alFinalCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
        retirarEfectosTemporales(portador, estado);
    }

    @Override
    public void retirarEfectosTemporales(Combatiente portador, EstadoHabilidad estado) {

        if (estado.isEfectoTemporalActivo()) {
            estado.setEfectoTemporalActivo(false);
            portador.setFuerza(portador.getAtributoBase(Atributo.FUERZA) - 3);
        }
    }

    @Override
    public void alRecibirDanio(ContextoAtaque contexto, EstadoHabilidad estado) {

        modificarFuerza(contexto.getDefensor(), estado);

    }

    @Override
    public void alCurarse(Combatiente combatiente, EstadoHabilidad estado) {

        modificarFuerza(combatiente, estado);
    }
}































