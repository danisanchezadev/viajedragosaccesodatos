package org.viajedragos;

import org.viajedragos.arma.*;
import org.viajedragos.catalogo.*;
import org.viajedragos.combate.*;
import org.viajedragos.habilidad.*;
import org.viajedragos.modelo.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Políticas y mediciones solo de pruebas; ejecuta Combate, no reproduce sus fórmulas. */
final class SimulacionBalance {
    static final String[] BASICO = {"espada_hierro", "armadura_cuero", "casco_cuero", "botas_ligeras"};
    static final String[] PRECRIPTA = {"espada_hierro", "armadura_cuero", "casco_hierro", "guantes_cuero", "botas_marcha", "anillo_vigilancia", "amuleto_vigor"};
    static final String[] MEJORADO = {"espada_acero", "armadura_reforzada", "casco_hierro", "guantes_reforzados", "botas_marcha", "anillo_vigilancia", "amuleto_vigor"};
    static final String[] MIXTO = {"espada_forja", "armadura_acero", "casco_hierro", "guantes_reforzados", "botas_veloces", "anillo_vigilancia", "amuleto_vigor"};
    static final String[] SUPERIOR = {"espada_forja", "armadura_acero", "casco_acero", "guantes_acero", "botas_veloces", "anillo_reflejos", "amuleto_vital"};
    record Perfil(int planta, String nombre, int nivel, String[] equipo, int mejorasPorPieza, int pequenas, int grandes, boolean habilidades) { }

    static List<Perfil> perfiles(int p) {
        return switch (p) {
            case 1 -> List.of(new Perfil(p,"A",1,new String[]{},0,0,0,false),
                    new Perfil(p,"B",1,new String[]{"espada_desgastada","armadura_cuero","casco_cuero"},0,1,0,false),
                    new Perfil(p,"C",2,BASICO,1,2,0,false));
            case 2 -> List.of(new Perfil(p,"A",1,new String[]{"espada_desgastada","armadura_cuero"},0,1,0,false),
                    new Perfil(p,"B",2,BASICO,0,2,1,false), new Perfil(p,"C",3,BASICO,1,3,2,false));
            case 3 -> List.of(new Perfil(p,"A",3,BASICO,0,1,0,false),
                    new Perfil(p,"B",4,PRECRIPTA,0,2,2,false), new Perfil(p,"C",5,PRECRIPTA,2,3,3,false));
            case 4 -> List.of(new Perfil(p,"A",5,PRECRIPTA,0,1,1,false),
                    new Perfil(p,"B",6,MEJORADO,0,2,2,false), new Perfil(p,"C",8,MEJORADO,2,3,3,true));
            case 5 -> List.of(new Perfil(p,"A",7,MEJORADO,0,1,1,false),
                    new Perfil(p,"B",8,MIXTO,0,2,2,false), new Perfil(p,"C",11,SUPERIOR,1,3,3,true));
            default -> throw new IllegalArgumentException();
        };
    }

    static final class Medido extends Jugador {
        int turnos, danio, curacion, pequenas, grandes;
        Medido() { super("Prueba", "", 6, 2, 3, 2, 2, 48, 48); }
        @Override public void recibirDanio(int n) { int antes=getVida(); super.recibirDanio(n); danio+=antes-getVida(); }
        @Override public void curarse(int n) { int antes=getVida(); super.curarse(n); curacion+=getVida()-antes; }
        @Override public int usarPocion(TipoPocion tipo) {
            int antes=getCantidadPociones(tipo);
            int cantidad=super.usarPocion(tipo);
            assertEquals(antes-(cantidad>0?1:0),getCantidadPociones(tipo));
            if (cantidad>0) { if(tipo==TipoPocion.PEQUENA) pequenas++; else grandes++; }
            return cantidad;
        }
    }

    static Medido crear(Perfil p) {
        var j = new Medido();
        int exp=0; for(int n=2;n<=p.nivel();n++) exp+=n*10;
        if(exp>0) j.agregarExperiencia(exp);
        repartir(j);
        var objetos=new RepositorioObjetosMemoria();
        var definiciones=new ArrayList<>(objetos.obtenerEquipamiento());
        objetos.obtenerArmas().forEach(a->definiciones.add(a.comoEquipo()));
        for(String id:p.equipo()) {
            var d=definiciones.stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();
            var pieza=d.crearEquipo(p.mejorasPorPieza()); j.agregarEquipo(pieza);
            // Solo tres piezas reciben mejoras, no un conjunto perfecto artificial.
            if(p.mejorasPorPieza()>0 && Set.of(TipoEquipo.ARMA,TipoEquipo.ARMADURA,TipoEquipo.BOTAS).contains(d.tipo())) {
                Atributo a=d.tipo()==TipoEquipo.ARMA?Atributo.FUERZA:d.tipo()==TipoEquipo.ARMADURA?Atributo.RESISTENCIA:Atributo.AGILIDAD;
                for(int n=0;n<p.mejorasPorPieza();n++) { j.agregarPergamino(new Pergamino(a)); assertTrue(j.usarPergamino(a,pieza)); }
            }
            assertTrue(j.equiparDesdeInventario(d.tipo(),0));
        }
        j.setVida(j.getVidaMaxima()); // Solo condición inicial, jamás entre encuentros.
        for(int n=0;n<p.pequenas();n++) j.agregarPocion(TipoPocion.PEQUENA);
        for(int n=0;n<p.grandes();n++) j.agregarPocion(TipoPocion.GRANDE);
        if(p.habilidades()) for(var h:List.of(HabilidadDisponible.INSTINTO_SUPERVIVENCIA,HabilidadDisponible.SED_DE_BATALLA)) {
            j.agregarPergaminoHabilidad(new PergaminoHabilidad(h)); assertTrue(j.usarPergaminoHabilidad(h));
        }
        j.aprenderHabilidad(new Habilidad("Observador sin efecto") {
            @Override public void alInicioTurno(Combatiente propio,Combatiente otro,EstadoHabilidad e) { j.turnos++; }
        },1);
        return j;
    }

    static void repartir(Jugador j) {
        while(j.getPuntosPendientes()>0) assertTrue(j.asignarPunto(j.getPuntosPendientes()%2==0?Atributo.FUERZA:Atributo.RESISTENCIA));
    }

    // Un bloque de entrada por decisión real. Scanner no recibe órdenes de turnos futuros.
    static ResultadoCombate combatir(Medido j, DefinicionEnemigo d, long semilla) {
        Random dados=new Random(semilla); int[] tiradas={0};
        var entrada=new Scanner((Readable) buffer->{
            String accion="1\n";
            if(j.getVida()*2<=j.getVidaMaxima()) {
                if(j.getCantidadPociones(TipoPocion.GRANDE)>0) accion="2\n2\n";
                else if(j.getCantidadPociones(TipoPocion.PEQUENA)>0) accion="2\n1\n";
            }
            buffer.put(accion); return accion.length();
        });
        try(entrada; var salida=new PrintStream(OutputStream.nullOutputStream())) {
            ResultadoCombate resultado=new Combate(j,d.crearEnemigo(),()->{
                assertTrue(++tiradas[0]<10000,"No debe haber combates interminables");
                return dados.nextInt(6)+1;
            }).combatir(entrada,salida);
            j.getHabilidades().forEach((habilidad,estado)->{
                assertFalse(estado.isEfectoTemporalActivo(),"Limpieza final de "+habilidad.getNombre());
                assertEquals(1,estado.getNivel(),"El combate no altera los niveles aprendidos");
            });
            return resultado;
        }
    }

    static final class Metricas {
        int muestras, victorias, derrotas, simultaneas, combates, supervivientes, nivelFinal;
        long vidaVencedores, vidaFinal, turnos, danio, curacion, pequenas, grandes, oro, exp, gastos, saldo;
        void combate(ResultadoCombate r, Medido j) {
            combates++;
            if(r==ResultadoCombate.ENEMIGO_MUERE) { victorias++; vidaVencedores+=j.getVida(); }
            else if(r==ResultadoCombate.AMBOS_MUEREN) simultaneas++;
            else { assertEquals(ResultadoCombate.JUGADOR_MUERE,r); derrotas++; }
        }
        void cerrar(Medido j) {
            muestras++; if(j.estaVivo()) supervivientes++;
            vidaFinal+=j.getVida(); turnos+=j.turnos; danio+=j.danio; curacion+=j.curacion;
            pequenas+=j.pequenas; grandes+=j.grandes; nivelFinal+=j.getNivel();
            saldo+=j.getOro();
            assertTrue(j.getVida()>=0 && j.getVida()<=j.getVidaMaxima());
        }
        String fila(String etiqueta) {
            return String.format(Locale.ROOT,"%s | %d | %d | %d | %d | %.1f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f",
                    etiqueta,muestras,victorias,derrotas,simultaneas,100.0*supervivientes/muestras,
                    victorias==0?0:vidaVencedores/(double)victorias,turnos/(double)combates,danio/(double)combates,
                    pequenas/(double)muestras,grandes/(double)muestras,oro/(double)muestras,exp/(double)muestras,
                    combates/(double)muestras,vidaFinal/(double)muestras,nivelFinal/(double)muestras,gastos/(double)muestras,saldo/(double)muestras,curacion/(double)muestras);
        }
    }

    static Metricas aislados(Perfil p,List<DefinicionEnemigo> enemigos,int muestrasPorEnemigo) {
        return aislados(p,enemigos,muestrasPorEnemigo,"");
    }

    static Metricas aislados(Perfil p,List<DefinicionEnemigo> enemigos,int muestrasPorEnemigo,String etiqueta) {
        var m=new Metricas();
        for(var d:enemigos) {
            var parcial=new Metricas();
            for(int n=0;n<muestrasPorEnemigo;n++) {
                var j=crear(p); var r=combatir(j,d,20261021L+p.planta()*100000L+enemigos.indexOf(d)*1000L+n);
                m.combate(r,j); m.cerrar(j); parcial.combate(r,j); parcial.cerrar(j);
            }
            if(p.nombre().equals("B") && !etiqueta.isEmpty()) System.out.println(parcial.fila("DETALLE "+etiqueta+" "+d.id()));
        }
        assertEquals(m.muestras,m.victorias+m.derrotas+m.simultaneas); return m;
    }

    static Metricas expediciones(Perfil p,List<DefinicionEnemigo> enemigos,int muestras,int oroVictoria) {
        var m=new Metricas(); var objetos=new RepositorioObjetosMemoria();
        var tienda=new RepositorioTiendaMemoria(objetos).paraPlanta(p.planta());
        var botin=new BotinEquipamiento(objetos.obtenerBotinEquipamiento());
        var normales=enemigos.stream().filter(e->!e.guardian()).toList();
        for(int n=0;n<muestras;n++) {
            long oroAntes=m.oro,gastosAntes=m.gastos;
            var j=crear(p); var azar=new Random(20261022L+p.planta()*10000L+n);
            var pociones=new Random(20261023L+p.planta()*10000L+n);
            var equipo=new Random(20261024L+p.planta()*10000L+n);
            var pergaminos=new Random(20261025L+p.planta()*10000L+n);
            var habilidades=new Random(20261026L+p.planta()*10000L+n);
            for(int encuentro=0;encuentro<4 && j.estaVivo();encuentro++) {
                // Planta 2: tres normales y después el guardián; las otras, cuatro normales.
                var d=p.planta()==2 && encuentro==3 && enemigos.stream().anyMatch(DefinicionEnemigo::guardian)
                        ? enemigos.stream().filter(DefinicionEnemigo::guardian).findFirst().orElseThrow()
                        : normales.get(azar.nextInt(normales.size()));
                var r=combatir(j,d,azar.nextLong()); m.combate(r,j);
                if(r!=ResultadoCombate.ENEMIGO_MUERE) break;
                j.agregarOro(oroVictoria); j.agregarExperiencia(d.experiencia()); repartir(j);
                m.oro+=oroVictoria; m.exp+=d.experiencia();
                for(TipoPocion t:TipoPocion.values()) if(pociones.nextInt(100)<(t==TipoPocion.PEQUENA?50:20)) j.agregarPocion(t);
                botin.sortear(d.planta(),d.nivel(),equipo).ifPresent(j::agregarEquipo);
                if(pergaminos.nextInt(100)<10) j.agregarPergamino(new Pergamino(Atributo.values()[pergaminos.nextInt(Atributo.values().length)]));
                if(habilidades.nextInt(100)<5) j.agregarPergaminoHabilidad(new PergaminoHabilidad(HabilidadDisponible.values()[habilidades.nextInt(HabilidadDisponible.values().length)]));
                if(habilidades.nextInt(100)<2) j.agregarPergaminoMaestria();
                // Compras reales con saldo disponible, después curación real; nada se repone gratis.
                if(encuentro<3) {
                    for(var tipo:List.of(TipoPocion.GRANDE,TipoPocion.PEQUENA)) {
                        int objetivo=tipo==TipoPocion.GRANDE?p.grandes():p.pequenas();
                        String id=tipo==TipoPocion.GRANDE?"pocion_grande":"pocion_pequena";
                        while(j.getCantidadPociones(tipo)<objetivo) {
                            int antes=j.getOro();
                            if(j.comprar(id,tienda)!=ResultadoCompra.COMPRADA) break;
                            m.gastos+=antes-j.getOro();
                        }
                    }
                    while(j.getVidaMaxima()-j.getVida()>=16 && j.getCantidadPociones(TipoPocion.GRANDE)>0) assertTrue(j.usarPocion(TipoPocion.GRANDE)>0);
                    while(j.getVidaMaxima()-j.getVida()>=8 && j.getCantidadPociones(TipoPocion.PEQUENA)>0) assertTrue(j.usarPocion(TipoPocion.PEQUENA)>0);
                }
            }
            assertEquals(15+m.oro-oroAntes-(m.gastos-gastosAntes),j.getOro(),"Solo victoria y compras modifican el saldo");
            m.cerrar(j);
        }
        return m;
    }
}
