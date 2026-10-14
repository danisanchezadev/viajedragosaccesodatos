package org.viajedragos.combate;

import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.EventosHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Combate {

    private Combatiente jugador;
    private Combatiente enemigo;

    private TiradorDado tirador;

    public Combate(Combatiente jugador, Combatiente enemigo, TiradorDado tirador) {
        this.jugador = jugador;
        this.enemigo = enemigo;
        this.tirador = tirador;
    }

    public Combatiente primerTurno() {

        int tiradaJugador = tirador.tirarDado();
        int tiradaEnemigo = tirador.tirarDado();

        int resultadoIniciativaJugador = tiradaJugador * jugador.getIniciativa();
        int resultadoIniciativaEnemigo = tiradaEnemigo * enemigo.getIniciativa();

        if (resultadoIniciativaJugador == resultadoIniciativaEnemigo) {

            System.out.println("Empate");
            return primerTurno();
        }

        if (resultadoIniciativaJugador > resultadoIniciativaEnemigo) return jugador;
        else return enemigo;

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

        //Las tiradas de ataque y defensa deben ser accesibles para el resto de métodos, así que salen aquí.
        int tiradaAtacante = tirador.tirarDado();
        int tiradaDefensor = tirador.tirarDado();

        //Pasamos las tiradas al contexto
        ca.setDadoAtaque(tiradaAtacante);
        ca.setDadoDefensa(tiradaDefensor);

        //Determinamos el daño bruto que consigue realizar el ataque.
        //Este daño nunca puede ser menor que 0.
        danioBruto(ca);

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

/*
    //Principal de combate, el que lo junta todo
    public ResultadoCombate combatir() {

        ResultadoCombate resultado = ResultadoCombate.CONTINUA;

        Combatiente primero = ordenHabilidades();
        Combatiente segundo = determinarOponente(jugador);

        dispararHabilidades(primero, h -> h.alInicioCombate(primero, segundo, estado));

        if(algunoMuerto()){
            ResultadoCombate resultadoMuertes = resolverMuertes(primero);
            if(algunoMuerto()){
                return finalDelCombate(resultadoMuertes);
            }
        }

        Combatiente poseedorPrimerTurno = primerTurno();
        Combatiente noPoseedorPrimerTurno = determinarOponente(poseedorPrimerTurno);

        int turno = 1;

        //Posibilidades del jugador: atacar, huir, usar objeto.



        //resultado de prueba, no es el que debe devolverre
        return ResultadoCombate.AMBOS_MUEREN;
    }

    //Cerrado
    //Se utilizará en los momentos donde las habilides se disparan
    //y es necesario ver en qué orden lo hacen (jugador o enemigo)
    private Combatiente ordenHabilidades() {

        int iniciativaJugador = jugador.getIniciativa();
        int iniciativaEnemigo = enemigo.getIniciativa();

        if (iniciativaJugador > iniciativaEnemigo) return jugador;
        else if (iniciativaEnemigo > iniciativaJugador) return enemigo;

        else {

            while (true) {

                int tiradaDesempateJugador = tirador.tirarDado();
                int tiradaDesempateEnemigo = tirador.tirarDado();

                if (tiradaDesempateJugador > tiradaDesempateEnemigo) {
                    return jugador;

                } else if (tiradaDesempateEnemigo > tiradaDesempateJugador) {
                    return enemigo;
                }
            }
        }
    }

    //Bucle que recorre todas las habilidades del combatiente con el consumer
    //Al consumer le tendremos que decir que parte de la habilidad tiene que disparar
    //Que se corresponde con el momento del combate en el que lo hace.
    private void dispararHabilidades(
            Combatiente combatiente,
            BiConsumer<Habilidad, EstadoHabilidad> accion) {

        for (Map.Entry<Habilidad, EstadoHabilidad> entrada
                : combatiente.getHabilidades().entrySet()) {

            accion.accept(entrada.getKey(), entrada.getValue());
        }
    }


    //================================================
    //------------- COMPROBAR MUERTES ----------------
    //================================================


    //Aquí determinamos si alguno de los personajes ha muerto combatiendo
    private boolean algunoMuerto() {
        return !jugador.estaVivo() || !enemigo.estaVivo();
    }

    private boolean ambosVivos() {
        return jugador.estaVivo() && enemigo.estaVivo();
    }

    //Si alguno de los personajes ha muerto (o los dos) pasamos a disparar sus habilidades alMorir()
    //y determinar que ocurre al lanzar esas habilidades.
    // PRECONDICIÓN:
    // Se llama únicamente si algunoMuerto() == true.
    //Cerrado

    //Leer normas del juego, apartado Combate
    private ResultadoCombate resolverMuertes(Combatiente prioridad) {

        //Comprobamos si es el enemigo el que está muerto
        if (jugador.estaVivo() && !enemigo.estaVivo()) {

            //Disparamos las habilidades que tengan alguna acción al morir.
            //Esto podría generar que resucitara, que matara al personaje, etc, etc...
            dispararHabilidades(enemigo, h -> h.alMorir(enemigo, jugador));

            //Por eso hay que volver a comprobar si está vivo o muerto

            //Si ambos están vivos tras la habilidad del enemigo: el combate continúa.
            if (ambosVivos()) {
                return ResultadoCombate.CONTINUA;

                //Si el enemigo continúa muerto tras sus habilidades, el combate finaliza con enemigo muerto
            } else if (jugador.estaVivo() && !enemigo.estaVivo()) {

                return ResultadoCombate.ENEMIGO_MUERE;

                //Si el jugador o ambos jugadores mueren, se disparan las habilidades del jugador.
                //El metodo jugadorMuere comprende todas las posibilidades que pueden ocurrir al lanzarse las habilidades
                //Jugador sigue muerto y enemigo vivo; ambos vivos, jugador resucita y enemigo muerto, ambos muertos.
            } else if ((!jugador.estaVivo() && enemigo.estaVivo())
                    || (!jugador.estaVivo() && !enemigo.estaVivo())) {

                return personajeMuere(jugador, enemigo);

            }

            //Comprobamos si es el jugador es el que está muerto.
        } else if (!jugador.estaVivo() && enemigo.estaVivo()) {

            dispararHabilidades(jugador, h -> h.alMorir(jugador, enemigo));

            //Hay que volver a comprobar si está vivo o muerto

            //Si ambos están vivos tras la habilidad del jugador: el combate continúa.
            if (ambosVivos()) return ResultadoCombate.CONTINUA;

            //Si el jugador continúa muerto tras sus habilidades, el combate finaliza con jugador muerto
            else if (enemigo.estaVivo() && !jugador.estaVivo()) return ResultadoCombate.JUGADOR_MUERE;

            else if ((!enemigo.estaVivo() && jugador.estaVivo())) return personajeMuere(enemigo, jugador);

            //Si mueren los dos, ya no me interesa que el enemigo lance sus habilidades, no es relevante
            else if (!enemigo.estaVivo() && !jugador.estaVivo()) return ResultadoCombate.AMBOS_MUEREN;

        } else{

            Combatiente noPrioridad = determinarOponente(prioridad);

            dispararHabilidades(prioridad, h -> h.alMorir(prioridad, noPrioridad));

            //Si ambos están vivos tras la primera habilidad, el combate continúa.
            if (ambosVivos()) return ResultadoCombate.CONTINUA;

            //Si muere el que no tenía prioridad y no había lanzado sus habilidades
            else if (prioridad.estaVivo() && !noPrioridad.estaVivo()) {

               return personajeMuere(noPrioridad, prioridad);

            } else if (!prioridad.estaVivo() && noPrioridad.estaVivo()) {

                if(prioridad == jugador){
                    return ResultadoCombate.JUGADOR_MUERE;

                }else{
                    return ResultadoCombate.ENEMIGO_MUERE;
                }

            } else if (!prioridad.estaVivo() && !noPrioridad.estaVivo()) {

                if(prioridad == jugador) return ResultadoCombate.AMBOS_MUEREN;
                else return personajeMuere(noPrioridad, prioridad);
            }
        }
        return ResultadoCombate.CONTINUA;
    }

    // PRECONDICIÓN:
    // "muerto" todavía no ha ejecutado alMorir() en esta resolución.
    // "sobrevive" ya ha ejecutado alMorir() en esta resolución.
    private ResultadoCombate personajeMuere(Combatiente muerto, Combatiente sobrevive) {

        dispararHabilidades(muerto, h -> h.alMorir(muerto, sobrevive));

        if (ambosVivos()) {
            return ResultadoCombate.CONTINUA;

        } else if (muerto.estaVivo() && !sobrevive.estaVivo()) {

            if (muerto == jugador) {
                return ResultadoCombate.ENEMIGO_MUERE;
            } else {
                return ResultadoCombate.JUGADOR_MUERE;
            }

        } else if (!muerto.estaVivo() && sobrevive.estaVivo()) {

            if (muerto == jugador) {
                return ResultadoCombate.JUGADOR_MUERE;
            } else {
                return ResultadoCombate.ENEMIGO_MUERE;
            }

        } else {

            return ResultadoCombate.AMBOS_MUEREN;
        }
    }


//================================================
//--------------FINAL DEL COMBATE-----------------
//================================================

    //Aqui se vuelve a lanzar las habilidades del jugador para que, cosas que se han modificado durante el combate
//Como atributos o vida, se reestablezcan en sus valores normales.
    private ResultadoCombate finalDelCombate(ResultadoCombate resultado) {

        dispararHabilidades(jugador, h -> h.alFinalCombate(jugador, enemigo));

        return resultado;
    }


 */
}