package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.viajedragos.catalogo.RepositorioEnemigos;
import org.viajedragos.modelo.DefinicionEnemigo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.combate.TiradorDado;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.modelo.Combatiente;
import org.viajedragos.modelo.Jugador;
import org.viajedragos.modelo.TipoPocion;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.*;

class BotinPocionesTest {
    private RepositorioEnemigos soloLobo() {
        return () -> java.util.List.of(new DefinicionEnemigo("lobo", "Lobo", "", 5, 2, 2, 1, 2, 18));
    }

    private static final int[] VICTORIA = {1, 1, 1, 1, 6, 1, 6};
    private static final int[] VICTORIA_CON_VIDA_6 = {1, 1, 1, 6, 6, 1, 6, 6, 1, 6};
    private static final int[] ENEMIGO_PRIMERO_LETAL = {1, 1, 1, 6, 6, 1, 6};
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();

    private Jugador jugador() {
        return new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 24, 24);
    }

    private PrintStream consola() {
        return new PrintStream(salida, true, StandardCharsets.UTF_8);
    }

    private String texto() {
        return org.viajedragos.TextoConsola.contenido(salida.toString(StandardCharsets.UTF_8));
    }

    private long veces(String fragmento) {
        return texto().lines().filter(linea -> linea.contains(fragmento)).count();
    }

    private boolean jugar(Jugador j, String entrada, Dados dados, AzarBotin azar) {
        boolean volver = assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.jugarAventura(j, new Scanner(entrada), consola(), dados, azar, soloLobo(), new Random(0)));
        dados.comprobarConsumo();
        return volver;
    }

    @ParameterizedTest
    @CsvSource({"0,0,1,1", "49,19,1,1", "49,20,1,0", "50,19,0,1",
            "50,20,0,0", "99,99,0,0", "0,99,1,0", "99,0,0,1"})
    void dosTiradasIndependientesRespetanLosLimitesYLosCuatroResultados(
            int tiradaPequena, int tiradaGrande, int pequenas, int grandes) {
        Jugador j = jugador();
        j.setVida(3);
        AzarBotin azar = new AzarBotin(tiradaPequena, tiradaGrande);
        app.obtenerBotin(j, azar, consola());
        azar.comprobarConsumo();
        assertEquals(pequenas, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(grandes, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals(pequenas, veces("Botín: Poción pequeña (+1)."));
        assertEquals(grandes, veces("Botín: Poción grande (+1)."));
        assertEquals(3, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertEquals(6, j.getFuerza());
    }

    @Test
    void enumeracionCompletaDeLasDiezMilParejasDaLasProbabilidadesExactas() {
        int[] resultados = new int[4];
        // Enumeración determinista, no un muestreo estadístico ni un archivo.
        try (PrintStream sinSalida = new PrintStream(OutputStream.nullOutputStream())) {
            for (int pequena = 0; pequena < 100; pequena++) {
                for (int grande = 0; grande < 100; grande++) {
                    Jugador j = jugador();
                    AzarBotin azar = new AzarBotin(pequena, grande);
                    app.obtenerBotin(j, azar, sinSalida);
                    azar.comprobarConsumo();
                    int indice = j.getCantidadPociones(TipoPocion.PEQUENA)
                            + 2 * j.getCantidadPociones(TipoPocion.GRANDE);
                    resultados[indice]++;
                }
            }
        }
        // Índices: ninguna, pequeña, grande, ambas.
        assertArrayEquals(new int[]{4000, 4000, 1000, 1000}, resultados);
        assertEquals(5000, resultados[1] + resultados[3]);
        assertEquals(2000, resultados[2] + resultados[3]);
    }

    @Test
    void acumulaEnTresVictoriasSinReiniciarElGeneradorNiConsumirDadosExtra() {
        Jugador j = jugador();
        AzarBotin azar = new AzarBotin(0, 0, 49, 19, 0, 20);
        Dados dados = new Dados(VICTORIA_CON_VIDA_6, VICTORIA, VICTORIA);
        dados.antes = indice -> {
            if (indice == 0) assertTrue(j.getInventarioPociones().isEmpty());
            if (indice == 10 || indice == 17) {
                int victoriasPrevias = indice == 10 ? 1 : 2;
                assertEquals(victoriasPrevias, j.getCantidadPociones(TipoPocion.PEQUENA));
                assertEquals(victoriasPrevias, j.getCantidadPociones(TipoPocion.GRANDE));
                assertEquals(victoriasPrevias * 2, azar.usadas);
                assertEquals(6, j.getVida());
                assertEquals(7, j.getFuerza());
            }
        };
        assertFalse(jugar(j, "1\n1\n3\n1\n2\n3\n1\n0\n", dados, azar));
        azar.comprobarConsumo();
        assertEquals(3, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(2, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals(6, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertEquals(3, veces("Victoria."));
        assertEquals(1, veces("Has encontrado una Espada desgastada"));
    }

    @Test
    void derrotaNoObtieneBotinNiConsultaSuGenerador() {
        Jugador j = jugador();
        j.setVida(5);
        AzarBotin azar = new AzarBotin();
        assertTrue(jugar(j, "", new Dados(ENEMIGO_PRIMERO_LETAL), azar));
        azar.comprobarConsumo();
        assertTrue(j.getInventarioPociones().isEmpty());
        assertFalse(texto().contains("Botín:"));
        assertFalse(texto().contains("Espada desgastada"));
    }

    @Test
    void muerteSimultaneaNoObtieneBotinNiConsultaSuGenerador() {
        Jugador j = jugador();
        j.setVida(5);
        j.aprenderHabilidad(new Habilidad("Contingencia exclusiva de prueba") {
            @Override
            public void alRecibirDanio(ContextoAtaque contexto, EstadoHabilidad estado) {
                contexto.getAtacante().recibirDanio(contexto.getAtacante().getVida());
            }
        }, 1);
        AzarBotin azar = new AzarBotin();
        assertTrue(jugar(j, "", new Dados(ENEMIGO_PRIMERO_LETAL), azar));
        azar.comprobarConsumo();
        assertTrue(texto().contains("Ambos han muerto."));
        assertTrue(j.getInventarioPociones().isEmpty());
        assertFalse(texto().contains("Botín:"));
    }

    @Test
    void combateInterrumpidoNoGeneraBotin() {
        Jugador j = jugador();
        AzarBotin azar = new AzarBotin();
        assertFalse(jugar(j, "", new Dados(new int[]{1, 1, 1, 1}), azar));
        azar.comprobarConsumo();
        assertTrue(texto().contains("combate queda sin terminar"));
        assertTrue(j.getInventarioPociones().isEmpty());
    }

    @Test
    void resurreccionDelEnemigoNoGeneraBotinHastaLaMuerteDefinitiva() {
        Jugador j = jugador();
        int[] muertes = {0};
        j.aprenderHabilidad(new Habilidad("Preparación exclusiva de prueba") {
            @Override
            public void alInicioCombate(Combatiente portador, Combatiente oponente, EstadoHabilidad estado) {
                oponente.aprenderHabilidad(new Habilidad("Resurrección exclusiva de prueba") {
                    @Override
                    public void alMorir(Combatiente p, Combatiente o, EstadoHabilidad e) {
                        if (++muertes[0] == 1) p.curarse(18);
                    }
                }, 1);
            }
        }, 1);
        AzarBotin azar = new AzarBotin(0, 0);
        Dados dados = new Dados(new int[]{1, 1, 1, 1, 6, 1, 6, 1, 2, 1, 6, 6, 1, 6});
        dados.antes = indice -> {
            assertEquals(0, azar.usadas);
            assertTrue(j.getInventarioPociones().isEmpty());
        };
        assertFalse(jugar(j, "1\n1\n2\n0\n", dados, azar));
        azar.comprobarConsumo();
        assertEquals(2, muertes[0]);
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(1, j.getCantidadPociones(TipoPocion.GRANDE));
    }

    @Test
    void repetirOpcionesInvalidasNoDuplicaBotinNiTiradas() {
        Jugador j = jugador();
        AzarBotin azar = new AzarBotin(0, 0);
        assertFalse(jugar(j, "1\nx\n0\n2\nx\n9\n0\n", new Dados(VICTORIA), azar));
        azar.comprobarConsumo();
        assertEquals(4, veces("Entrada inválida"));
        assertEquals(1, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(1, j.getCantidadPociones(TipoPocion.GRANDE));
        assertEquals(1, veces("Botín: Poción pequeña (+1)."));
        assertEquals(1, veces("Botín: Poción grande (+1)."));
    }

    @Test
    void unaSesionConDosAventurasComparteElGeneradorDeBotinSinReiniciarlo() {
        AzarBotin azar = new AzarBotin(0, 99, 99, 0);
        Dados dados = new Dados(VICTORIA, VICTORIA);
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> app.ejecutar(
                new Scanner("1\n1\n2\n4\n1\n1\n2\n0\n"), consola(), dados, azar, soloLobo(), new Random(0)));
        dados.comprobarConsumo();
        azar.comprobarConsumo();
        String primera = texto().substring(0, texto().lastIndexOf("ENCUENTRO 1"));
        String segunda = texto().substring(texto().lastIndexOf("ENCUENTRO 1"));
        assertTrue(primera.contains("Botín: Poción pequeña"));
        assertFalse(primera.contains("Botín: Poción grande"));
        assertFalse(segunda.contains("Botín: Poción pequeña"));
        assertTrue(segunda.contains("Botín: Poción grande"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void siguienteAventuraEmpiezaVaciaTrasTerminarOMorir(boolean morir) {
        Jugador anterior = jugador();
        AzarBotin azar = new AzarBotin(0, 0, 99, 99);
        Dados previos = morir ? new Dados(VICTORIA_CON_VIDA_6, ENEMIGO_PRIMERO_LETAL)
                : new Dados(VICTORIA_CON_VIDA_6);
        assertTrue(jugar(anterior, morir ? "1\n2\n3\n" : "1\n2\n4\n", previos, azar));

        // Igual que ejecutar: al volver al inicio, la siguiente aventura construye otro jugador.
        Jugador nuevo = jugador();
        assertNotSame(anterior, nuevo);
        assertTrue(nuevo.getInventarioPociones().isEmpty());
        Dados siguientes = new Dados(VICTORIA);
        siguientes.antes = indice -> assertTrue(nuevo.getInventarioPociones().isEmpty());
        assertFalse(jugar(nuevo, "1\n2\n0\n", siguientes, azar));
        azar.comprobarConsumo();
        assertTrue(nuevo.getInventarioPociones().isEmpty());
        assertEquals(24, nuevo.getVida());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void botinNoAlteraLaOfertaUnicaDeEspadaNiSuAceptacionORechazo(boolean equipar) {
        Jugador j = jugador();
        AzarBotin azar = new AzarBotin(0, 0, 0, 0);
        assertFalse(jugar(j, "1\n" + (equipar ? "1" : "2") + "\n3\n1\n2\n0\n",
                new Dados(VICTORIA, VICTORIA), azar));
        azar.comprobarConsumo();
        assertEquals(1, veces("Has encontrado una Espada desgastada"));
        assertEquals(equipar, j.getEquipoEquipado().containsKey(TipoEquipo.ARMA));
        assertEquals(equipar ? 7 : 6, j.getFuerza());
        assertEquals(24, j.getVida());
        assertEquals(2, j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(2, j.getCantidadPociones(TipoPocion.GRANDE));
        assertTrue(texto().indexOf("Victoria.") < texto().indexOf("Botín:"));
        assertTrue(texto().indexOf("Botín:") < texto().indexOf("Has encontrado"));
    }

    @Test
    void cerrarEntradaEnLaOfertaDeEspadaNoRepiteElBotinYaObtenido() {
        Jugador j = jugador();
        AzarBotin azar = new AzarBotin(0, 0);
        assertFalse(jugar(j, "1\n", new Dados(VICTORIA), azar));
        azar.comprobarConsumo();
        assertEquals(1, veces("Victoria."));
        assertEquals(1, veces("Botín: Poción pequeña (+1)."));
        assertEquals(1, veces("Botín: Poción grande (+1)."));
        assertEquals(1, veces("Comienza el combate"));
        assertFalse(texto().contains("3. Continuar al siguiente encuentro"));
    }

    private static class AzarBotin extends Random {
        private final int[] valores;
        private int usadas;

        private AzarBotin(int... valores) {
            super(0);
            this.valores = valores;
        }

        @Override
        public int nextInt(int limite) {
            assertEquals(100, limite, "El botín usa 100 resultados equiprobables");
            assertTrue(usadas < valores.length, "Comprobación de botín inesperada");
            return valores[usadas++];
        }

        private void comprobarConsumo() {
            assertEquals(valores.length, usadas, "Dos comprobaciones por victoria definitiva");
        }
    }

    private static class Dados implements TiradorDado {
        private final int[] valores;
        private int usadas;
        private IntConsumer antes = indice -> {};

        private Dados(int[]... secuencias) {
            valores = Arrays.stream(secuencias).flatMapToInt(Arrays::stream).toArray();
        }

        @Override
        public int tirarDado() {
            assertTrue(usadas < valores.length, "Dado de combate adicional o encuentro inesperado");
            antes.accept(usadas);
            return valores[usadas++];
        }

        private void comprobarConsumo() {
            assertEquals(valores.length, usadas, "El botín no consume dados de combate");
        }
    }
}
