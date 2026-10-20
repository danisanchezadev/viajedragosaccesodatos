package org.viajedragos;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.viajedragos.catalogo.RepositorioEnemigosMemoria;
import static org.junit.jupiter.api.Assertions.*;

class BalanceProgresionTest {
    @ParameterizedTest @ValueSource(ints={1,2,3,4,5})
    void compararPreparacionAisladaYExpedicionesConMotorReal(int planta) {
        for(var perfil:SimulacionBalance.perfiles(planta)) {
            var personaje=SimulacionBalance.crear(perfil);
            System.out.printf("PERFIL P%d %s | %d | %d/%d/%d/%d/%d | %d | %d/%d | %s | %d | %s%n",
                    planta,perfil.nombre(),perfil.nivel(),personaje.getFuerza(),personaje.getResistencia(),personaje.getIniciativa(),
                    personaje.getAgilidad(),personaje.getVelocidad(),personaje.getVidaMaxima(),perfil.pequenas(),perfil.grandes(),
                    String.join(", ",perfil.equipo()),perfil.mejorasPorPieza(),perfil.habilidades()?"Instinto + Sed de batalla, Nv.1":"ninguna");
            for(boolean antes:new boolean[]{true,false}) {
                var catalogo=(antes?new CatalogoAntesBalance().obtenerTodos():new RepositorioEnemigosMemoria().obtenerTodos())
                        .stream().filter(d->d.planta()==planta).toList();
                String etiqueta=(antes?"ANTES":"ACTUAL")+" P"+planta+" "+perfil.nombre();
                var aislado=SimulacionBalance.aislados(perfil,catalogo,100,etiqueta);
                var expedicion=SimulacionBalance.expediciones(perfil,catalogo,100,antes?5:org.viajedragos.modelo.Planta.porNumero(planta).oroVictoria());
                System.out.println(aislado.fila(etiqueta+" aislado"));
                System.out.println(expedicion.fila(etiqueta+" expedicion"));
                assertEquals(500,aislado.muestras); assertEquals(100,expedicion.muestras);
                assertTrue(expedicion.combates<=400); assertTrue(expedicion.combates>=100);
                if(!antes && perfil.nombre().equals("B")) {
                    // Regresión exacta de esta muestra, no umbrales impuestos a todos los jugadores.
                    assertEquals(new int[]{500,455,394,365,313}[planta-1],aislado.victorias);
                    assertEquals(new int[]{100,91,39,31,14}[planta-1],expedicion.supervivientes);
                }
                if(planta==2) {
                    var guardian=catalogo.stream().filter(d->d.guardian()).toList();
                    System.out.println(SimulacionBalance.aislados(perfil,guardian,200).fila(etiqueta+" capitan"));
                }
            }
        }
    }

    @ParameterizedTest @ValueSource(ints={1,2,3,4,5})
    void mismoVeteranoPuedeElegirPlantaSinEscaladoNiPenalizacionAlEntrenar(int planta) {
        var veterano=SimulacionBalance.perfiles(5).get(2);
        var perfil=new SimulacionBalance.Perfil(planta,"veterano",veterano.nivel(),veterano.equipo(),veterano.mejorasPorPieza(),3,3,true);
        var enemigos=new RepositorioEnemigosMemoria().obtenerTodos().stream().filter(d->d.planta()==planta && !d.guardian()).toList();
        var m=SimulacionBalance.expediciones(perfil,enemigos,100,org.viajedragos.modelo.Planta.porNumero(planta).oroVictoria());
        System.out.println(m.fila("ECONOMIA P"+planta+" veterano"));
        assertTrue(m.victorias>0); assertEquals(100,m.muestras);
    }

}
