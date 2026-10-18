package org.viajedragos.combate;

import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.modelo.TipoPocion;

import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
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

        // En el turno de dos acciones, este resultado determina quién actúa primero.
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
        ataque(atacante, defensor, null);
    }

    private void ataque(Combatiente atacante, Combatiente defensor, PrintStream salida) {

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
        int vidaAtacanteAntes = atacante.getVida();
        danioBruto(ca);
        int danioContraataque = Math.max(0, vidaAtacanteAntes - atacante.getVida());

        ca.setDanioFinal(ca.getDanioBruto());
        dispararHabilidades(atacante, (h, estado) -> h.antesDeAplicarDanio(ca, estado));
        dispararHabilidades(defensor, (h, estado) -> h.antesDeAplicarDanio(ca, estado));

        if (ca.isAtaqueFallido()) {
            ca.setDanioFinal(0);
            if (salida != null) {
                salida.printf("%s falla su ataque contra %s.%n", atacante.getNombre(), defensor.getNombre());
                if (ca.isContraataque()) {
                    salida.printf("%s contraataca a %s y causa %d puntos de daño.%n",
                            defensor.getNombre(), atacante.getNombre(), danioContraataque);
                }
            }
            return;
        }

        int vidaAntes = defensor.getVida();
        defensor.recibirDanio(ca.getDanioFinal());
        if (salida != null) {
            if (ca.getDadoAtaque() == 6 && ca.getDadoDefensa() == 1) {
                salida.println("¡Golpe crítico!");
            }
            salida.printf("%s golpea a %s y causa %d puntos de daño.%n",
                    atacante.getNombre(), defensor.getNombre(), Math.max(0, vidaAntes - defensor.getVida()));
        }
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
        return combatirInterno(null, null);
    }

    /**
     * Ejecuta el mismo motor, esperando la elección solo al llegar la acción del jugador.
     * Si se cierra la entrada, Scanner lanza NoSuchElementException: la sesión se
     * interrumpe, sin inventar un resultado de combate ni ejecutar su finalización.
     * Los flujos pertenecen al llamador y no se cierran aquí.
     */
    public ResultadoCombate combatir(Scanner entrada, PrintStream salida) {
        Objects.requireNonNull(entrada);
        Objects.requireNonNull(salida);
        return combatirInterno(entrada, salida);
    }

    private ResultadoCombate combatirInterno(Scanner entrada, PrintStream salida) {
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
        int turno = 1;
        while (true) {
            ResultadoCombate resultado = eventosTurno(true);
            if (resultado != ResultadoCombate.CONTINUA) return finalDelCombate(resultado);

            Combatiente primeroEnActuar = atacante(poseedorTurno, determinarOponente(poseedorTurno));
            if (salida != null) {
                salida.printf("%nTurno %d. Orden: %s, después %s.%n", turno,
                        primeroEnActuar.getNombre(), determinarOponente(primeroEnActuar).getNombre());
            }
            for (Combatiente actual : new Combatiente[]{primeroEnActuar, determinarOponente(primeroEnActuar)}) {
                if (salida != null) {
                    mostrarEstado(salida);
                    salida.printf("Actúa %s.%n", actual.getNombre());
                    if (actual == jugador) {
                        resultado = ejecutarAccionJugador(entrada, salida);
                        if (resultado != ResultadoCombate.CONTINUA) return finalDelCombate(resultado);
                    } else {
                        salida.println("El enemigo elige atacar.");
                        ataque(actual, determinarOponente(actual), salida);
                    }
                } else {
                    ataque(actual, determinarOponente(actual));
                }
                if (algunoMuerto()) {
                    resultado = resolverMuertes(null);
                    if (resultado != ResultadoCombate.CONTINUA) return finalDelCombate(resultado);
                }
            }

            resultado = eventosTurno(false);
            if (resultado != ResultadoCombate.CONTINUA) return finalDelCombate(resultado);
            poseedorTurno = determinarOponente(poseedorTurno);
            turno++;
        }
    }

    private void mostrarEstado(PrintStream salida) {
        salida.printf("%s: %d/%d de vida | %s: %d/%d de vida%n",
                jugador.getNombre(), jugador.getVida(), jugador.getVidaMaxima(),
                enemigo.getNombre(), enemigo.getVida(), enemigo.getVidaMaxima());
    }

    // Ejecuta una acción válida; cancelar o introducir inválidos mantiene la elección.
    private ResultadoCombate ejecutarAccionJugador(Scanner entrada, PrintStream salida) {
        while (true) {
            salida.printf("%n----------------------------------------%n  TU TURNO%n----------------------------------------%n");
            salida.printf("  %s%n  Vida: %d/%d PS%n%n", jugador.getNombre(), jugador.getVida(), jugador.getVidaMaxima());
            salida.println("1. Atacar");
            if (jugador instanceof Jugador) {
                salida.println("2. Usar objeto");
                salida.println("3. Huir");
            }
            salida.print("Elige una opción > ");
            salida.flush();
            // El cierre de la entrada interrumpe la sesión; no representa una huida.
            String opcion = entrada.nextLine().trim();
            if (opcion.equals("1")) {
                ataque(jugador, enemigo, salida);
                return ResultadoCombate.CONTINUA;
            }
            if (opcion.equals("3") && jugador instanceof Jugador) {
                return intentarHuir(salida) ? ResultadoCombate.JUGADOR_HUYE : ResultadoCombate.CONTINUA;
            }
            if (opcion.equals("2") && jugador instanceof Jugador portador) {
                if (usarPocionEnCombate(portador, entrada, salida)) {
                    salida.println("Tu acción ha terminado.");
                    return ResultadoCombate.CONTINUA;
                }
            } else {
                salida.println(jugador instanceof Jugador
                        ? "Entrada inválida. Escribe 1 para atacar, 2 para usar objeto o 3 para huir."
                        : "Entrada inválida. Escribe 1 para atacar.");
            }
        }
    }

    private boolean intentarHuir(PrintStream salida) {
        int dadoJugador = tirador.tirarDado();
        int dadoEnemigo = tirador.tirarDado();
        boolean escapa = jugador.getVelocidad() + dadoJugador > enemigo.getVelocidad() + dadoEnemigo;
        salida.println(escapa ? "Has conseguido huir del combate." : "La huida ha fracasado. Tu acción ha terminado.");
        return escapa;
    }

    // true solo tras consumir; cancelar devuelve false y no termina la acción.
    private boolean usarPocionEnCombate(Jugador portador, Scanner entrada, PrintStream salida) {
        salida.printf("%n----------------------------------------%n  USAR OBJETO%n----------------------------------------%n");
        salida.printf("  Vida actual: %d/%d PS%n%n", portador.getVida(), portador.getVidaMaxima());
        salida.printf("1. %s (x%d) +%d PS%n", TipoPocion.PEQUENA.getNombre(),
                portador.getCantidadPociones(TipoPocion.PEQUENA), TipoPocion.PEQUENA.getRecuperacionPS());
        salida.printf("2. %s (x%d) +%d PS%n", TipoPocion.GRANDE.getNombre(),
                portador.getCantidadPociones(TipoPocion.GRANDE), TipoPocion.GRANDE.getRecuperacionPS());
        salida.println("0. Cancelar");
        while (true) {
            salida.print("Elige una opción > ");
            salida.flush();
            String opcion = entrada.nextLine().trim();
            if (opcion.equals("0")) return false;
            TipoPocion tipo;
            if (opcion.equals("1")) tipo = TipoPocion.PEQUENA;
            else if (opcion.equals("2")) tipo = TipoPocion.GRANDE;
            else {
                salida.println("Entrada inválida. Escribe 1, 2 o 0.");
                continue;
            }
            if (portador.getCantidadPociones(tipo) == 0) {
                salida.println("No tienes esa poción.");
                continue;
            }
            if (portador.getVida() >= portador.getVidaMaxima()) {
                salida.println("La vida está completa. No se ha consumido la poción.");
                continue;
            }
            int recuperacion = portador.usarPocion(tipo);
            if (recuperacion > 0) {
                salida.printf("Has utilizado una %s.%nVida recuperada: +%d PS%nVida actual: %d/%d PS%n",
                        tipo.getNombre(), recuperacion, portador.getVida(), portador.getVidaMaxima());
                salida.printf("Unidades restantes de %s: %d%n", tipo.getNombre(), portador.getCantidadPociones(tipo));
                return true;
            }
            salida.println("No se ha recuperado vida. No se ha consumido la poción.");
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
