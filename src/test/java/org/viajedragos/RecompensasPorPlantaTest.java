package org.viajedragos;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.consola.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RecompensasPorPlantaTest {
    @ParameterizedTest @CsvSource({"1,5","2,8","3,15","4,24","5,35"})
    void oroCentralizadoPorPlanta(int planta,int oro) {
        assertEquals(oro,Planta.porNumero(planta).oroVictoria());
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> resultados() {
        return java.util.stream.IntStream.rangeClosed(1,5).boxed().flatMap(p->
                java.util.stream.Stream.of("victoria","derrota","simultanea","huida","interrumpido")
                        .map(r->org.junit.jupiter.params.provider.Arguments.of(p,r)));
    }

    @ParameterizedTest @MethodSource("resultados")
    void aventuraPremiaSoloVictoriaDefinitivaYSegunDatosDelEnemigo(int planta,String resultado) {
        var datos=new RepositorioEnemigosMemoria().obtenerTodos().stream().map(d->
                new DefinicionEnemigo("externo_"+d.id(),d.nombre(),d.descripcion(),1,0,1,0,0,1,1,d.nivel(),d.planta(),d.guardian())).toList();
        RepositorioEnemigos repo=()->datos;
        Jugador j=new Jugador("Prueba","",6,2,3,2,100,48,48);
        j.aprenderHabilidad(new Habilidad("Resolución controlada de prueba") {
            public void alInicioCombate(Combatiente p,Combatiente o,EstadoHabilidad e) {
                int actual=datos.stream().filter(d->d.nombre().equals(o.getNombre())).findFirst().orElseThrow().planta();
                if(actual<planta || resultado.equals("victoria")) o.setVida(0);
                else if(resultado.equals("derrota")) p.setVida(0);
                else if(resultado.equals("simultanea")) { p.setVida(0);o.setVida(0); }
            }
        },1);
        String prefijo="";
        if(planta>=2) prefijo+="3\n2\n3\n2\n3\n2\n2\n";
        if(planta>=3) prefijo+="6\n2\n2\n";
        if(planta>=4) prefijo+="3\n3\n3\n2\n2\n";
        if(planta>=5) prefijo+="3\n3\n3\n2\n2\n";
        String eleccion=resultado.equals("huida")?"3\n":resultado.equals("victoria")&&planta==1?"2\n":"";
        String entradas=prefijo+"3\n"+(resultado.equals("interrumpido")?"":eleccion+"0\n");
        Random ninguno=new Random(0) { public int nextInt(int n) { return n-1; } };
        Random encuentros=new Random(0) { int usadas; public int nextInt(int n) { return usadas++%n; } };
        var bytes=new ByteArrayOutputStream(); int[] pausas={0};
        app.jugarAventura(j,new Scanner(entradas),new PrintStream(bytes,true,StandardCharsets.UTF_8),()->4,
                ninguno,repo,encuentros,new RepositorioObjetosMemoria(),null,
                new RitmoConsola(RitmoConsola.Velocidad.NORMAL,ms->pausas[0]++),ninguno,ninguno,new TiempoAventura(()->0),ninguno);
        int previo=new int[]{15,30,38,83,155}[planta-1];
        int premio=new int[]{5,8,15,24,35}[planta-1];
        assertEquals(previo+(resultado.equals("victoria")?premio:0),j.getOro());
        int previas=new int[]{0,3,4,7,10}[planta-1];
        assertEquals(previas+(resultado.equals("victoria")?1:0),j.getExperiencia());
        String texto=TextoConsola.contenido(bytes.toString(StandardCharsets.UTF_8));
        assertEquals(previas+(resultado.equals("victoria")?1:0),texto.split("RECOMPENSAS",-1).length-1);
        if(resultado.equals("victoria")) assertTrue(texto.contains("Oro obtenido: +"+premio+" monedas"));
        TextoConsola.comprobarPaneles(bytes.toString(StandardCharsets.UTF_8));
        assertTrue(j.getInventarioPociones().isEmpty()); assertTrue(j.getInventarioEquipo().isEmpty());
    }
}
