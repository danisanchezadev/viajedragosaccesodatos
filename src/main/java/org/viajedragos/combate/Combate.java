package org.viajedragos.combate;

import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.modelo.TipoPocion;
import org.viajedragos.consola.PanelConsola;
import org.viajedragos.consola.RitmoConsola;

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
    private final RitmoConsola ritmo;

    public Combate(Combatiente jugador, Combatiente enemigo, TiradorDado tirador) {
        this(jugador, enemigo, tirador, new RitmoConsola(RitmoConsola.Velocidad.INSTANTANEA));
    }

    public Combate(Combatiente jugador, Combatiente enemigo, TiradorDado tirador, RitmoConsola ritmo) {
        this.jugador = jugador;
        this.enemigo = enemigo;
        this.tirador = tirador;
        this.ritmo = Objects.requireNonNull(ritmo);
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

        // Creo el contexto para este ataque específico.
        ContextoAtaque ca = new ContextoAtaque(atacante, defensor);

        dispararHabilidades(atacante, (h, estado) -> h.antesDeAtacar(ca, estado));

        // Las tiradas quedan accesibles para los eventos y el cálculo del daño.
        int tiradaAtacante = tirador.tirarDado();
        int tiradaDefensor = tirador.tirarDado();

        ca.setDadoAtaque(tiradaAtacante);
        ca.setDadoDefensa(tiradaDefensor);

        dispararHabilidades(atacante, (h, estado) -> h.antesDeCalcularDanio(ca, estado));
        dispararHabilidades(defensor, (h, estado) -> h.antesDeCalcularDanio(ca, estado));

        // Determino el daño bruto conservando las reglas de fallo y contraataque.
        int vidaAtacanteAntes = atacante.getVida();
        danioBruto(ca);
        int danioContraataque = Math.max(0, vidaAtacanteAntes - atacante.getVida());

        ca.setDanioFinal(ca.getDanioBruto());
        dispararHabilidades(atacante, (h, estado) -> h.antesDeAplicarDanio(ca, estado));
        dispararHabilidades(defensor, (h, estado) -> h.antesDeAplicarDanio(ca, estado));

        if (salida != null && atacante == enemigo) ritmo.pausar(salida);

        if (ca.isAtaqueFallido()) {
            ca.setDanioFinal(0);

            if (salida != null) {
                PanelConsola panel = new PanelConsola(salida, "ATAQUE FALLIDO");
                panel.linea(atacante.getNombre() + " falla su ataque contra " + defensor.getNombre() + ".");
                panel.linea("Daño causado: 0 PS");
                if (ca.isContraataque()) {
                    panel.separador();
                    panel.titulo("CONTRAATAQUE");
                    panel.linea(defensor.getNombre() + " contraataca a " + atacante.getNombre()
                            + " y causa " + danioContraataque + " puntos de daño.");
                }
                panel.cerrar();
                ritmo.pausar(salida);
            }
            return;
        }

        int vidaAntes = defensor.getVida();
        defensor.recibirDanio(ca.getDanioFinal());

        if (salida != null) {
            int danioCausado = Math.max(0, vidaAntes - defensor.getVida());
            boolean critico = ca.getDadoAtaque() == 6 && ca.getDadoDefensa() == 1;

            PanelConsola panel = new PanelConsola(salida, critico ? "GOLPE CRÍTICO" : "RESULTADO DEL ATAQUE");
            if (critico) panel.linea("¡Golpe crítico!");
            panel.linea(atacante.getNombre() + " golpea a " + defensor.getNombre()
                    + " y causa " + danioCausado + " puntos de daño.");
            panel.linea("Vida restante: " + defensor.getVida() + "/" + defensor.getVidaMaxima() + " PS");
            panel.cerrar();
        }

        boolean huboDanioEfectivo = defensor.getVida() < vidaAntes;

        if (huboDanioEfectivo) {
            dispararHabilidades(atacante, (h, estado) -> h.alHacerDanio(ca, estado));
            dispararHabilidades(defensor, (h, estado) -> h.alRecibirDanio(ca, estado));
        }
        if (salida != null) ritmo.pausar(salida);
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
                PanelConsola panel = new PanelConsola(salida, "TURNO " + turno);
                panel.linea("Turno " + turno + ". Orden: " + primeroEnActuar.getNombre()
                        + ", después " + determinarOponente(primeroEnActuar).getNombre() + ".");
                panel.cerrar();
                ritmo.pausar(salida);
            }
            for (Combatiente actual : new Combatiente[]{primeroEnActuar, determinarOponente(primeroEnActuar)}) {
                if (salida != null) {
                    salida.printf("Actúa %s.%n", actual.getNombre());
                    if (actual == jugador) {
                        resultado = ejecutarAccionJugador(entrada, salida);
                        if (resultado != ResultadoCombate.CONTINUA) return finalDelCombate(resultado);
                    } else {
                        mostrarEstado(salida);
                        PanelConsola panel = new PanelConsola(salida, "ACCIÓN DEL ENEMIGO");
                        panel.linea("El enemigo elige atacar.");
                        panel.cerrar();
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
        PanelConsola panel = new PanelConsola(salida, "ESTADO DEL COMBATE");
        datosCombatientes(panel);
        panel.cerrar();
    }

    private void datosCombatientes(PanelConsola panel) {
        panel.columnas(jugador.getNombre(), enemigo.getNombre());
        panel.columnas("PS " + jugador.getVida() + "/" + jugador.getVidaMaxima(),
                "PS " + enemigo.getVida() + "/" + enemigo.getVidaMaxima());
        panel.columnas(PanelConsola.barraVida(jugador.getVida(), jugador.getVidaMaxima()),
                PanelConsola.barraVida(enemigo.getVida(), enemigo.getVidaMaxima()));
    }

    // Ejecuta una acción válida; cancelar o introducir inválidos mantiene la elección.
    private ResultadoCombate ejecutarAccionJugador(Scanner entrada, PrintStream salida) {
        while (true) {
            PanelConsola panel = new PanelConsola(salida, "TU TURNO");
            datosCombatientes(panel);
            panel.separador();
            panel.titulo("ACCIONES");
            panel.opcion(1, "Atacar");
            if (jugador instanceof Jugador) {
                panel.opcion(2, "Usar objeto");
                panel.opcion(3, "Huir");
            }
            panel.cerrar();
            salida.print("Elige una opción > ");
            salida.flush();
            // El cierre de la entrada interrumpe la sesión; no representa una huida.
            String opcion = entrada.nextLine().trim();

            if (opcion.equals("1")) {
                ataque(jugador, enemigo, salida);
                return ResultadoCombate.CONTINUA;
            }

            if (opcion.equals("3") && jugador instanceof Jugador) {
                return intentarHuir(salida)
                        ? ResultadoCombate.JUGADOR_HUYE
                        : ResultadoCombate.CONTINUA;
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
        PanelConsola panel = new PanelConsola(salida, "USAR OBJETO");
        panel.linea("Vida actual: " + portador.getVida() + "/" + portador.getVidaMaxima() + " PS");
        int numero = 1;
        for (TipoPocion tipo : TipoPocion.values()) {
            panel.opcion(numero++, portador.getDefinicionPocion(tipo).nombre() + " (x"
                    + portador.getCantidadPociones(tipo) + ") +" + portador.getDefinicionPocion(tipo).recuperacionPS() + " PS");
        }
        panel.opcion(0, "Cancelar");
        panel.cerrar();
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
                        portador.getDefinicionPocion(tipo).nombre(), recuperacion, portador.getVida(), portador.getVidaMaxima());
                salida.printf("Unidades restantes de %s: %d%n", portador.getDefinicionPocion(tipo).nombre(), portador.getCantidadPociones(tipo));
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
