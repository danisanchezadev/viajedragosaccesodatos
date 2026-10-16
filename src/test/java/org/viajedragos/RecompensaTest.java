package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.viajedragos.arma.Arma;
import org.viajedragos.arma.Atributo;
import org.viajedragos.arma.Equipo;
import org.viajedragos.arma.TipoEquipo;
import org.viajedragos.combate.Combate;
import org.viajedragos.combate.ContextoAtaque;
import org.viajedragos.combate.TiradorDado;
import org.viajedragos.habilidad.EstadoHabilidad;
import org.viajedragos.habilidad.Habilidad;
import org.viajedragos.habilidad.implementacion.Impetu;
import org.viajedragos.habilidad.implementacion.InstintoSupervivencia;
import org.viajedragos.habilidad.implementacion.PrimerImpulso;
import org.viajedragos.habilidad.implementacion.Rabia;
import org.viajedragos.modelo.Enemigo;
import org.viajedragos.modelo.Jugador;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.*;

class RecompensaTest {
    private static final int[] VICTORIA = {1, 1, 1, 1, 6, 1, 6};
    private static final int[] VICTORIA_CON_VIDA_6 = {1, 1, 1, 6, 6, 1, 6, 6, 1, 6};
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();

    private Jugador jugador() {
        return new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 24, 24);
    }

    private PrintStream consola() {
        return new PrintStream(salida, true, StandardCharsets.UTF_8);
    }

    private String texto() {
        return salida.toString(StandardCharsets.UTF_8);
    }

    private long veces(String fragmento) {
        return texto().lines().filter(linea -> linea.contains(fragmento)).count();
    }

    private boolean jugar(Jugador j, String entrada, Dados dados) {
        boolean volver = assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.jugarAventura(j, new Scanner(entrada), consola(), dados));
        dados.comprobarConsumo();
        return volver;
    }

    @Test
    void primeraVictoriaOfreceEspadaDespuesDelCombateSinEquipoInicial() {
        Jugador j = jugador();
        Dados dados = new Dados(VICTORIA);
        dados.antes = indice -> {
            assertTrue(j.getEquipoEquipado().isEmpty());
            assertTrue(j.getHabilidades().isEmpty());
            assertEquals(6, j.getFuerza());
            assertFalse(texto().contains("Has encontrado"));
        };

        assertFalse(jugar(j, "1\n1\n0\n", dados));
        Equipo espada = j.getEquipoEquipado().get(TipoEquipo.ARMA);
        assertInstanceOf(Arma.class, espada);
        assertEquals("Espada desgastada", espada.getNombre());
        assertEquals(Map.of(Atributo.FUERZA, 1), espada.getBonificaciones());
        assertTrue(espada.getHabilidadesOtorgadas().isEmpty());
        assertTrue(j.getHabilidades().isEmpty());
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(7, j.getFuerza());
        assertEquals(24, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertTrue(texto().indexOf("Victoria.") < texto().indexOf("Has encontrado"));
        assertTrue(texto().contains("Fuerza +1"));
        assertTrue(texto().indexOf("Espada desgastada equipada")
                < texto().indexOf("1. Continuar al siguiente encuentro"));
    }

    @Test
    void espadaPersisteTresEncuentrosSinAcumularseNiVolverAOfrecerse() {
        Jugador j = jugador();
        Equipo[] espada = {null};
        Dados dados = new Dados(VICTORIA_CON_VIDA_6, VICTORIA, VICTORIA);
        dados.antes = indice -> {
            if (indice == 10 || indice == 17) {
                Equipo actual = j.getEquipoEquipado().get(TipoEquipo.ARMA);
                assertNotNull(actual);
                if (espada[0] == null) espada[0] = actual;
                else assertSame(espada[0], actual);
                assertEquals(1, j.getEquipoEquipado().size());
                assertEquals(1, j.getModificadoresEquipo().get(Atributo.FUERZA));
                assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
                assertEquals(7, j.getFuerza());
                assertEquals(6, j.getVida());
            }
        };

        assertFalse(jugar(j, "1\n1\n1\n1\n1\n1\n0\n", dados));
        assertEquals(3, veces("Victoria."));
        assertEquals(1, veces("Has encontrado una Espada desgastada"));
        assertEquals(1, veces("1. Equipar Espada desgastada"));
        assertSame(espada[0], j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(7, j.getFuerza());
        assertEquals(6, j.getVida());
    }

    @Test
    void rechazoNoAlteraPersonajeNiPermiteRecuperarLaEspadaDespues() {
        Jugador j = jugador();
        Map<Atributo, Integer> modificadores = new EnumMap<>(j.getModificadoresEquipo());
        assertFalse(jugar(j, "1\n2\n1\n1\n0\n", new Dados(VICTORIA, VICTORIA)));
        assertEquals(2, veces("Victoria."));
        assertEquals(1, veces("Has encontrado una Espada desgastada"));
        assertEquals(1, veces("2. Rechazar Espada desgastada"));
        assertFalse(texto().contains("Espada desgastada equipada"));
        assertEquals(modificadores, j.getModificadoresEquipo());
        assertEquals(6, j.getFuerza());
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(2, j.getResistencia());
        assertEquals(2, j.getAgilidad());
        assertEquals(3, j.getIniciativa());
        assertEquals(2, j.getVelocidad());
        assertEquals(24, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertTrue(j.getHabilidades().isEmpty());
    }

    @Test
    void invalidosRepitenEleccionYEquiparSeInvocaUnaSolaVezSinDadosExtra() {
        int[] llamadas = {0};
        Jugador j = new Jugador("Aventurero", "", 6, 2, 3, 2, 2, 24, 24) {
            @Override
            public boolean equipar(Equipo equipo) {
                llamadas[0]++;
                return super.equipar(equipo);
            }
        };
        assertFalse(jugar(j, "1\nx\n0\n\n 1 \n0\n", new Dados(VICTORIA)));
        assertEquals(3, veces("Entrada inválida"));
        assertEquals(1, veces("Has encontrado una Espada desgastada"));
        assertEquals(1, veces("Comienza el combate"));
        assertEquals(1, llamadas[0]);
        assertEquals(7, j.getFuerza());
        assertEquals(1, j.getModificadoresEquipo().get(Atributo.FUERZA));
    }

    @Test
    void cierreDeEntradaEnRecompensaTerminaSesionSinEquipar() {
        Jugador j = jugador();
        assertFalse(jugar(j, "1\n", new Dados(VICTORIA)));
        assertTrue(texto().contains("Victoria."));
        assertTrue(texto().contains("Entrada finalizada. Hasta pronto."));
        assertFalse(texto().contains("1. Continuar al siguiente encuentro"));
        assertEquals(1, veces("Comienza el combate"));
        assertTrue(j.getEquipoEquipado().isEmpty());
        assertEquals(6, j.getFuerza());
    }

    @Test
    void derrotaNoOfreceRecompensa() {
        Jugador j = jugador();
        j.setVida(5);
        assertTrue(jugar(j, "", new Dados(new int[]{1, 1, 1, 6, 6, 1, 6})));
        assertTrue(texto().contains("Derrota."));
        assertFalse(texto().contains("Espada desgastada"));
        assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @Test
    void muerteSimultaneaNoOfreceRecompensa() {
        Jugador j = jugador();
        j.setVida(5);
        j.aprenderHabilidad(new Habilidad("Contingencia exclusiva de prueba") {
            @Override
            public void alRecibirDanio(ContextoAtaque contexto, EstadoHabilidad estado) {
                contexto.getAtacante().recibirDanio(contexto.getAtacante().getVida());
            }
        }, 1);
        assertTrue(jugar(j, "", new Dados(new int[]{1, 1, 1, 6, 6, 1, 6})));
        assertTrue(texto().contains("Ambos han muerto."));
        assertFalse(texto().contains("Espada desgastada"));
        assertTrue(j.getEquipoEquipado().isEmpty());
    }

    @Test
    void aventuraNuevaTrasEquiparEmpiezaSinLaBonificacionAnterior() {
        // Segundo personaje: ataque normal 4/2 causa 7, no los 8 que daría la espada.
        Dados dados = new Dados(VICTORIA, new int[]{1, 1, 1, 1, 4, 2, 1, 2, 1, 6, 4, 1});
        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                app.ejecutar(new Scanner("1\n1\n1\n2\n1\n1\n1\n2\n0\n"), consola(), dados));
        dados.comprobarConsumo();
        assertEquals(2, veces("Encuentro 1."));
        assertEquals(2, veces("Has encontrado una Espada desgastada"));
        assertEquals(1, veces("Espada desgastada equipada"));
        String nueva = texto().substring(texto().lastIndexOf("Encuentro 1."));
        assertTrue(nueva.contains("Aventurero: 24/24 de vida | Lobo: 18/18 de vida"));
        assertTrue(nueva.contains("Aventurero golpea a Lobo y causa 7 puntos de daño"));
        assertFalse(nueva.contains("Espada desgastada equipada"));
    }

    @Test
    void espadaObtenidaPorLaOfertaModificaElAtaqueRealDeSieteAOcho() {
        Jugador sinEspada = jugador();
        Jugador conEspada = jugador();
        assertTrue(app.ofrecerEspada(conEspada, new Scanner("1\n"), consola()));
        Enemigo loboSin = new Enemigo("Lobo", "", 5, 2, 2, 1, 2, 18, 18);
        Enemigo loboCon = new Enemigo("Lobo", "", 5, 2, 2, 1, 2, 18, 18);
        Dados dadosSin = new Dados(new int[]{4, 2});
        Dados dadosCon = new Dados(new int[]{4, 2});

        new Combate(sinEspada, loboSin, dadosSin).ataque(sinEspada, loboSin);
        new Combate(conEspada, loboCon, dadosCon).ataque(conEspada, loboCon);

        dadosSin.comprobarConsumo();
        dadosCon.comprobarConsumo();
        assertEquals(11, loboSin.getVida());
        assertEquals(10, loboCon.getVida());
        assertEquals(6, conEspada.getAtributoBase(Atributo.FUERZA));
        assertEquals(7, conEspada.getFuerza());
    }

    @Test
    void limpiezaTemporalConservaLaEspadaRecibidaEntreEncuentros() {
        Jugador j = jugador();
        j.setVida(6);
        j.aprenderHabilidad(new Rabia(), 1);
        j.aprenderHabilidad(new PrimerImpulso(3), 3);
        j.aprenderHabilidad(new Impetu(), 1);
        j.aprenderHabilidad(new InstintoSupervivencia(), 1);
        Map<Habilidad, EstadoHabilidad> estados = Map.copyOf(j.getHabilidades());
        Dados dados = new Dados(VICTORIA, VICTORIA);
        dados.antes = indice -> {
            if (indice == 0) {
                assertTrue(j.getEquipoEquipado().isEmpty());
                assertEquals(11, j.getFuerza());
            }
            if (indice == 7) {
                assertEquals(12, j.getFuerza());
                assertEquals(1, j.getModificadoresEquipo().get(Atributo.FUERZA));
                assertEquals(3, j.getAgilidad());
                assertEquals(6, j.getVida());
            }
        };
        assertFalse(jugar(j, "1\n1\n1\n1\n0\n", dados));
        assertEquals(6, j.getAtributoBase(Atributo.FUERZA));
        assertEquals(7, j.getFuerza());
        assertEquals(2, j.getAgilidad());
        assertEquals(2, j.getResistencia());
        assertEquals(3, j.getIniciativa());
        assertEquals(6, j.getVida());
        assertEquals(24, j.getVidaMaxima());
        assertEquals("Espada desgastada", j.getEquipoEquipado().get(TipoEquipo.ARMA).getNombre());
        assertEquals(estados.size(), j.getHabilidades().size());
        estados.forEach((habilidad, estado) -> assertSame(estado, j.getHabilidades().get(habilidad)));
    }

    @Test
    void ranuraOcupadaNoSustituyeElEquipoNiAnunciaExito() {
        Jugador j = jugador();
        Arma anterior = new Arma("Arma de prueba", TipoEquipo.ARMA, Map.of(Atributo.FUERZA, 5), Map.of());
        assertTrue(j.equipar(anterior));
        assertTrue(app.ofrecerEspada(j, new Scanner("1\n"), consola()));
        assertSame(anterior, j.getEquipoEquipado().get(TipoEquipo.ARMA));
        assertEquals(11, j.getFuerza());
        assertTrue(texto().contains("ranura de arma está ocupada"));
        assertFalse(texto().contains("Espada desgastada equipada"));
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
            assertTrue(usadas < valores.length, "Tirada o encuentro inesperado");
            antes.accept(usadas);
            return valores[usadas++];
        }

        private void comprobarConsumo() {
            assertEquals(valores.length, usadas, "Consumo exacto de dados");
        }
    }
}
