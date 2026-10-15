package org.viajedragos.combate;

import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;

import java.util.Map;
import java.util.function.BiConsumer;

public class Combate {

    private Combatiente jugador;
    private Combatiente enemigo;

    private TiradorDado tirador;
    private Combatiente poseedorTurno;

    public Combate(Combatiente jugador, Combatiente enemigo, TiradorDado tirador) {
        this.jugador = jugador;
        this.enemigo = enemigo;
        this.tirador = tirador;
    }

    public Combatiente primerTurno() {
        while (true) {
            int tiradaJugador = tirador.tirarDado();
            int tiradaEnemigo = tirador.tirarDado();
            int iniciativaJugador = jugador.getIniciativa();
            int iniciativaEnemigo = enemigo.getIniciativa();
            int resultadoJugador = tiradaJugador * iniciativaJugador;
            int resultadoEnemigo = tiradaEnemigo * iniciativaEnemigo;
            if (iniciativaJugador == 0 && iniciativaEnemigo == 0) {
                resultadoJugador = tiradaJugador;
                resultadoEnemigo = tiradaEnemigo;
            }
            if (resultadoJugador > resultadoEnemigo) return jugador;
            if (resultadoEnemigo > resultadoJugador) return enemigo;
        }
    }

    public Combatiente atacante(Combatiente poseedorTurno, Combatiente oponente) {

        //La fórmula para ver quién es el atacante es iniciativa + 1D6 y quien saque la puntuación mayor es el atacante del turno.
        //Además, el poseedor del turno tendrá una bonificación de +2 a la iniciativa.
        //En caso de empate, el atacante será el poseedor del turno.

        if (poseedorTurno.getIniciativa() + 2 + tirador.tirarDado()
                >= oponente.getIniciativa() + tirador.tirarDado()) {

            return poseedorTurno;

        } else return oponente;
    }

    public Combatiente determinarOponente(Combatiente atacante) {

        if (atacante == jugador) return enemigo;
        else return jugador;

    }

    public void ataque(Combatiente atacante, Combatiente defensor) {

        //creo el contexto para este ataque en específico
        ContextoAtaque ca = new ContextoAtaque(atacante, defensor);

        dispararHabilidades(atacante, (h, estado) -> h.antesDeAtacar(ca, estado));

        //Las tiradas de ataque y defensa deben ser accesibles para el resto de métodos, así que salen aquí.
        int tiradaAtacante = tirador.tirarDado();
        int tiradaDefensor = tirador.tirarDado();

        //Pasamos las tiradas al contexto
        ca.setDadoAtaque(tiradaAtacante);
        ca.setDadoDefensa(tiradaDefensor);

        dispararHabilidades(atacante, (h, estado) -> h.antesDeCalcularDanio(ca, estado));
        dispararHabilidades(defensor, (h, estado) -> h.antesDeCalcularDanio(ca, estado));

        //Determinamos el daño bruto que consigue realizar el ataque.
        //Este daño nunca puede ser menor que 0.
        danioBruto(ca);

        ca.setDanioFinal(ca.getDanioBruto());
        dispararHabilidades(atacante, (h, estado) -> h.antesDeAplicarDanio(ca, estado));
        dispararHabilidades(defensor, (h, estado) -> h.antesDeAplicarDanio(ca, estado));

        if (ca.isAtaqueFallido()) {
            ca.setDanioFinal(0);
            return;
        }

        int vidaAntes = defensor.getVida();
        defensor.recibirDanio(ca.getDanioFinal());
        boolean huboDanioEfectivo = defensor.getVida() < vidaAntes;
        if (huboDanioEfectivo) {
            dispararHabilidades(atacante, (h, estado) -> h.alHacerDanio(ca, estado));
            dispararHabilidades(defensor, (h, estado) -> h.alRecibirDanio(ca, estado));
        }

    }

    private void danioBruto(ContextoAtaque ca) {

        int valorAtaque = valorAtaque(ca);

        if (ca.getDadoAtaque() == 1 && ca.getDadoDefensa() < 6) {

            //El ataque falla y no pasa nada
            ca.setAtaqueFallido(true);
            ca.setDanioBruto(0);
            return;

        } else if (ca.getDadoAtaque() == 1 && ca.getDadoDefensa() == 6) {

            //El defensor realiza contraataque, un ataque de oportunidad igual a su fuerza.
            ca.setAtaqueFallido(true);
            ca.setContraataque(true);
            ca.setDanioBruto(0);
            resolverContraataque(ca);

        } else if (ca.getDadoAtaque() > 1 && ca.getDadoAtaque() < 6 && ca.getDadoDefensa() == 1) {

            //El defensor falla su defensa y recibe el daño completo.
            ca.setDefensaFallida(true);
            ca.setDanioBruto(valorAtaque);

        } else if (ca.getDadoAtaque() == 6 && ca.getDadoDefensa() == 1) {

            //El defensor falla su defensa y el atacante tiene exito rotundo en su ataque,
            //dando su daño total de valor de ataque y 1D6 más como daño crítico.
            ca.setDefensaFallida(true);
            ca.setDanioBruto(valorAtaque + tirador.tirarDado());

        } else {

            //El ataque transcurre de forma normal y se hacne ataque y defensa sin modificadores.
            ca.setDanioBruto(Math.max(0, valorAtaque - (valorDefensa(ca))));
        }
    }

    //================================================
    //valorAtaque y valorDefensa se usan en danioBruto
    //================================================

    private int valorAtaque(ContextoAtaque ca) {
        return ca.getAtacante().getFuerza() + ca.getAtacante().getAgilidad() + ca.getDadoAtaque();
    }

    private int valorDefensa(ContextoAtaque ca) {
        return ca.getDefensor().getResistencia() + ca.getDefensor().getAgilidad() + ca.getDadoDefensa();
    }

    //Contraataque
    private void resolverContraataque(ContextoAtaque ca) {
        ca.getAtacante().recibirDanio(ca.getDefensor().getFuerza());
    }

    private void dispararHabilidades(
            Combatiente combatiente,
            BiConsumer<Habilidad, EstadoHabilidad> accion) {

        for (Map.Entry<Habilidad, EstadoHabilidad> entrada
                : combatiente.getHabilidades().entrySet()) {

            if (entrada.getValue().isActiva()) {
                accion.accept(entrada.getKey(), entrada.getValue());
            }
        }
    }

    public ResultadoCombate combatir() {
        poseedorTurno = null;
        Combatiente primero = ordenHabilidades();
        Combatiente segundo = determinarOponente(primero);

        if (primero.estaVivo()) {
            dispararHabilidades(primero, (h, estado) -> h.alInicioCombate(primero, segundo, estado));
        }
        // Si muere antes de su inicio, pasa a alMorir aunque luego resucite.
        boolean segundoPuedeIniciar = segundo.estaVivo();
        if (algunoMuerto()) {
            ResultadoCombate resultado = resolverMuertes(null);
            if (resultado != ResultadoCombate.CONTINUA) return finalDelCombate(resultado);
        }
        if (segundoPuedeIniciar && segundo.estaVivo()) {
            dispararHabilidades(segundo, (h, estado) -> h.alInicioCombate(segundo, primero, estado));
        }
        if (algunoMuerto()) {
            ResultadoCombate resultado = resolverMuertes(null);
            if (resultado != ResultadoCombate.CONTINUA) return finalDelCombate(resultado);
        }

        poseedorTurno = primerTurno();
        while (true) {
            ResultadoCombate resultado = eventosTurno(true);
            if (resultado != ResultadoCombate.CONTINUA) return finalDelCombate(resultado);

            Combatiente combatienteAtacante = atacante(poseedorTurno, determinarOponente(poseedorTurno));
            ataque(combatienteAtacante, determinarOponente(combatienteAtacante));
            if (algunoMuerto()) {
                resultado = resolverMuertes(null);
                if (resultado != ResultadoCombate.CONTINUA) return finalDelCombate(resultado);
            }

            resultado = eventosTurno(false);
            if (resultado != ResultadoCombate.CONTINUA) return finalDelCombate(resultado);
            poseedorTurno = determinarOponente(poseedorTurno);
        }
    }

    private ResultadoCombate eventosTurno(boolean inicio) {
        Combatiente primero = ordenHabilidades();
        Combatiente segundo = determinarOponente(primero);
        for (Combatiente combatiente : new Combatiente[]{primero, segundo}) {
            Combatiente oponente = determinarOponente(combatiente);
            if (combatiente.estaVivo()) {
                dispararHabilidades(combatiente, (h, estado) -> {
                    if (inicio) h.alInicioTurno(combatiente, oponente, estado);
                    else h.alFinalTurno(combatiente, oponente, estado);
                });
            }
            if (algunoMuerto()) {
                ResultadoCombate resultado = resolverMuertes(null);
                if (resultado != ResultadoCombate.CONTINUA) return resultado;
            }
        }
        return ResultadoCombate.CONTINUA;
    }

    private Combatiente ordenHabilidades() {
        int iniciativaJugador = jugador.getIniciativa();
        int iniciativaEnemigo = enemigo.getIniciativa();
        if (iniciativaJugador > iniciativaEnemigo) return jugador;
        if (iniciativaEnemigo > iniciativaJugador) return enemigo;
        if (poseedorTurno != null) return poseedorTurno;

        while (true) {
            int dadoJugador = tirador.tirarDado();
            int dadoEnemigo = tirador.tirarDado();
            if (dadoJugador > dadoEnemigo) return jugador;
            if (dadoEnemigo > dadoJugador) return enemigo;
        }
    }

    private boolean algunoMuerto() {
        return !jugador.estaVivo() || !enemigo.estaVivo();
    }

    private boolean ambosVivos() {
        return jugador.estaVivo() && enemigo.estaVivo();
    }

    // Cada participante ejecuta alMorir como máximo una vez en esta resolución.
    private ResultadoCombate resolverMuertes(Combatiente prioridad) {
        if (ambosVivos()) return ResultadoCombate.CONTINUA;
        Combatiente primero;
        if (!jugador.estaVivo() && !enemigo.estaVivo()) {
            primero = prioridad == null ? ordenHabilidades() : prioridad;
        } else {
            primero = jugador.estaVivo() ? enemigo : jugador;
        }
        Combatiente segundo = determinarOponente(primero);
        ResultadoCombate resultado = personajeMuere(primero, segundo);
        if (!segundo.estaVivo()) {
            resultado = personajeMuere(segundo, primero);
        }
        return resultado;
    }

    private ResultadoCombate personajeMuere(Combatiente muerto, Combatiente oponente) {
        dispararHabilidades(muerto, (h, estado) -> h.alMorir(muerto, oponente, estado));
        if (ambosVivos()) return ResultadoCombate.CONTINUA;
        if (!jugador.estaVivo() && !enemigo.estaVivo()) return ResultadoCombate.AMBOS_MUEREN;
        return jugador.estaVivo() ? ResultadoCombate.ENEMIGO_MUERE : ResultadoCombate.JUGADOR_MUERE;
    }

    private ResultadoCombate finalDelCombate(ResultadoCombate resultado) {
        // Excepción de limpieza: el jugador restaura sus efectos incluso muerto.
        dispararHabilidades(jugador, (h, estado) -> h.alFinalCombate(jugador, enemigo, estado));
        return resultado;
    }
}
