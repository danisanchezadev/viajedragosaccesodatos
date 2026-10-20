package org.viajedragos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.modelo.*;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

class DatosBalanceTest {
    static IntStream enemigos() { return IntStream.range(0,25); }
    static Stream<SimulacionBalance.Perfil> perfiles() { return IntStream.rangeClosed(1,5).boxed().flatMap(p->SimulacionBalance.perfiles(p).stream()); }

    @ParameterizedTest @MethodSource("enemigos")
    void datosAprobadosSinEscalarNiCambiarPSVelocidadNivelOIdentidad(int indice) {
        var anterior=new CatalogoAntesBalance().obtenerTodos().get(indice);
        var d=new RepositorioEnemigosMemoria().obtenerTodos().get(indice);
        int[] esperado=switch(d.id()) {
            case "capitan_barracones" -> new int[]{11,7,5,5,25};
            case "momia" -> new int[]{18,9,4,6,18};
            case "arana_cripta" -> new int[]{18,8,6,7,18};
            case "espectro" -> new int[]{19,9,6,7,20};
            case "necrofago" -> new int[]{20,10,5,6,22};
            case "sacerdote_oscuro" -> new int[]{20,9,6,7,24};
            case "golem_hierro" -> new int[]{26,14,5,7,26};
            case "salamandra" -> new int[]{25,12,8,9,24};
            case "herrero_maldito" -> new int[]{26,13,6,8,26};
            case "centinela_igneo" -> new int[]{27,13,7,9,28};
            case "demonio_forja" -> new int[]{28,14,8,8,30};
            case "caballero_abisal" -> new int[]{32,17,8,11,36};
            case "sombra_ancestral" -> new int[]{31,16,10,13,36};
            case "bestia_vacio" -> new int[]{33,17,9,12,40};
            case "hechicero_abisal" -> new int[]{32,16,10,13,40};
            case "guardian_abismo" -> new int[]{34,18,10,12,45};
            default -> new int[]{anterior.fuerza(),anterior.resistencia(),anterior.iniciativa(),anterior.agilidad(),anterior.experiencia()};
        };
        assertArrayEquals(esperado,new int[]{d.fuerza(),d.resistencia(),d.iniciativa(),d.agilidad(),d.experiencia()});
        assertEquals(anterior.id(),d.id()); assertEquals(anterior.nombre(),d.nombre()); assertEquals(anterior.descripcion(),d.descripcion());
        assertEquals(anterior.vidaMaxima(),d.vidaMaxima()); assertEquals(anterior.velocidad(),d.velocidad());
        assertEquals(anterior.nivel(),d.nivel()); assertEquals(anterior.planta(),d.planta()); assertEquals(anterior.guardian(),d.guardian());
        for(int nivel:new int[]{1,15}) {
            var j=new Jugador(); while(j.getNivel()<nivel) j.agregarExperiencia((int)j.getExperienciaNecesaria());
            var e=d.crearEnemigo(); new org.viajedragos.combate.Combate(j,e,()->4);
            assertArrayEquals(esperado,new int[]{e.getFuerza(),e.getResistencia(),e.getIniciativa(),e.getAgilidad(),e.getExperiencia()});
            assertEquals(d.vidaMaxima(),e.getVida()); assertEquals(d.nivel(),e.getNivel());
            assertTrue(e.getHabilidades().isEmpty()); assertTrue(e.getEquipoEquipado().isEmpty());
        }
    }

    @ParameterizedTest @MethodSource("perfiles")
    void equipoYMejorasDeLosPerfilesSonObteniblesConLosDatosReales(SimulacionBalance.Perfil perfil) {
        var objetos=new RepositorioObjetosMemoria(); var j=SimulacionBalance.crear(perfil);
        for(var pieza:j.getEquipoEquipado().values()) {
            var fuentes=objetos.obtenerBotinEquipamiento().stream().filter(d->d.equipo().id().equals(pieza.getIdDefinicion()))
                    .filter(d->d.plantaMinima()<=perfil.planta()).toList();
            boolean ofertaFija=pieza.getIdDefinicion().equals("espada_hierro");
            assertTrue(ofertaFija||!fuentes.isEmpty(),"No se usa equipo de plantas inaccesibles");
            if(!ofertaFija) assertTrue(fuentes.stream().anyMatch(d->{
                for(int p=d.plantaMinima();p<=Math.min(d.plantaMaxima(),perfil.planta());p++) {
                    int maximo=new int[]{1,2,2,3,3}[p-1];
                    if(maximo>=perfil.mejorasPorPieza()) return true;
                }
                return false;
            }));
            assertTrue(pieza.getMejorasConsumidas()<=3);
        }
        assertEquals(perfil.nivel(),j.getNivel()); assertEquals(0,j.getPuntosPendientes());
        assertEquals(perfil.pequenas(),j.getCantidadPociones(TipoPocion.PEQUENA));
        assertEquals(perfil.grandes(),j.getCantidadPociones(TipoPocion.GRANDE));
    }

    @Test void experienciaNuevaPermiteVariasSubidasConSobranteSinCambiarIncrementos() {
        var abismo=new RepositorioEnemigosMemoria().obtenerTodos().getLast();
        assertEquals(45,abismo.crearEnemigo().getExperiencia());
        var j=new Jugador("Prueba","",6,2,3,2,2,48,48);
        assertEquals(1,j.agregarExperiencia(45)); assertEquals(2,j.getNivel()); assertEquals(25,j.getExperiencia());
        assertEquals(2,j.agregarExperiencia(45)); assertEquals(4,j.getNivel()); assertEquals(0,j.getExperiencia());
        assertEquals(50,j.getExperienciaNecesaria()); assertEquals(6,j.getPuntosPendientes());
        assertEquals(9,j.getFuerza()); assertEquals(5,j.getResistencia()); assertEquals(66,j.getVida()); assertEquals(66,j.getVidaMaxima());
        j.agregarExperiencia(18); assertEquals(18,j.getExperiencia()); assertEquals(4,j.getNivel());
    }

    @Test void decisionesDePocionesYDadosReproducenExactamenteElEncuentro() {
        var perfil=SimulacionBalance.perfiles(2).get(1);
        var d=new RepositorioEnemigosMemoria().obtenerTodos().stream().filter(DefinicionEnemigo::guardian).findFirst().orElseThrow();
        var a=SimulacionBalance.crear(perfil); var b=SimulacionBalance.crear(perfil);
        assertEquals(SimulacionBalance.combatir(a,d,20261027L),SimulacionBalance.combatir(b,d,20261027L));
        assertArrayEquals(new int[]{a.getVida(),a.turnos,a.danio,a.pequenas,a.grandes},new int[]{b.getVida(),b.turnos,b.danio,b.pequenas,b.grandes});
    }
}
