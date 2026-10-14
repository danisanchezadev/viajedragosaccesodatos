package org.viajedragos.combate;

import org.viajedragos.excepciones.DadoInvalidoException;
import org.viajedragos.modelo.Combatiente;

public class ContextoAtaque {

    private final Combatiente atacante;
    private final Combatiente defensor;

    private int dadoAtaque;
    private int dadoDefensa;

    private int danioBruto;
    private int danioFinal;

    private boolean ataqueFallido;
    private boolean defensaFallida;
    private boolean contraataque;

    public ContextoAtaque(Combatiente atacante, Combatiente defensor) {
        this.atacante = atacante;
        this.defensor = defensor;
    }

    public Combatiente getAtacante() {
        return atacante;
    }

    public Combatiente getDefensor() {
        return defensor;
    }

    public int getDadoAtaque() {
        return dadoAtaque;
    }

    public void setDadoAtaque(int dadoAtaque) {

            if(dadoAtaque <1 || dadoAtaque > 6){
                throw new DadoInvalidoException(
                        "Tienes que introducir un valor de dado válido");
            }

            this.dadoAtaque = dadoAtaque;
    }

    public int getDadoDefensa() {
        return dadoDefensa;
    }

    public void setDadoDefensa(int dadoDefensa) {

        if(dadoDefensa <1 || dadoDefensa > 6){
            throw new DadoInvalidoException(
                    "Tienes que introducir un valor de dado válido");
        }

        this.dadoDefensa = dadoDefensa;
    }

    public int getDanioBruto() {
        return danioBruto;
    }

    public void setDanioBruto(int danioBruto) {

        this.danioBruto = Math.max(danioBruto, 0);
    }

    public int getDanioFinal() {
        return danioFinal;
    }

    public void setDanioFinal(int danioFinal) {

        this.danioFinal = Math.max(danioFinal, 0);
    }

    public boolean isAtaqueFallido() {
        return ataqueFallido;
    }

    public void setAtaqueFallido(boolean ataqueFallido) {
        this.ataqueFallido = ataqueFallido;
    }

    public boolean isDefensaFallida() {
        return defensaFallida;
    }

    public void setDefensaFallida(boolean defensaFallida) {
        this.defensaFallida = defensaFallida;
    }

    public boolean isContraataque() {

        return contraataque;
    }

    public void setContraataque(boolean contraataque) {

        this.contraataque = contraataque;
    }
}