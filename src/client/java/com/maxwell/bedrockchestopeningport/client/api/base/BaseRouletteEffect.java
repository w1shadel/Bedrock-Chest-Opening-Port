package com.maxwell.bedrockchestopeningport.client.api.base;

public abstract class BaseRouletteEffect implements RouletteEffect {
    private final double chancePercent;

    protected BaseRouletteEffect(double chancePercent) {
        if (!Double.isFinite(chancePercent) || chancePercent < 0.0 || chancePercent > 100.0) {
            throw new IllegalArgumentException("Roulette effect chance must be between 0 and 100: " + chancePercent);
        }
        this.chancePercent = chancePercent;
    }

    @Override
    public final double getChancePercent() {
        return chancePercent;
    }
}
