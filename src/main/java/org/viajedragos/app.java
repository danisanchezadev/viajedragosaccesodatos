package org.viajedragos;

import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.combate.Combate;
import org.viajedragos.combate.Dado;
import org.viajedragos.combate.ResultadoCombate;
import org.viajedragos.combate.TiradorDado;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Scanner;

public class app {
    public static void main(String[] args) {
        ejecutar(new Scanner(System.in), System.out, new Dado());
    }

    public static void ejecutar(Scanner entrada, PrintStream salida, TiradorDado dados) {
        salida.println("=== Viaje a Dragos ===");
        salida.println("Un combate por turnos. La iniciativa decide quién actúa primero.");
        while (true) {
            salida.println("1. Nueva aventura");
            salida.println("0. Salir");
            salida.print("> ");
            salida.flush();
            if (!entrada.hasNextLine()) {
                salida.println("Entrada finalizada. Hasta pronto.");
                return;
            }
            String opcion = entrada.nextLine().trim();
            if (opcion.equals("0")) {
                salida.println("Hasta pronto.");
                return;
            }
            if (!opcion.equals("1")) {
                salida.println("Entrada inválida. Escribe 1 o 0.");
                continue;
            }
            // Un jugador por aventura; continuar nunca reconstruye este personaje.
            Jugador jugador = new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 24, 24);
            if (!jugarAventura(jugador, entrada, salida, dados)) return;
        }
    }

    // Devuelve true para volver al menú inicial y false para terminar la sesión.
    static boolean jugarAventura(Jugador jugador, Scanner entrada, PrintStream salida, TiradorDado dados) {
        int encuentro = 1;
        while (true) {
            Enemigo enemigo = new Enemigo("Lobo", "", 5, 2, 2, 1, 2, 18, 18);
            salida.printf("%nEncuentro %d.%n", encuentro);
            salida.printf("Comienza el combate: %s contra %s.%n", jugador.getNombre(), enemigo.getNombre());
            ResultadoCombate resultado;
            try {
                resultado = new Combate(jugador, enemigo, dados).combatir(entrada, salida);
            } catch (NoSuchElementException finEntrada) {
                salida.println("Entrada finalizada. Sesión interrumpida; el combate queda sin terminar.");
                return false;
            }
            salida.printf("Vida final: %s %d/%d | %s %d/%d%n",
                    jugador.getNombre(), jugador.getVida(), jugador.getVidaMaxima(),
                    enemigo.getNombre(), enemigo.getVida(), enemigo.getVidaMaxima());
            mostrarResultado(resultado, salida);
            if (resultado == ResultadoCombate.JUGADOR_MUERE || resultado == ResultadoCombate.AMBOS_MUEREN) {
                salida.println("La aventura ha terminado.");
                return true;
            }

            if (resultado == ResultadoCombate.ENEMIGO_MUERE && encuentro == 1
                    && !ofrecerEspada(jugador, entrada, salida)) return false;

            while (true) {
                salida.println("1. Continuar al siguiente encuentro");
                salida.println("2. Terminar aventura y volver al menú inicial");
                salida.println("0. Salir del programa");
                salida.print("> ");
                salida.flush();
                if (!entrada.hasNextLine()) {
                    salida.println("Entrada finalizada. Hasta pronto.");
                    return false;
                }
                String opcion = entrada.nextLine().trim();
                if (opcion.equals("0")) {
                    salida.println("Hasta pronto.");
                    return false;
                }
                if (opcion.equals("2")) {
                    salida.println("La aventura ha terminado.");
                    return true;
                }
                if (opcion.equals("1")) break;
                salida.println("Entrada inválida. Escribe 1, 2 o 0.");
            }
            encuentro++;
        }
    }

    // La pieza se crea una sola vez por oferta. false indica cierre de entrada.
    static boolean ofrecerEspada(Jugador jugador, Scanner entrada, PrintStream salida) {
        Arma espada = new Arma("Espada desgastada", TipoEquipo.ARMA,
                Map.of(Atributo.FUERZA, 1), Map.of());
        salida.printf("Has encontrado una %s.%nFuerza %+d%n",
                espada.getNombre(), espada.getBonificaciones().get(Atributo.FUERZA));
        while (true) {
            salida.printf("1. Equipar %s%n", espada.getNombre());
            salida.printf("2. Rechazar %s%n", espada.getNombre());
            salida.print("> ");
            salida.flush();
            if (!entrada.hasNextLine()) {
                salida.println("Entrada finalizada. Hasta pronto.");
                return false;
            }
            String opcion = entrada.nextLine().trim();
            if (opcion.equals("1")) {
                if (jugador.equipar(espada)) {
                    salida.printf("%s equipada. Fuerza actual: %d.%n", espada.getNombre(), jugador.getFuerza());
                } else {
                    salida.println("No se puede equipar: la ranura de arma está ocupada.");
                }
                return true;
            }
            if (opcion.equals("2")) {
                salida.printf("Has rechazado la %s. La dejas atrás.%n", espada.getNombre());
                return true;
            }
            salida.println("Entrada inválida. Escribe 1 o 2.");
        }
    }

    static void mostrarResultado(ResultadoCombate resultado, PrintStream salida) {
        salida.println(switch (resultado) {
            case ENEMIGO_MUERE -> "Victoria. " + resultado.getDescripcion();
            case JUGADOR_MUERE -> "Derrota. " + resultado.getDescripcion();
            case AMBOS_MUEREN -> "Ambos han muerto. " + resultado.getDescripcion();
            default -> resultado.getDescripcion();
        });
    }
}
