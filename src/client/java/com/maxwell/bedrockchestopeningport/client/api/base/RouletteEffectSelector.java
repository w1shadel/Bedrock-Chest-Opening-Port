package com.maxwell.bedrockchestopeningport.client.api.base;

import com.maxwell.bedrockchestopeningport.client.CustomDropAnimationGui;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public final class RouletteEffectSelector {
    private RouletteEffectSelector() {
    }

    public static RouletteEffect select(
            List<? extends RouletteEffect> candidates,
            RouletteEffect fallback,
            CustomDropAnimationGui gui
    ) {
        if (fallback == null) throw new IllegalArgumentException("Fallback effect must not be null");
        double declaredTotal = 0.0;
        for (RouletteEffect effect : candidates) {
            if (effect == null) throw new IllegalArgumentException("Effect list must not contain null");
            double chance = effect.getChancePercent();
            if (!Double.isFinite(chance) || chance < 0.0 || chance > 100.0) {
                throw new IllegalArgumentException("Invalid chance for " + effect.getClass().getName() + ": " + chance);
            }
            declaredTotal += chance;
        }
        if (declaredTotal > 100.0 + 1.0e-9) {
            throw new IllegalArgumentException("Roulette effect chances total " + declaredTotal + "%, exceeding 100%");
        }

        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        double boundary = 0.0;
        for (RouletteEffect effect : candidates) {
            boundary += effect.getChancePercent();
            if (roll < boundary) {

                return effect.canApply(gui) ? effect : fallback;
            }
        }
        return fallback;
    }
}
