package org.viajedragos.combate;

import java.util.Random;

public class Dado implements TiradorDado{

    private Random random = new Random();

    public int tirarDado(){

        return random.nextInt(1,7);

    }
}
