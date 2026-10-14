package org.viajedragos.habilidad.implementacion;

import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;

import java.util.IntSummaryStatistics;
import java.util.List;

public class DebilidadEnemiga extends Habilidad {

    public DebilidadEnemiga() {
        super("Debilidad enemiga");
    }

    private double calcularMedia(Combatiente combatiente) {

        List<Integer> estadisticas = List.of(combatiente.getFuerza(),
                combatiente.getResistencia(),
                combatiente.getVelocidad(),
                combatiente.getAgilidad(),
                combatiente.getIniciativa());

        IntSummaryStatistics stats = estadisticas.stream()
                .mapToInt(Integer::intValue)
                .summaryStatistics();

        return stats.getAverage();
    }

    @Override
    public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {

        double mediaJugador = calcularMedia(portador);
        double mediaOponente = calcularMedia(oponente);

        System.out.println("Media del jugador = " + mediaJugador);
        System.out.println("Media del oponenete = " + mediaOponente);

        if(calcularMedia(portador) >= calcularMedia(oponente) * 2){

            oponente.recibirDanio(oponente.getVida());
        }
    }
}
