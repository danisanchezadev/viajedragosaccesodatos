package org.viajedragos;

import org.viajedragos.combate.Combate;
import org.viajedragos.combate.Dado;
import org.viajedragos.combate.ResultadoCombate;
import org.viajedragos.combate.TiradorDado;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class app {
    public static void main(String[] args) {
        ejecutar(new Scanner(System.in), System.out, new Dado());
    }

    public static void ejecutar(Scanner entrada, PrintStream salida, TiradorDado dados) {
        salida.println("=== Viaje a Dragos ===");
        salida.println("Un combate por turnos. La iniciativa decide quién actúa primero.");
        boolean primerCombate = true;
        while (true) {
            salida.println(primerCombate ? "1. Iniciar combate" : "1. Nuevo combate");
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
            // Cada combate utiliza personajes nuevos, sin conservar daño ni efectos.
            Jugador jugador = new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 24, 24);
            Enemigo enemigo = new Enemigo("Lobo", "", 5, 2, 2, 1, 2, 18, 18);
            salida.println("Comienza el combate: Aventurero contra Lobo.");
            ResultadoCombate resultado;
            try {
                resultado = new Combate(jugador, enemigo, dados).combatir(entrada, salida);
            } catch (NoSuchElementException finEntrada) {
                salida.println("Entrada finalizada. Sesión interrumpida; el combate queda sin terminar.");
                return;
            }
            salida.printf("Vida final: %s %d/%d | %s %d/%d%n",
                    jugador.getNombre(), jugador.getVida(), jugador.getVidaMaxima(),
                    enemigo.getNombre(), enemigo.getVida(), enemigo.getVidaMaxima());
            mostrarResultado(resultado, salida);
            primerCombate = false;
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
